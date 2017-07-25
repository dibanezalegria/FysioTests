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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pbluedotsoft.fysio.data.DbContract.TestEntry;
import com.pbluedotsoft.fysio.data.EXTRAS;
import com.pbluedotsoft.fysio.data.Test;

import java.util.Locale;

/**
 * Created by Daniel Ibanez on 2016-10-31.
 */

public class BasmiFragment extends Fragment implements TextWatcher {

    private static final String LOG_TAG = BasmiFragment.class.getSimpleName();

    // Save state constant
    private static final String STATE_CONTENT = "state_content";
    private static final String STATE_RESULT = "state_result";
    private static final String STATE_HIGH_ON = "state_high_on";

    private static final int N_QUESTIONS = 5;
    private Uri mTestUri;
    private int mTab, mInOut;
    private View mRootView;
    private String mResult;

    private EditText mEtH[], mEtV[], mEtM[], mEtP[];
    private TextView mTvQ[], mTvResult;

    private boolean mHighlightsON;

    public BasmiFragment() {
        mEtH = new EditText[N_QUESTIONS];
        mEtV = new EditText[N_QUESTIONS];
        mEtM = new EditText[N_QUESTIONS];
        mEtP = new EditText[N_QUESTIONS];
        mTvQ = new TextView[N_QUESTIONS];
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

        mRootView = inflater.inflate(R.layout.fragment_basmi, container, false);

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
        LinearLayout layout = (LinearLayout) mRootView.findViewById(R.id.basmi_layout_background);
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

        mTvQ[0] = (TextView) mRootView.findViewById(R.id.basmi_tv_q1);
        mTvQ[1] = (TextView) mRootView.findViewById(R.id.basmi_tv_q2);
        mTvQ[2] = (TextView) mRootView.findViewById(R.id.basmi_tv_q3);
        mTvQ[3] = (TextView) mRootView.findViewById(R.id.basmi_tv_q4);
        mTvQ[4] = (TextView) mRootView.findViewById(R.id.basmi_tv_q5);

        mEtH[0] = (EditText) mRootView.findViewById(R.id.basmi_et_h_q1);
        mEtV[0] = (EditText) mRootView.findViewById(R.id.basmi_et_v_q1);
        mEtM[0] = (EditText) mRootView.findViewById(R.id.basmi_et_medel_q1);
        mEtP[0] = (EditText) mRootView.findViewById(R.id.basmi_et_points_q1);

        mEtH[1] = (EditText) mRootView.findViewById(R.id.basmi_et_cm_q2);
        mEtP[1] = (EditText) mRootView.findViewById(R.id.basmi_et_points_q2);

        mEtH[2] = (EditText) mRootView.findViewById(R.id.basmi_et_h_q3);
        mEtV[2] = (EditText) mRootView.findViewById(R.id.basmi_et_v_q3);
        mEtM[2] = (EditText) mRootView.findViewById(R.id.basmi_et_medel_q3);
        mEtP[2] = (EditText) mRootView.findViewById(R.id.basmi_et_points_q3);

        mEtH[3] = (EditText) mRootView.findViewById(R.id.basmi_et_cm_q4);
        mEtP[3] = (EditText) mRootView.findViewById(R.id.basmi_et_points_q4);

        mEtH[4] = (EditText) mRootView.findViewById(R.id.basmi_et_cm_q5);
        mEtP[4] = (EditText) mRootView.findViewById(R.id.basmi_et_points_q5);

        // Listeners
        for (int i = 0; i < N_QUESTIONS; i++) {
            mEtH[i].addTextChangedListener(this);    // makes edit text non editable
            if (i == 0 || i == 2) {
                mEtV[i].addTextChangedListener(this);
                mEtM[i].setKeyListener(null);
            }
            mEtP[i].setKeyListener(null);
        }

        mTvResult = (TextView) mRootView.findViewById(R.id.basmi_total_sum_tv);

        Button button = (Button) mRootView.findViewById(R.id.basmi_btnDone);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save to database: return false if test incomplete
                if (!saveToDatabase()) {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
                    dialog.setMessage(getResources().getString(R.string.test_saved_incomplete));
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "VISA", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mHighlightsON = true;
                            highlightQuestions(markMissingAnswers());
                        }
                    });
                    dialog.show();
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
                    dialog.setMessage(getResources().getString(R.string.test_saved_complete));
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            highlightQuestions(markMissingAnswers()); // clear  highlights
                        }
                    });
                    dialog.show();
                }

                if (!mResult.equals("-1"))
                    mTvResult.setText(mResult);

                // Inform parent activity
                ((TestActivity) getActivity()).setUserHasSaved(true);
            }
        });

        // Get content from either saved instance OR database
        String contentStr;
        if (savedInstanceState != null) {
            // onRestoreInstanceState
            contentStr = savedInstanceState.getString(STATE_CONTENT);
            mResult = savedInstanceState.getString(STATE_RESULT);
            mHighlightsON = savedInstanceState.getBoolean(STATE_HIGH_ON);
            if (mHighlightsON) {
                highlightQuestions(markMissingAnswers());
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
                mResult = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            } else {
                contentStr = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
                mResult = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            }

            cursor.close();
            Log.d(LOG_TAG, "Content from database: " + contentStr);
        }

        // Content can be null. Database 'content_in' and 'content_out' are null when first created
        if (contentStr != null) {
            // Set radio buttons and total sum using info from content
            String[] content = contentStr.split("\\|");
            Log.d(LOG_TAG, "content length: " + content.length);
            int index = -1;
            for (int i = 0; i < N_QUESTIONS; i++) {
                mEtH[i].setText(content[++index]);
                if (i == 0 || i == 2) {
                    mEtV[i].setText(content[++index]);
                    mEtM[i].setText(content[++index]);
                }
                mEtP[i].setText(content[++index]);
            }

            // Restore total sum. Important to check -1 after rotation
            if (!mResult.equals("-1"))
                mTvResult.setText(mResult);
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
        outState.putString(STATE_RESULT, mResult);
        outState.putBoolean(STATE_HIGH_ON, mHighlightsON);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    /**
     * String representing state for all views in layout
     */
    private String generateContent() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < N_QUESTIONS; i++) {
            builder.append(mEtH[i].getText().toString());
            builder.append("|");
            if (i == 0 || i == 2) {
                builder.append(mEtV[i].getText().toString());
                builder.append("|");
                builder.append(mEtM[i].getText().toString());
                builder.append("|");
            }
            builder.append(mEtP[i].getText().toString());
            builder.append("|");
        }

        builder.append("*");   // fix splitting empty content

        Log.d(LOG_TAG, "generateContent: " + builder.toString());

        return builder.toString();
    }

    /**
     * Add points for each question and calculate mean value
     * Note: Only when all questions have a point value
     *
     * @return mean value of answers OR -1 if one or more questions have no answer
     */
    private String calculateSum() {
        if (!formIsComplete()) {
            return "-1";
        }

        int sum = 0;
        for (int i = 0; i < N_QUESTIONS; i++) {
            sum += Integer.parseInt(mEtP[i].getText().toString());
        }

        // Return mean value
        return String.format(Locale.ENGLISH, "%.1f", sum / 5.0f);
    }

    /**
     * @return true if all questions have 'points' result
     */
    private boolean formIsComplete() {
        int index = 0;
        while (index < N_QUESTIONS) {
            if (mEtP[index].getText().toString().length() == 0) {
                return false;
            }
            index++;
        }

        return true;
    }


    private boolean[] markMissingAnswers() {
        boolean miss[] = new boolean[N_QUESTIONS];
        for (int i = 0; i < N_QUESTIONS; i++) {
            if (mEtP[i].getText().toString().length() == 0) {
                miss[i] = true;
            }
        }
        return miss;
    }

    /**
     * Highlights unanswered question
     */
    private void highlightQuestions(boolean miss[]) {
        for (int i = 0; i < N_QUESTIONS; i++) {
            if (miss[i] && mHighlightsON) {
                mTvQ[i].setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
            } else {
                mTvQ[i].setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
            }
        }
    }

    /**
     * @return true if form is complete, false otherwise
     */
    public boolean saveToDatabase() {
        // Test status
        boolean complete = formIsComplete();
        mResult = calculateSum();   // -1 when form not complete
        int status;
        if (complete) {
            status = Test.COMPLETED;
            mTvResult.setText(mResult);
        } else {
            status = Test.INCOMPLETED;
        }

        ContentValues values = new ContentValues();
        if (mTab == Test.IN) {
            values.put(TestEntry.COLUMN_CONTENT_IN, generateContent());
            values.put(TestEntry.COLUMN_RESULT_IN, mResult);
            values.put(TestEntry.COLUMN_STATUS_IN, status);
        } else {
            values.put(TestEntry.COLUMN_CONTENT_OUT, generateContent());
            values.put(TestEntry.COLUMN_RESULT_OUT, mResult);
            values.put(TestEntry.COLUMN_STATUS_OUT, status);
        }

        int rows = getActivity().getContentResolver().update(mTestUri, values, null, null);
        Log.d(LOG_TAG, "rows updated: " + rows);

        return complete;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Set text views for mean and points if h and v have valid input
        try {
            for (int i = 0; i < N_QUESTIONS; i++) {
                if (i == 0 || i == 2) {
                    if (mEtH[i].getText().length() != 0 && mEtV[i].getText().length() != 0) {
                        float mean = (Float.parseFloat(mEtH[i].getText().toString()) +
                                Float.parseFloat(mEtV[i].getText().toString())) / 2.0f;

                        mEtM[i].setText(String.format(Locale.ENGLISH, "%.1f", mean));
                        mEtP[i].setText(String.valueOf(getPointsForMean(i, mean)));
                    } else {
                        mEtM[i].setText("");
                        mEtP[i].setText("");
                    }
                } else {
                    if (mEtH[i].getText().length() != 0) {
                        mEtP[i].setText(String.valueOf(getPointsForMean(i,
                                Float.parseFloat(mEtH[i].getText().toString()))));
                    } else {
                        mEtP[i].setText("");
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.d(LOG_TAG, "Number format exception");
        }

        highlightQuestions(markMissingAnswers());   // Dynamic highlighting

        // Update result UI if form complete
        if (formIsComplete()) {
            mResult = calculateSum();
            mTvResult.setText(mResult);
        } else {
            mTvResult.setText("");
        }

        // Inform parent activity that changes have been made
        ((TestActivity) getActivity()).setUserHasSaved(false);
    }

    /**
     * @return points for mean value for given question id
     */
    public int getPointsForMean(int question, float mean) {
        // Return points for mean value for given question id
        switch (question) {
            case 0: {
                if (mean > 85.0f)
                    return 0;
                else if (mean > 76.5f)
                    return 1;
                else if (mean > 68.0f)
                    return 2;
                else if (mean > 59.5f)
                    return 3;
                else if (mean > 51.0f)
                    return 4;
                else if (mean > 42.5f)
                    return 5;
                else if (mean > 34.0f)
                    return 6;
                else if (mean > 25.5f)
                    return 7;
                else if (mean > 17.0f)
                    return 8;
                else if (mean > 8.5f)
                    return 9;
                else
                    return 10;
            }
            case 1: {
                if (mean < 10.0f)
                    return 0;
                else if (mean < 13f)
                    return 1;
                else if (mean < 16f)
                    return 2;
                else if (mean < 19f)
                    return 3;
                else if (mean < 22f)
                    return 4;
                else if (mean < 25f)
                    return 5;
                else if (mean < 28f)
                    return 6;
                else if (mean < 31f)
                    return 7;
                else if (mean < 34f)
                    return 8;
                else if (mean < 37f)
                    return 9;
                else
                    return 10;
            }
            case 2: {
                if (mean > 20.0f)
                    return 0;
                else if (mean > 17.9f)
                    return 1;
                else if (mean > 15.8f)
                    return 2;
                else if (mean > 13.7f)
                    return 3;
                else if (mean > 11.6f)
                    return 4;
                else if (mean > 9.5f)
                    return 5;
                else if (mean > 7.4f)
                    return 6;
                else if (mean > 5.3f)
                    return 7;
                else if (mean > 3.2f)
                    return 8;
                else if (mean > 1.1f)
                    return 9;
                else
                    return 10;
            }
            case 3: {
                if (mean > 119.5f)
                    return 0;
                else if (mean > 109.5f)
                    return 1;
                else if (mean > 99.5f)
                    return 2;
                else if (mean > 89.5f)
                    return 3;
                else if (mean > 79.5f)
                    return 4;
                else if (mean > 69.5f)
                    return 5;
                else if (mean > 59.5f)
                    return 6;
                else if (mean > 49.5f)
                    return 7;
                else if (mean > 39.5f)
                    return 8;
                else if (mean >= 30f)
                    return 9;
                else
                    return 10;
            }

            case 4: {
                if (mean > 7.0f)
                    return 0;
                else if (mean > 6.3f)
                    return 1;
                else if (mean > 5.6f)
                    return 2;
                else if (mean > 4.9f)
                    return 3;
                else if (mean > 4.2f)
                    return 4;
                else if (mean > 3.5f)
                    return 5;
                else if (mean > 2.8f)
                    return 6;
                else if (mean > 2.1f)
                    return 7;
                else if (mean > 1.4f)
                    return 8;
                else if (mean > 0.7f)
                    return 9;
                else
                    return 10;
            }

            default:
                return -1;
        }
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


