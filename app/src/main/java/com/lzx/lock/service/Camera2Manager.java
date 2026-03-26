package com.lzx.lock.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hintergrund-Kamera-Manager für automatische Fotoaufnahme bei Fehleingabe.
 * Arbeitet ohne sichtbare Vorschau (headless).
 */
public class Camera2Manager {

    private static final String TAG = "Camera2Manager";
    /** Wartezeit in Millisekunden damit Belichtung, Weißabgleich und Fokus konvergieren. */
    private static final long CAMERA_WARMUP_DELAY_MS = 1200;

    private final Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraSession;
    private ImageReader mImageReader;
    private SurfaceTexture mDummySurfaceTexture;
    private Surface mDummySurface;

    /** Verhindert gleichzeitige Fotoaufnahmen. */
    private final AtomicBoolean mCapturing = new AtomicBoolean(false);

    public Camera2Manager(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Nimmt ein Foto mit der Frontkamera auf und speichert es im app-privaten Verzeichnis.
     * Funktioniert im Hintergrund ohne sichtbare Vorschau.
     */
    @SuppressLint("NewApi")
    public void capturePhoto() {
        if (!mCapturing.compareAndSet(false, true)) {
            Log.d(TAG, "Fotoaufnahme läuft bereits, Anfrage ignoriert");
            return;
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Keine Kamera-Berechtigung");
            mCapturing.set(false);
            return;
        }

        mHandlerThread = new HandlerThread("Camera2Background");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        try {
            CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = findFrontCameraId(manager);
            if (cameraId == null) {
                Log.w(TAG, "Keine Frontkamera gefunden");
                mCapturing.set(false);
                cleanup();
                return;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizeByArea());

            mImageReader = ImageReader.newInstance(
                    largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mHandler.post(new ImageSaver(reader.acquireNextImage()));
                }
            }, mHandler);

            // Unsichtbare Dummy-Surface für die Pflicht-Vorschau der Camera2-API
            mDummySurfaceTexture = new SurfaceTexture(1);
            mDummySurfaceTexture.setDefaultBufferSize(640, 480);
            mDummySurface = new Surface(mDummySurfaceTexture);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @SuppressLint("NewApi")
                @Override
                public void onOpened(CameraDevice camera) {
                    mCameraDevice = camera;
                    try {
                        CaptureRequest.Builder previewBuilder =
                                camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        previewBuilder.addTarget(mDummySurface);

                        camera.createCaptureSession(
                                Arrays.asList(mDummySurface, mImageReader.getSurface()),
                                new CameraCaptureSession.StateCallback() {
                                    @SuppressLint("NewApi")
                                    @Override
                                    public void onConfigured(CameraCaptureSession session) {
                                        mCameraSession = session;
                                        try {
                                            // Starte Vorschau-Loop damit der Sensor Belichtung,
                                            // Weißabgleich und Fokus einstellen kann
                                            CaptureRequest.Builder warmupBuilder =
                                                    mCameraDevice.createCaptureRequest(
                                                            CameraDevice.TEMPLATE_PREVIEW);
                                            warmupBuilder.addTarget(mDummySurface);
                                            warmupBuilder.set(CaptureRequest.CONTROL_MODE,
                                                    CaptureRequest.CONTROL_MODE_AUTO);
                                            warmupBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                                    CaptureRequest.CONTROL_AE_MODE_ON);
                                            warmupBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                                                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
                                            warmupBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                            session.setRepeatingRequest(
                                                    warmupBuilder.build(), null, mHandler);
                                            // Warte bis Belichtung und Weißabgleich konvergiert sind
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (mCameraSession != null) {
                                                        takeSinglePhoto();
                                                    }
                                                }
                                            }, CAMERA_WARMUP_DELAY_MS);
                                        } catch (CameraAccessException e) {
                                            Log.e(TAG, "Fehler beim Starten der Vorschau", e);
                                            cleanup();
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(CameraCaptureSession session) {
                                        Log.e(TAG, "Session-Konfiguration fehlgeschlagen");
                                        cleanup();
                                    }
                                }, mHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Fehler beim Erstellen der Session", e);
                        cleanup();
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    cleanup();
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    Log.e(TAG, "Kamera-Fehler: " + error);
                    cleanup();
                }

                // onClosed() is available from API 23; @SuppressLint suppresses the lint warning
                // (consistent with the rest of this class).
                @SuppressLint("NewApi")
                @Override
                public void onClosed(CameraDevice camera) {
                    // Handler-Thread erst nach dem Schließen der Kamera beenden,
                    // damit der onClosed-Callback noch zugestellt werden kann.
                    if (mHandlerThread != null) {
                        mHandlerThread.quitSafely();
                        mHandlerThread = null;
                    }
                }
            }, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Fehler beim Öffnen der Kamera", e);
            mCapturing.set(false);
            cleanup();
        }
    }

    @SuppressLint("NewApi")
    private void takeSinglePhoto() {
        try {
            // Beende den Vorschau-Loop bevor das Standbild aufgenommen wird
            mCameraSession.stopRepeating();

            CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);

            mCameraSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                        CaptureRequest request, TotalCaptureResult result) {
                    // Cleanup erfolgt in ImageSaver nach dem Speichern des Fotos
                }
            }, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Fehler bei der Fotoaufnahme", e);
            mCapturing.set(false);
            cleanup();
        }
    }

    /**
     * Ermittelt die Kamera-ID der Frontkamera.
     */
    @SuppressLint("NewApi")
    private String findFrontCameraId(CameraManager manager) throws CameraAccessException {
        for (String id : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return id;
            }
        }
        return null;
    }

    @SuppressLint("NewApi")
    private void cleanup() {
        mCapturing.set(false);
        try {
            if (mCameraSession != null) {
                mCameraSession.close();
                mCameraSession = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Schließen der Session", e);
        }
        // Track whether we actually closed a live camera device.
        // If so, the onClosed callback (delivered via mHandler) will quit the handler thread.
        // If not (early exit before the camera was opened, or close() threw an exception where
        // onClosed may not be delivered), quit the thread here directly as a fallback.
        boolean cameraBeingClosed = false;
        if (mCameraDevice != null) {
            try {
                mCameraDevice.close();
                // close() succeeded: onClosed will be delivered and will quit the thread.
                cameraBeingClosed = true;
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Schließen der Kamera", e);
                // close() failed – onClosed may not fire, so quit the thread here.
            }
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        if (mDummySurface != null) {
            mDummySurface.release();
            mDummySurface = null;
        }
        if (mDummySurfaceTexture != null) {
            mDummySurfaceTexture.release();
            mDummySurfaceTexture = null;
        }
        if (!cameraBeingClosed && mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
    }

    private class ImageSaver implements Runnable {
        private final Image mImage;

        ImageSaver(Image image) {
            mImage = image;
        }

        @SuppressLint("NewApi")
        @Override
        public void run() {
            // App-privates Verzeichnis: kein WRITE_EXTERNAL_STORAGE benötigt
            File dir = mContext.getExternalFilesDir("IntruderPhotos");
            if (dir == null) {
                dir = new File(mContext.getFilesDir(), "IntruderPhotos");
            }
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, System.currentTimeMillis() + ".jpg");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                try {
                    ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                    int bufferSize = buffer.remaining();
                    Log.d(TAG, "Buffer-Größe: " + bufferSize + " Bytes");
                    if (bufferSize == 0) {
                        Log.e(TAG, "Buffer ist leer – keine Bilddaten!");
                    }
                    byte[] bytes = new byte[bufferSize];
                    buffer.get(bytes);
                    fos.write(bytes);
                    Log.d(TAG, "Eindringlingsfoto gespeichert: " + file.getAbsolutePath());
                } finally {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Fehler beim Speichern des Fotos", e);
            } finally {
                mImage.close();
                // Kamera-Ressourcen freigeben, nachdem das Foto gespeichert wurde
                cleanup();
            }
        }
    }

    public static class CompareSizeByArea implements java.util.Comparator<Size> {
        @SuppressLint("NewApi")
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight()
                    - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
