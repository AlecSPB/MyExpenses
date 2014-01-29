package org.totschnig.myexpenses.fragment;

import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;


/**
 * @author Michael Totschnig
 *  provide helper functionality to create a CAB for a ListView
 *  below HoneyComb a context menu is used instead
 */
public class ContextualActionBarFragment extends Fragment implements OnGroupClickListener, OnChildClickListener {
  protected int menuResource;
  protected ActionMode mActionMode;
  int expandableListSelectionType = ExpandableListView.PACKED_POSITION_TYPE_NULL;
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    String className = this.getClass().getSimpleName().toLowerCase(Locale.US);
    String resourceName = className+"_context";
    menuResource = getResources().getIdentifier(resourceName, "menu", activity.getPackageName());
  }
  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    if (!getUserVisibleHint())
      return false;
    int itemId = item.getItemId();
    ContextMenuInfo info = item.getMenuInfo();
    if (item.getGroupId()==R.id.MenuSingle || item.getGroupId()==R.id.MenuSingleChild) {
      return dispatchCommandSingle(itemId,info);
    } else {
      int position;
      long id;
      if (info instanceof AdapterContextMenuInfo) {
        position = ((AdapterContextMenuInfo) info).position;
        id = ((AdapterContextMenuInfo) info).id;
      } else {
        ExpandableListContextMenuInfo elcmi = (ExpandableListContextMenuInfo) info;
        long packedPosition = elcmi.packedPosition;
        ExpandableListView elv = (ExpandableListView) elcmi.targetView.getParent();
        position = elv.getFlatListPosition(packedPosition);
        id = elcmi.id;
      }
      SparseBooleanArray sba = new SparseBooleanArray();
      sba.put(position, true);
      return dispatchCommandMultiple(itemId,sba,new Long[]{id});
    }
  }
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    return ctx.dispatchCommand(command, info);
  }
  public boolean dispatchCommandMultiple(int command, SparseBooleanArray positions,Long[]itemIds) {
    ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    //we send only the positions to the default dispatch command mechanism,
    //but subclasses can provide a method that handles the itemIds
    return ctx.dispatchCommand(command, positions);
  }
  protected void inflateHelper(Menu menu) {
    MenuInflater inflater = getActivity().getMenuInflater();
    inflater.inflate(R.menu.common_context,menu);
    if (menuResource!=0)
      inflater.inflate(menuResource, menu);
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    inflateHelper(menu);
    super.onCreateContextMenu(menu, v, menuInfo);
    expandableListSelectionType = (menuInfo instanceof ExpandableListContextMenuInfo) ?
      ExpandableListView.getPackedPositionType(((ExpandableListContextMenuInfo) menuInfo).packedPosition) :
        ExpandableListView.PACKED_POSITION_TYPE_NULL;
    configureMenuLegacy(menu,menuInfo);
  }
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void registerForContextualActionBar(final ListView lv) {
    if (Build.VERSION.SDK_INT >= 11) {
      lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
      lv.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
          int count = lv.getCheckedItemCount();
          if (lv instanceof ExpandableListView &&
              count == 1) {
            expandableListSelectionType = ExpandableListView.getPackedPositionType(
                ((ExpandableListView) lv).getExpandableListPosition(position));
          }
          mode.setTitle(String.valueOf(count));
          configureMenu11(mode.getMenu(), count);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
          //After orientation change,
          //setting expandableListSelectionType, as tried in setExpandableListSelectionType
          //does not work, because getExpandableListPosition does not return the correct value
          //probably because the adapter has not yet been set up correctly
          //thus we default to PACKED_POSITION_TYPE_GROUP
          //this workaround works because orientation change collapses the groups
          //so we never restore the CAB for PACKED_POSITION_TYPE_CHILD
          expandableListSelectionType = (lv instanceof ExpandableListView) ?
              ExpandableListView.PACKED_POSITION_TYPE_GROUP :
              ExpandableListView.PACKED_POSITION_TYPE_NULL;
          inflateHelper(menu);
          mode.setTitle(String.valueOf(lv.getCheckedItemCount()));
          mActionMode = mode;
          return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
          configureMenu11(menu, lv.getCheckedItemCount());
          return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
          int itemId = item.getItemId();
          SparseBooleanArray checkedItemPositions = lv.getCheckedItemPositions();
          int checkedItemCount = checkedItemPositions.size();
          boolean result = false;
          if (checkedItemPositions != null) {
            if (item.getGroupId()==R.id.MenuSingle || item.getGroupId()==R.id.MenuSingleChild) {
              for (int i=0; i<checkedItemCount; i++) {
                if (checkedItemPositions.valueAt(i)) {
                  int position = checkedItemPositions.keyAt(i);
                  ContextMenu.ContextMenuInfo info;
                  long id;
                  if (lv instanceof ExpandableListView) {
                    long pos = ((ExpandableListView) lv).getExpandableListPosition(position);
                    int groupPos = ExpandableListView.getPackedPositionGroup(pos);
                    if (ExpandableListView.getPackedPositionType(pos) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                      id = ((ExpandableListView) lv).getExpandableListAdapter().getGroupId(groupPos);
                    } else {
                      int childPos = ExpandableListView.getPackedPositionChild(pos);
                      id = ((ExpandableListView) lv).getExpandableListAdapter().getChildId(groupPos,childPos);
                    }
                    //getChildAt returned null in some cases
                    //thus we decide to not rely on it 
                    info = new ExpandableListContextMenuInfo(null, pos, id);
                  } else {
                    View v = lv.getChildAt(position);
                    id = lv.getItemIdAtPosition(position);
                    info = new AdapterContextMenuInfo(v,position,id);
                  }
                  result = dispatchCommandSingle(itemId,info);
                  break;
                }
              }
            } else {
              Long[] itemIdsObj = new Long[checkedItemCount];
              if (lv instanceof ExpandableListView) {
                for(int i = 0; i < checkedItemCount; ++i) {
                  if (checkedItemPositions.valueAt(i)) {
                    int position = checkedItemPositions.keyAt(i);
                    long pos = ((ExpandableListView) lv).getExpandableListPosition(position);
                    int groupPos = ExpandableListView.getPackedPositionGroup(pos);
                    if (ExpandableListView.getPackedPositionType(pos) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                      itemIdsObj[i] = ((ExpandableListView) lv).getExpandableListAdapter().getGroupId(groupPos);
                    } else {
                      int childPos = ExpandableListView.getPackedPositionChild(pos);
                      itemIdsObj[i] = ((ExpandableListView) lv).getExpandableListAdapter().getChildId(groupPos,childPos);
                    }
                  }
                }
              } else {
                long[] itemIdsPrim = lv.getCheckedItemIds();
                for(int i = 0; i < checkedItemCount; ++i){
                  itemIdsObj[i] = itemIdsPrim[i];
                }
              }
              //TODO:should we convert the flat positions here?
              result = dispatchCommandMultiple(
                  itemId,
                  checkedItemPositions,
                  itemIdsObj);
            }
          }
          mode.finish();
          return result;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
          mActionMode = null;
        }
      });
    } else {
      registerForContextMenu(lv);
    }
    if (lv instanceof ExpandableListView) {
      final ExpandableListView elv = (ExpandableListView) lv;
      elv.setOnGroupClickListener(this);
      elv.setOnChildClickListener(this);
    }
  }
  @Override
  public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
      if (mActionMode != null)  {
        if (expandableListSelectionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
          int flatPosition = parent.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(groupPosition));
          parent.setItemChecked(
              flatPosition,
              !parent.isItemChecked(flatPosition));
          return true;
        }
      }
      return false;
  }
  @Override
  public boolean onChildClick(ExpandableListView parent, View v,
      int groupPosition, int childPosition, long id) {
    if (mActionMode != null)  {
      if (expandableListSelectionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
        int flatPosition = parent.getFlatListPosition(
            ExpandableListView.getPackedPositionForChild(groupPosition,childPosition));
        parent.setItemChecked(
            flatPosition,
            !parent.isItemChecked(flatPosition));
      }
      return true;
  }
  return false;
  }
  protected void configureMenuLegacy(Menu menu, ContextMenuInfo menuInfo) {
    configureMenu(menu,1);
  }
  protected void configureMenu11(Menu menu, int count) {
    configureMenu(menu,count);
  }
  protected void configureMenu(Menu menu, int count) {
    if (expandableListSelectionType != ExpandableListView.PACKED_POSITION_TYPE_NULL) {
      boolean inGroup = expandableListSelectionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP;
      menu.setGroupVisible(R.id.MenuBulk, inGroup);
      menu.setGroupVisible(R.id.MenuSingle, inGroup && count==1);
      menu.setGroupVisible(R.id.MenuBulkChild, !inGroup);
      menu.setGroupVisible(R.id.MenuSingleChild, !inGroup && count==1);
    } else {
      menu.setGroupVisible(R.id.MenuSingle,count==1);
    }
  }
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void finishActionMode() {
    if (mActionMode != null)
      mActionMode.finish();
  }
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void invalidateCAB() {
    if (mActionMode != null) {
      //upon orientation change when action mode is restores the cursor might not yet been loaded
      mActionMode.invalidate();
    }
  }
 /* protected void setExpandableListSelectionType(ExpandableListView elv) {
    SparseBooleanArray checkedItemPositions = elv.getCheckedItemPositions();
    int checkedItemCount;
    if (checkedItemPositions != null && (checkedItemCount = checkedItemPositions.size())>0) {
      for (int i=0; i<checkedItemCount; i++) {
        if (checkedItemPositions.valueAt(i)) {
          int position = checkedItemPositions.keyAt(i);
          long pos = elv.getExpandableListPosition(position);
          expandableListSelectionType = ExpandableListView.getPackedPositionType(pos);
          //After orientation change getExpandableListPosition does not return the correct value
          //probably because the adapter has not yet been set up correctly
          //in that case we wall back to PACKED_POSITION_TYPE_GROUP
          //this workaround works because orientation change collapses the groups
          //so we never restore the CAB for PACKED_POSITION_TYPE_CHILD
          if (expandableListSelectionType == ExpandableListView.PACKED_POSITION_TYPE_NULL)
            expandableListSelectionType = ExpandableListView.PACKED_POSITION_TYPE_GROUP;
          break;
        }
      }
    }
  }*/
}
