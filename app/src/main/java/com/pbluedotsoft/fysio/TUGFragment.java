package com.pbluedotsoft.fysio;


import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
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


/**
 * A simple {@link Fragment} subclass.
 */
public class TUGFragment extends Fragment implements TextWatcher, AdapterView.OnItemSelectedListener {

    private static final String LOG_TAG = TUGFragment.class.getSimpleName();

    // Save state constant
    private static final String STATE_CONTENT = "state_content";
    private static final String STATE_HIGH_ON = "state_high_on";

    private TextView mTvSeconds, mTvHelp;
    private EditText mEtSeconds;
    private Spinner mSpinHelp;

    private Uri mTestUri;
    private int mTab, mInOut;

    private boolean mHighlightsON;

    public TUGFragment() {
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

        final View rootView = inflater.inflate(R.layout.fragment_tug, container, false);

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
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.tug_layout_background);
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

        mTvSeconds = (TextView) rootView.findViewById(R.id.tug_tv_seconds);
        mTvHelp = (TextView) rootView.findViewById(R.id.tug_tv_help);
        mEtSeconds = (EditText) rootView.findViewById(R.id.tug_et_seconds);

        mSpinHelp = (Spinner) rootView.findViewById(R.id.tug_help_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.tug_spinner_list_item,
                getResources().getStringArray(R.array.tug_help_spinner));
        mSpinHelp.setAdapter(adapter);

        // Listeners
        mEtSeconds.addTextChangedListener(this);
        mSpinHelp.setOnItemSelectedListener(this);

        // Done button
        Button btnDone = (Button) rootView.findViewById(R.id.tug_btnDone);
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
            mEtSeconds.setText(content[0]);
            int selected = 0;
            try {
                selected = Integer.parseInt(content[1]);
            } catch (NumberFormatException ex) {
                Log.d(LOG_TAG, "Gånghjälpmedel is not a number -> VinteTest v1.1");
            }
            mSpinHelp.setSelection(selected);
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
        outState.putBoolean(STATE_HIGH_ON, mHighlightsON);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    /**
     * Generate a string from information extracted for edit text views
     *
     * @return String representing edit text views
     */
    private String generateContent() {
        StringBuilder builder = new StringBuilder();
        builder.append(mEtSeconds.getText().toString().trim());
        builder.append("|");
        builder.append(mSpinHelp.getSelectedItemPosition());
        builder.append("|");
        builder.append("0|");   // Fix: split() can not handle all positions empty

        Log.d(LOG_TAG, "builder: " + builder.toString());

        return builder.toString();
    }

    /**
     * @return true if one or more question are not answered
     */
    private boolean missingAnswers() {
        boolean missing = false;
        if (mEtSeconds.getText().toString().trim().length() == 0) {
            missing = true;
        }

        return missing;
    }

    /**
     * Highlights unanswered question
     */
    private void highlightQuestions() {
        if (mHighlightsON && mEtSeconds.getText().toString().trim().length() == 0) {
            mTvSeconds.setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
        } else {
            mTvSeconds.setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
        }
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
            result = generateContent();
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

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        Log.d(LOG_TAG, "afterTextChanged");
        highlightQuestions();   // Dynamic highlighting

        // Inform parent activity
        ((TestActivity) getActivity()).setUserHasSaved(false);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
