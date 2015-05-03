package org.totschnig.myexpenses.test.activity.myexpenses;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.util.Fixture;
import com.robotium.solo.Solo;

import android.content.Intent;
import android.database.Cursor;


/**
 * We test that starting activity with account_id will
 * display the intended account
 * 
 * @author Michael Totschnig
 */
public class C_IntentTest extends MyActivityTest<MyExpenses> {
  
  public C_IntentTest() {
    super(MyExpenses.class);
  }
  public void setUp() throws Exception { 
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    
    Fixture.setup(getInstrumentation(), Locale.getDefault(), Currency.getInstance("USD"));
  }
  public void testNavigateToAccountReceivedThroughIntent() {
    setActivity(null);
    Cursor cursor = getInstrumentation().getContext().getContentResolver().query(
        TransactionProvider.ACCOUNTS_URI,  // the URI for the main data table
        new String[] {KEY_ROWID,KEY_LABEL},                       // no projection, get all columns
        null,                       // no selection criteria, get all records
        null,                       // no selection arguments
        null                        // use default sort order
    );
    cursor.moveToFirst();
    while(!cursor.isAfterLast()) {
      setActivityInitialTouchMode(false);
      Intent i = new Intent()
        .putExtra(KEY_ROWID, cursor.getLong(cursor.getColumnIndex(KEY_ROWID)))
        .setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.MyExpenses")
        ;
      setActivityIntent(i);
      mActivity = getActivity();
      mSolo = new Solo(getInstrumentation(), mActivity);
      mSolo.searchText(cursor.getString(cursor.getColumnIndex(KEY_LABEL)));
      mActivity.finish();  // close the activity
      setActivity(null);
      cursor.moveToNext();
    }
    cursor.close();
  }
}
