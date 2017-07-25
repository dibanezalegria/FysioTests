package com.pbluedotsoft.fysio;


import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.pbluedotsoft.fysio.data.DbContract.TestEntry;
import com.pbluedotsoft.fysio.data.EXTRAS;
import com.pbluedotsoft.fysio.data.Test;

import java.math.BigDecimal;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class ErgoFragment extends Fragment implements TextWatcher, AdapterView.OnItemSelectedListener {

    private static final String LOG_TAG = ErgoFragment.class.getSimpleName();

    private static final int N_ETs = 6;
    private static final int N_PULSE = 12;
    protected static final int GENDER_MALE = 1;
    protected static final int GENDER_FEMALE = 2;

    // Save state constant
    private static final String STATE_CONTENT = "state_content";
    private static final String STATE_RESULT = "state_result";
    private static final String STATE_HIGH_ON = "state_high_on";

    private Spinner mSpinGender, mSpinBelas;
    private TextView mTvGender, mTvBelas, mTvWeight, mTvAge, mTvPulse, mTvResult, mTvInterval;
    private EditText mEtData[];
    private EditText mPulse[];

    private Uri mTestUri;
    private int mTab, mInOut;

    private boolean mHighlightsON;

    private String[][] mValuesT3; // Table 3 from documentation
    private String[] mValuesT6;   // Table 6 from documentation

    private String mResult;

    public ErgoFragment() {
        mEtData = new EditText[N_ETs];
        mPulse = new EditText[N_PULSE];
        // Generate tables
        mValuesT3 = generateT3();
        mValuesT6 = generateT6();
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

        final View rootView = inflater.inflate(R.layout.fragment_ergo, container, false);

        // IN or OUT background color adjustments
        ScrollView scroll = (ScrollView) rootView.findViewById(R.id.scrollview);
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
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.ergo_layout_background);
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

        mTvWeight = (TextView) rootView.findViewById(R.id.ergo_tv_vikt);
        mTvAge = (TextView) rootView.findViewById(R.id.ergo_tv_ålder);
        mTvGender = (TextView) rootView.findViewById(R.id.ergo_tv_gender);
        mTvBelas = (TextView) rootView.findViewById(R.id.ergo_tv_belas);
        mTvPulse = (TextView) rootView.findViewById(R.id.ergo_tv_pulse);
        mTvResult = (TextView) rootView.findViewById(R.id.ergo_tv_result);
        mTvInterval = (TextView) rootView.findViewById(R.id.ergo_tv_interval);

        mSpinGender = (Spinner) rootView.findViewById(R.id.ergo_gender_spinner);
        mSpinBelas = (Spinner) rootView.findViewById(R.id.ergo_belas_spinner);

        // Spinner adapters
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.ergo_spinner_list_item,
                getResources().getStringArray(R.array.ergo_gender_spinner));
        adapter.setDropDownViewResource(R.layout.ergo_spinner_list_item);
        mSpinGender.setAdapter(adapter);
        adapter = new ArrayAdapter<>(getContext(),
                R.layout.ergo_spinner_list_item,
                getResources().getStringArray(R.array.ergo_belas_spinner_male));
        adapter.setDropDownViewResource(R.layout.ergo_spinner_list_item);
        mSpinBelas.setAdapter(adapter);

        mEtData[0] = (EditText) rootView.findViewById(R.id.ergo_et_cykel);
        mEtData[1] = (EditText) rootView.findViewById(R.id.ergo_et_sadel);
        mEtData[2] = (EditText) rootView.findViewById(R.id.ergo_et_vikt);
        mEtData[3] = (EditText) rootView.findViewById(R.id.ergo_et_längd);
        mEtData[4] = (EditText) rootView.findViewById(R.id.ergo_et_ålder);
        mEtData[5] = (EditText) rootView.findViewById(R.id.ergo_et_vilo);

        mPulse[0] = (EditText) rootView.findViewById(R.id.ergo_et_pulse1);
        mPulse[1] = (EditText) rootView.findViewById(R.id.ergo_et_pulse2);
        mPulse[2] = (EditText) rootView.findViewById(R.id.ergo_et_pulse3);
        mPulse[3] = (EditText) rootView.findViewById(R.id.ergo_et_pulse4);
        mPulse[4] = (EditText) rootView.findViewById(R.id.ergo_et_pulse5);
        mPulse[5] = (EditText) rootView.findViewById(R.id.ergo_et_pulse6);
        mPulse[6] = (EditText) rootView.findViewById(R.id.ergo_et_pulse7);
        mPulse[7] = (EditText) rootView.findViewById(R.id.ergo_et_pulse8);
        mPulse[8] = (EditText) rootView.findViewById(R.id.ergo_et_pulse9);
        mPulse[9] = (EditText) rootView.findViewById(R.id.ergo_et_pulse10);
        mPulse[10] = (EditText) rootView.findViewById(R.id.ergo_et_pulse11);
        mPulse[11] = (EditText) rootView.findViewById(R.id.ergo_et_pulse12);

        // Listeners
        for (EditText et : mEtData) {
            et.addTextChangedListener(this);
        }

        for (EditText et : mPulse) {
            et.addTextChangedListener(this);
        }

        // Belastning spinner values change depending on gender
        mSpinGender.setOnItemSelectedListener(this);
        mSpinBelas.setOnItemSelectedListener(this);

        // Done button
        Button btnDone = (Button) rootView.findViewById(R.id.ergo_btnDone);
        btnDone.setTransformationMethod(null);   // button text non capitalize
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save to database: return false if test incomplete
                if (!saveToDatabase()) {
                    mHighlightsON = true;
                    highlight();
//                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
//                    dialog.setMessage(getResources().getString(R.string.test_saved_incomplete));
//                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, "VISA", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            mHighlightsON = true;
//                            highlight();
//                        }
//                    });
//                    dialog.show();
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
            mResult = savedInstanceState.getString(STATE_RESULT);
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
                return rootView;
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

        Log.d(LOG_TAG, "mResult:" + mResult);

        // Content can be null. Database 'content_in' and 'content_out' are null when first created
        if (contentStr != null) {
            Log.d(LOG_TAG, "contentStr != null");
            // Set edit text views
            String[] content = contentStr.split("\\|");
            int counter = 0;
            mSpinGender.setSelection(Integer.parseInt(content[counter++]));
            mSpinBelas.setSelection(Integer.parseInt(content[counter++]));

            for (int i = 0; i < N_ETs; i++) {
                mEtData[i].setText(content[counter++]);
            }

            for (int i = 0; i < N_PULSE; i++) {
                mPulse[i].setText(content[counter++]);
            }
        }

        // Restore result and interval UI
        if (!mResult.equals("-1")) {
            mTvResult.setText(mResult);
//            int gender = mSpinGender.getSelectedItemPosition();
//            int age = Integer.parseInt(mEtData[4].getText().toString());
//            mTvInterval.setText(getInterval(gender, age, Integer.parseInt(mResult)));
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Activity has created from scratch or from save instance
        // Inform parent activity that view fields are up to date
        Log.d(LOG_TAG, "onActivityCreated");
        ((TestActivity) getActivity()).setUserHasSaved(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save state for views in layout
        String content = generateContent();
        outState.putString(STATE_CONTENT, content);
        outState.putString(STATE_RESULT, mResult);
        outState.putBoolean(STATE_HIGH_ON, mHighlightsON);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    public boolean saveToDatabase() {
        Log.d(LOG_TAG, "saveToDatabase");
        ContentValues values = new ContentValues();
        // Test status
        boolean missing = missingAnswers();
        int status;
        if (missing) {
            status = Test.INCOMPLETED;
        } else {
            status = Test.COMPLETED;
        }

        // Result and interval calculations
        int resultFromT6 = -1;
        if (!missing) {
            int meanPulse = getMeanPulse();
            int gender = mSpinGender.getSelectedItemPosition();
            int belas = mSpinBelas.getSelectedItemPosition();
            int weight = Integer.parseInt(mEtData[2].getText().toString());
            int age = Integer.parseInt(mEtData[4].getText().toString());
            Log.d(LOG_TAG, "Pulse: " + meanPulse + " Gender:" + gender + " Belas:" + belas +
                    " Weight:" + weight + " Age:" + age);
            // Look for result on table T6
            // Uses regression only when valueFromT3 is less than 1.5 or more than 6.0
            float valueFromT3 = getValueFromT3(gender, meanPulse, belas);
            float valueFromT5 = correctValueUsingT5(valueFromT3, age);
            if (valueFromT5 < 1.5) {
                resultFromT6 = getResultUsingRegression(weight, valueFromT5);
                Log.d(LOG_TAG, "Value " + valueFromT5 + " outside table 6 -> using regression analysis");
            } else if (valueFromT5 > 6.0) {
                resultFromT6 = getResultUsingRegression(weight, valueFromT5);
                Log.d(LOG_TAG, "Value " + valueFromT5 + " outside table 6 -> using regression analysis");
            } else {
                resultFromT6 = getResultFromT6(weight, valueFromT5);
            }

            mResult = String.valueOf(resultFromT6);

            // Update result UI
            mTvResult.setText(mResult);
//            mTvInterval.setText(getInterval(gender, age, resultFromT6));
        }

        // Values
        if (mTab == Test.IN) {
            values.put(TestEntry.COLUMN_CONTENT_IN, generateContent());
            values.put(TestEntry.COLUMN_STATUS_IN, status);
            values.put(TestEntry.COLUMN_RESULT_IN, String.valueOf(resultFromT6));
        } else {
            values.put(TestEntry.COLUMN_CONTENT_OUT, generateContent());
            values.put(TestEntry.COLUMN_STATUS_OUT, status);
            values.put(TestEntry.COLUMN_RESULT_OUT, String.valueOf(resultFromT6));
        }

        int rows = getActivity().getContentResolver().update(mTestUri, values, null, null);
        Log.d(LOG_TAG, "rows updated: " + rows);

        return !missing;
    }

    /**
     * Generate a string from information extracted for edit text views
     *
     * @return String representing edit text views
     */
    private String generateContent() {
        StringBuilder builder = new StringBuilder();
        builder.append(mSpinGender.getSelectedItemPosition());
        builder.append("|");
        builder.append(mSpinBelas.getSelectedItemPosition());
        builder.append("|");

        for (EditText et : mEtData) {
            builder.append(et.getText().toString().trim());
            builder.append("|");
        }

        for (EditText et : mPulse) {
            builder.append(et.getText().toString().trim());
            builder.append("|");
        }

        builder.append("0|");   // Fix: split() can not handle all positions empty

        Log.d(LOG_TAG, "content: " + builder.toString());

        return builder.toString();
    }

    /**
     * @return true if one or more question are not answered
     */
    private boolean missingAnswers() {
        // Only some fields are mandatory: gender, belastning, vikt, ålder and two pulses

        if (mSpinGender.getSelectedItemPosition() == 0) {
            showAlertDialog("Ange kön");
            return true;
        }

        if (mSpinBelas.getSelectedItemPosition() == 0) {
            showAlertDialog("Ange belastning");
            return true;
        }

        // Weight
        if (mEtData[2].getText().toString().isEmpty()) {
            showAlertDialog("Ange vikt");
            return true;
        }

        // Check weight range
        int weight = Integer.parseInt(mEtData[2].getText().toString());
        if (weight < 45 || weight > 130) {
            showAlertDialog("Vikt ogiltigt värde [45, 130]");
            return true;
        }

        // Age
        if (mEtData[4].getText().toString().isEmpty()) {
            showAlertDialog("Ange ålder");
            return true;
        }

        // Check age range
        int age = Integer.parseInt(mEtData[4].getText().toString());
        if (age < 15 || age > 100) {
            showAlertDialog("Ålder ogiltigt värde [15, 100]");
            return true;
        }

        // At least to pulses are mandatory
        int meanPulse = getMeanPulse();
        if (meanPulse == -1) {
            showAlertDialog("Två pulsvärden behövs för att beräkna resultat.");
            return true;
        } else if (meanPulse < 120 || meanPulse > 170) {
            showAlertDialog("Ogiltigt medelvärde för pulse [120, 170]");
            return true;
        }

        return false;
    }

    private void showAlertDialog(String msg) {
        AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
        dialog.setMessage(msg);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHighlightsON = true;
                highlight();
            }
        });
        dialog.show();
    }

    /**
     * Highlights unanswered question
     */
    private void highlight() {
        // Gender
        if (mHighlightsON && mSpinGender.getSelectedItemPosition() == 0) {
            mTvGender.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTvGender.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }

        // Belastning
        if (mHighlightsON && mSpinBelas.getSelectedItemPosition() == 0) {
            mTvBelas.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTvBelas.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }

        // Vikt
        if (mHighlightsON && mEtData[2].getText().toString().trim().length() == 0) {
            mTvWeight.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTvWeight.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }

        // Ålder
        if (mHighlightsON && mEtData[4].getText().toString().trim().length() == 0) {
            mTvAge.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTvAge.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }
    }

    protected float getValueFromT3(int gender, int pulse, int belas) {
        // Validate input
        if (pulse < 120 || pulse > 170) {
            Log.d(LOG_TAG, "Pulse outside valid range (120-170)");
            return -1;
        }

        if (gender == GENDER_FEMALE && belas > 5) {
            Log.d(LOG_TAG, "Belastning must be under 900 (female)");
            return -1;
        }

        // Table index row for pulse
        int rowPulse = pulse - 120;
        String[] values = mValuesT3[gender - 1][rowPulse].split(" ");

        // Table index column for belastning
//        Log.d(LOG_TAG, "belas pos: " + belas);
        float value = -1;
        try {
            if (gender == GENDER_MALE) {
                // Male
                switch (belas) {
                    // 300
                    case 1:
                        value = Float.parseFloat(values[0]);
                        break;
                    // 450
                    case 2:
                        value = (Float.parseFloat(values[0]) + Float.parseFloat(values[1])) / 2;
                        BigDecimal bd = new BigDecimal(value);
                        bd = bd.setScale(1, BigDecimal.ROUND_DOWN);
                        value = bd.floatValue();
                        break;
                    // 600
                    case 3:
                        value = Float.parseFloat(values[1]);
                        break;
                    // 750
                    case 4:
                        value = (Float.parseFloat(values[1]) + Float.parseFloat(values[2])) / 2;
                        bd = new BigDecimal(value);
                        bd = bd.setScale(1, BigDecimal.ROUND_DOWN);
                        value = bd.floatValue();
                        break;
                    // 900
                    case 5:
                        value = Float.parseFloat(values[2]);
                        break;
                    // 1050
                    case 6:
                        value = (Float.parseFloat(values[2]) + Float.parseFloat(values[3])) / 2;
                        bd = new BigDecimal(value);
                        bd = bd.setScale(1, BigDecimal.ROUND_DOWN);
                        value = bd.floatValue();
                        break;
                    // 1200
                    case 7:
                        value = Float.parseFloat(values[3]);
                        break;
                    // 1350
                    case 8:
                        value = (Float.parseFloat(values[3]) + Float.parseFloat(values[4])) / 2;
                        bd = new BigDecimal(value);
                        bd = bd.setScale(1, BigDecimal.ROUND_DOWN);
                        value = bd.floatValue();
                        break;
                    // 1500
                    case 9:
                        value = Float.parseFloat(values[4]);
                        break;
                }
            } else {
                // Female
                switch (belas) {
                    // 300
                    case 1:
                        value = Float.parseFloat(values[0]);
                        break;
                    // 450
                    case 2:
                        value = Float.parseFloat(values[1]);
                        break;
                    // 600
                    case 3:
                        value = Float.parseFloat(values[2]);
                        break;
                    // 750
                    case 4:
                        value = Float.parseFloat(values[3]);
                        break;
                    // 900
                    case 5:
                        value = Float.parseFloat(values[4]);
                        break;
                }
            }
        } catch (Exception ex) {
            Log.d(LOG_TAG, "Exception getValueFromT3. Pulse:" + pulse + " Belastning:" + belas);
            return -1;
        }

        return value;
    }

    /**
     * Reads a value directly from table 6 instead of using regression analysis.
     */
    protected int getResultFromT6(int weight, float uptake) {
        // Data validation
        if (weight < 45 || weight > 130 || uptake < 1.5 || uptake > 6.0) {
            return -1;
        }

        // For testing
        if (mValuesT6 == null) {
            mValuesT6 = generateT6();
        }

        int rowWeight = weight - 45;
        int colUptake = (int) (uptake * 10 - 15);

        String row = mValuesT6[rowWeight];

        String[] values = row.split(" ");

        int result = Integer.parseInt(values[colUptake]);

        return result;
    }

    /**
     * Estimates value for table 6 using regression analysis instead of directly
     * reading the value from the table.
     */
    protected int getResultUsingRegression(int weight, float valueFromT5) {
        // For testing
        if (mValuesT6 == null) {
            mValuesT6 = generateT6();
        }

        // Regression Analysis using the method of least squares
        final int N_COLS = 46;
        int rowWeight = weight - 45;
        String row = mValuesT6[rowWeight];

        String[] yValues = row.split(" ");
        String[] xValues = new String[N_COLS];
        float x = 1.5f;
        for (int i = 0; i < N_COLS; i++) {
            xValues[i] = String.format(Locale.ENGLISH, "%.1f", x);
            x += 0.1f;
        }

        // Sum x values
        float sumX = 0;
        for (String valueStr : xValues) {
            String sum = String.format(Locale.ENGLISH, "%.1f", sumX + Float.parseFloat(valueStr));
            sumX = Float.parseFloat(sum);
        }

        // Sum y values
        float sumY = 0;
        for (String valueStr : yValues) {
            sumY += Float.parseFloat(valueStr);
        }

        // Sum xy
        float sumXY = 0;
        for (int i = 0; i < N_COLS; i++) {
            float xVal = Float.parseFloat(xValues[i]);
            int yVal = Integer.parseInt(yValues[i]);
//            float yVal = Float.parseFloat(yValues[i]);
            sumXY += xVal * yVal;
        }

        // Sum x2
        float sumX2 = 0;
        for (String str : xValues) {
            float val = Float.parseFloat(str);
            sumX2 += val * val;
        }

        // Slope
        float up = sumXY - ((sumX * sumY) / N_COLS);
        float den = sumX2 - ((sumX * sumX) / N_COLS);
        float m = up / den;

        // Mean x and y
        float meanX = sumX / N_COLS;
        float meanY = sumY / N_COLS;

        // Interception
        float b = meanY - (m * meanX);

        // Equation: y = mx + b
        float y = (m * valueFromT5) + b;
        Log.d(LOG_TAG, "EquationT6: y=" + m + "*x +" + b + " -> y=" + y + " rounded: " + Math.round(y));

//        // Format to one decimal
//        return Float.parseFloat(String.format(Locale.ENGLISH, "%.1f", y));

        // Return closest integer
        return Math.round(y);
    }

    /**
     * Correction table 5
     */
    protected float correctValueUsingT5(float value, int age) {
        // Table 5 used for correction
        SparseArray<Float> table = new SparseArray<>();
        table.put(15, 1.10f);
        table.put(25, 1.00f);
        table.put(35, 0.87f);
        table.put(40, 0.83f);
        table.put(45, 0.78f);
        table.put(50, 0.75f);
        table.put(55, 0.71f);
        table.put(60, 0.68f);
        table.put(65, 0.65f);

        Float factor;
        if (age < 15 || age > 65) {
            factor = correctUsingRegression(age);
            Log.d(LOG_TAG, "factor after correcting age (regression):" + factor);
        } else {
            // Age correspond to a key in sparse array?
            factor = table.get(age);

            // Otherwise age lands in interval [a,b].
            if (factor == null) {
                // Get value for 'a'
                int downSteps = 0;
                while (factor == null) {
                    factor = table.get(--age);
                    downSteps++;
                }
                int indexOfA = table.indexOfKey(age);
                int indexOfB = indexOfA + 1;
                int keyA = table.keyAt(indexOfA);
                int keyB = table.keyAt(indexOfB);
                int nSteps = keyB - keyA;
                float valuePerStep = (table.valueAt(indexOfA) - table.valueAt(indexOfB)) / nSteps;
                factor = factor - valuePerStep * downSteps;
            }
        }

        //        String str = String.format(Locale.ENGLISH, "%.1f", Math.round(value * factor));

        // Adjusting to "snurra" results -> 2.96 becomes 2.9 instead of 3.0
        BigDecimal bd = new BigDecimal(Float.toString(value * factor));
        bd = bd.setScale(1, BigDecimal.ROUND_FLOOR);

        Log.d(LOG_TAG, "Correction factor: " + factor + " before: " + value + " after: " + bd);

        return bd.floatValue();
    }

    private float correctUsingRegression(int age) {
        // Regression Analysis using the method of least squares
        final int N_VALUES = 9;
        final int[] xValues = {15, 25, 35, 40, 45, 50, 55, 60, 65};
        final float[] yValues = {1.10f, 1.0f, 0.87f, 0.83f, 0.78f, 0.75f, 0.71f, 0.68f, 0.65f};

        // Sum x values
        int sumX = 0;
        for (int value : xValues) {
            sumX += value;
        }

        // Sum y values
        float sumY = 0;
        for (float value : yValues) {
            sumY += value;
        }

//        Log.d(LOG_TAG, "sumY: " + sumY);

        // Sum xy
        float sumXY = 0;
        for (int i = 0; i < N_VALUES; i++) {
            int xVal = xValues[i];
            float yVal = yValues[i];
            sumXY += xVal * yVal;
        }

//        Log.d(LOG_TAG, "sumXY: " + sumXY);

        // Sum x2
        int sumX2 = 0;
        for (int value : xValues) {
            sumX2 += value * value;
        }

        // Slope
        float up = sumXY - ((sumX * sumY) / N_VALUES);
        float den = sumX2 - ((sumX * sumX) / N_VALUES);
        float m = up / den;

        // Mean x and y
        float meanX = sumX / N_VALUES;
        float meanY = sumY / N_VALUES;

        // Interception
        float b = meanY - (m * meanX);

        // Equation: y = mx + b
        float y = (m * age) + b;
        Log.d(LOG_TAG, "EquationT5: y=" + m + "*x +" + b + " -> y=" + y);

        return y;
    }

    /**
     * @return -1 when less than two pulse measures found
     */
    private int getMeanPulse() {
        // Find last measure
        int totalPulses = 0;
        while (totalPulses < N_PULSE && !mPulse[totalPulses].getText().toString().isEmpty()) {
            totalPulses++;
        }

//        Log.d(LOG_TAG, "Total pulses: " + totalPulses);

        // Less than two pulse values -> early exit
        if (totalPulses < 2) {
            return -1;
        }

        // Mean value of last two pulses
        int pulseA, pulseB;
        try {
            pulseA = Integer.parseInt(mPulse[totalPulses - 1].getText().toString());
            pulseB = Integer.parseInt(mPulse[totalPulses - 2].getText().toString());
        } catch (Exception ex) {
            Log.d(LOG_TAG, "Exception processing pulses");
            return -1;
        }

        mPulse[totalPulses - 1].setTextColor(ContextCompat.getColor(getContext(), R.color.green_900));
        mPulse[totalPulses - 2].setTextColor(ContextCompat.getColor(getContext(), R.color.green_900));

        return (pulseA + pulseB) / 2;
    }

