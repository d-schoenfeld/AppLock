package com.lzx.lock.mvp.contract;

import com.lzx.lock.base.BasePresenter;
import com.lzx.lock.base.BaseView;
import com.lzx.lock.bean.LockStage;
import com.lzx.lock.widget.LockPatternView;

import java.util.List;

/**
 * Created by xian on 2017/2/17.
 */

public interface LockCreateContract {

    interface View extends BaseView<MainContract.Presenter> {
        void updateUiStage(LockStage stage); //UI-Status aktualisieren

        void updateChosenPattern(List<LockPatternView.Cell> mChosenPattern); //Passwort aktualisieren

        void updateLockTip(String text,boolean isToast); //Entsperrhinweis aktualisieren

        void setHeaderMessage(int headerMessage);

        void lockPatternViewConfiguration(boolean patternEnabled, LockPatternView.DisplayMode displayMode);  //Widget-Konfiguration

        void Introduction(); //Widget-Status (Anfang)

        void HelpScreen(); //Hilfe (nach wie vielen Fehlern die Hilfsanimation startet)

        void ChoiceTooShort(); //Entsperrmuster zu kurz

        void moveToStatusTwo(); //Zu Schritt 2 wechseln

        void clearPattern(); //Widget-Status zurücksetzen

        void ConfirmWrong(); //Die zwei Muster stimmen nicht überein

        void ChoiceConfirmed(); //Muster zweimal erfolgreich gezeichnet
    }

    interface Presenter extends BasePresenter {

        void updateStage(LockStage stage);

        void onPatternDetected(List<LockPatternView.Cell> pattern, List<LockPatternView.Cell> mChosenPattern, LockStage mUiStage);

        void onDestroy();
    }
}
