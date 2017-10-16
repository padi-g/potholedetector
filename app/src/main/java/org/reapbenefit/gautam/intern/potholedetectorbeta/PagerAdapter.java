package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.EasyModeFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.TriplistFragment;

public class PagerAdapter extends FragmentStatePagerAdapter {

    //integer to count number of tabs
    private int tabCount;

    //Constructor to the class
    public PagerAdapter(FragmentManager fm, int tabCount) {
        super(fm);
        //Initializing tab count
        this.tabCount= tabCount;
    }

    //Overriding method getItem
    @Override
    public Fragment getItem(int position) {
        //Returning the current tabs
        switch (position) {
            case 0:
                EasyModeFragment tab1 = new EasyModeFragment();
                return tab1;
            case 1:
                TriplistFragment tab2 = new TriplistFragment();
                return tab2;
            default:
                return null;
        }
    }

    //Overriden method getCount to get the number of tabs
    @Override
    public int getCount() {
        return tabCount;
    }
}
