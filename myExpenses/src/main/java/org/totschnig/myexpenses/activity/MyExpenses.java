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

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.BalanceDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.dialog.ExportDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionDetailFragment;
import org.totschnig.myexpenses.fragment.ContextualActionBarFragment;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountGrouping;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.SortDirection;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.CommentCriteria;
import org.totschnig.myexpenses.provider.filter.Criteria;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.CursorFragmentPagerAdapter;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.ui.ProtectedCursorLoader;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.FileUtils;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.ShareUtils;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.ads.AdHandler;
import org.totschnig.myexpenses.util.ads.AdHandlerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.Locale;

import javax.inject.Inject;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_CLEARED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_EXPORTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_FUTURE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_EXPORT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_PRINT;

/**
 * This is the main activity where all expenses are listed
 * From the menu subactivities (Insert, Reset, SelectAccount, Help, Settings)
 * are called
 */
public class MyExpenses extends LaunchActivity implements
    OnPageChangeListener, LoaderManager.LoaderCallbacks<Cursor>,
    ConfirmationDialogFragment.ConfirmationDialogCheckedListener,
    ConfirmationDialogListener, ContribIFace, SimpleDialog.OnDialogResultListener {

  public static final long TRESHOLD_REMIND_RATE = 47L;

  public static final int ACCOUNTS_CURSOR = -1;
  public static final String KEY_SEQUENCE_COUNT = "sequenceCount";

  private LoaderManager mManager;

  int mCurrentPosition = -1;
  private Cursor mAccountsCursor;

  private MyViewPagerAdapter mViewPagerAdapter;
  private StickyListHeadersAdapter mDrawerListAdapter;
  private ViewPager myPager;
  private long mAccountId = 0;
  private int mAccountCount = 0;

  @Inject
  protected AdHandlerFactory adHandlerFactory;
  private AdHandler adHandler;
  private Toolbar mToolbar;
  private String mCurrentBalance;
  private SubMenu sortMenu;

  public enum HelpVariant {
    crStatus
  }

  private void setHelpVariant() {
    Account account = Account.getInstanceFromDb(mAccountId);
    helpVariant = account == null || account.getType().equals(AccountType.CASH) ?
        null : HelpVariant.crStatus;
  }

  /**
   * stores the number of transactions that have been
   * created in the db, updated after each creation of
   * a new transaction
   */
  private long sequenceCount = 0;
  private int colorAggregate;
  private StickyListHeadersListView mDrawerList;
  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;

  private int columnIndexRowId, columnIndexColor, columnIndexCurrency, columnIndexDescription, columnIndexLabel;
  boolean indexesCalculated = false;
  private long idFromNotification = 0;
  private String mExportFormat = null;
  private AccountGrouping mAccountGrouping;

  @Inject
  CurrencyFormatter currencyFormatter;

  @Override
  protected void injectDependencies() {
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    Resources.Theme theme = getTheme();
    TypedValue value = new TypedValue();
    theme.resolveAttribute(R.attr.colorAggregate, value, true);
    colorAggregate = value.data;

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    adHandler = adHandlerFactory.create(findViewById(R.id.adContainer));
    adHandler.init();
    adHandler.maybeRequestNewInterstitial();

    mDrawerLayout = findViewById(R.id.drawer_layout);
    mDrawerList = findViewById(R.id.left_drawer);
    mToolbar = setupToolbar(false);
    mToolbar.addView(getLayoutInflater().inflate(R.layout.custom_title, mToolbar, false));
    if (mDrawerLayout != null) {
      mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
          mToolbar, R.string.drawer_open, R.string.drawer_close) {

        /**
         * Called when a drawer has settled in a completely closed state.
         */
        public void onDrawerClosed(View view) {
          super.onDrawerClosed(view);
          TransactionList tl = getCurrentFragment();
          if (tl != null)
            tl.onDrawerClosed();
          //ActivityCompat.invalidateOptionsMenu(MyExpenses.this); // creates call to onPrepareOptionsMenu()
        }

        /**
         * Called when a drawer has settled in a completely open state.
         */
        public void onDrawerOpened(View drawerView) {
          super.onDrawerOpened(drawerView);
          TransactionList tl = getCurrentFragment();
          if (tl != null)
            tl.onDrawerOpened();
          //ActivityCompat.invalidateOptionsMenu(MyExpenses.this); // creates call to onPrepareOptionsMenu()
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
          super.onDrawerSlide(drawerView, 0); // this disables the animation
        }
      };

      // Set the drawer toggle as the DrawerListener
      mDrawerLayout.addDrawerListener(mDrawerToggle);
    }
    String[] from = new String[]{
        KEY_DESCRIPTION,
        KEY_LABEL,
        KEY_OPENING_BALANCE,
        KEY_SUM_INCOME,
        KEY_SUM_EXPENSES,
        KEY_SUM_TRANSFERS,
        KEY_CURRENT_BALANCE,
        KEY_TOTAL,
        KEY_CLEARED_TOTAL,
        KEY_RECONCILED_TOTAL
    };
    // and an array of the fields we want to bind those fields to
    int[] to = new int[]{
        R.id.description,
        R.id.label,
        R.id.opening_balance,
        R.id.sum_income,
        R.id.sum_expenses,
        R.id.sum_transfer,
        R.id.current_balance,
        R.id.total,
        R.id.cleared_total,
        R.id.reconciled_total
    };
    mDrawerListAdapter = new MyGroupedAdapter(this, R.layout.account_row, null, from, to, 0);

    Toolbar accountsMenu = findViewById(R.id.accounts_menu);
    accountsMenu.setTitle(R.string.pref_manage_accounts_title);
    accountsMenu.inflateMenu(R.menu.accounts);
    accountsMenu.inflateMenu(R.menu.sort);

    Menu menu = accountsMenu.getMenu();

    //Sort submenu
    MenuItem menuItem = menu.findItem(R.id.SORT_COMMAND);
    MenuItemCompat.setShowAsAction(
        menuItem, MenuItemCompat.SHOW_AS_ACTION_NEVER);
    sortMenu = menuItem.getSubMenu();
    sortMenu.findItem(R.id.SORT_CUSTOM_COMMAND).setVisible(true);

    //Grouping submenu
    SubMenu groupingMenu = menu.findItem(R.id.GROUPING_ACCOUNTS_COMMAND)
        .getSubMenu();
    AccountGrouping accountGrouping;
    try {
      accountGrouping = AccountGrouping.valueOf(
          PrefKey.ACCOUNT_GROUPING.getString("TYPE"));
    } catch (IllegalArgumentException e) {
      accountGrouping = AccountGrouping.TYPE;
    }
    MenuItem activeItem;
    switch (accountGrouping) {
      case CURRENCY:
        activeItem = groupingMenu.findItem(R.id.GROUPING_ACCOUNTS_CURRENCY_COMMAND);
        break;
      case NONE:
        activeItem = groupingMenu.findItem(R.id.GROUPING_ACCOUNTS_NONE_COMMAND);
        break;
      default:
        activeItem = groupingMenu.findItem(R.id.GROUPING_ACCOUNTS_TYPE_COMMAND);
        break;
    }
    activeItem.setChecked(true);

    accountsMenu.setOnMenuItemClickListener(item -> handleSortOption(item) || handleAccountsGrouping(item) ||
        dispatchCommand(item.getItemId(), null));

    mDrawerList.setAdapter(mDrawerListAdapter);
    mDrawerList.setAreHeadersSticky(false);
    mDrawerList.setOnItemClickListener((parent, view, position, id) -> {
      if (mAccountId != id) {
        moveToPosition(position);
        ((SimpleCursorAdapter) mDrawerListAdapter).notifyDataSetChanged();
        closeDrawer();
      }
    });

    requireFloatingActionButtonWithContentDescription(Utils.concatResStrings(this, ". ",
        R.string.menu_create_transaction, R.string.menu_create_transfer, R.string.menu_create_split));
    if (savedInstanceState != null) {
      mExportFormat = savedInstanceState.getString("exportFormat");
      mAccountId = savedInstanceState.getLong(KEY_ACCOUNTID, 0L);
    } else {
      Bundle extras = getIntent().getExtras();
      if (extras != null) {
        mAccountId = Utils.getFromExtra(extras, KEY_ROWID, 0);
        idFromNotification = extras.getLong(KEY_TRANSACTIONID, 0);
        //detail fragment from notification should only be shown upon first instantiation from notification
        if (idFromNotification != 0) {
          FragmentManager fm = getSupportFragmentManager();
          if (fm.findFragmentByTag(TransactionDetailFragment.class.getName()) == null) {
            TransactionDetailFragment.newInstance(idFromNotification)
                .show(fm, TransactionDetailFragment.class.getName());
            getIntent().removeExtra(KEY_TRANSACTIONID);
          }
        }
      }
    }
    if (mAccountId == 0) {
      mAccountId = PrefKey.CURRENT_ACCOUNT.getLong(0L);
    }
    setup();
  }

  private void setup() {
    newVersionCheck();
    Resources.Theme theme = getTheme();
    TypedValue margin = new TypedValue();
    theme.resolveAttribute(R.attr.pageMargin, margin, true);
    mViewPagerAdapter = new MyViewPagerAdapter(this, getSupportFragmentManager(), null);
    myPager = (ViewPager) this.findViewById(R.id.viewpager);
    myPager.setAdapter(this.mViewPagerAdapter);
    myPager.setOnPageChangeListener(this);
    myPager.setPageMargin((int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()));
    myPager.setPageMarginDrawable(margin.resourceId);
    mManager = getSupportLoaderManager();
    mManager.initLoader(ACCOUNTS_CURSOR, null, this);
  }

  private void moveToPosition(int position) {
    if (myPager.getCurrentItem() == position)
      setCurrentAccount(position);
    else
      myPager.setCurrentItem(position, false);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    MenuItem balanceItem = menu.findItem(R.id.BALANCE_COMMAND);
    if (balanceItem != null) {
      boolean showBalanceCommand = false;
      if (mAccountId > 0 && mAccountsCursor != null && !mAccountsCursor.isClosed() &&
          mAccountsCursor.moveToPosition(mCurrentPosition)) {
        try {
          if (AccountType.valueOf(mAccountsCursor.getString(mAccountsCursor.getColumnIndexOrThrow(KEY_TYPE)))
              != AccountType.CASH) {
            showBalanceCommand = true;
          }
        } catch (IllegalArgumentException ex) {/*aggregate*/}
      }
      Utils.menuItemSetEnabledAndVisible(balanceItem, showBalanceCommand);
    }

    Account account = Account.getInstanceFromDb(mAccountId);

    MenuItem groupingItem = menu.findItem(R.id.GROUPING_COMMAND);
    if (groupingItem != null) {
      SubMenu groupingMenu = groupingItem.getSubMenu();
      if (account != null) {
        Utils.configureGroupingMenu(groupingMenu, account.getGrouping());
      }
    }

    MenuItem sortDirectionItem = menu.findItem(R.id.SORT_DIRECTION_COMMAND);
    if (sortDirectionItem != null) {
      SubMenu sortDirectionMenu = sortDirectionItem.getSubMenu();
      if (account != null) {
        Utils.configureSortDirectionMenu(sortDirectionMenu, account.getSortDirection());
      }
    }
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.expenses, menu);
    inflater.inflate(R.menu.grouping, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }

  /* (non-Javadoc)
  * check if we should show one of the reminderDialogs
  * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
  */
  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == EDIT_TRANSACTION_REQUEST && resultCode == RESULT_OK) {
      long nextReminder;
      sequenceCount = intent.getLongExtra(KEY_SEQUENCE_COUNT, 0);
      if (!DistribHelper.isGithub()) {
        nextReminder =
            PrefKey.NEXT_REMINDER_RATE.getLong(TRESHOLD_REMIND_RATE);
        if (nextReminder != -1 && sequenceCount >= nextReminder) {
          RemindRateDialogFragment f = new RemindRateDialogFragment();
          f.setCancelable(false);
          f.show(getSupportFragmentManager(), "REMIND_RATE");
          return;
        }
      }
      adHandler.onEditTransactionResult();
    }
    if (requestCode == CREATE_ACCOUNT_REQUEST && resultCode == RESULT_OK) {
      mAccountId = intent.getLongExtra(KEY_ROWID, 0);
    }
  }

  public void addFilterCriteria(Integer id, Criteria c) {
    TransactionList tl = getCurrentFragment();
    if (tl != null) {
      tl.addFilterCriteria(id, c);
    }
  }

  /**
   * start ExpenseEdit Activity for a new transaction/transfer/split
   * Originally the form for transaction is rendered, user can change from spinner in toolbar
   */
  private void createRow() {
    Intent i = new Intent(this, ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
    //if we are called from an aggregate cursor, we also hand over the currency
    if (mAccountId < 0 && mAccountsCursor != null && mAccountsCursor.moveToPosition(mCurrentPosition)) {
      i.putExtra(KEY_CURRENCY, mAccountsCursor.getString(columnIndexCurrency));
      i.putExtra(ExpenseEdit.KEY_AUTOFILL_MAY_SET_ACCOUNT, true);
    } else {
      //if accountId is 0 ExpenseEdit will retrieve the first entry from the accounts table
      i.putExtra(KEY_ACCOUNTID, mAccountId);
    }
    startActivityForResult(i, EDIT_TRANSACTION_REQUEST);
  }

  /**
   * @param command
   * @param tag
   * @return true if command has been handled
   */
  public boolean dispatchCommand(int command, Object tag) {
    Intent i;
    TransactionList tl;
    switch (command) {
      case R.id.DISTRIBUTION_COMMAND:
        tl = getCurrentFragment();
        if (tl != null && tl.hasMappedCategories()) {
          contribFeatureRequested(ContribFeature.DISTRIBUTION, null);
        } else {
          showMessage(R.string.dialog_command_disabled_distribution);
        }
        return true;
      case R.id.CREATE_COMMAND:
        createRow();
        return true;
      case R.id.BALANCE_COMMAND:
        tl = getCurrentFragment();
        if (tl != null && hasCleared()) {
          mAccountsCursor.moveToPosition(mCurrentPosition);
          Currency currency = Utils.getSaveInstance(mAccountsCursor.getString(columnIndexCurrency));
          Bundle bundle = new Bundle();
          bundle.putLong(KEY_ROWID,
              mAccountsCursor.getLong(columnIndexRowId));
          bundle.putString(KEY_LABEL,
              mAccountsCursor.getString(columnIndexLabel));
          bundle.putString(KEY_RECONCILED_TOTAL,
              currencyFormatter.formatCurrency(
                  new Money(currency,
                      mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_RECONCILED_TOTAL)))));
          bundle.putString(KEY_CLEARED_TOTAL, currencyFormatter.formatCurrency(
              new Money(currency,
                  mAccountsCursor.getLong(mAccountsCursor.getColumnIndex(KEY_CLEARED_TOTAL)))));
          BalanceDialogFragment.newInstance(bundle)
              .show(getSupportFragmentManager(), "BALANCE_ACCOUNT");
        } else {
          showMessage(R.string.dialog_command_disabled_balance);
        }
        return true;
      case R.id.RESET_COMMAND:
        tl = getCurrentFragment();
        if (tl != null && tl.hasItems()) {
          Result appDirStatus = AppDirHelper.checkAppDir(this);
          if (appDirStatus.success) {
            ExportDialogFragment.newInstance(mAccountId, tl.isFiltered())
                .show(this.getSupportFragmentManager(), "WARNING_RESET");
          } else {
            showSnackbar(appDirStatus.print(this), Snackbar.LENGTH_LONG);
          }
        } else {
          showExportDisabledCommand();
        }
        return true;
      case R.id.BACKUP_COMMAND:
        startActivity(new Intent("myexpenses.intent.backup"));
        return true;
      case R.id.REMIND_NO_RATE_COMMAND:
        PrefKey.NEXT_REMINDER_RATE.putLong(-1);
        return true;
      case R.id.REMIND_LATER_RATE_COMMAND:
        PrefKey.NEXT_REMINDER_RATE.putLong(sequenceCount + TRESHOLD_REMIND_RATE);
        return true;
      case R.id.HELP_COMMAND_DRAWER:
        i = new Intent(this, Help.class);
        i.putExtra(Help.KEY_CONTEXT, "NavigationDrawer");
        //for result is needed since it allows us to inspect the calling activity
        startActivity(i);
        return true;
      case R.id.HELP_COMMAND:
        setHelpVariant();
        break;
      case R.id.MANAGE_PLANS_COMMAND:
        i = new Intent(this, ManageTemplates.class);
        startActivity(i);
        return true;
      case R.id.CREATE_ACCOUNT_COMMAND:
        if (mAccountCount == 0) {
          showSnackbar(R.string.account_list_not_yet_loaded, Snackbar.LENGTH_LONG);
        }
        //we need the accounts to be loaded in order to evaluate if the limit has been reached
        else if (ContribFeature.ACCOUNTS_UNLIMITED.hasAccess() || mAccountCount < 5) {
          closeDrawer();
          i = new Intent(this, AccountEdit.class);
          if (tag != null)
            i.putExtra(KEY_CURRENCY, (String) tag);
          startActivityForResult(i, CREATE_ACCOUNT_REQUEST);
        } else {
          CommonCommands.showContribDialog(this, ContribFeature.ACCOUNTS_UNLIMITED, null);
        }
        return true;
      case R.id.DELETE_ACCOUNT_COMMAND_DO:
        //reset mAccountId will prevent the now defunct account being used in an immediately following "new transaction"
        mAccountId = 0;
        startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_ACCOUNT,
            new Long[]{(Long) tag},
            null,
            R.string.progress_dialog_deleting);
        return true;
      case R.id.SHARE_COMMAND:
        i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.putExtra(Intent.EXTRA_TEXT, Utils.getTellAFriendMessage(this));
        i.setType("text/plain");
        startActivity(Intent.createChooser(i, getResources().getText(R.string.menu_share)));
        return true;
      case R.id.CANCEL_CALLBACK_COMMAND:
        finishActionMode();
        return true;
      case R.id.OPEN_PDF_COMMAND: {
        i = new Intent();
        i.setAction(Intent.ACTION_VIEW);
        Uri data = AppDirHelper.ensureContentUri(Uri.parse((String) tag));
        i.setDataAndType(data, "application/pdf");
        i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!Utils.isIntentAvailable(this, i)) {
          showSnackbar(R.string.no_app_handling_pdf_available, Snackbar.LENGTH_LONG);
        } else {
          startActivity(i);
        }
        return true;
      }
      case R.id.SHARE_PDF_COMMAND: {
        Result shareResult = ShareUtils.share(this,
            Collections.singletonList(AppDirHelper.ensureContentUri(Uri.parse((String) tag))),
            PrefKey.SHARE_TARGET.getString("").trim(),
            "application/pdf");
        if (!shareResult.success) {
          showSnackbar(shareResult.print(this), Snackbar.LENGTH_LONG);
        }
        return true;
      }
      case R.id.QUIT_COMMAND:
        finish();
        return true;
      case R.id.EDIT_ACCOUNT_COMMAND:
        closeDrawer();
        long accountId = (Long) tag;
        if (accountId > 0) { //do nothing if accidentally we are positioned at an aggregate account
          i = new Intent(this, AccountEdit.class);
          i.putExtra(KEY_ROWID, accountId);
          startActivityForResult(i, EDIT_ACCOUNT_REQUEST);
        }
        return true;
      case R.id.DELETE_ACCOUNT_COMMAND:
        closeDrawer();
        accountId = (Long) tag;
        //do nothing if accidentally we are positioned at an aggregate account or try to delete the last account
        if (mAccountsCursor.getCount() > 1 && accountId > 0) {
          MessageDialogFragment.newInstance(
              R.string.dialog_title_warning_delete_account,
              getString(R.string.warning_delete_account, Account.getInstanceFromDb(accountId).getLabel()),
              new MessageDialogFragment.Button(R.string.menu_delete, R.id.DELETE_ACCOUNT_COMMAND_DO,
                  accountId),
              null,
              MessageDialogFragment.Button.noButton())
              .show(getSupportFragmentManager(), "DELETE_ACCOUNT");
        }
        return true;
    }
    return super.dispatchCommand(command, tag);
  }

  public void showExportDisabledCommand() {
    showMessage(R.string.dialog_command_disabled_reset_account);
  }

  private void closeDrawer() {
    if (mDrawerLayout != null) mDrawerLayout.closeDrawers();
  }

  private class MyViewPagerAdapter extends CursorFragmentPagerAdapter {
    public MyViewPagerAdapter(Context context, FragmentManager fm, Cursor cursor) {
      super(context, fm, cursor);
    }

    public String getFragmentName(int currentPosition) {
      return FragmentPagerAdapter.makeFragmentName(R.id.viewpager, getItemId(currentPosition));
    }

    @Override
    public Fragment getItem(Context context, Cursor cursor) {
      long accountId = cursor.getLong(columnIndexRowId);
      //calling the constructors, puts the objects into the cache from where the fragment can
      //retrieve it, without needing to create a new cursor
      Account.fromCacheOrFromCursor(cursor);
      return TransactionList.newInstance(accountId);
    }
  }

  @Override
  public void onPageSelected(int position) {
    finishActionMode();
    mCurrentPosition = position;
    setCurrentAccount(position);
  }

  public void finishActionMode() {
    if (mCurrentPosition != -1) {
      ContextualActionBarFragment f =
          (ContextualActionBarFragment) getSupportFragmentManager().findFragmentByTag(
              mViewPagerAdapter.getFragmentName(mCurrentPosition));
      if (f != null)
        f.finishActionMode();
    }
  }

  @SuppressWarnings("incomplete-switch")
  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    switch (feature) {
      case DISTRIBUTION:
        Account a = Account.getInstanceFromDb(mAccountId);
        recordUsage(feature);
        Intent i = new Intent(this, ManageCategories.class);
        i.setAction("myexpenses.intent.distribution");
        i.putExtra(KEY_ACCOUNTID, mAccountId);
        if (tag != null) {
          int year = (int) ((Long) tag / 1000);
          int groupingSecond = (int) ((Long) tag % 1000);
          i.putExtra("grouping", a != null ? a.getGrouping() : Grouping.NONE);
          i.putExtra("groupingYear", year);
          i.putExtra("groupingSecond", groupingSecond);
        }
        startActivity(i);
        break;
      case SPLIT_TRANSACTION:
        if (tag != null) {
          startTaskExecution(
              TaskExecutionFragment.TASK_SPLIT,
              (Object[]) tag,
              null,
              0);
        }
        break;
      case PRINT:
        TransactionList tl = getCurrentFragment();
        if (tl != null) {
          Bundle args = new Bundle();
          args.putSparseParcelableArray(TransactionList.KEY_FILTER, tl.getFilterCriteria());
          args.putLong(KEY_ROWID, mAccountId);
          getSupportFragmentManager().beginTransaction()
              .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_PRINT), ASYNC_TAG)
              .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_printing), PROGRESS_TAG)
              .commit();
        }
        break;
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    switch (id) {
      case ACCOUNTS_CURSOR:
        Uri.Builder builder = TransactionProvider.ACCOUNTS_URI.buildUpon();
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_MERGE_CURRENCY_AGGREGATES, "1");
        return new ProtectedCursorLoader(this, builder.build());
    }
    return null;
  }

  /**
   * set the Current account to the one in the requested position of mAccountsCursor
   *
   * @param position
   */
  private void setCurrentAccount(int position) {
    mAccountsCursor.moveToPosition(position);
    long newAccountId = mAccountsCursor.getLong(columnIndexRowId);
    if (mAccountId != newAccountId) {
      PrefKey.CURRENT_ACCOUNT.putLong(newAccountId);
    }
    int color = newAccountId < 0 ? colorAggregate : mAccountsCursor.getInt(columnIndexColor);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Window window = getWindow();
      //noinspection InlinedApi
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      //noinspection InlinedApi
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      int color700 = UiUtils.get700Tint(color);
      window.setStatusBarColor(color700);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        //noinspection InlinedApi
        getWindow().getDecorView().setSystemUiVisibility(
            UiUtils.isBrightColor(color700) ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0);
      }
    }
    UiUtils.setBackgroundTintListOnFab(floatingActionButton, color);
    mAccountId = newAccountId;
    setBalance();
    mDrawerList.setItemChecked(position, true);
    supportInvalidateOptionsMenu();
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    switch (loader.getId()) {
      case ACCOUNTS_CURSOR:
        //we postpone this until cursor is loaded, because prefkey is updated in migration to db schema 56
        Utils.configureSortMenu(sortMenu, PrefKey.SORT_ORDER_ACCOUNTS.getString("USAGES"));
        mAccountCount = 0;
        mAccountsCursor = cursor;
        if (mAccountsCursor == null) {
          return;
        }
        //when account grouping is changed in setting, cursor is reloaded,
        //and we need to refresh the value here
        try {
          mAccountGrouping = AccountGrouping.valueOf(
              PrefKey.ACCOUNT_GROUPING.getString("TYPE"));
        } catch (IllegalArgumentException e) {
          mAccountGrouping = AccountGrouping.TYPE;
        }
        ((SimpleCursorAdapter) mDrawerListAdapter).swapCursor(mAccountsCursor);
        //swaping the cursor is altering the accountId, if the
        //sort order has changed, but we want to move to the same account as before
        long cacheAccountId = mAccountId;
        mViewPagerAdapter.swapCursor(cursor);
        mAccountId = cacheAccountId;
        if (!indexesCalculated) {
          columnIndexRowId = mAccountsCursor.getColumnIndex(KEY_ROWID);
          columnIndexColor = mAccountsCursor.getColumnIndex(KEY_COLOR);
          columnIndexCurrency = mAccountsCursor.getColumnIndex(KEY_CURRENCY);
          columnIndexDescription = mAccountsCursor.getColumnIndex(KEY_DESCRIPTION);
          columnIndexLabel = mAccountsCursor.getColumnIndex(KEY_LABEL);
          indexesCalculated = true;
        }
        if (mAccountsCursor.moveToFirst()) {
          int position = 0;
          while (!mAccountsCursor.isAfterLast()) {
            long accountId = mAccountsCursor.getLong(columnIndexRowId);
            if (accountId == mAccountId) {
              position = mAccountsCursor.getPosition();
            }
            if (accountId > 0) {
              mAccountCount++;
            }
            mAccountsCursor.moveToNext();
          }
          mCurrentPosition = position;
          moveToPosition(mCurrentPosition);
          //should be triggered through onPageSelected
          //setCurrentAccount(mCurrentPosition);
        }
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    if (arg0.getId() == ACCOUNTS_CURSOR) {
      mViewPagerAdapter.swapCursor(null);
      ((SimpleCursorAdapter) mDrawerListAdapter).swapCursor(null);
      mCurrentPosition = -1;
      mAccountsCursor = null;
    }
  }

  @Override
  public void onPageScrollStateChanged(int arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onPageScrolled(int arg0, float arg1, int arg2) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (TransactionList.NEW_TEMPLATE_DIALOG.equals(dialogTag) && which == BUTTON_POSITIVE) {
      String label = extras.getString(SimpleInputDialog.TEXT);
      Uri uri = new Template(Transaction.getInstanceFromDb(extras.getLong(KEY_ROWID)), label).save();
      if (uri == null) {
        showSnackbar(R.string.template_create_error, Snackbar.LENGTH_LONG);
      } else {
        // show template edit activity
        Intent i = new Intent(this, ExpenseEdit.class);
        i.putExtra(DatabaseConstants.KEY_TEMPLATEID, ContentUris.parseId(uri));
        startActivity(i);
      }

      finishActionMode();
      return true;
    }
    if (TransactionList.FILTER_COMMENT_DIALOG.equals(dialogTag) && which == BUTTON_POSITIVE) {
      addFilterCriteria(R.id.FILTER_COMMENT_COMMAND,
          new CommentCriteria(extras.getString(SimpleInputDialog.TEXT)));
      return true;
    }
    return false;
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    Integer successCount;
    String msg;
    super.onPostExecute(taskId, o);
    switch (taskId) {
      case TaskExecutionFragment.TASK_SPLIT:
        successCount = (Integer) o;
        msg = successCount == 0 ? getString(R.string.split_transaction_error) :
            getResources().getQuantityString(R.plurals.split_transaction_success, successCount, successCount);
        showSnackbar(msg, Snackbar.LENGTH_LONG);
        break;
      case TaskExecutionFragment.TASK_EXPORT:
        ArrayList<Uri> files = (ArrayList<Uri>) o;
        if (files != null && !files.isEmpty()) {
          Result shareResult = ShareUtils.share(this, files,
              PrefKey.SHARE_TARGET.getString("").trim(),
              "text/" + mExportFormat.toLowerCase(Locale.US));
          if (!shareResult.success) {
            showSnackbar(shareResult.print(this), Snackbar.LENGTH_LONG);
          }
        }
        break;
      case TaskExecutionFragment.TASK_PRINT:
        Result result = (Result) o;
        if (result.success) {
          recordUsage(ContribFeature.PRINT);
          MessageDialogFragment f = MessageDialogFragment.newInstance(
              0,
              getString(result.getMessage(), FileUtils.getPath(this, (Uri) result.extra[0])),
              new MessageDialogFragment.Button(R.string.menu_open, R.id.OPEN_PDF_COMMAND, result.extra[0].toString(), true),
              MessageDialogFragment.Button.nullButton(R.string.button_label_close),
              new MessageDialogFragment.Button(R.string.button_label_share_file, R.id.SHARE_PDF_COMMAND, result.extra[0].toString(), true));
          f.setCancelable(false);
          f.show(getSupportFragmentManager(), "PRINT_RESULT");
        } else {
          showSnackbar(result.print(this), Snackbar.LENGTH_LONG);
        }
        break;
    }
  }

  public boolean hasExported() {
    //in case we are called before the accounts cursor is loaded, we return false
    if (mAccountsCursor == null || mAccountsCursor.getCount() == 0)
      return false;
    mAccountsCursor.moveToPosition(mCurrentPosition);
    return mAccountsCursor.getInt(mAccountsCursor.getColumnIndexOrThrow(KEY_HAS_EXPORTED)) > 0;
  }

  private boolean hasCleared() {
    //in case we are called before the accounts cursor is loaded, we return false
    if (mAccountsCursor == null || mAccountsCursor.getCount() == 0)
      return false;
    mAccountsCursor.moveToPosition(mCurrentPosition);
    return mAccountsCursor.getInt(mAccountsCursor.getColumnIndexOrThrow(KEY_HAS_CLEARED)) > 0;
  }

  private void setConvertedAmount(TextView tv, Currency currency) {
    tv.setText(currencyFormatter.convAmount(tv.getText().toString(), currency));
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    if (mDrawerToggle != null) mDrawerToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Pass the event to ActionBarDrawerToggle, if it returns
    // true, then it has handled the app icon touch event
    if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

    return handleGrouping(item) || handleSortDirection(item) || super.onOptionsItemSelected(item);

  }

  private void setBalance() {
    long balance = mAccountsCursor.getLong(mAccountsCursor.getColumnIndex
        (KEY_CURRENT_BALANCE));
    mCurrentBalance = currencyFormatter.formatCurrency(new Money(Utils.getSaveInstance(mAccountsCursor
        .getString(columnIndexCurrency)), balance));
    TextView balanceTextView = (TextView) mToolbar.findViewById(R.id.end);
    balanceTextView.setTextColor(balance < 0 ? colorExpense : colorIncome);
    balanceTextView.setText(mCurrentBalance);
  }

  public void setTitle(String title) {
    ((TextView) mToolbar.findViewById(R.id.action_bar_title)).setText(title);
  }

  public TransactionList getCurrentFragment() {
    if (mViewPagerAdapter == null)
      return null;
    return (TransactionList) getSupportFragmentManager().findFragmentByTag(
        mViewPagerAdapter.getFragmentName(mCurrentPosition));
  }

  public class MyGroupedAdapter extends SimpleCursorAdapter implements StickyListHeadersAdapter {
    public static final int CARD_ELEVATION_DIP = 24;
    LayoutInflater inflater;

    public MyGroupedAdapter(Context context, int layout, Cursor c, String[] from,
                            int[] to, int flags) {
      super(context, layout, c, from, to, flags);
      inflater = LayoutInflater.from(MyExpenses.this);
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.accounts_header, parent, false);
      }
      Cursor c = getCursor();
      c.moveToPosition(position);
      long headerId = getHeaderId(position);
      TextView sectionLabelTV = (TextView) convertView.findViewById(R.id.sectionLabel);
      switch (mAccountGrouping) {
        case CURRENCY:
          sectionLabelTV.setText(CurrencyEnum.valueOf(c.getString(columnIndexCurrency)).toString());
          break;
        case NONE:
          sectionLabelTV.setText(headerId == 0 ? R.string.pref_manage_accounts_title : R.string.menu_aggregates);
          break;
        case TYPE:
          int headerRes;
          if (headerId == AccountType.values().length) {
            headerRes = R.string.menu_aggregates;
          } else {
            headerRes = AccountType.values()[(int) headerId].toStringResPlural();
          }
          sectionLabelTV.setText(headerRes);
        default:
          break;

      }
      return convertView;
    }

    @Override
    public long getHeaderId(int position) {
      Cursor c = getCursor();
      c.moveToPosition(position);
      switch (mAccountGrouping) {
        case CURRENCY:
          return CurrencyEnum.valueOf(c.getString(columnIndexCurrency)).ordinal();
        case NONE:
          return c.getLong(columnIndexRowId) > 0 ? 0 : 1;
        case TYPE:
          AccountType type;
          try {
            type = AccountType.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE)));
            return type.ordinal();
          } catch (IllegalArgumentException ex) {
            return AccountType.values().length;
          }
      }
      return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
      View row = super.getView(position, convertView, parent);
      final Cursor c = getCursor();
      c.moveToPosition(position);

      View v = row.findViewById(R.id.color1);
      TextView labelTv = row.findViewById(R.id.label);
      final View accountMenu = row.findViewById(R.id.account_menu);

      Currency currency = Utils.getSaveInstance(c.getString(columnIndexCurrency));
      final long rowId = c.getLong(columnIndexRowId);
      long sum_transfer = c.getLong(c.getColumnIndex(KEY_SUM_TRANSFERS));

      boolean isHighlighted = rowId == mAccountId;
      boolean has_future = c.getInt(c.getColumnIndex(KEY_HAS_FUTURE)) > 0;
      final boolean isAggregate = rowId < 0;
      final int count = c.getCount();
      boolean hide_cr;
      int colorInt;

      ((CardView) row.findViewById(R.id.card)).setCardElevation(isHighlighted ?
          TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP, CARD_ELEVATION_DIP, getResources().getDisplayMetrics()) :
          0);
      labelTv.setTypeface(
          Typeface.create(labelTv.getTypeface(), Typeface.NORMAL),
          isHighlighted ? Typeface.BOLD : Typeface.NORMAL);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        row.findViewById(R.id.selected_indicator).setVisibility(isHighlighted ? View.VISIBLE : View.GONE);
      }
      if (isAggregate) {
        accountMenu.setVisibility(View.INVISIBLE);
        accountMenu.setOnClickListener(null);
      } else {
        accountMenu.setVisibility(View.VISIBLE);
        boolean upVisible = false, downVisible = false;
        if (PrefKey.SORT_ORDER_ACCOUNTS.getString(SORT_ORDER_USAGES).equals(SORT_ORDER_CUSTOM)) {
          if (position > 0 && getHeaderId(position - 1) == getHeaderId(position)) {
            getCursor().moveToPosition(position - 1);
            if (c.getLong(columnIndexRowId) > 0) upVisible = true; //ignore if previous is aggregate
          }
          if (position + 1 < getCount() && getHeaderId(position + 1) == getHeaderId(position)) {
            getCursor().moveToPosition(position + 1);
            if (c.getLong(columnIndexRowId) > 0) downVisible = true;
          }
          getCursor().moveToPosition(position);
        }
        final boolean finalUpVisible = upVisible, finalDownVisible = downVisible;
        accountMenu.setOnClickListener(v1 -> {
          PopupMenu popup = new PopupMenu(MyExpenses.this, accountMenu);
          popup.inflate(R.menu.accounts_context);
          Menu menu = popup.getMenu();
          menu.findItem(R.id.DELETE_ACCOUNT_COMMAND).setVisible(count > 1);
          menu.findItem(R.id.UP_COMMAND).setVisible(finalUpVisible);
          menu.findItem(R.id.DOWN_COMMAND).setVisible(finalDownVisible);
          popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
              return handleSwap(item.getItemId(), position) ||
                  dispatchCommand(item.getItemId(), rowId);
            }

            private boolean handleSwap(int itemId, int position1) {
              if (itemId != R.id.UP_COMMAND && itemId != R.id.DOWN_COMMAND) return false;
              Cursor c1 = getCursor();
              c1.moveToPosition(position1);
              String sortKey1 = c1.getString(c1.getColumnIndex(KEY_SORT_KEY));
              c1.moveToPosition(itemId == R.id.UP_COMMAND ? position1 - 1 : position1 + 1);
              String sortKey2 = c1.getString(c1.getColumnIndex(KEY_SORT_KEY));
              startTaskExecution(
                  TaskExecutionFragment.TASK_SWAP_SORT_KEY,
                  new String[]{sortKey1, sortKey2},
                  null,
                  R.string.progress_dialog_saving);
              return true;
            }
          });
          popup.show();
        });
      }

      if (isAggregate) {
        hide_cr = true;
        if (mAccountGrouping == AccountGrouping.CURRENCY) {
          labelTv.setText(R.string.menu_aggregates);
        }
        colorInt = colorAggregate;
      } else {
        //for deleting we need the position, because we need to find out the account's label
        try {
          hide_cr = AccountType.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE))).equals(AccountType.CASH);
        } catch (IllegalArgumentException ex) {
          hide_cr = true;
        }
        colorInt = c.getInt(columnIndexColor);
      }
      row.findViewById(R.id.TransferRow).setVisibility(
          sum_transfer == 0 ? View.GONE : View.VISIBLE);
      row.findViewById(R.id.TotalRow).setVisibility(
          has_future ? View.VISIBLE : View.GONE);
      row.findViewById(R.id.ClearedRow).setVisibility(
          hide_cr ? View.GONE : View.VISIBLE);
      row.findViewById(R.id.ReconciledRow).setVisibility(
          hide_cr ? View.GONE : View.VISIBLE);
      if (c.getLong(columnIndexRowId) > 0) {
        setConvertedAmount(row.findViewById(R.id.sum_transfer), currency);
      }
      v.setBackgroundColor(colorInt);
      setConvertedAmount(row.findViewById(R.id.opening_balance), currency);
      setConvertedAmount(row.findViewById(R.id.sum_income), currency);
      setConvertedAmount(row.findViewById(R.id.sum_expenses), currency);
      setConvertedAmount(row.findViewById(R.id.current_balance), currency);
      setConvertedAmount(row.findViewById(R.id.total), currency);
      setConvertedAmount(row.findViewById(R.id.reconciled_total), currency);
      setConvertedAmount(row.findViewById(R.id.cleared_total), currency);
      String description = c.getString(columnIndexDescription);
      row.findViewById(R.id.description).setVisibility(
          TextUtils.isEmpty(description) ? View.GONE : View.VISIBLE);
      return row;
    }
  }

  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    //detail fragment from notification should only be shown once
    if (idFromNotification != 0) {
      outState.putLong("idFromNotification", 0);
    }
    outState.putString("exportFormat", mExportFormat);
    outState.putLong(KEY_ACCOUNTID, mAccountId);
  }

  @Override
  public void onPositive(Bundle args) {
    super.onPositive(args);
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.START_EXPORT_COMMAND:
        mExportFormat = args.getString("format");
        args.putSparseParcelableArray(TransactionList.KEY_FILTER,
            getCurrentFragment().getFilterCriteria());
        getSupportFragmentManager().beginTransaction()
            .add(TaskExecutionFragment.newInstanceWithBundle(args, TaskExecutionFragment.TASK_EXPORT),
                ASYNC_TAG)
            .add(ProgressDialogFragment.newInstance(
                R.string.pref_category_title_export, 0, ProgressDialog.STYLE_SPINNER, true), PROGRESS_TAG)
            .commit();
        break;
      case R.id.BALANCE_COMMAND_DO:
        startTaskExecution(TaskExecutionFragment.TASK_BALANCE,
            new Long[]{args.getLong(KEY_ROWID)},
            args.getBoolean("deleteP"), 0);
        break;
      case R.id.DELETE_COMMAND_DO:
        //Confirmation dialog was shown without Checkbox, because it was called with only void transactions
        onPositive(args, false);
    }
  }

  @Override
  public void onPositive(Bundle args, boolean checked) {
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.DELETE_COMMAND_DO:
        finishActionMode();
        startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_TRANSACTION,
            ArrayUtils.toObject(args.getLongArray(TaskExecutionFragment.KEY_OBJECT_IDS)),
            Boolean.valueOf(checked),
            R.string.progress_dialog_deleting);
    }
  }

  @Override
  public void onNegative(Bundle args) {
    int command = args.getInt(ConfirmationDialogFragment.KEY_COMMAND_NEGATIVE);
    if (command != 0) {
      dispatchCommand(command, null);
    }
  }

  @Override
  public void onDismissOrCancel(Bundle args) {
  }

  @Override
  protected void onResume() {
    super.onResume();
    adHandler.onResume();
  }

  @Override
  public void onDestroy() {
    adHandler.onDestroy();
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    adHandler.onPause();
    super.onPause();
  }

  public void onBackPressed() {
    if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
      mDrawerLayout.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  public void copyToClipBoard(View view) {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    clipboard.setText(mCurrentBalance);
    showSnackbar(R.string.copied_to_clipboard, Snackbar.LENGTH_LONG);
  }

  protected boolean handleSortOption(MenuItem item) {
    String newSortOrder = Utils.getSortOrderFromMenuItemId(item.getItemId());
    if (newSortOrder != null) {
      if (!item.isChecked()) {
        PrefKey.SORT_ORDER_ACCOUNTS.putString(newSortOrder);
        item.setChecked(true);

        if (mManager.getLoader(ACCOUNTS_CURSOR) != null && !mManager.getLoader(ACCOUNTS_CURSOR).isReset()) {
          mManager.restartLoader(ACCOUNTS_CURSOR, null, this);
        } else {
          mManager.initLoader(ACCOUNTS_CURSOR, null, this);
        }
        if (item.getItemId() == R.id.SORT_CUSTOM_COMMAND) {
          showMessage(R.string.dialog_title_information,
              getString(R.string.dialog_info_custom_sort));
        }
      }
      return true;
    }
    return false;
  }

  protected boolean handleAccountsGrouping(MenuItem item) {
    AccountGrouping newGrouping = null;
    switch (item.getItemId()) {
      case R.id.GROUPING_ACCOUNTS_CURRENCY_COMMAND:
        newGrouping = AccountGrouping.CURRENCY;
        break;
      case R.id.GROUPING_ACCOUNTS_TYPE_COMMAND:
        newGrouping = AccountGrouping.TYPE;
        break;
      case R.id.GROUPING_ACCOUNTS_NONE_COMMAND:
        newGrouping = AccountGrouping.NONE;
        break;
    }
    if (newGrouping != null) {
      if (!item.isChecked()) {
        PrefKey.ACCOUNT_GROUPING.putString(newGrouping.name());
        item.setChecked(true);

        if (mManager.getLoader(ACCOUNTS_CURSOR) != null && !mManager.getLoader(ACCOUNTS_CURSOR).isReset())
          mManager.restartLoader(ACCOUNTS_CURSOR, null, this);
        else
          mManager.initLoader(ACCOUNTS_CURSOR, null, this);
      }
      return true;
    }
    return false;
  }

  protected boolean handleGrouping(MenuItem item) {
    Grouping newGrouping = Utils.getGroupingFromMenuItemId(item.getItemId());
    if (newGrouping != null) {
      if (!item.isChecked()) {
        item.setChecked(true);
        Account.getInstanceFromDb(mAccountId).persistGrouping(newGrouping);
      }
      return true;
    }
    return false;
  }

  protected boolean handleSortDirection(MenuItem item) {
    SortDirection newSortDirection = Utils.getSortDirectionFromMenuItemId(item.getItemId());
    if (newSortDirection != null) {
      if (!item.isChecked()) {
        item.setChecked(true);
        Account.getInstanceFromDb(mAccountId).persistSortDirection(newSortDirection);
      }
      return true;
    }
    return false;
  }

  @Override
  protected boolean shouldKeepProgress(int taskId) {
    return taskId == TASK_EXPORT;
  }

}