//    private String getInterval(int gender, int age, int result) {
//        String intervalStr = null;
//        switch (gender) {
//            case GENDER_MALE:
//                if (age < 30) {
//                    if (result < 39) {
//                        intervalStr = "<= 38";
//                    } else if (result < 44) {
//                        intervalStr = "39-43";
//                    } else if (result < 52) {
//                        intervalStr = "44-51";
//                    } else if (result < 57) {
//                        intervalStr = "52-56";
//                    } else {
//                        intervalStr = ">=57";
//                    }
//                } else if (age < 40) {
//                    if (result < 35) {
//                        intervalStr = "<= 34";
//                    } else if (result < 40) {
//                        intervalStr = "35-39";
//                    } else if (result < 48) {
//                        intervalStr = "40-47";
//                    } else if (result < 52) {
//                        intervalStr = "48-51";
//                    } else {
//                        intervalStr = ">=52";
//                    }
//                } else if (age < 50) {
//                    if (result < 31) {
//                        intervalStr = "<= 30";
//                    } else if (result < 36) {
//                        intervalStr = "31-35";
//                    } else if (result < 44) {
//                        intervalStr = "36-43";
//                    } else if (result < 48) {
//                        intervalStr = "44-47";
//                    } else {
//                        intervalStr = ">=48";
//                    }
//                } else if (age < 60) {
//                    if (result < 26) {
//                        intervalStr = "<= 25";
//                    } else if (result < 32) {
//                        intervalStr = "26-31";
//                    } else if (result < 40) {
//                        intervalStr = "32-39";
//                    } else if (result < 44) {
//                        intervalStr = "40-43";
//                    } else {
//                        intervalStr = ">=44";
//                    }
//                } else {
//                    if (result < 22) {
//                        intervalStr = "<= 21";
//                    } else if (result < 27) {
//                        intervalStr = "22-26";
//                    } else if (result < 36) {
//                        intervalStr = "27-35";
//                    } else if (result < 40) {
//                        intervalStr = "36-39";
//                    } else {
//                        intervalStr = ">=40";
//                    }
//                }
//                break;
//            case GENDER_FEMALE:
//                if (age < 30) {
//                    if (result < 29) {
//                        intervalStr = "<= 28";
//                    } else if (result < 35) {
//                        intervalStr = "29-34";
//                    } else if (result < 44) {
//                        intervalStr = "35-43";
//                    } else if (result < 49) {
//                        intervalStr = "44-48";
//                    } else {
//                        intervalStr = ">=49";
//                    }
//                } else if (age < 40) {
//                    if (result < 28) {
//                        intervalStr = "<= 27";
//                    } else if (result < 34) {
//                        intervalStr = "28-33";
//                    } else if (result < 42) {
//                        intervalStr = "34-41";
//                    } else if (result < 48) {
//                        intervalStr = "42-47";
//                    } else {
//                        intervalStr = ">=48";
//                    }
//                } else if (age < 50) {
//                    if (result < 26) {
//                        intervalStr = "<= 25";
//                    } else if (result < 32) {
//                        intervalStr = "26-31";
//                    } else if (result < 41) {
//                        intervalStr = "32-40";
//                    } else if (result < 46) {
//                        intervalStr = "41-45";
//                    } else {
//                        intervalStr = ">=46";
//                    }
//                } else {
//                    if (result < 22) {
//                        intervalStr = "<= 21";
//                    } else if (result < 29) {
//                        intervalStr = "22-28";
//                    } else if (result < 37) {
//                        intervalStr = "29-36";
//                    } else if (result < 42) {
//                        intervalStr = "37-41";
//                    } else {
//                        intervalStr = ">=42";
//                    }
//                }
//                break;
//        }
//
//        return intervalStr;
//    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        highlight();   // Dynamic highlighting

        // Pulse background color back to transparent
        for (int i = 0; i < N_PULSE; i++) {
            mPulse[i].setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }

