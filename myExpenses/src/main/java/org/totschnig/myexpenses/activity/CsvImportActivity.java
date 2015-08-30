package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.widget.Toast;

import org.apache.commons.csv.CSVRecord;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.fragment.CsvImportDataFragment;
import org.totschnig.myexpenses.fragment.CsvImportParseFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;

import java.util.ArrayList;
import java.util.Locale;


public class CsvImportActivity extends ProtectedFragmentActivity implements
    ActionBar.TabListener,ConfirmationDialogFragment.ConfirmationDialogListener {

  public static final String KEY_DATA_READY = "KEY_DATA_READY";
  public static final String KEY_USAGE_RECORDED = "KEY_USAGE_RECORDED";
  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  SectionsPagerAdapter mSectionsPagerAdapter;

  /**
   * The {@link ViewPager} that will host the section contents.
   */
  ViewPager mViewPager;

  private boolean mDataReady = false;
  private boolean mUsageRecorded = false;

  private void setmDataReady(boolean mDataReady) {
    this.mDataReady = mDataReady;
    mSectionsPagerAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.viewpager);
    final ActionBar actionBar = getSupportActionBar();
    actionBar.setTitle(getString(R.string.pref_import_title, "CSV"));

    // Set up the action bar.
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.viewpager);
    mViewPager.setAdapter(mSectionsPagerAdapter);

    // When swiping between different sections, select the corresponding
    // tab. We can also use ActionBar.Tab#select() to do this if we have
    // a reference to the Tab.
    mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        actionBar.setSelectedNavigationItem(position);
      }
    });

    //we only add the first tab, the second one once data has been parsed
    addTab(0);
    if (savedInstanceState != null) {
      mUsageRecorded = savedInstanceState.getBoolean(KEY_USAGE_RECORDED);
      if (savedInstanceState.getBoolean(KEY_DATA_READY)) {
        addTab(1);
        setmDataReady(true);
      }
    }
  }


  @Override
  public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    // When the given tab is selected, switch to the corresponding page in
    // the ViewPager.
    mViewPager.setCurrentItem(tab.getPosition());
  }

  @Override
  public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
  }

  @Override
  public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
  }

  @Override
  public void onPositive(Bundle args) {
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.SET_HEADER_COMMAND:
      CsvImportDataFragment df = (CsvImportDataFragment) getSupportFragmentManager().findFragmentByTag(
          mSectionsPagerAdapter.getFragmentName(1));
      df.setHeader();
    }
  }

  @Override
  public void onNegative(Bundle args) {

  }

  @Override
  public void onDismissOrCancel(Bundle args) {

  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
     switch (position) {
       case 0:
         return CsvImportParseFragment.newInstance();
       case 1:
         return CsvImportDataFragment.newInstance();
     }
      return null;
    }

    @Override
    public int getCount() {
      return mDataReady ? 2 : 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      Locale l = Locale.getDefault();
      switch (position) {
        case 0:
          return getString(R.string.menu_parse);
        case 1:
          return getString(R.string.csv_import_preview);
      }
      return null;
    }
    public String getFragmentName(int currentPosition) {
      //http://stackoverflow.com/questions/7379165/update-data-in-listfragment-as-part-of-viewpager
      //would call this function if it were visible
      //return makeFragmentName(R.id.viewpager,currentPosition);
      return "android:switcher:"+R.id.viewpager+":"+getItemId(currentPosition);
    }
  }

  @Override
  public void onPostExecute(int taskId, Object result) {
    super.onPostExecute(taskId, result);
    switch (taskId) {
      case TaskExecutionFragment.TASK_CSV_PARSE:
        if (result != null) {
          if (!mDataReady) {
            addTab(1);
            setmDataReady(true);
          }
          CsvImportDataFragment df = (CsvImportDataFragment) getSupportFragmentManager().findFragmentByTag(
              mSectionsPagerAdapter.getFragmentName(1));
          if (df != null) df.setData((ArrayList<CSVRecord>) result);
          getSupportActionBar().setSelectedNavigationItem(1);
        } else {
          Toast.makeText(this, R.string.parse_error_no_data_found, Toast.LENGTH_LONG).show();
        }
        break;
      case TaskExecutionFragment.TASK_CSV_IMPORT:
        Result r = (Result) result;
        if (r.success) {
          if (!mUsageRecorded) {
            recordUsage(ContribFeature.CSV_IMPORT);
            mUsageRecorded = true;
          }
          Integer imported = (Integer) r.extra[0];
          Integer failed = (Integer) r.extra[1];
          Integer discarded = (Integer) r.extra[2];
          String label = (String) r.extra[3];
          String msg = getString(R.string.import_transactions_success, imported, label) + ".";
          if (failed>0) {
            msg += " " + getString(R.string.csv_import_records_failed,failed);
          }
          if (discarded>0) {
            msg += " " + getString(R.string.csv_import_records_discarded,discarded);
          }
          Toast.makeText(this, msg,Toast.LENGTH_LONG).show();
        }
    }
  }

  @Override
  public void onProgressUpdate(Object progress) {
    if (progress instanceof String) {
      Toast.makeText(this, (String) progress, Toast.LENGTH_LONG).show();
    } else {
      super.onProgressUpdate(progress);
    }
  }

  private void addTab(int index) {
    final ActionBar actionBar = getSupportActionBar();
    actionBar.addTab(
        actionBar.newTab()
            .setText(mSectionsPagerAdapter.getPageTitle(index))
            .setTabListener(this));
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(KEY_DATA_READY, mDataReady);
    outState.putBoolean(KEY_USAGE_RECORDED,mUsageRecorded);
  }

  public long getAccountId() {
    CsvImportParseFragment pf = (CsvImportParseFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(0));
    return pf.getAccountId();
  }

  public String getCurrency() {
    CsvImportParseFragment pf = (CsvImportParseFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(0));
    return pf.getCurrency();
  }

  public QifDateFormat getDateFormat() {
    CsvImportParseFragment pf = (CsvImportParseFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(0));
    return pf.getDateFormat();
  }

  public Account.Type getAccountType() {
    CsvImportParseFragment pf = (CsvImportParseFragment) getSupportFragmentManager().findFragmentByTag(
        mSectionsPagerAdapter.getFragmentName(0));
    return pf.getAccountType();
  }

}
