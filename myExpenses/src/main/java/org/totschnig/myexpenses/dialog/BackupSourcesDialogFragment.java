package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.MyApplication.PrefKey;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RadioButton;

public class BackupSourcesDialogFragment extends ImportSourceDialogFragment implements
DialogInterface.OnClickListener {
  RadioGroup mRestorePlanStrategie;
  
  public static final BackupSourcesDialogFragment newInstance() {
    return new BackupSourcesDialogFragment();
  }
  @Override
  protected int getLayoutId() {
    return R.layout.backup_restore_dialog;
  }
  @Override
  protected void setupDialogView(View view) {
    super.setupDialogView(view);
    mRestorePlanStrategie = (RadioGroup) view.findViewById(R.id.restore_calendar_handling);
    mRestorePlanStrategie.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        setButtonState();
      }
    });
    String calendarId = PrefKey.PLANNER_CALENDAR_ID.getString("-1");
    String calendarPath = PrefKey.PLANNER_CALENDAR_PATH.getString("");
    RadioButton configured = (RadioButton) view.findViewById(R.id.restore_calendar_handling_configured);
    if ((calendarId.equals("-1")) || calendarPath.equals("")) {
      configured.setEnabled(false);
    } else {
      configured.setText(configured.getText() + " (" + calendarPath + ")");
    }
  }
  @Override
  protected int getLayoutTitle() {
    return R.string.pref_restore_title;
  }

  @Override
  String getTypeName() {
    return "Zip";
  }
  @Override
  String getPrefKey() {
    return "backup_restore_file_uri";
  }
  @Override
  protected String getMimeType() {
    return "application/zip";
  }
  @Override
  protected boolean checkTypeParts(String[] typeParts) {
    return typeParts[0].equals("application") && 
    typeParts[1].equals("zip");
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (getActivity()==null) {
      return;
    }
    if (id == AlertDialog.BUTTON_POSITIVE) {
      SharedPreferencesCompat.apply(
        MyApplication.getInstance().getSettings().edit()
        .putString(getPrefKey(), mUri.toString()));
      ((BackupRestoreActivity) getActivity()).onSourceSelected(
          mUri,
          mRestorePlanStrategie.getCheckedRadioButtonId());
    } else {
      super.onClick(dialog, id);
    }
  }
  @Override
  protected boolean isReady() {
    if (super.isReady()) {
      return mRestorePlanStrategie.getCheckedRadioButtonId() != -1;
    } else {
      return false;
    }
  }
}
