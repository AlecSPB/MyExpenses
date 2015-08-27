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

package org.totschnig.myexpenses.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * Util class with helper methods
 * @author Michael Totschnig
 *
 */
public class DialogUtils {
  /**
   * @return Dialog to be used from Preference,
   * and from version update
   */
  public static Dialog sendWithFTPDialog(final Activity ctx) {
    return new AlertDialog.Builder(ctx)
    .setMessage(R.string.no_app_handling_ftp_available)
    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
           ctx.dismissDialog(R.id.FTP_DIALOG);
           Intent intent = new Intent(Intent.ACTION_VIEW);
           intent.setData(Uri.parse(MyApplication.getMarketPrefix() + "org.totschnig.sendwithftp"));
           if (Utils.isIntentAvailable(ctx,intent)) {
             ctx.startActivity(intent);
           } else {
             Toast.makeText(
                 ctx.getBaseContext(),
                 R.string.error_accessing_market,
                 Toast.LENGTH_LONG)
               .show();
           }
         }
      })
    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        ctx.dismissDialog(R.id.FTP_DIALOG);
      }
    }).create();
  }

  public static void showPasswordDialog(Activity ctx,AlertDialog dialog) {
    ctx.findViewById(android.R.id.content).setVisibility(View.GONE);
    if (ctx instanceof ActionBarActivity) {
      ((ActionBarActivity) ctx).getSupportActionBar().hide();
    }
    dialog.show();
    PasswordDialogListener l = new PasswordDialogListener(ctx,dialog);
    Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
    if (b != null)
      b.setOnClickListener(l);
    b = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
    if (b != null)
      b.setOnClickListener(l);
  }
  public static AlertDialog passwordDialog(final Activity ctx) {
    final String securityQuestion = MyApplication.PrefKey.SECURITY_QUESTION.getString("");
    Context wrappedCtx = wrapContext2(ctx);
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.password_check, null);
    view.findViewById(R.id.password).setTag(Boolean.valueOf(false));
    AlertDialog.Builder builder = new AlertDialog.Builder(wrappedCtx)
      .setTitle(R.string.password_prompt)
      .setView(view)
      .setOnCancelListener(new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
          ctx.moveTaskToBack(true);
        }
      });
    if (ContribFeature.SECURITY_QUESTION.hasAccess() && !securityQuestion.equals("")) {
      builder.setNegativeButton(R.string.password_lost, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {}
      });
    }
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {}
    });
    return builder.create();
  }

  public static Uri handleFilenameRequestResult(
      Intent data, EditText mFilename, String typeName, UriTypePartChecker checker) {
    Uri mUri = data.getData();
    if (mUri != null) {
      Context context = MyApplication.getInstance();
      mFilename.setError(null);
      String displayName = getDisplayName(mUri);
      mFilename.setText(displayName);
      if (displayName == null) {
        mUri = null;
        //SecurityException raised during getDisplayName
        mFilename.setError("Error while retrieving document");
      } else {
        String type = context.getContentResolver().getType(mUri);
        if (type != null) {
          String[] typeParts = type.split("/");
          if (typeParts.length==0 ||
              !checker.checkTypeParts(typeParts)) {
            mUri = null;
            mFilename.setError(context.getString(R.string.import_source_select_error, typeName));
          }
        }
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mUri != null) {
        final int takeFlags = data.getFlags()
            & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
          //this probably will not succeed as long as we stick to ACTION_GET_CONTENT
          context.getContentResolver().takePersistableUriPermission(mUri, takeFlags);
        } catch (SecurityException e) {
          //Utils.reportToAcra(e);
        }
      }
    }
    return mUri;
  }
  public interface UriTypePartChecker {
    boolean checkTypeParts(String[] typeParts);
  }
  public static boolean checkTypePartsDefault(String[] typeParts) {
    return typeParts[0].equals("*") ||
        typeParts[0].equals("text") ||
        typeParts[0].equals("application");
  }
  //https://developer.android.com/guide/topics/providers/document-provider.html
  /**
   * @return display name for document stored at mUri.
   * Returns null if accessing mUri raises {@link SecurityException}
   */
  @SuppressLint("NewApi")
  public static String getDisplayName(Uri uri) {

    if (!"file".equalsIgnoreCase(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      // The query, since it only applies to a single document, will only return
      // one row. There's no need to filter, sort, or select fields, since we want
      // all fields for one document.
      try {
        Cursor cursor = MyApplication.getInstance().getContentResolver()
            .query(uri, null, null, null, null, null);

        if (cursor != null) {
          try {
            if (cursor.moveToFirst()) {
              // Note it's called "Display Name".  This is
              // provider-specific, and might not necessarily be the file name.
              int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
              if (columnIndex != -1) {
                return cursor.getString(columnIndex);
              }
            }
          } catch (Exception e) {}
          finally {
            cursor.close();
          }
        }
      } catch (SecurityException e) {
        //this can happen if the user has restored a backup and
        //we do not have a persistable permision
        return null;
      }
    }
    List<String> filePathSegments = uri.getPathSegments();
    if (filePathSegments.size()>0) {
      return filePathSegments.get(filePathSegments.size()-1);
    } else {
      return "UNKNOWN";
    }
  }

  static class PasswordDialogListener implements View.OnClickListener {
    private final AlertDialog dialog;
    private final Activity ctx;
    public PasswordDialogListener(Activity ctx, AlertDialog dialog) {
        this.dialog = dialog;
        this.ctx = ctx;
    }
    @Override
    public void onClick(View v) {
      final SharedPreferences settings = MyApplication.getInstance().getSettings();
      final String securityQuestion = MyApplication.PrefKey.SECURITY_QUESTION.getString("");
      EditText input = (EditText) dialog.findViewById(R.id.password);
      TextView error = (TextView) dialog.findViewById(R.id.passwordInvalid);
      if (v == dialog.getButton(AlertDialog.BUTTON_NEGATIVE)) {
        if ((Boolean) input.getTag()) {
          input.setTag(Boolean.valueOf(false));
          ((Button) v).setText(R.string.password_lost);
          dialog.setTitle(R.string.password_prompt);
        } else {
          input.setTag(Boolean.valueOf(true));
          dialog.setTitle(securityQuestion);
          ((Button) v).setText(android.R.string.cancel);
        }
      } else {
        String value = input.getText().toString();
        boolean isInSecurityQuestion = (Boolean) input.getTag();
        if (Utils.md5(value).equals(
            (isInSecurityQuestion ? MyApplication.PrefKey.SECURITY_ANSWER : MyApplication.PrefKey.SET_PASSWORD).getString(""))) {
          input.setText("");
          error.setText("");
          MyApplication.getInstance().setLocked(false);
          ctx.findViewById(android.R.id.content).setVisibility(View.VISIBLE);
          if (ctx instanceof ActionBarActivity) {
            ((ActionBarActivity) ctx).getSupportActionBar().show();
          }
          if (isInSecurityQuestion) {
            MyApplication.PrefKey.PERFORM_PROTECTION.putBoolean(false);
            Toast.makeText(ctx.getBaseContext(),R.string.password_disabled_reenable, Toast.LENGTH_LONG).show();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.password_lost);
            dialog.setTitle(R.string.password_prompt);
            input.setTag(Boolean.valueOf(false));
          }
          dialog.dismiss();
        } else {
          input.setText("");
          error.setText(isInSecurityQuestion ? R.string.password_security_answer_not_valid : R.string.password_not_valid);
        }
      }
    }
  }
  /**
   * @param ctx
   * @return Context wrapped with style AboutDialog in API 10 and lower
   * currently this is only needed in ExportDialogFragment, and makes
   * sure that RadioButtons get correct style
   */
  public static Context wrapContext1(Context ctx) {
    return Build.VERSION.SDK_INT < 11 ?
        wrapDialogTheme(ctx) : ctx;
  }
  /**
   * @param ctx
   * @return Context wrapped with current theme (dark or light) in API 11 and higher
   * Applying the dark/light theme only works starting from 11, below, the dialog uses a dark theme
   * this is necessary only when we are called from one of the transparent activities,
   * but does not harm in the other cases
   */
  public static Context wrapContext2(Context ctx) {
    return Build.VERSION.SDK_INT > 10 ?
        wrapAppTheme(ctx) : ctx;
  }

  /**
   *
   * @param ctx
   * @return for API 10 and lower, Context is wrapped as in {@link #wrapDialogTheme(Context)}, for 11 and higher
   * as in {@link #wrapAppTheme(Context)}. This is needed for Dialogs that both are used in a transparent
   * activity, and have checkboxes
   */
  public static Context wrapContext12(Context ctx) {
    return Build.VERSION.SDK_INT < 11 ?
        wrapDialogTheme(ctx) : wrapAppTheme(ctx);
  }

  private static ContextThemeWrapper wrapAppTheme(Context ctx) {
    return new ContextThemeWrapper(ctx, MyApplication.getThemeId());
  }
  private static ContextThemeWrapper wrapDialogTheme(Context ctx) {
    return new ContextThemeWrapper(ctx, R.style.AboutDialog);
  }

  public static RadioGroup configureCalendarRestoreStrategy(
      View view, final CalendarRestoreStrategyChangedListener dialog) {
    RadioGroup restorePlanStrategie = (RadioGroup) view.findViewById(R.id.restore_calendar_handling);
    restorePlanStrategie.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        dialog.onCheckedChanged();
      }
    });
    String calendarId = MyApplication.PrefKey.PLANNER_CALENDAR_ID.getString("-1");
    String calendarPath = MyApplication.PrefKey.PLANNER_CALENDAR_PATH.getString("");
    RadioButton configured = (RadioButton) view.findViewById(R.id.restore_calendar_handling_configured);
    if ((calendarId.equals("-1")) || calendarPath.equals("")) {
      configured.setEnabled(false);
    } else {
      configured.setText(configured.getText() + " (" + calendarPath + ")");
    }
    return restorePlanStrategie;
  }
  interface CalendarRestoreStrategyChangedListener {
    void onCheckedChanged();
  }

  public static Spinner configureDateFormat(View view, Context context, String prefName) {
    Spinner spinner = (Spinner) view.findViewById(R.id.DateFormat);
    ArrayAdapter<QifDateFormat> dateFormatAdapter =
        new ArrayAdapter<QifDateFormat>(
            context, android.R.layout.simple_spinner_item, QifDateFormat.values());
    dateFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(dateFormatAdapter);
    QifDateFormat qdf;
    try {
      qdf = QifDateFormat.valueOf(
          MyApplication.getInstance().getSettings()
              .getString(prefName, "EU"));
    } catch (IllegalArgumentException e) {
      qdf = QifDateFormat.EU;
    }
    spinner.setSelection(qdf.ordinal());
    return spinner;
  }

  public static Spinner configureEncoding(View view, Context context, String prefName) {
    Spinner spinner = (Spinner) view.findViewById(R.id.Encoding);
    spinner.setSelection(
        Arrays.asList(context.getResources().getStringArray(R.array.pref_qif_export_file_encoding))
            .indexOf(MyApplication.getInstance().getSettings()
                .getString(prefName, "UTF-8")));
    return spinner;
  }

  public static Spinner configureDelimiter(View view, Context context, String prefName) {
    Spinner spinner = (Spinner) view.findViewById(R.id.Delimiter);
    spinner.setSelection(
        Arrays.asList(context.getResources().getStringArray(R.array.pref_csv_import_delimiter_values))
            .indexOf(MyApplication.getInstance().getSettings()
                .getString(prefName, ",")));
    return spinner;
  }

  public static EditText configureFilename(View view) {
    EditText filename = (EditText) view.findViewById(R.id.Filename);
    filename.setEnabled(false);
    return filename;
  }

  public static Spinner configureCurrencySpinner(
      View view, Context context, AdapterView.OnItemSelectedListener listener) {
    Spinner spinner = (Spinner) view.findViewById(R.id.Currency);
    ArrayAdapter<Account.CurrencyEnum> curAdapter = new ArrayAdapter<Account.CurrencyEnum>(
        context, android.R.layout.simple_spinner_item, android.R.id.text1,Account.CurrencyEnum.values());
    curAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(curAdapter);
    spinner.setOnItemSelectedListener(listener);
    spinner.setSelection(
        Account.CurrencyEnum
            .valueOf(Account.getLocaleCurrency().getCurrencyCode())
            .ordinal());
    return spinner;
  }

  public static Spinner configureTypeSpinner(View view, Context context) {
    Spinner spinner = (Spinner) view.findViewById(R.id.AccountType);
    ArrayAdapter<Account.Type> typAdapter = new ArrayAdapter<Account.Type>(
        context, android.R.layout.simple_spinner_item, android.R.id.text1,Account.Type.values());
    typAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(typAdapter);
    return spinner;
  }

  public static void openBrowse(Uri uri, Fragment fragment) {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);//TODO implement preference that allows to use ACTION_OPEN_DOCUMENT
    intent.addCategory(Intent.CATEGORY_OPENABLE);

    intent.setDataAndType(uri,"*/*");

    try {
      fragment.startActivityForResult(intent, ProtectedFragmentActivity.IMPORT_FILENAME_REQUESTCODE);
    } catch (ActivityNotFoundException e) {
      // No compatible file manager was found.
      Toast.makeText(fragment.getActivity(), R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
    } catch(SecurityException ex) {
      Toast.makeText(fragment.getActivity(),
          String.format(
              "Sorry, this destination does not accept %s request. Please select a different one.",intent.getAction()),
          Toast.LENGTH_SHORT)
          .show();
    }
  }
}
