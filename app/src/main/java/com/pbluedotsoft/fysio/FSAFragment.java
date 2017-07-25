package com.pbluedotsoft.fysio;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.pbluedotsoft.fysio.data.DbContract.TestEntry;
import com.pbluedotsoft.fysio.data.EXTRAS;
import com.pbluedotsoft.fysio.data.Test;
import com.pbluedotsoft.fysio.databinding.FragmentFsaBinding;

/**
 * Created by Daniel Ibanez on 2016-11-01.
 */

public class FSAFragment extends Fragment implements View.OnClickListener, TextWatcher {

    private static final String LOG_TAG = FSAFragment.class.getSimpleName();

    // Save state constant
    private static final String STATE_CONTENT = "state_content";
    private static final String STATE_HIGH_ON = "state_high_on";

    private static final int N_QUESTIONS = 5;
    private static final int N_ANSWERS = 6;
    private static final int N_SIDES = 2;

    private Uri mTestUri;
    private int mTab, mInOut;
    private boolean mHighlightsON;

    private CustomRadioGroup mCustomRG[][];
    private RadioButton mRB[][][];      // [question][answer][side]
    private EditText mEtSmart[][];      // [question][side]
    //    private View mRootView;
    private TextView mTvSumH, mTvSumV, mTvTotalSum;
    private TextView mTvSumSH, mTvSumSV, mTvTotalSumS;

    private FragmentFsaBinding bind;

