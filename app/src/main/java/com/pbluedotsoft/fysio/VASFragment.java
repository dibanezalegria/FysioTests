package com.pbluedotsoft.fysio;


import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.pbluedotsoft.fysio.data.DbContract.TestEntry;
import com.pbluedotsoft.fysio.data.EXTRAS;
import com.pbluedotsoft.fysio.data.Test;


/**
 * A simple {@link Fragment} subclass.
 */
public class VASFragment extends Fragment {

    private static final String LOG_TAG = VASFragment.class.getSimpleName();

    private static final int N_SLIDERS = 4;

    // Save state constant
    private static final String STATE_CONTENT = "state_content";

    private SeekBar[] mSeekBars;
    private TextView[] mTextViews;

    private Uri mTestUri;
    private int mTab, mInOut;

    public VASFragment() {
        mSeekBars = new SeekBar[N_SLIDERS];
        mTextViews = new TextView[N_SLIDERS];
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Test URI
        mTestUri = Uri.parse(getArguments().getString(EXTRAS.KEY_URI));

        // Tab (IN or OUT)
        mTab = getArguments().getInt(EXTRAS.KEY_TAB);

        // IN or OUT selected at TestListActivity
        mInOut = getArguments().getInt(EXTRAS.KEY_INOUT);

        final View rootView = inflater.inflate(R.layout.fragment_vas, container, false);

        // SeekBars
        mSeekBars[0] = (SeekBar) rootView.findViewById(R.id.seekbar_kondition);
        mSeekBars[1] = (SeekBar) rootView.findViewById(R.id.seekbar_smarta);
        mSeekBars[2] = (SeekBar) rootView.findViewById(R.id.seekbar_stelhet);
        mSeekBars[3] = (SeekBar) rootView.findViewById(R.id.seekbar_trotthet);

        // IN or OUT background color adjustments
        ScrollView scroll = (ScrollView) rootView.findViewById(R.id.scrollview);
        if (mTab == Test.IN) {
            scroll.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_in));
            for (SeekBar sb : mSeekBars) {
                sb.setProgressDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.seek_bar_progress_in));
            }

        } else {
            scroll.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_out));
            for (SeekBar sb : mSeekBars) {
                sb.setProgressDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.seek_bar_progress_out));
            }
        }

        // Disable touch events in the 'other' tab
        if ((mTab == Test.IN && mInOut == 1) || (mTab == Test.OUT && mInOut == 0)) {
            disableTouchOnLayout(scroll);
        }

        // Layout background listener closes soft keyboard
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.vas_layout_background);
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Hide soft keyboard
                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return false;
            }
        });

        mTextViews[0] = (TextView) rootView.findViewById(R.id.tv_kondition_value);
        mTextViews[1] = (TextView) rootView.findViewById(R.id.tv_smarta_value);
        mTextViews[2] = (TextView) rootView.findViewById(R.id.tv_stelhet_value);
        mTextViews[3] = (TextView) rootView.findViewById(R.id.tv_trotthet_value);

        /**
         * Listeners
         */
        for (int i = 0; i < mSeekBars.length; i++) {
            mTextViews[i].setText(String.valueOf(mSeekBars[i].getProgress()));
            final int index = i;
            mSeekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mTextViews[index].setText(String.valueOf(progress));
                    // Inform parent activity that form is outdated
                    ((TestActivity) getActivity()).setUserHasSaved(false);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        // Done button
        Button doneBtn = (Button) rootView.findViewById(R.id.btn_done);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToDatabase();
                // Inform parent activity
                ((TestActivity) getActivity()).setUserHasSaved(true);
                // Show dialog
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
                dialog.setMessage(getResources().getString(R.string.test_saved_complete));
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
                dialog.show();
            }
        });

        // Get content from either saved instance OR database
        String contentStr;
        if (savedInstanceState != null) {
            // onRestoreInstanceState
            contentStr = savedInstanceState.getString(STATE_CONTENT);
            Log.d(LOG_TAG, "Content from savedInstance: " + contentStr);
        } else {
            // Read test content from database
            Cursor cursor = getActivity().getContentResolver().query(mTestUri, null, null, null, null);
            // Early exit: should never happen
            if (cursor == null || cursor.getCount() == 0) {
                return rootView;
            }
            cursor.moveToFirst();
            if (mTab == Test.IN) {
                contentStr = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
            } else {
                contentStr = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
            }

            cursor.close();
            Log.d(LOG_TAG, "Content from database: " + contentStr);
        }

        // Content can be null. Database 'content_in' and 'content_out' are null when first created
        if (contentStr != null) {
            // Update sliders
            String[] content = contentStr.split("\\|");
            for (int i = 0; i < N_SLIDERS; i++) {
                mSeekBars[i].setProgress(Integer.parseInt(content[i]));
            }
        }

        // Inform parent activity that form is up to date
        ((TestActivity) getActivity()).setUserHasSaved(true);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save state for radio groups and total sum
        String content = generateContent();
        outState.putString(STATE_CONTENT, content);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    /**
     * @return String content representing state of views in layout
     */
    private String generateContent() {
        // Create content
        StringBuilder builder = new StringBuilder();
        for (SeekBar slider : mSeekBars) {
            builder.append(String.valueOf(slider.getProgress()));
            builder.append("|");
        }

        return builder.toString();
    }

    public boolean saveToDatabase() {
        ContentValues values = new ContentValues();
        if (mTab == Test.IN) {
            String content = generateContent();
            values.put(TestEntry.COLUMN_CONTENT_IN, content);
            values.put(TestEntry.COLUMN_RESULT_IN, content);
            values.put(TestEntry.COLUMN_STATUS_IN, Test.COMPLETED);
        } else {
            String content = generateContent();
            values.put(TestEntry.COLUMN_CONTENT_OUT, content);
            values.put(TestEntry.COLUMN_RESULT_OUT, content);
            values.put(TestEntry.COLUMN_STATUS_OUT, Test.COMPLETED);
        }

        int rows = getActivity().getContentResolver().update(mTestUri, values, null, null);
        Log.d(LOG_TAG, "rows updated: " + rows);

        return true;
    }

    /**
     * Disable all views in a given layout
     */
    private void disableTouchOnLayout(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            child.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });

            if (child instanceof ViewGroup) {
                disableTouchOnLayout((ViewGroup) child);
            }
        }
    }

}
