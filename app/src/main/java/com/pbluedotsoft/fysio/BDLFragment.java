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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pbluedotsoft.fysio.data.DbContract.TestEntry;
import com.pbluedotsoft.fysio.data.EXTRAS;
import com.pbluedotsoft.fysio.data.Test;

/**
 * A simple {@link Fragment} subclass.
 */
public class BDLFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {

    private static final String LOG_TAG = BDLFragment.class.getSimpleName();
    private static final int N_QUESTIONS = 11;

    // Save state constant
    private static final String STATE_CONTENT = "state_content";
    private static final String STATE_RESULT = "state_result";
    private static final String STATE_HIGH_ON = "state_high_on";

    private static boolean mMissingAnswers[];    // help to highlight mMissingAnswers answers
    private RadioGroup mRgroup[];
    private TextView mTVgroup[];
    private TextView mTvResult;

    private Uri mTestUri;
    private int mTab, mInOut;
    private int mResult;

    private View mRootView;
    private boolean mHighlightsON;

    public BDLFragment() {
        mRgroup = new RadioGroup[N_QUESTIONS];
        mTVgroup = new TextView[N_QUESTIONS];
        mResult = -1;
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


        mRootView = inflater.inflate(R.layout.fragment_bdl, container, false);

        // IN or OUT background color adjustments
        ScrollView scroll = (ScrollView) mRootView.findViewById(R.id.scrollview);
        if (mTab == Test.IN) {
            scroll.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_in));

        } else {
            scroll.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_out));
        }

        // Disable touch events in the 'other' tab
        if ((mTab == Test.IN && mInOut == 1) || (mTab == Test.OUT && mInOut == 0)) {
            disableTouchOnLayout(scroll);
        }

        // Layout background listener closes soft keyboard
        LinearLayout layout = (LinearLayout) mRootView.findViewById(R.id.bdl_layout_background);
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

        // Hook up radio groups from view
        mRgroup[0] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg1);
        mRgroup[1] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg2a);
        mRgroup[2] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg2b);
        mRgroup[3] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg3);
        mRgroup[4] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg4);
        mRgroup[5] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg5);
        mRgroup[6] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg6);
        mRgroup[7] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg7);
        mRgroup[8] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg8);
        mRgroup[9] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg9);
        mRgroup[10] = (RadioGroup) mRootView.findViewById(R.id.bdl_rg10);

        // Listeners
        for (int i = 0; i < mRgroup.length; i++) {
            mRgroup[i].setOnCheckedChangeListener(this);
        }

        mTvResult = (TextView) mRootView.findViewById(R.id.bdl_total_sum_tv);

        // Done button
        Button button = (Button) mRootView.findViewById(R.id.bdl_btnDone);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Save to database: return false if test incomplete
                if (!saveToDatabase()) {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
                    dialog.setMessage(getResources().getString(R.string.test_saved_incomplete));
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "VISA", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mHighlightsON = true;
                            highlightQuestions();
                        }
                    });
                    dialog.show();
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
                    dialog.setMessage(getResources().getString(R.string.test_saved_complete));
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            highlightQuestions(); // clear  highlights
                        }
                    });
                    dialog.show();
                }

                if (mResult != -1)
                    mTvResult.setText(String.valueOf(mResult));

                // Inform parent activity
                ((TestActivity) getActivity()).setUserHasSaved(true);
            }
        });

        // Get content from either saved instance OR database
        String contentStr;
        if (savedInstanceState != null) {
            // onRestoreInstanceState
            contentStr = savedInstanceState.getString(STATE_CONTENT);
            mResult = savedInstanceState.getInt(STATE_RESULT);
            mHighlightsON = savedInstanceState.getBoolean(STATE_HIGH_ON);
            if (mHighlightsON) {
                calculateSum(); // updates missing[] -> needed for highlighting
                highlightQuestions();
            }
            Log.d(LOG_TAG, "Content from savedInstance: " + contentStr);
        } else {
            // Read test content from database
            Cursor cursor = getActivity().getContentResolver().query(mTestUri, null, null, null, null);
            // Early exit: should never happen
            if (cursor == null || cursor.getCount() == 0) {
                return mRootView;
            }
            cursor.moveToFirst();
            if (mTab == Test.IN) {
                contentStr = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
                mResult = Integer.parseInt(
                        cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN)));
            } else {
                contentStr = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
                mResult = Integer.parseInt(
                        cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT)));
            }

            cursor.close();
            Log.d(LOG_TAG, "Content from database: " + contentStr);
        }

        // Content can be null. Database 'content_in' and 'content_out' are null when first created
        if (contentStr != null) {
            // Set radio buttons and total sum using info from content
            String[] content = contentStr.split("\\|");
            RadioButton radioButton;
            for (int i = 0; i < N_QUESTIONS; i++) {
                if (!content[i].trim().equals("-1")) {
                    int childIndex = Integer.parseInt(content[i].trim());
                    radioButton = (RadioButton) mRgroup[i].getChildAt(childIndex);
                    radioButton.setChecked(true);
                }
            }

            // Restore total sum. Important to check -1 after rotation
            if (mResult != -1)
                mTvResult.setText(String.valueOf(mResult));
        }

        // Inform parent activity that form is up to date
        ((TestActivity) getActivity()).setUserHasSaved(true);

        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save state for radio groups and total sum
        String content = generateContent();
        outState.putString(STATE_CONTENT, content);
        outState.putInt(STATE_RESULT, mResult);
        outState.putBoolean(STATE_HIGH_ON, mHighlightsON);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    /**
     * @return true if one or more radio groups have no selected radio button
     */
    private boolean missingAnswers() {
        int i = 0;
        while (i < N_QUESTIONS) {
            if (mRgroup[i].getCheckedRadioButtonId() == -1) {
                return true;
            }
            i++;
        }
        return false;
    }

    /**
     * Save index of selected radio button for each radio group
     *
     * @return String representing state for radio groups in layout
     */
    private String generateContent() {
        View radioButton;
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < N_QUESTIONS; i++) {
            int radioButtonID = mRgroup[i].getCheckedRadioButtonId();
            if (radioButtonID != -1) {
                radioButton = mRgroup[i].findViewById(radioButtonID);
                int index = mRgroup[i].indexOfChild(radioButton);
                contentBuilder.append(index);
            } else {
                contentBuilder.append("-1");
            }

            contentBuilder.append("|");
        }

        Log.d(LOG_TAG, "generateContent: " + contentBuilder.toString());

        return contentBuilder.toString();
    }

    /**
     * @return true if all radio buttons are unselected
     */
    private boolean notEvenOneSelected() {
        int i = 0;
        while (i < N_QUESTIONS) {
            if (mRgroup[i].getCheckedRadioButtonId() != -1) {
                return false;
            }
            i++;
        }
        return true;
    }

    /**
     * Calculates the total sum of points
     *
     * @return total sum
     */
    private int calculateSum() {
        mMissingAnswers = new boolean[N_QUESTIONS];    // false by default
        View radioButton;
        int sum = 0;
        // Check all radio groups
        for (int i = 0; i < N_QUESTIONS; i++) {
            // Check index of selected radio button
            int radioButtonID = mRgroup[i].getCheckedRadioButtonId();
            if (radioButtonID != -1) {
                radioButton = mRgroup[i].findViewById(radioButtonID);
                int index = mRgroup[i].indexOfChild(radioButton);
                sum += index;
            } else {
                mMissingAnswers[i] = true;
            }
        }

        // Database result should remain -1 if no radio button is selected
        if (notEvenOneSelected()) {
            return -1;
        }

        return sum;
    }

    /**
     * Highlights unanswered question
     */
    private void highlightQuestions() {
        // Find text mRootView questions here to avoid slowing down fragment inflate
        if (mTVgroup[0] == null) {
            mTVgroup[0] = (TextView) mRootView.findViewById(R.id.bdl_tv_1);
            mTVgroup[1] = (TextView) mRootView.findViewById(R.id.bdl_tv_2a);
            mTVgroup[2] = (TextView) mRootView.findViewById(R.id.bdl_tv_2b);
            mTVgroup[3] = (TextView) mRootView.findViewById(R.id.bdl_tv_3);
            mTVgroup[4] = (TextView) mRootView.findViewById(R.id.bdl_tv_4);
            mTVgroup[5] = (TextView) mRootView.findViewById(R.id.bdl_tv_5);
            mTVgroup[6] = (TextView) mRootView.findViewById(R.id.bdl_tv_6);
            mTVgroup[7] = (TextView) mRootView.findViewById(R.id.bdl_tv_7);
            mTVgroup[8] = (TextView) mRootView.findViewById(R.id.bdl_tv_8);
            mTVgroup[9] = (TextView) mRootView.findViewById(R.id.bdl_tv_9);
            mTVgroup[10] = (TextView) mRootView.findViewById(R.id.bdl_tv_10);
        }

        for (int i = 0; i < N_QUESTIONS; i++) {
            if (mMissingAnswers[i] && mHighlightsON) {
                mTVgroup[i].setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
            } else {
                mTVgroup[i].setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
            }
        }
    }

    public boolean saveToDatabase() {
        // Test status
        boolean missing = missingAnswers();
        String outputResult = "-1";
        int status;
        if (missing) {
            status = Test.INCOMPLETED;
        } else {
            status = Test.COMPLETED;
            outputResult = String.valueOf(mResult);
        }

        // Calculate sum
        mResult = calculateSum();

        ContentValues values = new ContentValues();
        if (mTab == Test.IN) {
            values.put(TestEntry.COLUMN_CONTENT_IN, generateContent());
            values.put(TestEntry.COLUMN_RESULT_IN, outputResult);
            values.put(TestEntry.COLUMN_STATUS_IN, status);
        } else {
            values.put(TestEntry.COLUMN_CONTENT_OUT, generateContent());
            values.put(TestEntry.COLUMN_RESULT_OUT, outputResult);
            values.put(TestEntry.COLUMN_STATUS_OUT, status);
        }

        int rows = getActivity().getContentResolver().update(mTestUri, values, null, null);
        Log.d(LOG_TAG, "rows updated: " + rows);

        return !missing;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        mResult = calculateSum();
        if (mResult != -1)
            mTvResult.setText(String.valueOf(mResult));

        highlightQuestions();   // Dynamic highlighting

        // Inform parent activity that changes have been made
        ((TestActivity) getActivity()).setUserHasSaved(false);
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
