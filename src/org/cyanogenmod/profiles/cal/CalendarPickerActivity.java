package org.cyanogenmod.profiles.cal;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import org.cyanogenmod.profiles.R;

import java.util.Set;

public class CalendarPickerActivity extends Activity {

    private static final String TAG = CalendarPickerActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, new CalendarPickerFragment(), "cal")
                    .commit();
        }

    }

    public static class CalendarPickerFragment extends Fragment
            implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

        public CalendarPickerFragment() {
            super();
        }

        public String[] mFromColumns = {
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME
        };
        public int[] mToFields = {
                android.R.id.text1, android.R.id.text2
        };

        CheckedCursorAdapter mAdapter;
        ListView mListView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mAdapter = new CheckedCursorAdapter(
                    getActivity(), R.layout.calendar_row,
                    null, mFromColumns, mToFields
            );
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 Bundle savedInstanceState) {
            getLoaderManager().initLoader(0, null, this);

            View view = inflater.inflate(R.layout.dialog_content, container, false);

            ListView list = mListView = (ListView) view.findViewById(R.id.list);
            list.setAdapter(mAdapter);
            list.setOnItemClickListener(this);
            list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            list.setItemsCanFocus(false);


            return view;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case 0:
                    return new CursorLoader(
                            getActivity(),
                            CalendarContract.Calendars.CONTENT_URI,
                            new String[]{
                                    CalendarContract.Calendars._ID,
                                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                                    CalendarContract.Calendars.ACCOUNT_NAME,
                                    CalendarContract.Calendars.OWNER_ACCOUNT
                            },
                            null, // selection
                            null, // selection args
                            null // sorting order
                    );
                default:
                    break;
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.changeCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.changeCursor(null);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            CheckedTextView checkbox = (CheckedTextView) view.findViewById(android.R.id.text1);
            checkbox.toggle();
            mAdapter.notifyDataSetChanged();

            TextView text = (TextView) view.findViewById(android.R.id.text2);

            SharedPreferences prefs = getActivity().getSharedPreferences(
                    CalendarServiceObserver.PREFS, 0);
            Set<String> stringSet = prefs.getStringSet(CalendarServiceObserver.PREFS_CALENDARS,
                    new ArraySet<String>());

            CalendarRowWrapper wrapper = (CalendarRowWrapper) view.getTag();

            if (checkbox.isChecked()) {
                stringSet.add(wrapper.getCombinedId());
            } else {
                stringSet.remove(wrapper.getCombinedId());
            }
            prefs
                    .edit()
                    .putStringSet(CalendarServiceObserver.PREFS_CALENDARS, stringSet)
                    .commit();

            getActivity().startService(new Intent(getActivity(), CalendarServiceObserver.class));
        }

        public class CheckedCursorAdapter extends SimpleCursorAdapter {



            public CheckedCursorAdapter(Activity context, int rowContact, Cursor cursor,
                                        String[] strings, int[] is) {
                super(context, rowContact, cursor, strings, is, 0);
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;
                CalendarRowWrapper wrapper;

                if (row == null) {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    row = inflater.inflate(R.layout.calendar_row, null);
                    //
                    wrapper = new CalendarRowWrapper(row);
                    row.setTag(wrapper);
                } else {
                    wrapper = (CalendarRowWrapper) row.getTag();
                }
                getCursor().moveToPosition(position);
                String calendarAccount = mCursor.getString(
                        mCursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME));
                String ownerAccount = mCursor.getString(
                        mCursor.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)
                );
                wrapper.getTextView().setText(calendarAccount);
                wrapper.getCheckBox().setText(mCursor.getString(
                        mCursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)));

                wrapper.setCalendarAccountName(calendarAccount);
                wrapper.setCalendarOwner(ownerAccount);

                wrapper.getCheckBox().setChecked(mListView.isItemChecked(position) ||
                        isCalendarSelected(wrapper));
                return row;
            }

            private boolean isCalendarSelected(CalendarRowWrapper cal) {
                SharedPreferences prefs = getActivity().getSharedPreferences(
                        CalendarServiceObserver.PREFS, 0);

                return prefs.getStringSet(CalendarServiceObserver.PREFS_CALENDARS,
                        new ArraySet<String>()).contains(cal.getCombinedId());
            }
        }

        public static class CalendarRowWrapper {
            TextView text1;
            CheckedTextView checkBox;

            private String mCalendarOwner;
            private String mCalendarAccountName;

            public CalendarRowWrapper(View view) {
                checkBox = (CheckedTextView) view.findViewById(android.R.id.text1);
                text1 = (TextView) view.findViewById(android.R.id.text2);
            }

            public void setCalendarOwner(String owner) {
                mCalendarOwner = owner;
            }

            public String getCalendarOwner() {
                return mCalendarOwner;
            }

            public void setCalendarAccountName(String name) {
                mCalendarAccountName = name;
            }

            public String getCalendarAccountName() {
                return mCalendarAccountName;
            }

            public TextView getTextView() {
                return text1;
            }

            public CheckedTextView getCheckBox() {
                return checkBox;
            }

            public String getCombinedId() {
                return getCalendarOwner() + "/" + getCalendarAccountName();
            }
        }
    }


}