//        Log.d(LOG_TAG, "afterTextChanged -> userInteracting: " + ((TestActivity) getActivity()).mUserInteracting);

        // EditText views call several times afterTextChanged() during fragment initialization.
        // The flag 'mUserInteraction' allows us to reset results UI ONLY when user is entering
        // new data in the fields.
        if (((TestActivity) getActivity()).mUserInteracting) {
            mTvResult.setText("");
//            mTvInterval.setText("");
        }

        // Inform parent activity
        ((TestActivity) getActivity()).setUserHasSaved(false);
    }

    /**
     * Interface implementation for AdapterView.OnItemSelectedListener for spinners
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) parent;
        if (spinner.getId() == R.id.ergo_gender_spinner) {
            if (position == GENDER_MALE) {
                int selected = mSpinBelas.getSelectedItemPosition();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        R.layout.ergo_spinner_list_item,
                        getResources().getStringArray(R.array.ergo_belas_spinner_male));
                adapter.setDropDownViewResource(R.layout.ergo_spinner_list_item);
                mSpinBelas.setAdapter(adapter);
                mSpinBelas.setSelection(selected);
            } else {
                int selected = mSpinBelas.getSelectedItemPosition();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        R.layout.ergo_spinner_list_item,
                        getResources().getStringArray(R.array.ergo_belas_spinner_female));
                adapter.setDropDownViewResource(R.layout.ergo_spinner_list_item);
                mSpinBelas.setAdapter(adapter);
                if (selected > 0 && selected < 6) {
                    mSpinBelas.setSelection(selected);
                }
            }
        }

//        Log.d(LOG_TAG, "onItemSelected -> userInteracting: " + ((TestActivity) getActivity()).mUserInteracting);

        // Spinners call several times OnItemSelected during fragment initialization.
        // The flag 'mUserInteraction' allows us to reset results UI ONLY when user is selecting
        // different gender/belastning.
        if (((TestActivity) getActivity()).mUserInteracting) {
            mTvResult.setText("");
//            mTvInterval.setText("");
            // Inform parent activity
            ((TestActivity) getActivity()).setUserHasSaved(false);
        }

        highlight();   // Dynamic highlighting
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private String[][] generateT3() {
        // Oxygen uptake men
        String[][] uptake = {
                // male
                {"2.2 3.5 4.8 6.3 7.8", "2.2 3.4 4.7 6.3 7.7", "2.2 3.4 4.6 6.2 7.6", "2.1 3.4 4.6 6.1 7.6", "2.1 3.3 4.5 6.0 7.5",
                        "2.0 3.2 4.4 5.9 7.2", "2.0 3.2 4.4 5.8 7.1", "2.0 3.1 4.3 5.7 7.0", "2.0 3.1 4.2 5.6 6.9", "1.9 3.0 4.2 5.6 6.8",
                        "1.9 3.0 4.1 5.5 6.8", "1.9 2.9 4.0 5.4 6.7", "1.8 2.9 4.0 5.3 6.6", "1.8 2.8 3.9 5.3 6.5", "1.8 2.8 3.9 5.2 6.4",
                        "1.7 2.8 3.8 5.1 6.3", "1.7 2.7 3.8 5.0 6.2", "1.7 2.7 3.7 5.0 6.1", "1.6 2.7 3.7 4.9 6.1", "1.6 2.6 3.6 4.8 6.0",
                        "1.6 2.6 3.6 4.8 6.0", "1.6 2.6 3.5 4.7 5.9", "1.6 2.5 3.5 4.6 5.8", "1.6 2.5 3.4 4.6 5.7", "1.6 2.5 3.4 4.5 5.7",
                        "1.5 2.4 3.4 4.5 5.6", "1.5 2.4 3.3 4.4 5.6", "1.5 2.4 3.3 4.4 5.5", "1.5 2.4 3.2 4.3 5.4", "1.5 2.3 3.2 4.3 5.4",
                        "1.5 2.3 3.2 4.2 5.3", "1.5 2.3 3.1 4.2 5.2", "1.4 2.3 3.1 4.1 5.2", "1.4 2.2 3.0 4.1 5.1", "1.4 2.2 3.0 4.0 5.1",
                        "1.4 2.2 3.0 4.0 5.0", "1.4 2.2 2.9 4.0 5.0", "1.3 2.1 2.9 3.9 4.9", "1.3 2.1 2.9 3.9 4.9", "1.3 2.1 2.8 3.8 4.8",
                        "1.3 2.1 2.8 3.8 4.8", "1.2 2.0 2.8 3.7 4.7", "1.2 2.0 2.8 3.7 4.6", "1.2 2.0 2.8 3.7 4.6", "1.2 2.0 2.7 3.6 4.5",
                        "1.2 2.0 2.7 3.6 4.5", "1.1 1.9 2.7 3.6 4.5", "1.1 1.9 2.6 3.5 4.4", "1.1 1.9 2.6 3.5 4.4", "1.1 1.9 2.6 3.5 4.3",
                        "1.1 1.8 2.6 3.4 4.3"},
                // female
                {"2.6 3.4 4.1 4.8 5.6", "2.5 3.3 4.0 4.8 5.6", "2.5 3.2 3.9 4.7 5.5", "2.4 3.1 3.9 4.6 5.3", "2.4 3.1 3.8 4.5 5.2",
                        "2.3 3.0 3.7 4.4 5.1", "2.3 3.0 3.6 4.3 5.0", "2.2 2.9 3.5 4.2 4.9", "2.2 2.8 3.5 4.2 4.8", "2.2 2.8 3.4 4.1 4.8",
                        "2.1 2.7 3.4 4.0 4.7", "2.1 2.7 3.4 4.0 4.6", "2.0 2.7 3.3 3.9 4.5", "2.0 2.6 3.2 3.8 4.4", "2.0 2.6 3.2 3.8 4.4",
                        "2.0 2.6 3.1 3.7 4.3", "1.9 2.5 3.1 3.6 4.2", "1.9 2.5 3.0 3.6 4.2", "1.8 2.4 3.0 3.5 4.1", "1.8 2.4 2.9 3.5 4.0",
                        "1.8 2.4 2.8 3.4 4.0", "1.8 2.3 2.8 3.4 3.9", "1.7 2.3 2.8 3.3 3.9", "1.7 2.2 2.7 3.3 3.8", "1.7 2.2 2.7 3.2 3.8",
                        "1.6 2.2 2.7 3.2 3.7", "1.6 2.2 2.6 3.2 3.7", "1.6 2.1 2.6 3.1 3.6", "1.6 2.1 2.6 3.1 3.6", "1.6 2.1 2.6 3.0 3.5",
                        "1.6 2.0 2.5 3.0 3.5", "1.6 2.0 2.5 3.0 3.4", "1.6 2.0 2.5 2.9 3.4", "1.5 2.0 2.4 2.9 3.3", "1.5 2.0 2.4 2.8 3.3",
                        "1.5 1.9 2.4 2.8 3.2", "1.5 1.9 2.3 2.8 3.2", "1.5 1.9 2.3 2.7 3.2", "1.4 1.8 2.3 2.7 3.1", "1.4 1.8 2.2 2.7 3.1",
                        "1.4 1.8 2.2 2.6 3.0", "1.4 1.8 2.2 2.6 3.0", "1.4 1.8 2.2 2.6 3.0", "1.4 1.7 2.2 2.6 2.9", "1.4 1.7 2.1 2.5 2.9",
                        "1.4 1.7 2.1 2.5 2.9", "1.3 1.7 2.1 2.5 2.8", "1.3 1.6 2.1 2.4 2.8", "1.3 1.6 2.0 2.4 2.8", "1.2 1.6 2.0 2.4 2.8",
                        "1.2 1.6 2.0 2.4 2.7"}
        };

        return uptake;
    }

    private String[] generateT6() {
        String[] table6 = {
                // 45 - 49
                "32 34 36 38 40 45 47 49 51 53 55 57 59 61 63 65 67 69 71 73 75 77 79 81 83 86 88 90 92 94 96 98 100 102 104 106 108 110 112 114 116 118 120 122 124 126",
                "32 34 35 38 40 44 46 48 50 52 54 56 58 60 62 64 66 68 70 72 74 76 78 80 82 84 86 88 90 92 94 96 98 100 102 104 106 108 110 112 114 116 118 120 122 124",
                "31 33 35 37 39 43 45 47 49 51 53 55 57 59 61 63 65 67 69 71 73 75 77 79 81 84 86 88 90 92 94 96 98 100 102 104 106 108 110 112 114 116 118 120 122 124",
                "31 33 35 37 39 42 44 46 48 50 52 54 56 58 60 62 64 66 68 70 72 74 76 78 80 82 84 86 88 90 92 94 96 98 100 102 104 106 108 110 112 114 116 118 120 122",
                "30 32 34 36 38 41 43 45 47 49 51 53 55 57 59 61 63 65 67 69 71 73 75 77 79 82 84 86 88 90 92 94 96 98 100 102 104 106 108 110 112 114 116 118 120 122",

                // 50 - 60
                "30 32 34 36 38 40 42 44 46 48 50 52 54 56 58 60 62 64 66 68 70 72 74 76 78 80 82 84 86 88 90 92 94 96 98 100 102 104 106 108 110 112 114 116 118 120",
                "29 31 33 35 37 39 41 43 45 47 49 51 53 55 57 59 61 63 65 67 69 71 73 75 76 78 80 82 84 86 88 90 92 94 96 98 100 102 104 106 108 110 112 114 116 118",
                "29 31 33 35 37 38 40 42 44 46 48 50 52 54 56 58 60 62 63 65 67 69 71 73 75 77 79 81 83 85 87 88 90 92 94 96 98 100 102 104 106 108 110 112 113 115",
                "28 30 32 34 36 38 40 42 43 45 47 49 51 53 55 57 58 60 62 64 66 68 70 72 74 75 77 79 81 83 85 87 89 91 92 94 96 98 100 102 104 106 108 109 111 113",
                "28 30 31 33 35 37 39 41 43 44 46 48 50 52 54 56 57 59 61 63 65 67 69 70 72 74 76 78 80 81 83 85 87 89 91 93 94 96 98 100 102 104 106 107 109 111",
                "27 29 31 33 35 36 38 40 42 44 45 47 49 51 53 55 56 58 60 62 64 65 67 69 71 73 75 76 78 80 82 84 85 87 89 91 93 95 96 98 100 102 104 105 107 109",
                "27 29 30 32 34 36 38 39 41 43 45 46 48 50 52 54 55 57 59 61 63 64 66 68 70 71 73 75 77 79 80 82 84 86 88 89 91 93 95 96 98 100 102 104 105 107",
                "26 28 30 32 33 35 37 39 40 42 44 46 47 49 51 53 54 56 58 60 61 63 65 67 68 70 72 74 75 77 79 81 82 84 86 88 89 91 93 95 96 98 100 102 104 105",
                "26 28 29 31 33 34 36 38 40 41 43 45 47 48 50 52 53 55 57 59 60 62 64 66 67 69 71 72 74 76 78 79 81 83 84 86 88 90 91 93 95 97 98 100 102 103",
                "25 27 29 31 32 34 36 37 39 41 42 44 46 47 49 51 53 54 56 58 59 61 63 64 66 68 69 71 73 75 76 78 80 81 83 85 86 88 90 92 93 95 97 98 100 102",
                "25 27 28 30 32 33 35 37 38 40 42 43 45 47 48 50 52 53 55 57 58 60 62 63 65 67 68 70 72 73 75 77 78 80 82 83 85 87 88 90 92 93 95 97 98 100",

                // 61 - 70
                "25 26 28 30 31 33 34 36 38 39 41 43 44 46 48 49 51 52 54 56 57 59 61 62 64 66 67 69 70 72 74 75 77 79 80 82 84 85 87 89 90 92 93 95 97 98",
                "24 26 27 29 31 32 34 35 37 39 40 42 44 45 47 48 50 52 53 55 56 58 60 61 63 65 66 68 69 71 73 74 76 77 79 81 82 84 85 87 89 90 92 94 95 97",
                "24 25 27 29 30 32 33 35 37 38 40 41 43 44 46 48 49 51 52 54 56 57 59 60 62 63 65 67 68 70 71 73 75 76 78 79 81 83 84 86 87 89 90 92 94 95",
                "23 25 27 28 30 31 33 34 36 38 39 41 42 44 45 47 48 50 52 53 55 56 58 59 61 63 64 66 67 69 70 72 73 75 77 78 80 81 83 84 86 87 89 91 92 94",
                "23 25 26 28 29 31 32 34 35 37 38 40 42 43 45 46 48 49 51 52 54 55 57 58 60 62 63 65 66 68 69 71 72 74 75 77 78 80 82 83 85 86 88 89 91 92",
                "23 24 26 27 29 30 32 33 35 36 38 39 41 42 44 45 47 48 50 52 53 55 56 58 59 61 62 64 65 67 68 70 71 73 74 76 77 79 80 82 83 85 86 88 89 91",
                "22 24 25 27 28 30 31 33 34 36 37 39 40 42 43 45 46 48 49 51 52 54 55 57 58 60 61 63 64 66 67 69 70 72 73 75 76 78 79 81 82 84 85 87 88 90",
                "22 24 25 26 28 29 31 32 34 35 37 38 40 41 43 44 46 47 49 50 51 53 54 56 57 59 60 62 63 65 66 68 69 71 72 74 75 76 78 79 81 82 84 85 87 88",
                "22 23 25 26 28 29 30 32 33 35 36 38 39 41 42 43 45 46 48 49 51 52 54 55 57 58 59 61 62 64 65 67 68 70 71 72 74 75 77 78 80 81 83 84 86 87",
                "21 23 24 26 27 29 30 31 33 34 36 37 39 40 41 43 44 46 47 49 50 51 53 54 56 57 59 60 61 63 64 66 67 69 70 71 73 74 76 77 79 80 81 83 84 86",

                // 71 -80
                "21 23 24 25 27 28 30 31 32 34 35 37 38 39 41 42 44 45 46 48 49 51 52 54 55 56 58 59 61 62 63 65 66 68 69 70 72 73 75 76 77 79 80 82 83 85",
                "21 22 24 25 26 28 29 31 32 33 35 36 38 39 40 42 43 44 46 47 49 50 51 53 54 56 57 58 60 61 63 64 65 67 68 69 71 72 74 75 76 78 79 81 82 83",
                "21 22 23 25 26 27 29 30 32 33 34 36 37 38 40 41 42 44 45 47 48 49 51 52 53 55 56 58 59 60 62 63 64 66 67 68 70 71 73 74 75 77 78 79 81 82",
                "20 22 23 24 26 27 28 30 31 32 34 35 36 38 39 41 42 43 45 46 47 49 50 51 53 54 55 57 58 59 61 62 64 65 66 68 69 70 72 72 74 76 77 78 80 81",
                "20 21 23 24 25 27 28 29 31 32 33 35 36 37 39 40 41 43 44 45 47 48 49 51 52 53 55 56 57 59 60 61 63 64 65 67 68 69 71 72 73 75 76 77 79 80",
                "20 21 22 24 25 26 28 29 30 32 33 34 36 37 38 39 41 42 43 45 46 47 49 50 51 53 54 55 57 58 59 61 62 63 64 66 67 68 70 71 72 74 75 76 78 79",
                "19 21 22 23 25 26 27 29 30 31 32 34 35 36 38 39 40 42 43 44 45 47 48 49 51 52 53 55 56 57 58 60 61 62 64 65 66 68 69 70 71 73 74 75 77 78",
                "19 21 22 23 24 26 27 28 29 31 32 33 35 36 37 38 40 41 42 44 45 46 47 49 50 51 53 54 55 56 58 59 60 62 63 64 65 67 68 69 71 72 73 74 76 77",
                "19 20 22 23 24 25 27 28 29 30 32 33 34 35 37 38 39 41 42 43 44 46 47 48 49 51 52 53 54 56 57 58 59 61 62 63 65 66 67 68 70 71 72 73 75 76",
                "19 20 21 23 24 25 26 28 29 30 31 33 34 35 36 38 39 40 41 43 44 45 46 48 49 50 51 53 54 55 56 58 59 60 61 63 64 65 66 68 69 70 71 72 74 75",

                // 81 - 90
                "19 20 21 22 23 25 26 27 28 30 31 32 33 35 36 37 38 40 41 42 43 44 46 47 48 49 51 52 53 54 56 57 58 59 60 62 63 64 65 67 68 69 70 72 73 74",
                "18 20 21 22 23 24 26 27 28 29 30 32 33 34 35 37 38 39 40 41 43 44 45 46 48 49 50 51 52 54 55 56 57 59 60 61 62 63 65 66 67 68 70 71 72 73",
                "18 19 20 22 23 24 25 27 28 29 30 31 33 34 35 36 37 39 40 41 42 43 45 46 47 48 49 51 52 53 54 55 57 58 59 60 61 63 64 65 66 67 69 70 71 72",
                "18 19 20 21 23 24 25 26 27 29 30 31 32 33 35 36 37 38 39 40 42 43 44 45 46 48 49 50 51 52 54 55 56 57 58 60 61 62 63 64 65 67 68 69 70 71",
                "18 19 20 21 22 24 25 26 27 28 29 31 32 33 34 35 36 38 39 40 41 42 44 45 46 47 48 49 51 52 53 54 55 56 58 59 60 61 62 64 65 66 67 68 69 71",
                "17 19 20 21 22 23 24 26 27 28 29 30 31 33 34 35 36 37 38 40 41 42 43 44 45 47 48 49 50 51 52 53 55 56 57 58 59 60 62 63 64 65 66 67 69 70",
                "17 18 20 21 22 23 24 25 26 28 29 30 31 32 33 34 36 37 38 39 40 41 43 44 45 46 47 48 49 51 52 53 54 55 56 57 59 60 61 62 63 64 66 67 68 69",
                "17 18 19 20 22 23 24 25 26 27 28 30 31 32 33 34 35 36 38 39 40 41 42 43 44 45 47 48 49 50 51 52 53 55 56 57 58 59 60 61 62 64 65 66 67 68",
                "17 18 19 20 21 22 24 25 26 27 28 29 30 31 33 34 35 36 37 38 39 40 42 43 44 45 46 47 48 49 51 52 53 54 55 56 57 58 60 61 62 63 64 65 66 67",
                "17 18 19 20 21 22 23 24 26 27 28 29 30 31 32 33 34 36 37 38 39 40 41 42 43 44 46 47 48 49 50 51 52 53 54 56 57 58 59 60 61 62 63 64 66 67",

                // 91 - 100
                "16 18 19 20 21 22 23 24 25 26 27 29 30 31 32 33 34 35 36 37 38 40 41 42 43 44 45 46 47 48 49 51 52 53 54 55 56 57 58 59 60 62 63 64 65 66",
                "16 17 18 20 21 22 23 24 25 26 27 28 29 30 32 33 34 35 36 37 38 39 40 41 42 43 45 46 47 48 49 50 51 52 53 54 55 57 58 59 60 61 62 63 64 65",
                "16 17 18 19 20 22 23 24 25 26 27 28 29 30 31 32 33 34 35 37 38 39 40 41 42 43 44 45 46 47 48 49 51 52 53 54 55 56 57 58 59 60 61 62 63 65",
                "16 17 18 19 20 21 22 23 24 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 59 60 61 62 63 64",
                "16 17 18 19 20 21 22 23 24 25 26 27 28 29 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 51 52 53 54 55 56 57 58 59 60 61 62 63",
                "16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59 60 61 62",
                "15 16 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 51 52 53 54 55 56 57 58 59 60 61 62",
                "15 16 17 18 19 20 21 22 23 24 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59 60 61",
                "15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 51 52 53 54 55 56 57 58 59 60 61",
                "15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59 60",

                // 101 - 110
                "14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59",
                "14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59",
                "14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59",
                "13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58",
                "13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58",
                "13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58",
                "12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57",
                "12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57",
                "12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57",
                "11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56",


                // 111 - 120
                "11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56",
                "11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56",
                "10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55",
                "10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55",
                "10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55",
                "9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54",
                "9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54",
                "9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54",
                "8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53",
                "8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53",

                // 121 - 130
                "8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53",
                "7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52",
                "7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52",
                "7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52",
                "6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51",
                "6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51",
                "6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51",
                "5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50",
                "5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50",
                "5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50",
        };

        return table6;
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
