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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.widget.AbstractWidget;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;

public abstract class AmountActivity extends EditActivity {
  protected DecimalFormat nfDLocal;
  protected EditText mAmountText;
  public static final boolean INCOME = true;
  public static final boolean EXPENSE = false;
  //stores if we deal with an EXPENSE or an INCOME
  protected boolean mType = EXPENSE;
  protected CompoundButton mTypeButton;
  protected TextView mAmountLabel;

  @Override
  public void setContentView(int layoutResID) {
    super.setContentView(layoutResID);
    mAmountLabel = (TextView) findViewById(R.id.AmountLabel);
    mAmountText = (EditText) findViewById(R.id.Amount);
  }

  /**
   * configures the decimal format and the amount EditText based on configured
   * currency_decimal_separator 
   * @param fractionDigits 
   */
  protected void configAmountInput(int fractionDigits) {
    char decimalSeparator = Utils.getDefaultDecimalSeparator();
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setDecimalSeparator(decimalSeparator);
    String pattern = "#0";
    if (fractionDigits>0) {
      pattern += "." + new String(new char[fractionDigits]).replace("\0", "#");
    }
    nfDLocal = new DecimalFormat(pattern,symbols);
    nfDLocal.setGroupingUsed(false);
    Utils.configDecimalSeparator(mAmountText, decimalSeparator,fractionDigits);
  }

  /**
   * 
   */
  protected void configTypeButton() {
    mTypeButton = (CompoundButton) findViewById(R.id.TaType);
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (resultCode == RESULT_OK && requestCode == CALCULATOR_REQUEST && intent != null) {
      try {
        mAmountText.setText(nfDLocal.format(new BigDecimal(intent.getStringExtra(KEY_AMOUNT))));
        mAmountText.setError(null);
      } catch (Exception  e) {
        Utils.reportToAcra(e);
      }
    }
  }
  protected void onTypeChanged(boolean isChecked) {
    mType = isChecked;
    mIsDirty = true;
    configureType();
  }

  protected void configureType() {
    mTypeButton.setChecked(mType);
  }

  protected BigDecimal validateAmountInput(boolean showToUser) {
    String strAmount = mAmountText.getText().toString();
    if (strAmount.equals("")) {
      if (showToUser)
        mAmountText.setError(getString(R.string.no_amount_given));
      return null;
    }
    BigDecimal amount = Utils.validateNumber(nfDLocal, strAmount);
    if (amount == null) {
      if (showToUser)
        mAmountText.setError(getString(R.string.invalid_number_format,nfDLocal.format(11.11)));
      return null;
    }
    return amount;
  }
  public void showCalculator(View view) {
    if (mAmountText == null) {
      return;
    }
    Intent intent = new Intent(this,CalculatorInput.class);
    forwardDataEntryFromWidget(intent);
    BigDecimal amount = validateAmountInput(false);
    if (amount!=null) {
      intent.putExtra(KEY_AMOUNT,amount);
    }
    startActivityForResult(intent, CALCULATOR_REQUEST);
  }
  protected void forwardDataEntryFromWidget(Intent intent) {
    intent.putExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY,
        getIntent().getBooleanExtra(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, false));
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("type", mType);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    mType = savedInstanceState.getBoolean("type");
    configureType();
  }
  protected void setupListeners() {
    mAmountText.addTextChangedListener(this);
    mTypeButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        onTypeChanged(isChecked);
      }
    });
  }
  protected void linkInputsWithLabels() {
    linkInputWithLabel(mAmountText, mAmountLabel);
    linkInputWithLabel(mTypeButton, mAmountLabel);
    linkInputWithLabel(findViewById(R.id.Calculator),mAmountLabel);
  }
}