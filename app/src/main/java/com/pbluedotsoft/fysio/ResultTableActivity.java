package com.pbluedotsoft.fysio;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import com.pbluedotsoft.fysio.data.DbContract.TestEntry;
import com.pbluedotsoft.fysio.data.EXTRAS;
import com.pbluedotsoft.fysio.data.Test;

public class ResultTableActivity extends AppCompatActivity {

    private static final String LOG_TAG = ResultTableActivity.class.getSimpleName();

    private int mUserID, mPatientID;
    private String mUserName;
    private String mHeaderString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_result_table);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Extract info from Bundle
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUserID = extras.getInt(EXTRAS.KEY_USER_ID, mUserID);
            mUserName = extras.getString(EXTRAS.KEY_USER_NAME);
            mPatientID = extras.getInt(EXTRAS.KEY_PATIENT_ID);
            mHeaderString = extras.getString(EXTRAS.KEY_HEADER);
            Log.d(LOG_TAG, "Getting extras from Bundle -> mPatientID: " + mPatientID +
                    " mHeader: " + mHeaderString);
        }

        // Activity's title
        setTitle(mHeaderString);

        // Get all tests for patient
        String selection = TestEntry.COLUMN_PATIENT_ID_FK + "=?";
        String[] selectionArgs = {String.valueOf(mPatientID)};
        Cursor cursor = getContentResolver().query(TestEntry.CONTENT_URI, null, selection,
                selectionArgs, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            Log.d(LOG_TAG, "test count: " + cursor.getCount());
            // Process each test, extract result and populate table
            while (cursor.moveToNext()) {
                String testCode = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CODE));
                switch (testCode) {
                    case "EQ5D":
                        handleEQ5D(cursor);
                        break;
                    case "VAS":
                        handleVAS(cursor);
                        break;
                    case "TUG":
                        handleTUG(cursor);
                        break;
                    case "6MIN":
                        handleMIN6(cursor);
                        break;
                    case "BERGS":
                        handleBERGS(cursor);
                        break;
                    case "BDL":
                        handleBDL(cursor);
                        break;
                    case "IMF":
                        handleIMF(cursor);
                        break;
                    case "BASMI":
                        handleBASMI(cursor);
                        break;
                    case "FSA":
                        handleFSA(cursor);
                        break;
                    case "FSS":
                        handleFSS(cursor);
                        break;
                    case "BASFI":
                        handleBASFI(cursor);
                        break;
                    case "BASDAI":
                        handleBASDAI(cursor);
                        break;
                    case "TST":
                        handleTST(cursor);
                        break;
                    case "BASG":
                        handleBASG(cursor);
                        break;
                    case "ERGO":
                        handleErgo(cursor);
                        break;
                    case "IPAQ":
                        handleIPAQ(cursor);
                        break;
                    case "OTT":
                        handleOTT(cursor);
                        break;
                    case "THORAX":
                        handleThorax(cursor);
                        break;
                    default:
                        break;
                }
            }
        } else {
            Log.d(LOG_TAG, "cursor error for patient id: " + mPatientID);
        }

        if (cursor != null)
            cursor.close();
    }

    /**
     * Navigate up
     */
    private void goBackToPatientListActivity() {
        Intent upIntent = NavUtils.getParentActivityIntent(ResultTableActivity.this);
        upIntent.putExtra(EXTRAS.KEY_USER_ID, mUserID);
        upIntent.putExtra(EXTRAS.KEY_USER_NAME, mUserName);
        NavUtils.navigateUpTo(ResultTableActivity.this, upIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Log.d(LOG_TAG, "Back from ResultTableActivity: arrow");
                goBackToPatientListActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.d(LOG_TAG, "Back from ResultTableActivity: back pressed");
        goBackToPatientListActivity();
    }

    private void handleEQ5D(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvIn = (TextView) findViewById(R.id.table_eq5d_in);
            TextView tvHealthIn = (TextView) findViewById(R.id.table_eq5d_health_in);
            String resultIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            tvIn.setText(resultIn);

            // slider
            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
            String[] contentArray = contentIn.split("\\|");
            tvHealthIn.setText(contentArray[EQ5DFragment.N_QUESTIONS]);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvOut = (TextView) findViewById(R.id.table_eq5d_out);
            TextView tvHealthOut = (TextView) findViewById(R.id.table_eq5d_health_out);
            String resultOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            tvOut.setText(resultOut);

            // slider
            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
            String[] contentArray = contentOut.split("\\|");
            tvHealthOut.setText(contentArray[EQ5DFragment.N_QUESTIONS]);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_eq5d_notes);
            notes.setText("*");
        }
    }

    private void handleVAS(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvKonIn = (TextView) findViewById(R.id.table_vas_kon_in);
            TextView tvSmaIn = (TextView) findViewById(R.id.table_vas_sma_in);
            TextView tvSteIn = (TextView) findViewById(R.id.table_vas_ste_in);
            TextView tvTroIn = (TextView) findViewById(R.id.table_vas_tro_in);
//            TextView tvGenIn = (TextView) findViewById(R.id.table_vas_gen_in);

            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
            String[] contentArray = contentIn.split("\\|");
            tvKonIn.setText(contentArray[0]);
            tvSmaIn.setText(contentArray[1]);
            tvSteIn.setText(contentArray[2]);
            tvTroIn.setText(contentArray[3]);
//            tvGenIn.setText(contentArray[4]);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvKonOut = (TextView) findViewById(R.id.table_vas_kon_out);
            TextView tvSmaOut = (TextView) findViewById(R.id.table_vas_sma_out);
            TextView tvSteOut = (TextView) findViewById(R.id.table_vas_ste_out);
            TextView tvTroOut = (TextView) findViewById(R.id.table_vas_tro_out);
//            TextView tvGenOut = (TextView) findViewById(R.id.table_vas_gen_out);

            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
            String[] contentArray = contentOut.split("\\|");
            tvKonOut.setText(contentArray[0]);
            tvSmaOut.setText(contentArray[1]);
            tvSteOut.setText(contentArray[2]);
            tvTroOut.setText(contentArray[3]);
//            tvGenOut.setText(contentArray[4]);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_vas_notes);
            notes.setText("*");
        }
    }

    private void handleTUG(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvIn = (TextView) findViewById(R.id.table_tug_in);
            TextView tvHelpIn = (TextView) findViewById(R.id.table_tug_help_in);

            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
            String[] content = contentIn.split("\\|");
            tvIn.setText(content[0]);
            String help;
            switch (content[1]) {
                case "0":
                    help = "-";
                    break;
                case "1":
                    help = "rollator";
                    break;
                case "2":
                    help = "käpp";
                    break;
                case "3":
                    help = "1 kkp";
                    break;
                case "4":
                    help = "2 kkp";
                    break;
                case "5":
                    help = "ortos";
                    break;
                default:
                    Log.d(LOG_TAG, "Compatibility mode with v1.1");
                    help = content[1];

            }
            tvHelpIn.setText(help);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvOut = (TextView) findViewById(R.id.table_tug_out);
            TextView tvHelpOut = (TextView) findViewById(R.id.table_tug_help_out);

            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
            String[] content = contentOut.split("\\|");
            tvOut.setText(content[0]);
            String help;
            switch (content[1]) {
                case "0":
                    help = "-";
                    break;
                case "1":
                    help = "rollator";
                    break;
                case "2":
                    help = "käpp";
                    break;
                case "3":
                    help = "1 kkp";
                    break;
                case "4":
                    help = "2 kkp";
                    break;
                case "5":
                    help = "ortos";
                    break;
                default:
                    Log.d(LOG_TAG, "Compatibility mode with v1.1");
                    help = content[1];

            }
            tvHelpOut.setText(help);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_tug_notes);
            notes.setText("*");
        }
    }

    private void handleMIN6(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvMeter = (TextView) findViewById(R.id.table_6min_meter_in);
            TextView tvHelp = (TextView) findViewById(R.id.table_6min_help_in);
            TextView tvPulseStart = (TextView) findViewById(R.id.table_6min_start_pulse_in);
            TextView tvPulseEnd = (TextView) findViewById(R.id.table_6min_end_pulse_in);
            TextView tvCR10Start = (TextView) findViewById(R.id.table_6min_cr10_start_in);
            TextView tvCR10End = (TextView) findViewById(R.id.table_6min_cr10_end_in);
            TextView tvRpeStart = (TextView) findViewById(R.id.table_6min_rpe_start_in);
            TextView tvRpeEnd = (TextView) findViewById(R.id.table_6min_rpe_end_in);

            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            String[] content = contentIn.split("\\|");
            tvMeter.setText(content[0]);
            String help;
            switch (content[1]) {
                case "0":
                    help = "-";
                    break;
                case "1":
                    help = "rollator";
                    break;
                case "2":
                    help = "käpp";
                    break;
                case "3":
                    help = "1 kkp";
                    break;
                case "4":
                    help = "2 kkp";
                    break;
                case "5":
                    help = "ortos";
                    break;
                default:
                    Log.d(LOG_TAG, "Compatibility mode with v1.1");
                    help = content[1];

            }
            tvHelp.setText(help);
            tvPulseStart.setText(content[2]);
            tvPulseEnd.setText(content[3]);
            tvCR10Start.setText(content[4]);
            tvCR10End.setText(content[5]);
            tvRpeStart.setText(content[6]);
            tvRpeEnd.setText(content[7]);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvMeter = (TextView) findViewById(R.id.table_6min_meter_out);
            TextView tvHelp = (TextView) findViewById(R.id.table_6min_help_out);
            TextView tvPulseStart = (TextView) findViewById(R.id.table_6min_start_pulse_out);
            TextView tvPulseEnd = (TextView) findViewById(R.id.table_6min_end_pulse_out);
            TextView tvCR10Start = (TextView) findViewById(R.id.table_6min_cr10_start_out);
            TextView tvCR10End = (TextView) findViewById(R.id.table_6min_cr10_end_out);
            TextView tvRpeStart = (TextView) findViewById(R.id.table_6min_rpe_start_out);
            TextView tvRpeEnd = (TextView) findViewById(R.id.table_6min_rpe_end_out);

            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            String[] content = contentOut.split("\\|");
            tvMeter.setText(content[0]);
            String help;
            switch (content[1]) {
                case "0":
                    help = "-";
                    break;
                case "1":
                    help = "rollator";
                    break;
                case "2":
                    help = "käpp";
                    break;
                case "3":
                    help = "1 kkp";
                    break;
                case "4":
                    help = "2 kkp";
                    break;
                case "5":
                    help = "ortos";
                    break;
                default:
                    Log.d(LOG_TAG, "Compatibility mode with v1.1");
                    help = content[1];

            }
            tvHelp.setText(help);
            tvPulseStart.setText(content[2]);
            tvPulseEnd.setText(content[3]);
            tvCR10Start.setText(content[4]);
            tvCR10End.setText(content[5]);
            tvRpeStart.setText(content[6]);
            tvRpeEnd.setText(content[7]);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_6min_notes);
            notes.setText("*");
        }
    }

    private void handleBERGS(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvResultIn = (TextView) findViewById(R.id.table_bergs_result_in);
            String resultIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            tvResultIn.setText(resultIn);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvResultOut = (TextView) findViewById(R.id.table_bergs_result_out);
            String resultOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            tvResultOut.setText(resultOut);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_bergs_result_notes);
            notes.setText("*");
        }
    }

    private void handleBDL(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvResultIn = (TextView) findViewById(R.id.table_bdl_result_in);
            String resultIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            tvResultIn.setText(resultIn);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvResultOut = (TextView) findViewById(R.id.table_bdl_result_out);
            String resultOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            tvResultOut.setText(resultOut);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_bdl_result_notes);
            notes.setText("*");
        }
    }

    private void handleIMF(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvResultIn = (TextView) findViewById(R.id.table_imf_result_in);
            String resultIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            tvResultIn.setText(resultIn);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvResultOut = (TextView) findViewById(R.id.table_imf_result_out);
            String resultOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            tvResultOut.setText(resultOut);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_imf_result_notes);
            notes.setText("*");
        }
    }

    private void handleGeneric(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {

        }
    }

    private void handleBASMI(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvResultIn = (TextView) findViewById(R.id.table_basmi_result_in);
            String resultIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            tvResultIn.setText(resultIn);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvResultOut = (TextView) findViewById(R.id.table_basmi_result_out);
            String resultOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            tvResultOut.setText(resultOut);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_basmi_result_notes);
            notes.setText("*");
        }
    }

    private void handleFSA(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            String[] part = result.split("\\|");

            if (!part[0].equals("-1")) {
                TextView tvSumH = (TextView) findViewById(R.id.table_fsa_sum_h_in);
                tvSumH.setText(part[0]);
            }

            if (!part[1].equals("-1")) {
                TextView tvSumV = (TextView) findViewById(R.id.table_fsa_sum_v_in);
                tvSumV.setText(part[1]);
            }

            if (!part[2].equals("-1")) {
                TextView tvTotal = (TextView) findViewById(R.id.table_fsa_total_in);
                tvTotal.setText(part[2]);
            }

            if (!part[3].equals("-1")) {
                TextView tvSmartH = (TextView) findViewById(R.id.table_fsa_smart_h_in);
                tvSmartH.setText(part[3]);
            }

            if (!part[4].equals("-1")) {
                TextView tvSmartV = (TextView) findViewById(R.id.table_fsa_smart_v_in);
                tvSmartV.setText(part[4]);
            }

            if (!part[5].equals("-1")) {
                TextView tvSmartV = (TextView) findViewById(R.id.table_fsa_smart_total_in);
                tvSmartV.setText(part[5]);
            }
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            String[] part = result.split("\\|");

            if (!part[0].equals("-1")) {
                TextView tvSumH = (TextView) findViewById(R.id.table_fsa_sum_h_out);
                tvSumH.setText(part[0]);
            }

            if (!part[1].equals("-1")) {
                TextView tvSumV = (TextView) findViewById(R.id.table_fsa_sum_v_out);
                tvSumV.setText(part[1]);
            }

            if (!part[2].equals("-1")) {
                TextView tvTotal = (TextView) findViewById(R.id.table_fsa_total_out);
                tvTotal.setText(part[2]);
            }

            if (!part[3].equals("-1")) {
                TextView tvSmartH = (TextView) findViewById(R.id.table_fsa_smart_h_out);
                tvSmartH.setText(part[3]);
            }

            if (!part[4].equals("-1")) {
                TextView tvSmartV = (TextView) findViewById(R.id.table_fsa_smart_v_out);
                tvSmartV.setText(part[4]);
            }

            if (!part[5].equals("-1")) {
                TextView tvSmartV = (TextView) findViewById(R.id.table_fsa_smart_total_out);
                tvSmartV.setText(part[5]);
            }
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_fsa_notes);
            notes.setText("*");
        }
    }

    private void handleFSS(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_fss_in);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            tvResult.setText(result);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_fss_out);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            tvResult.setText(result);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_fss_notes);
            notes.setText("*");
        }
    }

    private void handleBASFI(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_basfi_in);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            tvResult.setText(result);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_basfi_out);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            tvResult.setText(result);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_basfi_notes);
            notes.setText("*");
        }
    }

    private void handleBASDAI(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_basdai_in);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            tvResult.setText(result);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_basdai_out);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            tvResult.setText(result);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_basdai_notes);
            notes.setText("*");
        }
    }

    private void handleTST(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_tst_in);
            String content = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
            String[] parts = content.split("\\|");
            tvResult.setText(parts[0]);
            // Skor
            TextView tvShoes = (TextView) findViewById(R.id.table_tst_shoes_in);
            if (parts[1].equals("1"))
                tvShoes.setText("med");
            else
                tvShoes.setText("utan");
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_tst_out);
            String content = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
            String[] parts = content.split("\\|");
            tvResult.setText(parts[0]);
            // Skor
            TextView tvShoes = (TextView) findViewById(R.id.table_tst_shoes_out);
            if (parts[1].equals("1"))
                tvShoes.setText("med");
            else
                tvShoes.setText("utan");
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_tst_notes);
            notes.setText("*");
        }
    }

    private void handleBASG(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvBasg1 = (TextView) findViewById(R.id.table_basg1_in);
            TextView tvBasg2 = (TextView) findViewById(R.id.table_basg2_in);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            String[] part = result.split("\\|");
            tvBasg1.setText(part[0]);
            tvBasg2.setText(part[1]);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvBasg1 = (TextView) findViewById(R.id.table_basg1_out);
            TextView tvBasg2 = (TextView) findViewById(R.id.table_basg2_out);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            String[] part = result.split("\\|");
            tvBasg1.setText(part[0]);
            tvBasg2.setText(part[1]);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_basg1_notes);
            notes.setText("*");
        }
    }

    private void handleErgo(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_ergo_result_in);
            TextView tvVikt = (TextView) findViewById(R.id.table_ergo_vikt_in);
            TextView tvLängd = (TextView) findViewById(R.id.table_ergo_längd_in);
            TextView tvÅlder = (TextView) findViewById(R.id.table_ergo_ålder_in);
            TextView tvBelas = (TextView) findViewById(R.id.table_ergo_belas_in);

            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            tvResult.setText(result);

            String content = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
            String[] parts = content.split("\\|");
            tvVikt.setText(parts[4]);
            tvLängd.setText(parts[5]);
            tvÅlder.setText(parts[6]);
            int belasPos = Integer.parseInt(parts[1]);
            tvBelas.setText(String.valueOf((belasPos + 1) * 150));
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvResult = (TextView) findViewById(R.id.table_ergo_result_out);
            TextView tvVikt = (TextView) findViewById(R.id.table_ergo_vikt_out);
            TextView tvLängd = (TextView) findViewById(R.id.table_ergo_längd_out);
            TextView tvÅlder = (TextView) findViewById(R.id.table_ergo_ålder_out);
            TextView tvBelas = (TextView) findViewById(R.id.table_ergo_belas_out);

            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            tvResult.setText(result);

            String content = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
            String[] parts = content.split("\\|");
            tvVikt.setText(parts[4]);
            tvLängd.setText(parts[5]);
            tvÅlder.setText(parts[6]);
            int belasPos = Integer.parseInt(parts[1]);
            tvBelas.setText(String.valueOf((belasPos + 1) * 150));
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_ergo_result_notes);
            notes.setText("*");
        }
    }

    private void handleIPAQ(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tvF = (TextView) findViewById(R.id.table_ipaq_fysisk_in);
            TextView tvS = (TextView) findViewById(R.id.table_ipaq_sittande_in);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
            String[] part = result.split("\\|");
            tvF.setText(part[0]);
            tvS.setText(part[1]);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tvF = (TextView) findViewById(R.id.table_ipaq_fysisk_out);
            TextView tvS = (TextView) findViewById(R.id.table_ipaq_sittande_out);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
            String[] part = result.split("\\|");
            tvF.setText(part[0]);
            tvS.setText(part[1]);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_ipaq_fysisk_notes);
            notes.setText("*");
        }
    }

    private void handleOTT(Cursor cursor) {
        // No need to check from COMPLETE status. If either flexion or extension have values
        // we print them out.
        // In
        TextView tvFlex = (TextView) findViewById(R.id.table_ottflex_in);
        TextView tvExt = (TextView) findViewById(R.id.table_ottext_in);
        String content = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
        if (content != null) {
            String[] part = content.split("\\|");
            tvFlex.setText(part[0]);
            tvExt.setText(part[1]);
        }


        // Out
        tvFlex = (TextView) findViewById(R.id.table_ottflex_out);
        tvExt = (TextView) findViewById(R.id.table_ottext_out);
        content = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
        if (content != null) {
            String[] part = content.split("\\|");
            tvFlex.setText(part[0]);
            tvExt.setText(part[1]);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_ottflex_notes);
            notes.setText("*");
        }
    }

    private void handleThorax(Cursor cursor) {
        // In
        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
        if (statusIn == Test.COMPLETED) {
            TextView tv = (TextView) findViewById(R.id.table_thorax_in);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
            String[] part = result.split("\\|");
            tv.setText(part[0]);
        }

        // Out
        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
        if (statusOut == Test.COMPLETED) {
            TextView tv = (TextView) findViewById(R.id.table_thorax_out);
            String result = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
            String[] part = result.split("\\|");
            tv.setText(part[0]);
        }

        // Notes
        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));

        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
            TextView notes = (TextView) findViewById(R.id.table_thorax_notes);
            notes.setText("*");
        }
    }

