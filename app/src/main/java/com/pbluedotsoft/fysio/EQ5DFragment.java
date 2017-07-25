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
import android.util.SparseArray;
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
import android.widget.SeekBar;
import android.widget.TextView;

import com.pbluedotsoft.fysio.data.DbContract.TestEntry;
import com.pbluedotsoft.fysio.data.EXTRAS;
import com.pbluedotsoft.fysio.data.Test;

import static java.lang.Integer.parseInt;

/**
 * A simple {@link Fragment} subclass.
 */
public class EQ5DFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {

    private static final String LOG_TAG = EQ5DFragment.class.getSimpleName();

    public static final int N_QUESTIONS = 5;    // public -> accessed from ResultTableActivity

    // Save state constant
    private static final String STATE_CONTENT = "state_content";
    private static final String STATE_RESULT = "state_result";
    private static final String STATE_HIGH_ON = "state_high_on";

    private SeekBar mSlider;
    private TextView[] mTVgroup;    // Used for highlighting
    private RadioGroup[] mRgroup;   // One radio group per question
    private int[] mPattern;         // Health mPattern pattern

    private TextView mTvResult;
    private String mResult;
    private Uri mTestUri;
    private int mTab, mInOut;

    private View mRootView;
    private boolean mHighlightsON;

    public EQ5DFragment() {
        mRgroup = new RadioGroup[N_QUESTIONS];
        mPattern = new int[N_QUESTIONS];
        mTVgroup = new TextView[5];
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

        mRootView = inflater.inflate(R.layout.fragment_eq5d, container, false);

        // IN or OUT background color adjustments
        ScrollView scroll = (ScrollView) mRootView.findViewById(R.id.scrollview);
        SeekBar seekBar = (SeekBar) mRootView.findViewById(R.id.eq5d_slider);
        if (mTab == Test.IN) {
            scroll.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_in));
            seekBar.setProgressDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.seek_bar_progress_in));

        } else {
            scroll.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_out));
            seekBar.setProgressDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.seek_bar_progress_out));
        }

        // Disable touch events in the 'other' tab
        if ((mTab == Test.IN && mInOut == 1) || (mTab == Test.OUT && mInOut == 0)) {
            disableTouchOnLayout(scroll);
        }

        // Layout background listener closes soft keyboard
        LinearLayout layout = (LinearLayout) mRootView.findViewById(R.id.eq5d_layout_background);
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
        mRgroup[0] = (RadioGroup) mRootView.findViewById(R.id.eq5d_radioGroup1);
        mRgroup[1] = (RadioGroup) mRootView.findViewById(R.id.eq5d_radioGroup2);
        mRgroup[2] = (RadioGroup) mRootView.findViewById(R.id.eq5d_radioGroup3);
        mRgroup[3] = (RadioGroup) mRootView.findViewById(R.id.eq5d_radioGroup4);
        mRgroup[4] = (RadioGroup) mRootView.findViewById(R.id.eq5d_radioGroup5);

        // Listeners
        for (int i = 0; i < mRgroup.length; i++) {
            mRgroup[i].setOnCheckedChangeListener(this);
        }

        // Questions
        mTVgroup[0] = (TextView) mRootView.findViewById(R.id.eq5d_tv_1);
        mTVgroup[1] = (TextView) mRootView.findViewById(R.id.eq5d_tv_2);
        mTVgroup[2] = (TextView) mRootView.findViewById(R.id.eq5d_tv_3);
        mTVgroup[3] = (TextView) mRootView.findViewById(R.id.eq5d_tv_4);
        mTVgroup[4] = (TextView) mRootView.findViewById(R.id.eq5d_tv_5);

        // Slider
        mSlider = (SeekBar) mRootView.findViewById(R.id.eq5d_slider);

        final TextView outputSeekBar = (TextView) mRootView.findViewById(R.id.eq5d_hälso_output_text_view);
        outputSeekBar.setText(String.valueOf(mSlider.getProgress()));

        mSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                outputSeekBar.setText(String.valueOf(progress));
                // Inform parent activity that changes have been made
                ((TestActivity) getActivity()).setUserHasSaved(false);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mTvResult = (TextView) mRootView.findViewById(R.id.eq5d_result);

        // Done button
        Button button = (Button) mRootView.findViewById(R.id.eq5d_btnDone);
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

                // mResult has valid result only when test is complete
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
                createPattern(); // needed before highlighting
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
            // Update radio buttons and total sum using info from content
            String[] content = contentStr.split("\\|");
            RadioButton radioButton;
            for (int i = 0; i < N_QUESTIONS; i++) {
                if (!content[i].trim().equals("-1")) {
                    int childIndex = parseInt(content[i].trim());
                    Log.d(LOG_TAG, "i: " + i + " childIndex: " + childIndex);
                    radioButton = (RadioButton) mRgroup[i].getChildAt(childIndex);
                    radioButton.setChecked(true);
                }
            }

            // Restore slider
            Log.d(LOG_TAG, "Slider: " + content[N_QUESTIONS]);
            mSlider.setProgress(Integer.parseInt(content[N_QUESTIONS]));

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
     * Saves content, result and status
     *
     * @return true if test is complete, false if there are answers mMissingAnswers
     */
    public boolean saveToDatabase() {
        // Test status
        int status;
        boolean complete = createPattern();
        if (complete) {
            status = Test.COMPLETED;
            // Calculate result
            mResult = getValueFromPattern();
        } else {
            status = Test.INCOMPLETED;
            mResult = "-1";
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

    /**
     * @return String content representing state of views in layout
     */
    private String generateContent() {
        // Create content
        StringBuilder builder = new StringBuilder();
        createPattern();
        for (int value : mPattern) {
            if (value != -1) {
                value--;
            }
            builder.append(value);
            builder.append("|");
        }

        // Add slider value
        builder.append(String.valueOf(mSlider.getProgress()));

        return builder.toString();
    }

    /**
     * Generates pattern array with answered questions
     */
    private boolean createPattern() {
        boolean isValid = true;
        // Check all radio groups
        View radioButton;
        for (int i = 0; i < N_QUESTIONS; i++) {
            int radioButtonID = mRgroup[i].getCheckedRadioButtonId();
            if (radioButtonID != -1) {
                radioButton = mRgroup[i].findViewById(radioButtonID);
                int index = mRgroup[i].indexOfChild(radioButton);
                mPattern[i] = index + 1;
            } else {
                mPattern[i] = -1;
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Highlights unanswered question
     */
    private void highlightQuestions() {
        for (int i = 0; i < N_QUESTIONS; i++) {
            if (mPattern[i] == -1 && mHighlightsON) {
                mTVgroup[i].setTextColor(ContextCompat.getColor(getContext(), R.color.highlight));
            } else {
                mTVgroup[i].setTextColor(ContextCompat.getColor(getContext(), R.color.textColor));
            }
        }
    }

    /**
     * Extract value from equivalence tables for given pattern
     */
    public String getValueFromPattern() {
        SparseArray<String> sparseArray = new SparseArray<>();
        sparseArray.put(11111, "1.000");
        sparseArray.put(11112, "0.848");
        sparseArray.put(11113, "0.414");
        sparseArray.put(11121, "0.796");
        sparseArray.put(11122, "0.725");
        sparseArray.put(11123, "0.291");
        sparseArray.put(11131, "0.264");
        sparseArray.put(11132, "0.193");
        sparseArray.put(11133, "0.028");
        sparseArray.put(11211, "0.883");
        sparseArray.put(11212, "0.812");
        sparseArray.put(11213, "0.378");
        sparseArray.put(11221, "0.760");
        sparseArray.put(11222, "0.689");
        sparseArray.put(11223, "0.255");
        sparseArray.put(11231, "0.228");
        sparseArray.put(11232, "0.157");
        sparseArray.put(11233, "-0.008");
        sparseArray.put(11311, "0.556");
        sparseArray.put(11312, "0.485");
        sparseArray.put(11313, "0.320");
        sparseArray.put(11321, "0.433");
        sparseArray.put(11322, "0.362");
        sparseArray.put(11323, "0.197");
        sparseArray.put(11331, "0.170");
        sparseArray.put(11332, "0.099");
        sparseArray.put(11333, "-0.066");

        sparseArray.put(12111, "0.815");
        sparseArray.put(12112, "0.744");
        sparseArray.put(12113, "0.310");
        sparseArray.put(12121, "0.692");
        sparseArray.put(12122, "0.621");
        sparseArray.put(12123, "0.187");
        sparseArray.put(12131, "0.160");
        sparseArray.put(12132, "0.089");
        sparseArray.put(12133, "-0.076");
        sparseArray.put(12211, "0.779");
        sparseArray.put(12212, "0.708");
        sparseArray.put(12213, "0.274");
        sparseArray.put(12221, "0.656");
        sparseArray.put(12222, "0.585");
        sparseArray.put(12223, "0.151");
        sparseArray.put(12231, "0.124");
        sparseArray.put(12232, "0.053");
        sparseArray.put(12233, "-0.112");
        sparseArray.put(12311, "0.452");
        sparseArray.put(12312, "0.381");
        sparseArray.put(12313, "0.216");
        sparseArray.put(12321, "0.329");
        sparseArray.put(12322, "0.258");
        sparseArray.put(12323, "0.093");
        sparseArray.put(12331, "0.066");
        sparseArray.put(12332, "-0.005");
        sparseArray.put(12333, "-0.170");

        sparseArray.put(13111, "0.436");
        sparseArray.put(13112, "0.365");
        sparseArray.put(13113, "0.200");
        sparseArray.put(13121, "0.313");
        sparseArray.put(13122, "0.242");
        sparseArray.put(13123, "0.077");
        sparseArray.put(13131, "0.050");
        sparseArray.put(13132, "-0.021");
        sparseArray.put(13133, "-0.186");
        sparseArray.put(13211, "0.400");
        sparseArray.put(13212, "0.329");
        sparseArray.put(13213, "0.164");
        sparseArray.put(13221, "0.277");
        sparseArray.put(13222, "0.206");
        sparseArray.put(13223, "0.041");
        sparseArray.put(13231, "0.014");
        sparseArray.put(13232, "-0.057");
        sparseArray.put(13233, "-0.222");
        sparseArray.put(13311, "0.342");
        sparseArray.put(13312, "0.271");
        sparseArray.put(13313, "0.106");
        sparseArray.put(13321, "0.219");
        sparseArray.put(13322, "0.143");
        sparseArray.put(13323, "-0.017");
        sparseArray.put(13331, "-0.044");
        sparseArray.put(13332, "-0.115");
        sparseArray.put(13333, "-0.280");

        sparseArray.put(21111, "0.850");
        sparseArray.put(21112, "0.779");
        sparseArray.put(21113, "0.345");
        sparseArray.put(21121, "0.727");
        sparseArray.put(21122, "0.656");
        sparseArray.put(21123, "0.222");
        sparseArray.put(21131, "0.195");
        sparseArray.put(21132, "0.142");
        sparseArray.put(21133, "-0.041");
        sparseArray.put(21211, "0.814");
        sparseArray.put(21212, "0.743");
        sparseArray.put(21213, "0.309");
        sparseArray.put(21221, "0.691");
        sparseArray.put(21222, "0.620");
        sparseArray.put(21223, "0.186");
        sparseArray.put(21231, "0.159");
        sparseArray.put(21232, "0.088");
        sparseArray.put(21233, "-0.077");
        sparseArray.put(21311, "0.487");
        sparseArray.put(21312, "0.416");
        sparseArray.put(21313, "0.251");
        sparseArray.put(21321, "0.364");
        sparseArray.put(21322, "0.293");
        sparseArray.put(21323, "0.128");
        sparseArray.put(21331, "0.101");
        sparseArray.put(21332, "0.030");
        sparseArray.put(21333, "-0.135");

        sparseArray.put(22111, "0.746");
        sparseArray.put(22112, "0.675");
        sparseArray.put(22113, "0.241");
        sparseArray.put(22121, "0.623");
        sparseArray.put(22122, "0.552");
        sparseArray.put(22123, "0.118");
        sparseArray.put(22131, "0.091");
        sparseArray.put(22132, "0.020");
        sparseArray.put(22133, "-0.145");
        sparseArray.put(22211, "0.710");
        sparseArray.put(22212, "0.639");
        sparseArray.put(22213, "0.205");
        sparseArray.put(22221, "0.587");
        sparseArray.put(22222, "0.516");
        sparseArray.put(22223, "0.082");
        sparseArray.put(22231, "0.055");
        sparseArray.put(22232, "-0.016");
        sparseArray.put(22233, "-0.181");
        sparseArray.put(22311, "0.383");
        sparseArray.put(22312, "0.312");
        sparseArray.put(22313, "0.147");
        sparseArray.put(22321, "0.260");
        sparseArray.put(22322, "0.189");
        sparseArray.put(22323, "0.024");
        sparseArray.put(22331, "-0.003");
        sparseArray.put(22332, "-0.074");
        sparseArray.put(22333, "-0.239");

        sparseArray.put(23111, "0.367");
        sparseArray.put(23112, "0.216");
        sparseArray.put(23113, "0.131");
        sparseArray.put(23121, "0.244");
        sparseArray.put(23122, "0.173");
        sparseArray.put(23123, "0.008");
        sparseArray.put(23131, "-0.019");
        sparseArray.put(23132, "-0.090");
        sparseArray.put(23133, "-0.255");
        sparseArray.put(23211, "0.331");
        sparseArray.put(23212, "0.260");
        sparseArray.put(23213, "0.095");
        sparseArray.put(23221, "0.208");
        sparseArray.put(23222, "0.137");
        sparseArray.put(23223, "-0.028");
        sparseArray.put(23231, "-0.055");
        sparseArray.put(23232, "-0.126");
        sparseArray.put(23233, "-0.291");
        sparseArray.put(23311, "0.273");
        sparseArray.put(23312, "0.202");
        sparseArray.put(23313, "0.037");
        sparseArray.put(23321, "0.150");
        sparseArray.put(23322, "0.079");
        sparseArray.put(23323, "-0.086");
        sparseArray.put(23331, "-0.113");
        sparseArray.put(23332, "-0.184");
        sparseArray.put(23333, "-0.349");

        sparseArray.put(31111, "0.336");
        sparseArray.put(31112, "0.265");
        sparseArray.put(31113, "0.100");
        sparseArray.put(31121, "0.213");
        sparseArray.put(31122, "0.142");
        sparseArray.put(31123, "-0.023");
        sparseArray.put(31131, "-0.050");
        sparseArray.put(31132, "-0.121");
        sparseArray.put(31133, "-0.286");
        sparseArray.put(31211, "0.300");
        sparseArray.put(31212, "0.229");
        sparseArray.put(31213, "0.064");
        sparseArray.put(31221, "0.177");
        sparseArray.put(31222, "0.106");
        sparseArray.put(31223, "-0.059");
        sparseArray.put(31231, "-0.086");
        sparseArray.put(31232, "-0.157");
        sparseArray.put(31233, "-0.322");
        sparseArray.put(31311, "0.242");
        sparseArray.put(31312, "0.171");
        sparseArray.put(31313, "0.006");
        sparseArray.put(31321, "0.119");
        sparseArray.put(31322, "0.048");
        sparseArray.put(31323, "-0.117");
        sparseArray.put(31331, "-0.144");
        sparseArray.put(31332, "-0.215");
        sparseArray.put(31333, "-0.380");

        sparseArray.put(32111, "0.232");
        sparseArray.put(32112, "0.161");
        sparseArray.put(32113, "-0.004");
        sparseArray.put(32121, "0.109");
        sparseArray.put(32122, "0.038");
        sparseArray.put(32123, "-0.127");
        sparseArray.put(32131, "-0.154");
        sparseArray.put(32132, "-0.225");
        sparseArray.put(32133, "-0.390");
        sparseArray.put(32211, "0.196");
        sparseArray.put(32212, "0.125");
        sparseArray.put(32213, "-0.040");
        sparseArray.put(32221, "0.073");
        sparseArray.put(32222, "0.002");
        sparseArray.put(32223, "-0.163");
        sparseArray.put(32231, "-0.190");
        sparseArray.put(32232, "-0.261");
        sparseArray.put(32233, "-0.426");
        sparseArray.put(32311, "0.138");
        sparseArray.put(32312, "0.067");
        sparseArray.put(32313, "-0.098");
        sparseArray.put(32321, "0.015");
        sparseArray.put(32322, "-0.056");
        sparseArray.put(32323, "-0.221");
        sparseArray.put(32331, "-0.248");
        sparseArray.put(32332, "-0.319");
        sparseArray.put(32333, "-0.484");

        sparseArray.put(33111, "0.122");
        sparseArray.put(33112, "0.051");
        sparseArray.put(33113, "-0.114");
        sparseArray.put(33121, "-0.001");
        sparseArray.put(33122, "-0.072");
        sparseArray.put(33123, "-0.237");
        sparseArray.put(33131, "-0.264");
        sparseArray.put(33132, "-0.335");
        sparseArray.put(33133, "-0.500");
        sparseArray.put(33211, "0.086");
        sparseArray.put(33212, "0.015");
        sparseArray.put(33213, "-0.150");
        sparseArray.put(33221, "-0.037");
        sparseArray.put(33222, "-0.108");
        sparseArray.put(33223, "-0.273");
        sparseArray.put(33231, "-0.300");
        sparseArray.put(33232, "-0.371");
        sparseArray.put(33233, "-0.536");
        sparseArray.put(33311, "0.028");
        sparseArray.put(33312, "-0.043");
        sparseArray.put(33313, "-0.208");
        sparseArray.put(33321, "-0.095");
        sparseArray.put(33322, "-0.166");
        sparseArray.put(33323, "-0.331");
        sparseArray.put(33331, "-0.358");
        sparseArray.put(33332, "-0.429");
        sparseArray.put(33333, "-0.594");

        // Generate key from int[]
        int key = mPattern[0] * 10000 + mPattern[1] * 1000 + mPattern[2] * 100 + mPattern[3] * 10 + mPattern[4];

        Log.d(LOG_TAG, "key: " + key);

        return sparseArray.get(key);
    }

    /**
     * JUnit helper method
     */
    public String getValueFromPattern(int[] pattern) {
        mPattern = pattern;
        return getValueFromPattern();
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // Result update only when all radio groups have selected radio buttons
        if (createPattern()) {
            mResult = getValueFromPattern();
            mTvResult.setText(mResult);
        }

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
