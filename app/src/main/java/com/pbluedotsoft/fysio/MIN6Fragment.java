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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.pbluedotsoft.fysio.data.DbContract.TestEntry;
import com.pbluedotsoft.fysio.data.EXTRAS;
import com.pbluedotsoft.fysio.data.Test;

/**
 * A simple {@link Fragment} subclass.
 */
public class MIN6Fragment extends Fragment implements TextWatcher, AdapterView.OnItemSelectedListener {

    private static final String LOG_TAG = MIN6Fragment.class.getSimpleName();

    // Save state constant
    private static final String STATE_CONTENT = "state_content";
    private static final String STATE_HIGH_ON = "state_high_on";
    private static final String STATE_TICK_COUNTER = "state_tick_counter";

    private Spinner mSpinHelp;
    private Spinner spRpeStart, spRpeFinish, spCR10Start, spCR10Finish;
    private EditText mETmeters, mETpulseStart, mETpulseFinish;
    private TextView mTVmeters, mTVhelp, mTVpulseStart, mTVpulseFinish, mTVcr10Start, mTVcr10Finish,
            mTVRpeStart, mTVRpeFinish;
    private ImageButton mBtnMeterMinus, mBtnMeterPlus;
    private TextView mTvMeterTicks;
    private int mTickCounter;

    private Uri mTestUri;
    private int mTab, mInOut;
    private boolean mHighlightsON;

    public MIN6Fragment() {
        // Required empty public constructor
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

        final View rootView = inflater.inflate(R.layout.fragment_min6, container, false);

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
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.min6_layout_background);
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

        // Spinners CR10
        spCR10Start = (Spinner) rootView.findViewById(R.id.min6_sp_cr10_test_start);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.min6_sp_cr10_values,
                R.layout.min6_spinner_list_item);
        spCR10Start.setAdapter(adapter);

        spCR10Finish = (Spinner) rootView.findViewById(R.id.min6_sp_cr10_test_finish);
        adapter = ArrayAdapter.createFromResource(getContext(), R.array.min6_sp_cr10_values,
                R.layout.min6_spinner_list_item);
        spCR10Finish.setAdapter(adapter);

        // Spinners RPE
        spRpeStart = (Spinner) rootView.findViewById(R.id.min6_sp_rpe_test_start);
        adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.min6_sp_rpe_values, R.layout.min6_spinner_list_item);
        spRpeStart.setAdapter(adapter);

        spRpeFinish = (Spinner) rootView.findViewById(R.id.min6_sp_rpe_test_finish);
        adapter = ArrayAdapter.createFromResource(getContext(), R.array.min6_sp_rpe_values,
                R.layout.min6_spinner_list_item);
        spRpeFinish.setAdapter(adapter);

        // Spinner help
        mSpinHelp = (Spinner) rootView.findViewById(R.id.min6_help_spinner);
        ArrayAdapter<String> helpAdapter = new ArrayAdapter<>(getContext(),
                R.layout.min6_help_spinner_list_item,
                getResources().getStringArray(R.array.min6_help_spinner));
        mSpinHelp.setAdapter(helpAdapter);
        mSpinHelp.setOnItemSelectedListener(this);

        // Edit text
        mETmeters = (EditText) rootView.findViewById(R.id.min6_et_meters);
        mETpulseStart = (EditText) rootView.findViewById(R.id.min6_et_start_pulse);
        mETpulseFinish = (EditText) rootView.findViewById(R.id.min6_et_finish_pulse);

        // Listeners
        mETmeters.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