    public FSAFragment() {
        mCustomRG = new CustomRadioGroup[N_QUESTIONS][N_SIDES];
        mRB = new RadioButton[N_QUESTIONS][N_ANSWERS][N_SIDES];
        mEtSmart = new EditText[N_QUESTIONS][N_SIDES];
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Binding instead of findViewById
        bind = DataBindingUtil.inflate(inflater, R.layout.fragment_fsa, container, false);

        // Test URI
        mTestUri = Uri.parse(getArguments().getString(EXTRAS.KEY_URI));

        // Tab (IN or OUT)
        mTab = getArguments().getInt(EXTRAS.KEY_TAB);

        // IN or OUT selected at TestListActivity
        mInOut = getArguments().getInt(EXTRAS.KEY_INOUT);

        // IN or OUT background color adjustments
        if (mTab == Test.IN) {
            bind.scrollview.setBackgroundColor(ContextCompat.getColor(getContext(), R.color
                    .background_in));

        } else {
            bind.scrollview.setBackgroundColor(ContextCompat.getColor(getContext(), R.color
                    .background_out));
        }

        // Disable touch events in the 'other' tab
        if ((mTab == Test.IN && mInOut == 1) || (mTab == Test.OUT && mInOut == 0)) {
            disableTouchOnLayout(bind.scrollview);
        }

        // Layout background listener closes soft keyboard
        bind.fsaLayoutBackground.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Hide soft keyboard
                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return false;
            }
        });

        // Setup views in layout
        setupRadioButtons();

        // Sum text views
        mTvSumH = bind.fsaHSumTv;
        mTvSumV = bind.fsaVSumTv;
        mTvTotalSum = bind.fsaTotalSumTv;

        mTvSumSH = bind.fsaHSmartTv;
        mTvSumSV = bind.fsaVSmartTv;
        mTvTotalSumS = bind.fsaTotalSmartTv;

        // Edit text
        mEtSmart[0][0] = bind.fsaEt1h;
        mEtSmart[0][1] = bind.fsaEt1v;
        mEtSmart[1][0] = bind.fsaEt2h;
        mEtSmart[1][1] = bind.fsaEt2v;
        mEtSmart[2][0] = bind.fsaEt3h;
        mEtSmart[2][1] = bind.fsaEt3v;
        mEtSmart[3][0] = bind.fsaEt4h;
        mEtSmart[3][1] = bind.fsaEt4v;
        mEtSmart[4][0] = bind.fsaEt5h;
        mEtSmart[4][1] = bind.fsaEt5v;

        // Edit text input filter
        for (int q = 0; q < N_QUESTIONS; q++) {
            for (int s = 0; s < N_SIDES; s++) {
                mEtSmart[q][s].setFilters(new InputFilter[]{new InputFilterMinMax("0", "10", 2, 1)});
                mEtSmart[q][s].addTextChangedListener(this);
            }
        }

        // Done button
        bind.fsaBtnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save to database: return false if test incomplete
                if (!saveToDatabase()) {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
                    dialog.setMessage(getResources().getString(R.string.test_saved_incomplete));
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "VISA",
                            new DialogInterface.OnClickListener() {
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
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    highlight(); // clear  highlights
                                }
                            });
                    dialog.show();
                }

                // Sums UI
                updateSumsTv();

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
            Log.d(LOG_TAG, "Content from savedInstance: ");
        } else {
            // Read test content from database
            Cursor cursor = getActivity().getContentResolver().query(mTestUri, null, null, null, null);
            // Early exit: should never happen
            if (cursor == null || cursor.getCount() == 0) {
                return bind.getRoot();
            }

            cursor.moveToFirst();
            if (mTab == Test.IN) {
                contentStr = cursor.getString(cursor
                        .getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
            } else {
                contentStr = cursor.getString(cursor
                        .getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
            }

            cursor.close();
            Log.d(LOG_TAG, "Content from database: " + contentStr);
        }

        // Content can be null. Database 'content_in' and 'content_out' are null when first created
        if (contentStr != null) {
            // Set radio buttons and total sum using info from content
            String[] content = contentStr.split("\\|");
            int i = 0;
            for (int q = 0; q < N_QUESTIONS; q++) {
                if (!content[i].equals("-1")) {
                    mCustomRG[q][0].checkButtonAt(Integer.parseInt(content[i]));
                }
                i++;
                if (!content[i].equals("-1")) {
                    mCustomRG[q][1].checkButtonAt(Integer.parseInt(content[i]));
                }
                i++;
                mEtSmart[q][0].setText(content[i++]);
                mEtSmart[q][1].setText(content[i++]);
            }

            // Sum UI
            updateSumsTv();
        }

        // Redo highlight after rotation
        // In this fragment, it needs to be done after selected buttons in radio groups
        // have been updated
        if (mHighlightsON) {
            highlight();
        }

        // Inform parent activity that form is up to date
        ((TestActivity) getActivity()).setUserHasSaved(true);

        return bind.getRoot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save save for highlight state so rotation does not remove highlights
        outState.putString(STATE_CONTENT, generateContent());
        outState.putBoolean(STATE_HIGH_ON, mHighlightsON);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    public boolean saveToDatabase() {
        int status = Test.COMPLETED;
        int sumH = -1;
        StringBuilder builder = new StringBuilder();
        if (isHcomplete()) {
            sumH = calculateHsum();
            builder.append(String.valueOf(sumH));
        } else {
            builder.append("-1");
            status = Test.INCOMPLETED;
        }

        builder.append("|");
        int sumV = -1;
        if (isVcomplete()) {
            sumV = calculateVsum();
            builder.append(String.valueOf(sumV));
        } else {
            builder.append("-1");
            status = Test.INCOMPLETED;
        }

        builder.append("|");
        if (sumH != -1 && sumV != -1) {
            builder.append(String.valueOf(sumH + sumV));
        } else {
            builder.append("-1");
        }

        double smartH = -1;
        builder.append("|");
        if (isSmartHcomplete()) {
            smartH = calculateSmartHsum();
            builder.append(String.valueOf(smartH));
        } else {
            builder.append("-1");
            status = Test.INCOMPLETED;
        }

        double smartV = -1;
        builder.append("|");
        if (isSmartVcomplete()) {
            smartV = calculateSmartVsum();
            builder.append(String.valueOf(smartV));
        } else {
            builder.append("-1");
            status = Test.INCOMPLETED;
        }

        builder.append("|");
        if (smartH != -1 && smartV != -1) {
            builder.append(String.valueOf(smartH + smartV));
        } else {
            builder.append("-1");
        }

        ContentValues values = new ContentValues();
        if (mTab == Test.IN) {
            values.put(TestEntry.COLUMN_CONTENT_IN, generateContent());
            values.put(TestEntry.COLUMN_RESULT_IN, builder.toString());
            values.put(TestEntry.COLUMN_STATUS_IN, status);
        } else {
            values.put(TestEntry.COLUMN_CONTENT_OUT, generateContent());
            values.put(TestEntry.COLUMN_RESULT_OUT, builder.toString());
            values.put(TestEntry.COLUMN_STATUS_OUT, status);
        }

        int rows = getActivity().getContentResolver().update(mTestUri, values, null, null);
        Log.d(LOG_TAG, "rows updated: " + rows);

        return status == Test.COMPLETED;
    }

    private void updateSumsTv() {
        int sumH = calculateHsum();
        int sumV = calculateVsum();
        if (sumH != 0)
            mTvSumH.setText(String.valueOf(sumH));

        if (sumV != 0)
            mTvSumV.setText(String.valueOf(sumV));

        if (sumH != 0 && sumV != 0)
            mTvTotalSum.setText(String.valueOf(sumH + sumV));

        double sumSH = calculateSmartHsum();
        double sumSV = calculateSmartVsum();
        if (sumSH != -1) {
            mTvSumSH.setText(String.valueOf(sumSH));
        }

        if (sumSV != -1) {
            mTvSumSV.setText(String.valueOf(sumSV));
        }

        if (sumSH != -1 && sumSV != -1) {
            mTvTotalSumS.setText(String.valueOf(sumSH + sumSV));
        }
    }

    private String generateContent() {
        StringBuilder builder = new StringBuilder();
        for (int q = 0; q < N_QUESTIONS; q++) {
            builder.append(mCustomRG[q][0].getPositionSelectedButton());
            builder.append("|");
            builder.append(mCustomRG[q][1].getPositionSelectedButton());
            builder.append("|");
            // Smärta
            builder.append(mEtSmart[q][0].getText().toString().trim());
            builder.append("|");
            builder.append(mEtSmart[q][1].getText().toString().trim());
            builder.append("|");
        }
        builder.append("0|");   // fix: so empty characters do not get discarded by split

        return builder.toString();
    }

    /**
     * @return true if all höger answers are filled in
     */
    private boolean isHcomplete() {
        for (int q = 0; q < N_QUESTIONS; q++) {
            if (!mCustomRG[q][0].isSelected())
                return false;
        }
        return true;
    }

    /**
     * @return true if all vänster answers are filled in
     */
    private boolean isVcomplete() {
        for (int q = 0; q < N_QUESTIONS; q++) {
            if (!mCustomRG[q][1].isSelected())
                return false;
        }
        return true;
    }

    private boolean isSmartHcomplete() {
        for (int q = 0; q < N_QUESTIONS; q++) {
            if (mEtSmart[q][0].getText().toString().trim().isEmpty())
                return false;
        }
        return true;
    }

    private boolean isSmartVcomplete() {
        for (int q = 0; q < N_QUESTIONS; q++) {
            if (mEtSmart[q][1].getText().toString().trim().isEmpty())
                return false;
        }
        return true;
    }

    private int calculateHsum() {
        int sum = 0;
        for (int q = 0; q < N_QUESTIONS; q++) {
            if (mCustomRG[q][0].isSelected()) {
                sum += mCustomRG[q][0].getPositionSelectedButton() + 1;
            }
        }
        return sum;
    }

    private int calculateVsum() {
        int sum = 0;
        for (int q = 0; q < N_QUESTIONS; q++) {
            if (mCustomRG[q][1].isSelected()) {
                sum += mCustomRG[q][1].getPositionSelectedButton() + 1;
            }
        }
        return sum;
    }

    private double calculateSmartHsum() {
        boolean allEmpty = true;
        double sum = 0;
        for (int q = 0; q < N_QUESTIONS; q++) {
            if (!mEtSmart[q][0].getText().toString().trim().isEmpty()) {
                sum += Double.parseDouble(mEtSmart[q][0].getText().toString().trim());
                allEmpty = false;
            }
        }

        if (allEmpty) {
            return -1;
        } else
            return sum;
    }

    private double calculateSmartVsum() {
        boolean allEmpty = true;
        double sum = 0;
        for (int q = 0; q < N_QUESTIONS; q++) {
            if (!mEtSmart[q][1].getText().toString().trim().isEmpty()) {
                sum += Double.parseDouble(mEtSmart[q][1].getText().toString().trim());
                allEmpty = false;
            }
        }

        if (allEmpty) {
            return -1;
        } else
            return sum;
    }

    private void highlight() {
        TextView tvQ[] = new TextView[N_QUESTIONS];
        tvQ[0] = bind.fsaTvQ1;
        tvQ[1] = bind.fsaTvQ2;
        tvQ[2] = bind.fsaTvQ3;
        tvQ[3] = bind.fsaTvQ4;
        tvQ[4] = bind.fsaTvQ5;

        TextView tvSmart[] = new TextView[N_QUESTIONS];
        tvSmart[0] = bind.fsaTvSmart1;
        tvSmart[1] = bind.fsaTvSmart2;
        tvSmart[2] = bind.fsaTvSmart3;
        tvSmart[3] = bind.fsaTvSmart4;
        tvSmart[4] = bind.fsaTvSmart5;

        for (int q = 0; q < N_QUESTIONS; q++) {
            // Highlight if no button is selected in either höger or vänster columns
            if (mHighlightsON && (!mCustomRG[q][0].isSelected() || !mCustomRG[q][1].isSelected())) {
                tvQ[q].setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
            } else {
                tvQ[q].setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
            }

            // Highlight 'smärta'
            if (mHighlightsON && (mEtSmart[q][0].getText().toString().trim().isEmpty()) ||
                    mHighlightsON && (mEtSmart[q][1].getText().toString().trim().isEmpty())) {
                tvSmart[q].setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
            } else {
                tvSmart[q].setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
            }
        }
    }

    @Override
    public void onClick(View v) {
        // Hide soft keyboard
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

        CustomRadioGroup selectedGroup = getGroupForButton(v);
        if (selectedGroup != null) {
            selectedGroup.informGroupButtonSelected(v);
        }

        // Inform parent activity
        ((TestActivity) getActivity()).setUserHasSaved(false);

        if (mHighlightsON)
            highlight();

        // Update sums UI
        updateSumsTv();
    }

    /**
     * @return custom radio group the button belongs to
     */
    private CustomRadioGroup getGroupForButton(View v) {
        for (int q = 0; q < N_QUESTIONS; q++) {
            for (int s = 0; s < N_SIDES; s++) {
                if (mCustomRG[q][s].getButtonPositionInGroup(v) != -1)
                    return mCustomRG[q][s];
            }
        }
        return null;
    }

    private void setupRadioButtons() {
        // Radio groups
        for (int i = 0; i < N_QUESTIONS; i++) {
            for (int s = 0; s < N_SIDES; s++)
                mCustomRG[i][s] = new CustomRadioGroup();
        }

        // Question 1
        mRB[0][0][0] = bind.fsaBtn11h;
        mRB[0][0][1] = bind.fsaBtn11v;
        mRB[0][1][0] = bind.fsaBtn12h;
        mRB[0][1][1] = bind.fsaBtn12v;
        mRB[0][2][0] = bind.fsaBtn13h;
        mRB[0][2][1] = bind.fsaBtn13v;
        mRB[0][3][0] = bind.fsaBtn14h;
        mRB[0][3][1] = bind.fsaBtn14v;
        mRB[0][4][0] = bind.fsaBtn15h;
        mRB[0][4][1] = bind.fsaBtn15v;
        mRB[0][5][0] = bind.fsaBtn16h;
        mRB[0][5][1] = bind.fsaBtn16v;

        // Question 2
        mRB[1][0][0] = bind.fsaBtn21h;
        mRB[1][0][1] = bind.fsaBtn21v;
        mRB[1][1][0] = bind.fsaBtn22h;
        mRB[1][1][1] = bind.fsaBtn22v;
        mRB[1][2][0] = bind.fsaBtn23h;
        mRB[1][2][1] = bind.fsaBtn23v;
        mRB[1][3][0] = bind.fsaBtn24h;
        mRB[1][3][1] = bind.fsaBtn24v;
        mRB[1][4][0] = bind.fsaBtn25h;
        mRB[1][4][1] = bind.fsaBtn25v;
        mRB[1][5][0] = bind.fsaBtn26h;
        mRB[1][5][1] = bind.fsaBtn26v;

        // Question 3
        mRB[2][0][0] = bind.fsaBtn31h;
        mRB[2][0][1] = bind.fsaBtn31v;
        mRB[2][1][0] = bind.fsaBtn32h;
        mRB[2][1][1] = bind.fsaBtn32v;
        mRB[2][2][0] = bind.fsaBtn33h;
        mRB[2][2][1] = bind.fsaBtn33v;
        mRB[2][3][0] = bind.fsaBtn34h;
        mRB[2][3][1] = bind.fsaBtn34v;
        mRB[2][4][0] = bind.fsaBtn35h;
        mRB[2][4][1] = bind.fsaBtn35v;
        mRB[2][5][0] = bind.fsaBtn36h;
        mRB[2][5][1] = bind.fsaBtn36v;

        // Question 4
        mRB[3][0][0] = bind.fsaBtn41h;
        mRB[3][0][1] = bind.fsaBtn41v;
        mRB[3][1][0] = bind.fsaBtn42h;
        mRB[3][1][1] = bind.fsaBtn42v;
        mRB[3][2][0] = bind.fsaBtn43h;
        mRB[3][2][1] = bind.fsaBtn43v;
        mRB[3][3][0] = bind.fsaBtn44h;
        mRB[3][3][1] = bind.fsaBtn44v;
        mRB[3][4][0] = bind.fsaBtn45h;
        mRB[3][4][1] = bind.fsaBtn45v;
        mRB[3][5][0] = bind.fsaBtn46h;
        mRB[3][5][1] = bind.fsaBtn46v;

        // Question 5
        mRB[4][0][0] = bind.fsaBtn51h;
        mRB[4][0][1] = bind.fsaBtn51v;
        mRB[4][1][0] = bind.fsaBtn52h;
        mRB[4][1][1] = bind.fsaBtn52v;
        mRB[4][2][0] = bind.fsaBtn53h;
        mRB[4][2][1] = bind.fsaBtn53v;
        mRB[4][3][0] = bind.fsaBtn54h;
        mRB[4][3][1] = bind.fsaBtn54v;
        mRB[4][4][0] = bind.fsaBtn55h;
        mRB[4][4][1] = bind.fsaBtn55v;
        mRB[4][5][0] = bind.fsaBtn56h;
        mRB[4][5][1] = bind.fsaBtn56v;

        for (int q = 0; q < N_QUESTIONS; q++) {
            for (int an = 0; an < N_ANSWERS; an++) {
                mRB[q][an][0].setOnClickListener(this);
                mCustomRG[q][0].addButton(mRB[q][an][0]);
                mRB[q][an][1].setOnClickListener(this);
                mCustomRG[q][1].addButton(mRB[q][an][1]);
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        highlight();   // Dynamic highlighting

        updateSumsTv();

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
