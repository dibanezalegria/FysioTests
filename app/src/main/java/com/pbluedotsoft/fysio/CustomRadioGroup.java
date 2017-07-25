package com.pbluedotsoft.fysio;

import android.util.Log;
import android.view.View;
import android.widget.RadioButton;

import java.util.ArrayList;

/**
 * Created by Daniel Ibanez on 2016-11-01.
 */

public class CustomRadioGroup {

    private static final String LOG_TAG = CustomRadioGroup.class.getSimpleName();

    private ArrayList<View> mViewList;
    private boolean mIsSelected;

    public CustomRadioGroup() {
        mViewList = new ArrayList<>();
    }

    /**
     * Add radio button to the group
     *
     * @param view
     */
    public void addButton(View view) {
        mViewList.add(view);
    }


    public void checkButtonAt(int position) {
        // This radio group has one selected button now
        mIsSelected = true;
        RadioButton button = (RadioButton) mViewList.get(position);
        // Early exit: should never happen
        if (button == null) {
            Log.d(LOG_TAG, "checkButtonAt: button not found at position " + position);
            return;
        }

        button.setChecked(true);
        // Uncheck rest of buttons in the group
        for (View v : mViewList) {
            if (v.getId() != button.getId()) {
                ((RadioButton) v).setChecked(false);
            }
        }
    }

    /**
     * Set check as false for the rest of radio buttons in the group
     *
     * @param view
     */
    public void informGroupButtonSelected(View view) {
        // One button in the group has been selected
        mIsSelected = true;
        // Uncheck rest of buttons in the group
        for (View v : mViewList) {
            if (v.getId() != view.getId()) {
                ((RadioButton) v).setChecked(false);
            }
        }
    }

    /**
     * @return  position of radio button in the radio group
     */
    public int getButtonPositionInGroup(View view) {
        return mViewList.indexOf(view);
    }

    /**
     *
     * @return position of selected button in the group
     */
    public int getPositionSelectedButton() {
        for (View v : mViewList) {
            if (((RadioButton)v).isChecked())
                return mViewList.indexOf(v);
        }
        return -1;
    }

    public boolean isSelected() {
        return mIsSelected;
    }
}
