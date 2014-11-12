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

import java.io.Serializable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribIFace;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;

public class ContribDialogFragment extends CommitSafeDialogFragment implements DialogInterface.OnClickListener{
  Feature feature;
  int usagesLeft;

  public static final ContribDialogFragment newInstance(Feature feature, Serializable tag) {
    ContribDialogFragment dialogFragment = new ContribDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putSerializable(ContribInfoDialogActivity.KEY_FEATURE, feature);
    bundle.putSerializable(ContribInfoDialogActivity.KEY_TAG, tag);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      final Bundle bundle = getArguments();
      feature = (Feature) bundle.getSerializable(ContribInfoDialogActivity.KEY_FEATURE);
      usagesLeft = feature.usagesLeft();
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx  = getActivity();
    Resources res = getResources();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    CharSequence featureList = Utils.getContribFeatureLabelsAsFormattedList(ctx,feature);
    CharSequence featureDescription;
    if (feature.hasTrial)
      featureDescription = Html.fromHtml(
          getString(
              R.string.dialog_contrib_premium_feature,
              "<i>" + getString(res.getIdentifier(
                  "contrib_feature_" + feature + "_label", "string", ctx.getPackageName()))+"</i>") +
          (usagesLeft > 0 ?
              res.getQuantityString(R.plurals.dialog_contrib_usage_count, usagesLeft, usagesLeft) :
              getString(R.string.dialog_contrib_no_usages_left)));
    else
      featureDescription = getText(res.getIdentifier("contrib_feature_" + feature + "_description", "string", ctx.getPackageName()));
    CharSequence
      linefeed = Html.fromHtml("<br><br>"),
      message = TextUtils.concat(
        featureDescription," ",
        getText(R.string.dialog_contrib_reminder_remove_limitation)," ",
        getString(R.string.dialog_contrib_reminder_gain_access),
        linefeed,
        featureList);
    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(R.string.dialog_title_contrib_feature)
      .setMessage(message)
      .setNegativeButton(R.string.dialog_contrib_no, this)
      .setPositiveButton(R.string.dialog_contrib_yes, this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    ContribInfoDialogActivity ctx = (ContribInfoDialogActivity) getActivity();
    if (ctx==null) {
      return;
    }
    if (which == AlertDialog.BUTTON_POSITIVE) {
      ctx.contribBuyDo();
    } else {
      if (usagesLeft > 0) {
        ctx.contribFeatureCalled(feature, getArguments().getSerializable(ContribInfoDialogActivity.KEY_TAG));
      } else {
        ctx.contribFeatureNotCalled();
      }
    }
  }
  @Override
  public void onCancel (DialogInterface dialog) {
    ContribIFace ctx = (ContribIFace)getActivity();
    if (ctx!=null) {
      ctx.contribFeatureNotCalled();
    }
  }
}