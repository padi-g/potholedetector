package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.EasyModeFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.OverviewFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.TriplistFragment;

public class PagerAdapter extends FragmentStatePagerAdapter {

    //integer to count number of tabs
    private int tabCount;
    private boolean inCar;
    //Constructor to the class
    public PagerAdapter(FragmentManager fm, int tabCount, boolean inCar) {
        super(fm);
        //Initializing tab count
        this.tabCount= tabCount;
        this.inCar = inCar;
        // Log.i(getClass().getSimpleName(), this.inCar + "");
    }

    //Overriding method getItem
    @Override
    public Fragment getItem(int position) {
        //Returning the current tabs
        switch (position) {
            case 0:
                Bundle bundle = new Bundle();
                bundle.putBoolean("inCar", inCar);
                EasyModeFragment tab1 = new EasyModeFragment();
                tab1.setArguments(bundle);
                return tab1;
            case 1:
                OverviewFragment tab2 = new OverviewFragment();
                return tab2;
            default:
                return null;
        }
    }

    //Overriden method getHits to get the number of tabs
    @Override
    public int getCount() {
        return tabCount;
    }
}
