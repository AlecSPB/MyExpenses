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

package org.totschnig.myexpenses.model;

import java.util.Date;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

/**
 * Domain class for transactions
 * @author Michael Totschnig
 *
 */
public class Transaction extends Model {
  public Long id = 0L;
  public String comment="",label="",payee = "",referenceNumber="";
  protected Date date;
  public Money amount;
  public Long catId;
  //stores a short label of the category or the account the transaction is linked to
  public Long accountId;
  public Long transfer_peer;
  public Long transfer_account;
  public Long methodId;
  public Long parentId = null;
  /**
   * id of the template which defines the plan for which this transaction has been created
   */
  public Long originTemplateId = null;
  /**
   * id of an instance of the event (plan) for which this transaction has been created
   */
  public Long originPlanInstanceId = null;
  /**
   * 0 = is normal, special states are
   * {@link org.totschnig.myexpenses.provider.DatabaseConstants#STATUS_EXPORTED} and
   * {@link org.totschnig.myexpenses.provider.DatabaseConstants#STATUS_UNCOMMITTED}
   */
  public int status = 0;
  public static String[] PROJECTION_BASE, PROJECTION_EXTENDED;
  static {
    PROJECTION_BASE = new String[]{
        KEY_ROWID,
        KEY_DATE,
        KEY_AMOUNT,
        KEY_COMMENT,
        KEY_CATID,
        LABEL_MAIN,
        LABEL_SUB,
        KEY_PAYEE_NAME,
        KEY_TRANSFER_PEER,
        KEY_METHODID,
        KEY_CR_STATUS,
        KEY_REFERENCE_NUMBER,
        YEAR + " AS year",
        YEAR_OF_WEEK_START + " AS year_of_week_start",
        MONTH + " AS month",
        WEEK + " AS week",
        DAY + " AS day",
        THIS_YEAR + " AS this_year",
        THIS_YEAR_OF_WEEK_START + " AS this_year_of_week_start",
        THIS_WEEK + " AS this_week",
        THIS_DAY + " AS this_day",
        WEEK_START+ " AS week_start",
        WEEK_END +" AS week_end"
    };
    int baseLength = PROJECTION_BASE.length;
    PROJECTION_EXTENDED = new String[baseLength+2];
    System.arraycopy(PROJECTION_BASE, 0, PROJECTION_EXTENDED, 0, baseLength);
    PROJECTION_EXTENDED[baseLength] = KEY_COLOR;
    //the definition of column TRANSFER_PEER_PARENT refers to view_extended,
    //thus can not be used in PROJECTION_BASE
    PROJECTION_EXTENDED[baseLength+1] = TRANSFER_PEER_PARENT +" AS transfer_peer_parent";
  }
  public static final Uri CONTENT_URI = TransactionProvider.TRANSACTIONS_URI;

  public enum CrStatus {
    UNRECONCILED(Color.GRAY,""),CLEARED(Color.BLUE,"*"),RECONCILED(Color.GREEN,"X");
    public int color;
    public String symbol;
    private CrStatus(int color,String symbol) {
      this.color = color;
      this.symbol = symbol;
    }
    public String toString() {
      Context ctx = MyApplication.getInstance();
      switch (this) {
      case CLEARED:
        return ctx.getString(R.string.status_cleared);
      case RECONCILED:
        return ctx.getString(R.string.status_reconciled);
      case UNRECONCILED:
        return ctx.getString(R.string.status_uncreconciled);
      }
      return super.toString();
    }
    public static final String JOIN;
    static {
      JOIN = Utils.joinEnum(CrStatus.class);
    }
  }
  public CrStatus crStatus;

