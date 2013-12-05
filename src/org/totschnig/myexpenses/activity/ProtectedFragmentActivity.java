/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.activity;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class ProtectedFragmentActivity extends SherlockFragmentActivity
    implements MessageDialogListener, OnSharedPreferenceChangeListener,
    TaskExecutionFragment.TaskCallbacks{
  public static final int ACTIVITY_EDIT=1;
  public static final int ACTIVITY_EDIT_ACCOUNT=2;
  public static final int ACTIVITY_EXPORT=3;
  public static final int ACTIVITY_PREFERENCES=4;
  private AlertDialog pwDialog;
  private ProtectionDelegate protection;
  private boolean scheduledRestart = false;
  public Enum<?> helpVariant = null;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
/*    if (MyApplication.debug) {
      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
      .detectDiskReads()
      .detectDiskWrites()
      .detectNetwork()   // or .detectAll() for all detectable problems
      .penaltyLog()
      .build());
      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
      .detectLeakedSqlLiteObjects()
      //.detectLeakedClosableObjects()
      .penaltyLog()
      .penaltyDeath()
      .build());
    }*/

    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getSettings().registerOnSharedPreferenceChangeListener(this);
    protection = new ProtectionDelegate(this);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

  }
  @Override
  protected void onPause() {
    super.onPause();
    protection.handleOnPause(pwDialog);
  }
  @Override
  protected void onDestroy() {
    super.onDestroy();
    MyApplication.getInstance().getSettings().unregisterOnSharedPreferenceChangeListener(this);
    protection.handleOnDestroy();
  }
  @Override
  protected void onResume() {
    super.onResume();
    if(scheduledRestart) {
      scheduledRestart = false;
      if (android.os.Build.VERSION.SDK_INT>=11)
        recreate();
      else {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
      }
    } else {
      pwDialog = protection.hanldeOnResume(pwDialog);
    }
  }
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals(MyApplication.PREFKEY_UI_THEME_KEY) ||
        key.equals(MyApplication.PREFKEY_UI_LANGUAGE) ||
        key.equals(MyApplication.PREFKEY_UI_FONTSIZE)) {
      scheduledRestart = true;
    }
  }

  public void cancelDialog() {
    // TODO Auto-generated method stub
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.common, menu);
    return true;
  }
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
      if (dispatchCommand(item.getItemId(),null))
        return true;
      return super.onMenuItemSelected(featureId, item);
  }
  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (CommonCommands.dispatchCommand(this, command))
      return true;
    return false;
  }
  @Override
  public void onPreExecute() {
    // TODO Auto-generated method stub
    
  }
  @Override
  public void onProgressUpdate(int percent) {
    // TODO Auto-generated method stub
    
  }
  @Override
  public void onCancelled() {
    // TODO Auto-generated method stub
    
  }
  @Override
  public void onPostExecute(int taskId, Object o) {
    FragmentManager m = getSupportFragmentManager();
    FragmentTransaction t = m.beginTransaction();
    ProgressDialogFragment f = ((ProgressDialogFragment) m.findFragmentByTag("PROGRESS"));
    if (f!=null)
      t.remove(f);
    t.remove(m.findFragmentByTag("ASYNC_TASK"));
    t.commitAllowingStateLoss();
  }
}
