package org.totschnig.myexpenses.test.activity.myexpenses;

import android.app.Instrumentation;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import com.robotium.solo.Solo;

import junit.framework.Assert;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.fragment.CategoryList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitPartCategory;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.util.Fixture;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import java.io.InputStream;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;


/**
 * @author Michael Totschnig
 * It is run on a Nexus S in portrait mode, and on a Android x86 VM, in landscape mode
 * It operates on the assumption that on pre-ICS device, keys invoked with invokeMenuActionSync
 * are not part of the visible action bar, but in the menu
 * on a fresh install A_InstallTest has to be run first
 */
public class E_SearchFilterTest extends MyActivityTest<MyExpenses> {

  Account account1;
  private String catLabel1, catLabel2;
  private MyApplication app;

  public E_SearchFilterTest() {
    super(MyExpenses.class);
  }
  public void setUp() throws Exception {
    super.setUp();
    app = (MyApplication) getInstrumentation().getTargetContext().getApplicationContext();
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    setup(Locale.getDefault(), Currency.getInstance("USD"));
    android.content.SharedPreferences pref = app.getSettings();
    //make sure we have no filters sticking around
    pref.edit().clear().commit();
  }

  public void testCatFilter() {
    Intent i = new Intent()
        .putExtra(KEY_ROWID, account1.getId())
        .setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.MyExpenses")
        ;
    setActivityIntent(i);
    mActivity = getActivity();
    dismissWelcomeScreen();
    //before setting the filter both categories are visible
    assertTrue(mSolo.searchText(catLabel1));
    assertTrue(mSolo.searchText(catLabel2));
    clickOnActionBarItem("SEARCH");
    mSolo.clickOnText(mActivity.getString(R.string.category));
    assertTrue(mSolo.waitForActivity(ManageCategories.class.getSimpleName()));
    mSolo.clickLongOnText(catLabel1);
    mSolo.clickOnView(mSolo.getView(R.id.SELECT_COMMAND_MULTIPLE));
    assertTrue(mSolo.waitForActivity(MyExpenses.class.getSimpleName()));
    //after setting the filter only first category is visible
    assertTrue(mSolo.searchText(catLabel1));
    assertFalse(mSolo.searchText(catLabel2));
  }

  public void setup(Locale locale, Currency defaultCurrency) {
    Context testContext = mInstrumentation.getContext();
    Context appContext = mInstrumentation.getTargetContext().getApplicationContext();
    Fixture.setUpCategories(locale,appContext);

    account1 = Account.getInstanceFromDb(0);
    account1.reset(null, Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null);
    catLabel1 = testContext.getString(org.totschnig.myexpenses.test.R.string.testData_transaction1MainCat);
    catLabel2 = testContext.getString(org.totschnig.myexpenses.test.R.string.testData_transaction2MainCat);
    //Transaction 0 for D_ContextActionTest
    Transaction op = Transaction.getNewInstance(account1.getId());
    op.amount = new Money(defaultCurrency,-1200L);
    op.setCatId(Fixture.findCat(catLabel1, null));
    op.save();
    op.setCatId(Fixture.findCat(catLabel2, null));
    op.saveAsNew();
  }
}