  /**
   * factory method for retrieving an instance from the db with the given id
   * @param mDbHelper
   * @param id
   * @return instance of {@link Transaction} or {@link Transfer} or null if not found
   */
  public static Transaction getInstanceFromDb(long id)  {
    Transaction t;
    String[] projection = new String[] {KEY_ROWID,KEY_DATE,KEY_AMOUNT,KEY_COMMENT, KEY_CATID,
        SHORT_LABEL,KEY_PAYEE_NAME,KEY_TRANSFER_PEER,KEY_TRANSFER_ACCOUNT,KEY_ACCOUNTID,KEY_METHODID,
        KEY_PARENTID,KEY_CR_STATUS,KEY_REFERENCE_NUMBER};

    Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection,null,null, null);
    if (c == null) {
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      return null;
    }
    c.moveToFirst();
    Long transfer_peer = DbUtils.getLongOrNull(c, KEY_TRANSFER_PEER);
    long account_id = c.getLong(c.getColumnIndexOrThrow(KEY_ACCOUNTID));
    long amount = c.getLong(c.getColumnIndexOrThrow(KEY_AMOUNT));
    Long parent_id = DbUtils.getLongOrNull(c, KEY_PARENTID);
    Long catId = DbUtils.getLongOrNull(c, KEY_CATID);
    if (transfer_peer != null) {
      t = parent_id != null ? new SplitPartTransfer(account_id,amount,parent_id) : new Transfer(account_id,amount);
    }
    else {
      if (catId == DatabaseConstants.SPLIT_CATID) {
        t = new SplitTransaction(account_id,amount);
      } else {
        t = parent_id != null ? new SplitPartCategory(account_id,amount,parent_id) : new Transaction(account_id,amount);
      }
    }
    try {
      t.crStatus = CrStatus.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_CR_STATUS)));
    } catch (IllegalArgumentException ex) {
      t.crStatus = CrStatus.UNRECONCILED;
    }
    t.methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
    t.catId = catId;
    t.payee = DbUtils.getString(c,KEY_PAYEE_NAME);
    t.transfer_peer = transfer_peer;
    t.transfer_account = DbUtils.getLongOrNull(c, KEY_TRANSFER_ACCOUNT);
    t.id = id;
    t.setDate(c.getLong(
        c.getColumnIndexOrThrow(KEY_DATE))*1000L);
    t.comment = DbUtils.getString(c,KEY_COMMENT);
    t.referenceNumber = DbUtils.getString(c, KEY_REFERENCE_NUMBER);
    t.label = DbUtils.getString(c,KEY_LABEL);
    c.close();
    return t;
  }
  public static Transaction getInstanceFromTemplate(long id) {
    Template te = Template.getInstanceFromDb(id);
    return te == null ? null : getInstanceFromTemplate(te);
  }
  public static Transaction getInstanceFromTemplate(Template te) {
    Transaction tr;
    if (te.isTransfer) {
      tr = new Transfer(te.accountId,te.amount);
      tr.transfer_account = te.transfer_account;
    }
    else {
      tr = new Transaction(te.accountId,te.amount);
      tr.methodId = te.methodId;
      tr.catId = te.catId;
    }
    tr.comment = te.comment;
    tr.payee = te.payee;
    tr.label = te.label;
    tr.originTemplateId = te.id;
    cr().update(
        TransactionProvider.TEMPLATES_URI.buildUpon().appendPath(String.valueOf(te.id)).appendPath("increaseUsage").build(),
        null, null, null);
    return tr;
  }

  /**
   * factory method for creating an object of the correct type and linked to a given account
   * @param operationType either {@link MyExpenses#TYPE_TRANSACTION} or
   * {@link MyExpenses#TYPE_TRANSFER} or {@link MyExpenses#TYPE_SPLIT}
   * SplitTransaction is persisted to DB as uncommitted
   * @param accountId the account the transaction belongs two
   * @param parentId if != 0L this is the id of a split part's parent
   * @return instance of {@link Transaction} or {@link Transfer} or {@link SplitTransaction} with date initialized to current date
   * if parentId == 0L, otherwise {@link SplitPartCategory} or {@link SplitPartTransfer}
   */
  public static Transaction getTypedNewInstance(int operationType, long accountId, Long parentId) {
    Account account = Account.getInstanceFromDb(accountId);
    if (account == null) {
      return null;
    }
    switch (operationType) {
    case MyExpenses.TYPE_TRANSACTION:
      return parentId != 0L ? new SplitPartCategory(account,0L,parentId) :  new Transaction(account,0L);
    case MyExpenses.TYPE_TRANSFER:
      return parentId != 0L ? new SplitPartTransfer(account,0L,parentId) : new Transfer(account,0L);
    case MyExpenses.TYPE_SPLIT:
      SplitTransaction t = new SplitTransaction(account,0L);
        t.status = STATUS_UNCOMMITTED;
        //TODO: Strict mode
        t.persistForEdit();
        return t;
    }
    return null;
  }
  public static Transaction getTypedNewInstance(int operationType, long accountId) {
    return getTypedNewInstance(operationType,accountId,0L);
  }

  public static void delete(long id) {
    /*    cr().delete(
        TransactionProvider.PLAN_INSTANCE_STATUS_URI,
        KEY_TRANSACTIONID + " = ?",
        new String[]{String.valueOf(id)});*/
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_STATUS, STATUS_DELETED);
    cr().update(ContentUris.appendId(CONTENT_URI.buildUpon(),id).build(), initialValues, null, null);
  }
  public static void undelete(long id) {
    /*    cr().delete(
        TransactionProvider.PLAN_INSTANCE_STATUS_URI,
        KEY_TRANSACTIONID + " = ?",
        new String[]{String.valueOf(id)});*/
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_STATUS, 0);
    cr().update(ContentUris.appendId(CONTENT_URI.buildUpon(),id).build(), initialValues, null, null);
  }

  //needed for Template subclass
  public Transaction() {
    setDate(new Date());
    this.crStatus = CrStatus.UNRECONCILED;
  }
  /**
   * new empty transaction
   * @param mDbHelper
   */
  public Transaction(long accountId,Long amount) {
    this(Account.getInstanceFromDb(accountId),amount);
  }
  public Transaction(long accountId,Money amount) {
    this();
    this.accountId = accountId;
    this.amount = amount;
  }
  public Transaction(Account account, long amount) {
    this();
    this.accountId = account.id;
    this.amount = new Money(account.currency,amount);
  }
  public void setDate(Date date){
    this.date = date;
  }
  private void setDate(Long unixEpoch) {
    this.date = new Date(unixEpoch);
  }
  public Date getDate() {
    return date;
  }
  /**
   * 
   * @param payee
   */
  public void setPayee(String payee) {
    this.payee = payee;
  }
  /**
   * Saves the transaction, creating it new if necessary
   * as a side effect calls {@link Payee#create(long, String)}
   * @return the URI of the transaction. Upon creation it is returned from the content provider
   */
  public Uri save() {
    Uri uri;
    Long payee_id = (payee != null && !payee.equals("")) ?
      Payee.require(payee) : null;
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_COMMENT, comment);
    initialValues.put(KEY_REFERENCE_NUMBER, referenceNumber);
    //store in UTC
    initialValues.put(KEY_DATE, date.getTime()/1000);

    initialValues.put(KEY_AMOUNT, amount.getAmountMinor());
    initialValues.put(KEY_CATID, catId);
    initialValues.put(KEY_PAYEEID, payee_id);
    initialValues.put(KEY_METHODID, methodId);
    initialValues.put(KEY_CR_STATUS,crStatus.name());
    initialValues.put(KEY_ACCOUNTID, accountId);
    if (id == 0) {
      initialValues.put(KEY_PARENTID, parentId);
      initialValues.put(KEY_STATUS, status);
      uri = cr().insert(CONTENT_URI, initialValues);
      if (uri==null)
        return null;
      id = ContentUris.parseId(uri);
      if (catId != null && catId != DatabaseConstants.SPLIT_CATID)
        cr().update(
            TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(String.valueOf(catId)).appendPath("increaseUsage").build(),
            null, null, null);
      if (parentId == null)
        cr().update(
            TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(String.valueOf(accountId)).appendPath("increaseUsage").build(),
            null, null, null);
      if (originPlanInstanceId != null) {
        ContentValues values = new ContentValues();
        values.put(KEY_TEMPLATEID, originTemplateId);
        values.put(KEY_INSTANCEID, originPlanInstanceId);
        values.put(KEY_TRANSACTIONID, id);
        cr().insert(TransactionProvider.PLAN_INSTANCE_STATUS_URI, values);
      }
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
      cr().update(uri,initialValues,null,null);
    }
    return uri;
  }
  public Uri saveAsNew() {
    id = 0L;
    setDate(new Date());
    Uri result = save();
    id = ContentUris.parseId(result);
    return result;
  }
  /**
   * @param whichTransactionId
   * @param whereAccountId
   * 
   */
  public static void move(long whichTransactionId, long whereAccountId) {
    ContentValues args = new ContentValues();
    args.put(KEY_ACCOUNTID, whereAccountId);
    cr().update(Uri.parse(CONTENT_URI + "/" + whichTransactionId + "/move/" + whereAccountId), null,null,null);
  }
  public static int count(Uri uri,String selection,String[] selectionArgs) {
    Cursor cursor = cr().query(uri,new String[] {"count(*)"},
        selection, selectionArgs, null);
    if (cursor.getCount() == 0) {
      cursor.close();
      return 0;
    } else {
      cursor.moveToFirst();
      int result = cursor.getInt(0);
      cursor.close();
      return result;
    }
  }
  public static int countAll(Uri uri) {
    return count(uri,null,null);
  }
  public static int countPerCategory(Uri uri,long catId) {
    return count(uri, KEY_CATID + " = ?",new String[] {String.valueOf(catId)});
  }
  public static int countPerMethod(Uri uri,long methodId) {
    return count(uri, KEY_METHODID + " = ?",new String[] {String.valueOf(methodId)});
  }
  public static int countPerAccount(Uri uri,long accountId) {
    return count(uri, KEY_ACCOUNTID + " = ?",new String[] {String.valueOf(accountId)});
  }
  public static int countPerCategory(long catId) {
    return countPerCategory(CONTENT_URI,catId);
  }
  public static int countPerMethod(long methodId) {
    return countPerMethod(CONTENT_URI,methodId);
  }
  public static int countPerAccount(long accountId) {
    return countPerAccount(CONTENT_URI,accountId);
  }
  public static int countAll() {
    return countAll(CONTENT_URI);
  }
  /**
   * @return the number of transactions that have been created since creation of the db based on sqllite sequence
   */
  public static Long getSequenceCount() {
    Cursor mCursor = cr().query(TransactionProvider.SQLITE_SEQUENCE_TRANSACTIONS_URI,
        null, null, null, null);
    if (mCursor.getCount() == 0)
      return 0L;
    mCursor.moveToFirst();
    Long result = mCursor.getLong(0);
    mCursor.close();
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Transaction other = (Transaction) obj;
    if (accountId == null) {
      if (other.accountId != null)
        return false;
    } else if (!accountId.equals(other.accountId))
      return false;
    if (amount == null) {
      if (other.amount != null)
        return false;
    } else if (!amount.equals(other.amount))
      return false;
    if (catId == null) {
      if (other.catId != null)
        return false;
    } else if (!catId.equals(other.catId))
      return false;
    if (comment == null) {
      if (other.comment != null)
        return false;
    } else if (!comment.equals(other.comment))
      return false;
    if (date == null) {
      if (other.date != null)
        return false;
    } else if (Math.abs(date.getTime()-other.date.getTime())>30000) //30 seconds tolerance
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (label == null) {
      if (other.label != null)
        return false;
    } else if (!label.equals(other.label))
      return false;
    if (methodId == null) {
      if (other.methodId != null)
        return false;
    } else if (!methodId.equals(other.methodId))
      return false;
    if (payee == null) {
      if (other.payee != null)
        return false;
    } else if (!payee.equals(other.payee))
      return false;
    if (transfer_account == null) {
      if (other.transfer_account != null)
        return false;
    } else if (!transfer_account.equals(other.transfer_account))
      return false;
    if (transfer_peer == null) {
      if (other.transfer_peer != null)
        return false;
    } else if (!transfer_peer.equals(other.transfer_peer))
      return false;
    return true;
  }
}
