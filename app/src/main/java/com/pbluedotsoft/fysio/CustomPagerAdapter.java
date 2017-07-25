package com.pbluedotsoft.fysio;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.pbluedotsoft.fysio.data.EXTRAS;

/**
 * Created by Daniel Ibanez on 2016-10-04.
 */

public class CustomPagerAdapter extends FragmentPagerAdapter {

    private static final String LOG_TAG = CustomPagerAdapter.class.getSimpleName();

    private String mTestURI, mTestCode;
    private int mInOut;

    public CustomPagerAdapter(Context context, FragmentManager fm, Bundle bundle) {
        super(fm);
        mTestCode = bundle.getString(EXTRAS.KEY_TEST_CODE);
        mTestURI = bundle.getString(EXTRAS.KEY_URI);
        mInOut = bundle.getInt(EXTRAS.KEY_INOUT);
    }

    @Override
    public Fragment getItem(int position) {
        // What test should I create?
        Fragment fragment;
        switch (mTestCode) {
            case "VAS":
                fragment = new VASFragment();
                break;
            case "EQ5D":
                fragment = new EQ5DFragment();
                break;
            case "IPAQ":
                fragment = new IpaqFragment();
                break;
            case "6MIN":
                fragment = new MIN6Fragment();
                break;
            case "TUG":
                fragment = new TUGFragment();
                break;
            case "ERGO":
                fragment = new ErgoFragment();
                break;
            case "TST":
                fragment = new TSTFragment();
                break;
            case "IMF":
                fragment = new IMFFragment();
                break;
            case "FSA":
                fragment = new FSAFragment();
                break;
            case "LED":
                fragment = new LedFragment();
                break;
            case "BERGS":
                fragment = new BergsFragment();
                break;
            case "BDL":
                fragment = new BDLFragment();
                break;
            case "FSS":
                fragment = new FSSFragment();
                break;
            case "BASMI":
                fragment = new BasmiFragment();
                break;
            case "OTT":
                fragment = new OttFragment();
                break;
            case "THORAX":
                fragment = new ThoraxFragment();
                break;
            case "BASDAI":
                fragment = new BasdaiFragment();
                break;
            case "BASFI":
                fragment = new BasfiFragment();
                break;
            case "BASG":
                fragment = new BasgFragment();
                break;
            default:
                fragment = new VASFragment();
                break;
        }

        // Android recommends to use Bundle to pass parameters to Fragments
        // instead of parameters in the constructor.
        // What tab is it? IN or OUT
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRAS.KEY_TAB, position);
        bundle.putString(EXTRAS.KEY_URI, mTestURI);
        bundle.putInt(EXTRAS.KEY_INOUT, mInOut);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "IN";
            case 1:
                return "UT";
            default:
                return "";
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

}