//    private static final String LOG_TAG = ResultTableActivity.class.getSimpleName();
//
//    private int mUserID, mPatientID;
//    private String mUserName;
//    private String mHeaderString;
//
//    private ActivityResultTableBinding bind;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_result_table);
//
//        // Keep screen on
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        // Binding instead of findViewById
//        bind = DataBindingUtil.setContentView(this, R.layout.activity_result_table);
//
//        // Extract info from Bundle
//        Bundle extras = getIntent().getExtras();
//        if (extras != null) {
//            mUserID = extras.getInt(EXTRAS.KEY_USER_ID, mUserID);
//            mUserName = extras.getString(EXTRAS.KEY_USER_NAME);
//            mPatientID = extras.getInt(EXTRAS.KEY_PATIENT_ID);
//            mHeaderString = extras.getString(EXTRAS.KEY_HEADER);
//            Log.d(LOG_TAG, "Getting extras from Bundle -> mPatientID: " + mPatientID +
//                    " mHeader: " + mHeaderString);
//        }
//
//        // Activity's title
//        setTitle(mHeaderString);
//
//        // Get all tests for patient
//        String selection = TestEntry.COLUMN_PATIENT_ID_FK + "=?";
//        String[] selectionArgs = {String.valueOf(mPatientID)};
//        Cursor cursor = getContentResolver().query(TestEntry.CONTENT_URI, null, selection,
//                selectionArgs, null, null);
//
//        if (cursor != null && cursor.getCount() > 0) {
//            Log.d(LOG_TAG, "test count: " + cursor.getCount());
//            // Process each test, extract result and populate table
//            while (cursor.moveToNext()) {
//                String testCode = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CODE));
//                switch (testCode) {
//                    case "VAS":
//                        handleVAS(cursor);
//                        break;
//                    case "SOFI":
//                        handleSOFI(cursor);
//                        break;
//                    case "HAQ":
//                        handleHAQ(cursor);
//                        break;
//                    case "JAMAR":
//                        handleJAMAR(cursor);
//                        break;
//                    case "VIGO":
//                        handleVIGO(cursor);
//                        break;
//                    case "NINE":
//                        handleNINE(cursor);
//                        break;
//                    case "BOX":
//                        handleBOX(cursor);
//                        break;
//                    case "GAT":
//                        handleGAT(cursor);
//                        break;
//                }
//            }
//        } else {
//            Log.d(LOG_TAG, "cursor error for patient id: " + mPatientID);
//        }
//
//        if (cursor != null)
//            cursor.close();
//    }
//
//    private void handleVAS(Cursor cursor) {
//        // In
//        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
//        if (statusIn == Test.COMPLETED) {
//            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
//            String[] contentArray = contentIn.split("\\|");
//            bind.vasHandInH.setText(contentArray[0]);
//            bind.vasHandInV.setText(contentArray[1]);
//            bind.vasSmartaInH.setText(contentArray[2]);
//            bind.vasSmartaInV.setText(contentArray[3]);
//            bind.vasStelhetInH.setText(contentArray[4]);
//            bind.vasStelhetInV.setText(contentArray[5]);
//        }
//
//        // Out
//        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
//        if (statusOut == Test.COMPLETED) {
//            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
//            String[] contentArray = contentOut.split("\\|");
//            bind.vasHandUtH.setText(contentArray[0]);
//            bind.vasHandUtV.setText(contentArray[1]);
//            bind.vasSmartaUtH.setText(contentArray[2]);
//            bind.vasSmartaUtV.setText(contentArray[3]);
//            bind.vasStelhetUtH.setText(contentArray[4]);
//            bind.vasStelhetUtV.setText(contentArray[5]);
//        }
//
//        // Notes
//        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
//        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));
//
//        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
//            bind.tvVasNotes.setText("*");
//        }
//    }
//
//    private void handleSOFI(Cursor cursor) {
//        // In
//        String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
//        String[] contentArray = contentIn.split("\\|");
//        // Format: hand|arm|
//        // But only after user has saved once, before -1 as default
//        if (contentArray.length == 2) {
//            if (!contentArray[0].equals("-1")) {
//                bind.sofiHandIn.setText(contentArray[0]);
//            }
//            if (!contentArray[1].equals("-1")) {
//                bind.sofiArmIn.setText(contentArray[1]);
//            }
//        }
//
//        // Out
//        String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
//        contentArray = contentOut.split("\\|");
//        // Format: hand|arm|
//        // But only after user has saved once, before -1 as default
//        if (contentArray.length == 2) {
//            if (!contentArray[0].equals("-1")) {
//                bind.sofiHandUt.setText(contentArray[0]);
//            }
//            if (!contentArray[1].equals("-1")) {
//                bind.sofiArmUt.setText(contentArray[1]);
//            }
//        }
//
//        // Notes
//        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
//        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));
//
//        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
//            bind.tvSofiNotes.setText("*");
//        }
//    }
//
//    private void handleHAQ(Cursor cursor) {
//        // In
//        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
//        if (statusIn == Test.COMPLETED) {
//            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
//            String[] contentArray = contentIn.split("\\|");
//            bind.haqIn.setText(contentArray[0]);
//        }
//
//        // Out
//        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
//        if (statusOut == Test.COMPLETED) {
//            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
//            String[] contentArray = contentOut.split("\\|");
//            bind.haqUt.setText(contentArray[0]);
//        }
//
//        // Notes
//        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
//        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));
//
//        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
//            bind.tvHaqNotes.setText("*");
//        }
//    }
//
//    private void handleJAMAR(Cursor cursor) {
//        // In
//        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
//        if (statusIn == Test.COMPLETED) {
//            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
//            String[] contentArray = contentIn.split("\\|");
//            bind.jamarHandInH.setText(contentArray[0]);
//            bind.jamarHandInV.setText(contentArray[1]);
//            bind.jamarNormalInH.setText(contentArray[2]);
//            bind.jamarNormalInV.setText(contentArray[3]);
//        }
//
//        // Out
//        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
//        if (statusOut == Test.COMPLETED) {
//            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
//            String[] contentArray = contentOut.split("\\|");
//            bind.jamarHandUtH.setText(contentArray[0]);
//            bind.jamarHandUtV.setText(contentArray[1]);
//            bind.jamarNormalUtH.setText(contentArray[2]);
//            bind.jamarNormalUtV.setText(contentArray[3]);
//        }
//
//        // Notes
//        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
//        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));
//
//        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
//            bind.tvJamarNotes.setText("*");
//        }
//    }
//
//    private void handleVIGO(Cursor cursor) {
//        // In
//        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
//        if (statusIn == Test.COMPLETED || statusIn == Test.INCOMPLETED) {
//            String contentStr = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_IN));
//            String[] content = contentStr.split("\\|");
//            // Values
//            if (!content[0].equals("-1")) {
//                bind.vigoHandInH.setText(content[0]);
//            }
//            if (!content[1].equals("-1")) {
//                bind.vigoHandInV.setText(content[1]);
//            }
//            if (!content[2].equals("-1")) {
//                bind.vigoFingerDig2InH.setText(content[2]);
//            }
//            if (!content[3].equals("-1")) {
//                bind.vigoFingerDig2InV.setText(content[3]);
//            }
//            if (!content[4].equals("-1")) {
//                bind.vigoFingerDig3InH.setText(content[4]);
//            }
//            if (!content[5].equals("-1")) {
//                bind.vigoFingerDig3InV.setText(content[5]);
//            }
//            if (!content[6].equals("-1")) {
//                bind.vigoFingerDig4InH.setText(content[6]);
//            }
//            if (!content[7].equals("-1")) {
//                bind.vigoFingerDig4InV.setText(content[7]);
//            }
//            if (!content[8].equals("-1")) {
//                bind.vigoFingerDig5InH.setText(content[8]);
//            }
//            if (!content[9].equals("-1")) {
//                bind.vigoFingerDig5InV.setText(content[9]);
//            }
//            if (!content[10].equals("-1")) {
//                bind.vigoThumbInH.setText(content[10]);
//            }
//            if (!content[11].equals("-1")) {
//                bind.vigoThumbInV.setText(content[11]);
//            }
//            // Normal values
//            bind.vigoNormalHandInH.setText(content[12]);
//            bind.vigoNormalHandInV.setText(content[12]);
//            bind.vigoNormalFingerInH.setText(content[13]);
//            bind.vigoNormalFingerInV.setText(content[13]);
//            bind.vigoNormalThumbInH.setText(content[14]);
//            bind.vigoNormalThumbInV.setText(content[14]);
//        }
//
//        // Out
//        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
//        if (statusOut == Test.COMPLETED || statusOut == Test.INCOMPLETED) {
//            String contentStr = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_CONTENT_OUT));
//            String[] content = contentStr.split("\\|");
//            // Values
//            if (!content[0].equals("-1")) {
//                bind.vigoHandUtH.setText(content[0]);
//            }
//            if (!content[1].equals("-1")) {
//                bind.vigoHandUtV.setText(content[1]);
//            }
//            if (!content[2].equals("-1")) {
//                bind.vigoFingerDig2UtH.setText(content[2]);
//            }
//            if (!content[3].equals("-1")) {
//                bind.vigoFingerDig2UtV.setText(content[3]);
//            }
//            if (!content[4].equals("-1")) {
//                bind.vigoFingerDig3UtH.setText(content[4]);
//            }
//            if (!content[5].equals("-1")) {
//                bind.vigoFingerDig3UtV.setText(content[5]);
//            }
//            if (!content[6].equals("-1")) {
//                bind.vigoFingerDig4UtH.setText(content[6]);
//            }
//            if (!content[7].equals("-1")) {
//                bind.vigoFingerDig4UtV.setText(content[7]);
//            }
//            if (!content[8].equals("-1")) {
//                bind.vigoFingerDig5UtH.setText(content[8]);
//            }
//            if (!content[9].equals("-1")) {
//                bind.vigoFingerDig5UtV.setText(content[9]);
//            }
//            if (!content[10].equals("-1")) {
//                bind.vigoThumbUtH.setText(content[10]);
//            }
//            if (!content[11].equals("-1")) {
//                bind.vigoThumbUtV.setText(content[11]);
//            }
//            // Normal values
//            bind.vigoNormalHandUtH.setText(content[12]);
//            bind.vigoNormalHandUtV.setText(content[12]);
//            bind.vigoNormalFingerUtH.setText(content[13]);
//            bind.vigoNormalFingerUtV.setText(content[13]);
//            bind.vigoNormalThumbUtH.setText(content[14]);
//            bind.vigoNormalThumbUtV.setText(content[14]);
//        }
//
//        // Notes
//        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
//        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));
//
//        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
//            bind.tvVigoNotes.setText("*");
//        }
//    }
//
//    private void handleNINE(Cursor cursor) {
//        // In
//        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
//        if (statusIn == Test.COMPLETED) {
//            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
//            String[] contentArray = contentIn.split("\\|");
//            bind.nineInH.setText(contentArray[0]);
//            bind.nineInV.setText(contentArray[1]);
//            bind.nineNormalInH.setText(contentArray[2]);
//            bind.nineNormalInV.setText(contentArray[3]);
//        }
//
//        // Out
//        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
//        if (statusOut == Test.COMPLETED) {
//            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
//            String[] contentArray = contentOut.split("\\|");
//            bind.nineUtH.setText(contentArray[0]);
//            bind.nineUtV.setText(contentArray[1]);
//            bind.nineNormalUtH.setText(contentArray[2]);
//            bind.nineNormalUtV.setText(contentArray[3]);
//        }
//
//        // Notes
//        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
//        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));
//
//        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
//            bind.tvNineNotes.setText("*");
//        }
//    }
//
//    private void handleBOX(Cursor cursor) {
//        // In
//        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
//        if (statusIn == Test.COMPLETED) {
//            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
//            String[] contentArray = contentIn.split("\\|");
//            bind.boxInH.setText(contentArray[0]);
//            bind.boxInV.setText(contentArray[1]);
//            bind.boxNormalInH.setText(contentArray[2]);
//            bind.boxNormalInV.setText(contentArray[3]);
//        }
//
//        // Out
//        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
//        if (statusOut == Test.COMPLETED) {
//            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
//            String[] contentArray = contentOut.split("\\|");
//            bind.boxUtH.setText(contentArray[0]);
//            bind.boxUtV.setText(contentArray[1]);
//            bind.boxNormalUtH.setText(contentArray[2]);
//            bind.boxNormalUtV.setText(contentArray[3]);
//        }
//
//        // Notes
//        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
//        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));
//
//        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
//            bind.tvBoxNotes.setText("*");
//        }
//    }
//
//    private void handleGAT(Cursor cursor) {
//        // In
//        int statusIn = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_IN));
//        if (statusIn == Test.COMPLETED) {
//            String contentIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_IN));
//            String[] contentArray = contentIn.split("\\|");
//            bind.gatIn.setText(contentArray[0]);
//        }
//
//        // Out
//        int statusOut = cursor.getInt(cursor.getColumnIndex(TestEntry.COLUMN_STATUS_OUT));
//        if (statusOut == Test.COMPLETED) {
//            String contentOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_RESULT_OUT));
//            String[] contentArray = contentOut.split("\\|");
//            bind.gatUt.setText(contentArray[0]);
//        }
//
//        // Notes
//        String notesIn = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_IN));
//        String notesOut = cursor.getString(cursor.getColumnIndex(TestEntry.COLUMN_NOTES_OUT));
//
//        if ((notesIn != null && !notesIn.isEmpty()) || (notesOut != null && !notesOut.isEmpty())) {
//            bind.tvGatNotes.setText("*");
//        }
//    }
//
//
//    /**
//     * Navigate up
//     */
//    private void goBackToPatientListActivity() {
//        Intent upIntent = NavUtils.getParentActivityIntent(ResultTableActivity.this);
//        upIntent.putExtra(EXTRAS.KEY_USER_ID, mUserID);
//        upIntent.putExtra(EXTRAS.KEY_USER_NAME, mUserName);
//        NavUtils.navigateUpTo(ResultTableActivity.this, upIntent);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            // Respond to the action bar's Up/Home button
//            case android.R.id.home:
//                Log.d(LOG_TAG, "Back from ResultTableActivity: arrow");
//                goBackToPatientListActivity();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    @Override
//    public void onBackPressed() {
//        Log.d(LOG_TAG, "Back from ResultTableActivity: back pressed");
//        goBackToPatientListActivity();
//    }
}
