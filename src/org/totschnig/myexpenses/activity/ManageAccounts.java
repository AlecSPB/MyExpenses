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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.io.Serializable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.AggregatesDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import android.support.v4.app.FragmentManager;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * allows to manage accounts
 * @author Michael Totschnig
 *
 */
public class ManageAccounts extends LaunchActivity implements
    OnItemClickListener,ContribIFace, TaskExecutionFragment.TaskCallbacks {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    mSettings = MyApplication.getInstance().getSettings();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_accounts);
    setTitle(R.string.pref_manage_accounts_title);
    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    newVersionCheck();
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.accounts, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }
  
  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    Intent i = new Intent(this, MyExpenses.class);
    i.putExtra(KEY_ROWID, id);
    //i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivityForResult(i,0);
  }
  public boolean dispatchCommand(int command, Object tag) {
    Intent i;
    switch(command) {
    //in super home executes finish(), which is not what we want here
    case android.R.id.home:
      return true;
    case R.id.AGGREGATES_COMMAND:
      //TODO Strict mode violation
      Cursor c = getContentResolver().query(TransactionProvider.AGGREGATES_URI.buildUpon().appendPath("count").build(),
          null, null, null, null);
      if (c.getCount()>0 ) {
        if (MyApplication.getInstance().isContribEnabled) {
          contribFeatureCalled(Feature.AGGREGATE, null);
        } else {
          CommonCommands.showContribDialog(this,Feature.AGGREGATE, null);
        }
      } else {
        MessageDialogFragment.newInstance(
            R.string.dialog_title_menu_command_disabled,
            "This command is only enabled if for any currency there exist at least two accounts.", //will not localize since AGGREGATES_COMMAND will be removed in 1.11
            MessageDialogFragment.Button.okButton(),
            null,null)
         .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      c.close();
      return true;
    case R.id.DELETE_COMMAND_DO:
      FragmentManager fm = getSupportFragmentManager();
      fm.beginTransaction()
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_DELETE_ACCOUNT,(Long)tag, null), "ASYNC_TASK")
        .commit();
      return true;
    case R.id.RESET_ACCOUNT_ALL_COMMAND:
      if (Transaction.countAll() > 0 ) {
        if (MyApplication.getInstance().isContribEnabled) {
          contribFeatureCalled(Feature.RESET_ALL, null);
        } else {
          CommonCommands.showContribDialog(this,Feature.RESET_ALL, null);
        }
      } else {
        MessageDialogFragment.newInstance(
            R.string.dialog_title_menu_command_disabled,
            R.string.dialog_command_disabled_reset_account,
            MessageDialogFragment.Button.okButton(),
            null,null)
         .show(getSupportFragmentManager(),"BUTTON_DISABLED_INFO");
      }
      return true;
    }
    return super.dispatchCommand(command, tag);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    if (Account.count(null, null) > 1)
      menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
    if (Transaction.countPerAccount(info.id) > 0)
       menu.add(0,R.id.RESET_ACCOUNT_COMMAND,0,R.string.menu_reset);
    menu.add(0,R.id.EDIT_ACCOUNT_COMMAND,0,R.string.menu_edit);
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case R.id.DELETE_COMMAND:
      MessageDialogFragment.newInstance(
          R.string.dialog_title_warning_delete_account,
          R.string.warning_delete_account,
          new MessageDialogFragment.Button(android.R.string.yes, R.id.DELETE_COMMAND_DO, info.id),
          null,
          MessageDialogFragment.Button.noButton())
        .show(getSupportFragmentManager(),"DELETE_ACCOUNT");
      return true;
    case R.id.RESET_ACCOUNT_COMMAND:
      DialogUtils.showWarningResetDialog(this, info.id);
      return true;
    case R.id.EDIT_ACCOUNT_COMMAND:
      Intent i = new Intent(this, AccountEdit.class);
      i.putExtra(KEY_ROWID, info.id);
      startActivityForResult(i, 0);
      return true;
    }
    return super.onContextItemSelected(item);
  }
  @SuppressWarnings("incomplete-switch")
  @Override
  public void contribFeatureCalled(Feature feature, Serializable tag) {
    switch (feature) {
    case AGGREGATE:
      feature.recordUsage();
      showAggregatesDialog();
      break;
    case RESET_ALL:
      DialogUtils.showWarningResetDialog(this, null);
      break;
    }
  }
  private void showAggregatesDialog() {
    new AggregatesDialogFragment().show(getSupportFragmentManager(),"AGGREGATES");
  }
  @Override
  public void contribFeatureNotCalled() {
  }
}
