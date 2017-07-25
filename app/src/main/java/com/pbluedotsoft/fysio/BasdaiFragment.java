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

import java.util.Locale;

/**
 * Created by Daniel Ibanez on 2016-11-04.
 */

public class BasdaiFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {

    private static final String LOG_TAG = BasdaiFragment.class.getSimpleName();

    private static int N_QUESTIONS = 6;

    // Save state constant
    private static final String STATE_CONTENT = "state_content";

    private SeekBar mSlider[];
    private TextView mTvResult;

    private Uri mTestUri;
    private int mTab, mInOut;

    public BasdaiFragment() {
        mSlider = new SeekBar[N_QUESTIONS];
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

        final View rootView = inflater.inflate(R.layout.fragment_basdai, container, false);

        // Sliders
        mSlider[0] = (SeekBar) rootView.findViewById(R.id.basdai_slider1);
        mSlider[1] = (SeekBar) rootView.findViewById(R.id.basdai_slider2);
        mSlider[2] = (SeekBar) rootView.findViewById(R.id.basdai_slider3);
        mSlider[3] = (SeekBar) rootView.findViewById(R.id.basdai_slider4);
        mSlider[4] = (SeekBar) rootView.findViewById(R.id.basdai_slider5);
        mSlider[5] = (SeekBar) rootView.findViewById(R.id.basdai_slider6);

        for (SeekBar s : mSlider) {
            s.setOnSeekBarChangeListener(this);
        }

        // IN or OUT background color adjustments
        ScrollView scroll = (ScrollView) rootView.findViewById(R.id.scrollview);
        if (mTab == Test.IN) {
            scroll.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_in));
            for (int i = 0; i < N_QUESTIONS - 1; i++) {
                mSlider[i].setProgressDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.seek_bar_progress_in));
            }

        } else {
            scroll.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_out));
            for (int i = 0; i < N_QUESTIONS - 1; i++) {
                mSlider[i].setProgressDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.seek_bar_progress_out));
            }
        }

        // Disable touch events in the 'other' tab
        if ((mTab == Test.IN && mInOut == 1) || (mTab == Test.OUT && mInOut == 0)) {
            disableTouchOnLayout(scroll);
        }

        // Layout background listener closes soft keyboard
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.basdai_layout_background);
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

        mTvResult = (TextView) rootView.findViewById(R.id.basdai_result);

        // Done button
        Button doneBtn = (Button) rootView.findViewById(R.id.basdai_btnDone);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
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
            for (int i = 0; i < N_QUESTIONS; i++) {
                mSlider[i].setProgress(Integer.parseInt(content[i]));
            }
        }

        // Calculate average and update result text view
        calculate();

        // Inform parent activity that form is up to date
        ((TestActivity) getActivity()).setUserHasSaved(true);

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

    public boolean saveToDatabase() {
        ContentValues values = new ContentValues();
        if (mTab == Test.IN) {
            values.put(TestEntry.COLUMN_CONTENT_IN, generateContent());
            values.put(TestEntry.COLUMN_RESULT_IN, calculate());
            values.put(TestEntry.COLUMN_STATUS_IN, Test.COMPLETED);
        } else {
            values.put(TestEntry.COLUMN_CONTENT_OUT, generateContent());
            values.put(TestEntry.COLUMN_RESULT_OUT, calculate());
            values.put(TestEntry.COLUMN_STATUS_OUT, Test.COMPLETED);
        }

        int rows = getActivity().getContentResolver().update(mTestUri, values, null, null);
        Log.d(LOG_TAG, "rows updated: " + rows);

        return true;
    }

    private String calculate() {
        float sum = mSlider[0].getProgress();
        sum += mSlider[1].getProgress();
        sum += mSlider[2].getProgress();
        sum += mSlider[3].getProgress();
        sum /= 10;

        // Convert slider from 8 units to 10
        float convertSix = mSlider[5].getProgress() * 10 / 8;
        // Average sliders 5 and 6
        sum += (mSlider[4].getProgress() / 10 + convertSix) / 2;

        String result = String.format(Locale.ENGLISH, "%.1f", sum / 5);
        mTvResult.setText(result);

        return result;
    }

    /**
     * @return String content representing state of views in layout
     */
    private String generateContent() {
        // Create content
        StringBuilder builder = new StringBuilder();
        for (SeekBar slider : mSlider) {
            builder.append(String.valueOf(slider.getProgress()));
            builder.append("|");
        }

        Log.d(LOG_TAG, "content: " + builder.toString());

        return builder.toString();
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Update result text view
        calculate();

        // Inform parent activity that form is outdated
        ((TestActivity) getActivity()).setUserHasSaved(false);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    /**
     * Disable all views in a given layout
     */
    private void disableTouchOnLayout(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            child.setFocusable(false);  // needed for EditText
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