//                Log.d(LOG_TAG, "Update only mETmeters");
                if (!mETmeters.getText().toString().equals(""))
                    mTickCounter = Integer.parseInt(mETmeters.getText().toString()) / 30;

                updateTickDisplay();

                highlightQuestions();   // Dynamic highlighting

                // Inform parent activity
                ((TestActivity) getActivity()).setUserHasSaved(false);
            }
        });

        mETpulseStart.addTextChangedListener(this);
        mETpulseFinish.addTextChangedListener(this);

        // Text views - Labels
        mTVmeters = (TextView) rootView.findViewById(R.id.min6_tv_meters);
        mTVhelp = (TextView) rootView.findViewById(R.id.min6_tv_help);
        mTVpulseStart = (TextView) rootView.findViewById(R.id.min6_tv_start_pulse);
        mTVpulseFinish = (TextView) rootView.findViewById(R.id.min6_tv_finish_pulse);
        mTVcr10Start = (TextView) rootView.findViewById(R.id.min6_tv_cr10_test_start);
        mTVcr10Finish = (TextView) rootView.findViewById(R.id.min6_tv_cr10_test_finish);
        mTVRpeStart = (TextView) rootView.findViewById(R.id.min6_tv_rpe_test_start);
        mTVRpeFinish = (TextView) rootView.findViewById(R.id.min6_tv_rpe_test_finish);

        // Meter ticks
        mTvMeterTicks = (TextView) rootView.findViewById(R.id.min6_tv_meter_ticks);
        mBtnMeterMinus = (ImageButton) rootView.findViewById(R.id.min6_btn_meter_minus);
        mBtnMeterPlus = (ImageButton) rootView.findViewById(R.id.min6_btn_meter_plus);

        mBtnMeterMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTickCounter != 0) {
                    mTickCounter--;
                    int meters = mTickCounter * 30;
                    mETmeters.setText(String.valueOf(meters));
                }
            }
        });

        mBtnMeterPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTickCounter < 30) {
                    mTickCounter++;
                    int meters = mTickCounter * 30;
                    mETmeters.setText(String.valueOf(meters));
                }
            }
        });

        // Done button
        Button btnDone = (Button) rootView.findViewById(R.id.min6_btnDone);
        btnDone.setOnClickListener(new View.OnClickListener() {
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
                highlightQuestions();
            }
            mTickCounter = savedInstanceState.getInt(STATE_TICK_COUNTER);
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
            // Set edit text views
            String[] content = contentStr.split("\\|");
            if (content[0].equals("")) {
                content[0] = "0";
                mTickCounter = 0;
            } else {
                mTickCounter = Integer.parseInt(content[0]) / 30;
            }

            mETmeters.setText(content[0]);
            int selected = 0;
            try {
                selected = Integer.parseInt(content[1]);
            } catch (NumberFormatException ex) {
                Log.d(LOG_TAG, "Gånghjälpmedel is not a number -> VinteTest v1.1");
            }
            mSpinHelp.setSelection(selected);
            mETpulseStart.setText(content[2]);
            mETpulseFinish.setText(content[3]);
            // Set spinners
            spCR10Start.setSelection(Integer.parseInt(content[4]));
            spCR10Finish.setSelection(Integer.parseInt(content[5]));
            spRpeStart.setSelection(Integer.parseInt(content[6]));
            spRpeFinish.setSelection(Integer.parseInt(content[7]));
        }

        // Update tick display
        if (mTickCounter > 0) {
            updateTickDisplay();
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // TRICK
        // Setting listeners after onCreateView + calling setSelection with false
        // avoids that first onItemSelected call per spinner that ruins my navigation 'save changes?'

        // Call selection before setting listener does the trick, but it has to be done
        // after onCreateView
        spCR10Start.setSelection(spCR10Start.getSelectedItemPosition(), false);
        spCR10Finish.setSelection(spCR10Finish.getSelectedItemPosition(), false);
        spRpeStart.setSelection(spRpeStart.getSelectedItemPosition(), false);
        spRpeFinish.setSelection(spRpeFinish.getSelectedItemPosition(), false);

        // Listener
        spCR10Start.setOnItemSelectedListener(this);
        spCR10Finish.setOnItemSelectedListener(this);
        spRpeStart.setOnItemSelectedListener(this);
        spRpeFinish.setOnItemSelectedListener(this);

        // Inform parent activity that form is up to date
        ((TestActivity) getActivity()).setUserHasSaved(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save state for views in layout
        String content = generateContent();
        outState.putString(STATE_CONTENT, content);
        outState.putBoolean(STATE_HIGH_ON, mHighlightsON);
        outState.putInt(STATE_TICK_COUNTER, mTickCounter);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    private void updateTickDisplay() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mTickCounter; i++) {
            if (i % 5 == 0) {
                builder.append(" ");
            }
            builder.append("|");
        }

        mTvMeterTicks.setText(builder.toString());
    }

    public boolean saveToDatabase() {
        Log.d(LOG_TAG, "saveToDatabase");
        ContentValues values = new ContentValues();

        // Test status
        boolean missing = missingAnswers();
        int status;
        String result = "-1";
        if (missing) {
            status = Test.INCOMPLETED;
        } else {
            status = Test.COMPLETED;
            // Result is a String of values in this test in particular
            result = generateResult();
        }

        // Values
        if (mTab == Test.IN) {
            values.put(TestEntry.COLUMN_CONTENT_IN, generateContent());
            values.put(TestEntry.COLUMN_RESULT_IN, result);
            values.put(TestEntry.COLUMN_STATUS_IN, status);
        } else {
            values.put(TestEntry.COLUMN_CONTENT_OUT, generateContent());
            values.put(TestEntry.COLUMN_RESULT_OUT, result);
            values.put(TestEntry.COLUMN_STATUS_OUT, status);
        }

        int rows = getActivity().getContentResolver().update(mTestUri, values, null, null);
        Log.d(LOG_TAG, "rows updated: " + rows);

        return !missing;
    }

    /**
     * String representing the state of the views in layout
     */
    private String generateResult() {
        StringBuilder builder = new StringBuilder();
        builder.append(mETmeters.getText().toString().trim());
        builder.append("|");
        builder.append(mSpinHelp.getSelectedItemPosition());
        builder.append("|");
        builder.append(mETpulseStart.getText().toString().trim());
        builder.append("|");
        builder.append(mETpulseFinish.getText().toString().trim());
        builder.append("|");

        SparseArray<String> mapCR10 = new SparseArray<>(20);
        mapCR10.append(1, "0");
        mapCR10.append(2, "0.3");
        mapCR10.append(3, "0.5");
        mapCR10.append(4, "1");
        mapCR10.append(5, "1.5");
        mapCR10.append(6, "2");
        mapCR10.append(7, "2.5");
        mapCR10.append(8, "3");
        mapCR10.append(9, "4");
        mapCR10.append(10, "5");
        mapCR10.append(11, "6");
        mapCR10.append(12, "7");
        mapCR10.append(13, "8");
        mapCR10.append(14, "9");
        mapCR10.append(15, "10");
        mapCR10.append(16, "11");
        mapCR10.append(17, "* max");

        // CR10 start
        builder.append(mapCR10.get(spCR10Start.getSelectedItemPosition()));
        builder.append("|");

        // CR10 finish
        builder.append(mapCR10.get(spCR10Finish.getSelectedItemPosition()));
        builder.append("|");

        // RPE start
        builder.append(String.valueOf(spRpeStart.getSelectedItemPosition() + 5));
        builder.append("|");

        // RPE finish
        builder.append(String.valueOf(spRpeFinish.getSelectedItemPosition() + 5));

        Log.d(LOG_TAG, "generate result: " + builder.toString());
        return builder.toString();
    }

    /**
     * Save content for each view in the layout
     *
     * @return String representing state for radio groups in layout
     */
    private String generateContent() {
        StringBuilder builder = new StringBuilder();
        builder.append(mETmeters.getText().toString().trim());
        builder.append("|");
        builder.append(mSpinHelp.getSelectedItemPosition());
        builder.append("|");
        builder.append(mETpulseStart.getText().toString().trim());
        builder.append("|");
        builder.append(mETpulseFinish.getText().toString().trim());
        builder.append("|");
        builder.append(String.valueOf(spCR10Start.getSelectedItemPosition()));
        builder.append("|");
        builder.append(String.valueOf(spCR10Finish.getSelectedItemPosition()));
        builder.append("|");
        builder.append(String.valueOf(spRpeStart.getSelectedItemPosition()));
        builder.append("|");
        builder.append(String.valueOf(spRpeFinish.getSelectedItemPosition()));

        Log.d(LOG_TAG, "builder: " + builder.toString());

        return builder.toString();
    }

    /**
     * Highlights unanswered question
     */
    private void highlightQuestions() {
        if (mHighlightsON && mETmeters.getText().toString().trim().length() == 0) {
            mTVmeters.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTVmeters.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }

//        if (mHighlightsON && mETpulseStart.getText().toString().trim().length() == 0) {
//            mTVpulseStart.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
//        } else {
//            mTVpulseStart.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
//        }
//
//        if (mHighlightsON && mETpulseFinish.getText().toString().trim().length() == 0) {
//            mTVpulseFinish.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
//        } else {
//            mTVpulseFinish.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
//        }

        if (mHighlightsON && spCR10Start.getSelectedItemPosition() == 0) {
            mTVcr10Start.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTVcr10Start.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }

        if (mHighlightsON && spCR10Finish.getSelectedItemPosition() == 0) {
            mTVcr10Finish.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTVcr10Finish.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }

        if (mHighlightsON && spRpeStart.getSelectedItemPosition() == 0) {
            mTVRpeStart.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTVRpeStart.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }

        if (mHighlightsON && spRpeFinish.getSelectedItemPosition() == 0) {
            mTVRpeFinish.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTVRpeFinish.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }
    }

    /**
     * @return true if one or more question are not answered
     */
    private boolean missingAnswers() {
        boolean missing = false;
        if (mETmeters.getText().toString().trim().length() == 0) {
            missing = true;
        }

//        if (mETpulseStart.getText().toString().trim().length() == 0) {
//            missing = true;
//        }
//
//        if (mETpulseFinish.getText().toString().trim().length() == 0) {
//            missing = true;
//        }

        if (spCR10Start.getSelectedItemPosition() == 0) {
            missing = true;
        }

        if (spCR10Finish.getSelectedItemPosition() == 0) {
            missing = true;
        }

        if (spRpeStart.getSelectedItemPosition() == 0) {
            missing = true;
        }

        if (spRpeFinish.getSelectedItemPosition() == 0) {
            missing = true;
        }

        return missing;
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
        highlightQuestions();   // Dynamic highlighting

        // Inform parent activity
        ((TestActivity) getActivity()).setUserHasSaved(false);
    }

    /**
     * Spinner listener interface method implementation
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(LOG_TAG, "onItemSelected");
        highlightQuestions();   // Dynamic highlighting

        if (((TestActivity) getActivity()).mUserInteracting) {
            // Inform parent activity
            ((TestActivity) getActivity()).setUserHasSaved(false);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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
