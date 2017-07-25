package com.pbluedotsoft.fysio;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Daniel Ibanez on 2016-11-03.
 */

public class InputFilterMinMax implements InputFilter {

    private Pattern mPattern;
    private double min, max;

    public InputFilterMinMax(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public InputFilterMinMax(String min, String max, int digitsBeforeZero, int digitsAfterZero) {
        this.min = Double.parseDouble(min);
        this.max = Double.parseDouble(max);
        mPattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) +
                "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?");
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        Matcher matcher = mPattern.matcher(dest);
        if (!matcher.matches())
            return "";

        try {
            double input = Double.parseDouble(dest.toString() + source.toString());
            if (isInRange(min, max, input))
                return null;
        } catch (NumberFormatException nfe) {
        }
        return "";
    }

    private boolean isInRange(double a, double b, double c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }
}