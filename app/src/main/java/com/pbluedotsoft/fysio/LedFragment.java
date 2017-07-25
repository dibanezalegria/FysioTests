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

/**
 * A simple {@link Fragment} subclass.
 */
public class LedFragment extends Fragment implements TextWatcher {

    private static final String LOG_TAG = LedFragment.class.getSimpleName();

    private static final int N_QUESTIONS = 25;

    // Save state constant
    private static final String STATE_CONTENT = "state_content";
    private static final String STATE_HIGH_ON = "state_high_on";

    private EditText[] mEtH, mEtV;
    private TextView[] mTitles;

    private Uri mTestUri;
    private int mTab, mInOut;

    private View mRootView;
    private boolean mHighlightsON;

    public LedFragment() {
        mEtH = new EditText[N_QUESTIONS];
        mEtV = new EditText[N_QUESTIONS];
        mTitles = new TextView[N_QUESTIONS];
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

        mRootView = inflater.inflate(R.layout.fragment_ledstatus, container, false);

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
        LinearLayout layout = (LinearLayout) mRootView.findViewById(R.id.ledstatus_layout_background);
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

        setupViews();

        // Done button
        Button doneBtn = (Button) mRootView.findViewById(R.id.ledstatus_btnDone);
        doneBtn.setOnClickListener(new View.OnClickListener() {
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
                            highlight();
                        }
                    });
                    dialog.show();
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
                    dialog.setMessage(getResources().getString(R.string.test_saved_complete));
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            highlight(); // clear  highlights
                        }
                    });
                    dialog.show();
                }

                // Inform parent activity
                ((TestActivity) getActivity()).setUserHasSaved(true);
            }
        });

        // Get content from either saved instance OR database
        String contentStr;
        if (savedInstanceState != null) {
            // onRestoreInstanceState
            contentStr = savedInstanceState.getString(STATE_CONTENT);
            mHighlightsON = savedInstanceState.getBoolean(STATE_HIGH_ON);
            if (mHighlightsON) {
                highlight();
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
            } else {
                contentStr = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
            }

            // Content can be null. Database 'content_in' and 'content_out' are null when first created
            if (contentStr != null) {
                // Set edit text views
                String[] content = contentStr.split("\\|");
                int count = 0;
                for (int i = 0; i < N_QUESTIONS; i++) {
                    mEtH[i].setText(content[count++]);
                    mEtV[i].setText(content[count++]);
                }
            }

            cursor.close();
            Log.d(LOG_TAG, "Content from database: " + contentStr);
        }

        // Inform parent activity that form is up to date
        ((TestActivity) getActivity()).setUserHasSaved(true);

        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save state for views in layout
        String content = generateContent();
        outState.putString(STATE_CONTENT, content);
        outState.putBoolean(STATE_HIGH_ON, mHighlightsON);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    public boolean saveToDatabase() {
        Log.d(LOG_TAG, "saveToDatabase");
        ContentValues values = new ContentValues();

        // Test status
        int status;
        if (isComplete()) {
            status = Test.COMPLETED;
        } else {
            status = Test.INCOMPLETED;
        }

        // Values
        if (mTab == Test.IN) {
            values.put(TestEntry.COLUMN_CONTENT_IN, generateContent());
            values.put(TestEntry.COLUMN_STATUS_IN, status);
        } else {
            values.put(TestEntry.COLUMN_CONTENT_OUT, generateContent());
            values.put(TestEntry.COLUMN_STATUS_OUT, status);
        }

        int rows = getActivity().getContentResolver().update(mTestUri, values, null, null);
        Log.d(LOG_TAG, "rows updated: " + rows);

        return isComplete();
    }

    /**
     * Save content for each view in the layout
     *
     * @return String representing state for radio groups in layout
     */
    private String generateContent() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < N_QUESTIONS; i++) {
            builder.append(mEtH[i].getText().toString());
            builder.append("|");
            builder.append(mEtV[i].getText().toString());
            builder.append("|");
        }

        builder.append("0|");

        Log.d(LOG_TAG, "content: " + builder.toString());

        return builder.toString();
    }

    /**
     * Highlights unanswered question
     */
    private void highlight() {
        for (int i = 0; i < N_QUESTIONS; i++) {
            if (mEtH[i].getText().toString().length() == 0 ||
                    mEtV[i].getText().toString().length() == 0) {
                if (mHighlightsON) {
                    mTitles[i].setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
                }
            } else {
                mTitles[i].setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
            }
        }
    }

    private boolean isComplete() {
        for (int i = 0; i < N_QUESTIONS; i++) {
            if (mEtH[i].getText().toString().length() == 0 ||
                    mEtV[i].getText().toString().length() == 0) {
                return false;
            }
        }
        return true;
    }

    private void setupViews() {
        mEtH[0] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q1_flexion);
        mEtH[1] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q1_extension);
        mEtH[2] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q1_rotation_h);
        mEtH[3] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q1_latflex_h);

        mEtH[4] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_flexion_h);
        mEtH[5] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_extension_h);
        mEtH[6] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_abduktion_h);
        mEtH[7] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_utåtrotation_h);
        mEtH[8] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_inåtrotation_h);

        mEtH[9] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q3_flexion_h);
        mEtH[10] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q3_extension_h);
        mEtH[11] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q3_pronation_h);
        mEtH[12] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q3_supination_h);

        mEtH[13] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q4_dorsal_h);
        mEtH[14] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q4_volar_h);
        mEtH[15] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q4_knyt_h);

        mEtH[16] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_flexion_h);
        mEtH[17] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_extension_h);
        mEtH[18] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_abduktion_h);
        mEtH[19] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_utåtrotation_h);
        mEtH[20] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_inåtrotation_h);

        mEtH[21] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q6_flexion_h);
        mEtH[22] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q6_extension_h);

        mEtH[23] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q7_dorsal_h);
        mEtH[24] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q7_plantar_h);

        mEtV[0] = mEtH[0];
        mEtV[1] = mEtH[1];
        mEtV[2] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q1_rotation_v);
        mEtV[3] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q1_latflex_v);

        mEtV[4] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_flexion_v);
        mEtV[5] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_extension_v);
        mEtV[6] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_abduktion_v);
        mEtV[7] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_utåtrotation_v);
        mEtV[8] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q2_inåtrotation_v);

        mEtV[9] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q3_flexion_v);
        mEtV[10] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q3_extension_v);
        mEtV[11] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q3_pronation_v);
        mEtV[12] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q3_supination_v);

        mEtV[13] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q4_dorsal_v);
        mEtV[14] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q4_volar_v);
        mEtV[15] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q4_knyt_v);

        mEtV[16] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_flexion_v);
        mEtV[17] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_extension_v);
        mEtV[18] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_abduktion_v);
        mEtV[19] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_utåtrotation_v);
        mEtV[20] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q5_inåtrotation_v);

        mEtV[21] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q6_flexion_v);
        mEtV[22] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q6_extension_v);

        mEtV[23] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q7_dorsal_v);
        mEtV[24] = (EditText) mRootView.findViewById(R.id.ledstatus_et_q7_plantar_v);

        // Listeners
        for (int i = 0; i < N_QUESTIONS; i++) {
            mEtH[i].addTextChangedListener(this);
            mEtV[i].addTextChangedListener(this);
        }

        // Titles
        mTitles[0] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q1_flexion);
        mTitles[1] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q1_extension);
        mTitles[2] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q1_rotation);
        mTitles[3] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q1_latflex);
        mTitles[4] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q2_flexion);
        mTitles[5] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q2_extension);
        mTitles[6] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q2_abduktion);
        mTitles[7] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q2_utåtrotation);
        mTitles[8] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q2_inåtrotation);
        mTitles[9] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q3_flexion);
        mTitles[10] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q3_extension);
        mTitles[11] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q3_pronation);
        mTitles[12] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q3_supination);
        mTitles[13] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q4_dorsal);
        mTitles[14] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q4_volar);
        mTitles[15] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q4_knyt);
        mTitles[16] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q5_flexion);
        mTitles[17] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q5_extension);
        mTitles[18] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q5_abduktion);
        mTitles[19] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q5_utåtrotation);
        mTitles[20] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q5_inåtrotation);
        mTitles[21] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q6_flexion);
        mTitles[22] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q6_extension);
        mTitles[23] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q7_dorsal);
        mTitles[24] = (TextView) mRootView.findViewById(R.id.ledstatus_tv_q7_plantar);
    }

    /**
     * TextWatcher listener interface method implementation
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        highlight();   // Dynamic highlighting

        // Inform parent activity
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
