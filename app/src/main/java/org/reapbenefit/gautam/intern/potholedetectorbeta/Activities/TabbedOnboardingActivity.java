package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.animation.ArgbEvaluator;
import android.annotation.SuppressLint;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

public class TabbedOnboardingActivity extends AppCompatActivity {
    private int page = 0;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private Button skipButton, finishButton;
    private ImageButton rightButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        skipButton = findViewById(R.id.intro_btn_skip);
        finishButton = findViewById(R.id.intro_btn_finish);
        rightButton = findViewById(R.id.intro_btn_next);

        mViewPager = findViewById(R.id.container);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        // Set up the ViewPager with the sections adapter.

        @SuppressLint("ResourceType") final int greyColor = android.R.color.darker_gray;
        @SuppressLint("ResourceType") final int orangeColor = R.color.colorAccent;
        @SuppressLint("ResourceType") final int greenColor = R.color.colorPrimary;

        final int [] colorList = new int[]{greyColor, orangeColor, greenColor};
        final ArgbEvaluator evaluator = new ArgbEvaluator();

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //updating page colour
                int colorUpdate = (Integer) evaluator.evaluate(positionOffset, colorList[position], colorList[position == 2?
                        position: position + 1]);
                mViewPager.setBackgroundColor(colorUpdate);
            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        mViewPager.setBackgroundColor(getResources().getColor(greyColor));
                        break;
                    case 1:
                        mViewPager.setBackgroundColor(getResources().getColor(orangeColor));
                        break;
                    case 2:
                        mViewPager.setBackgroundColor(getResources().getColor(greenColor));
                        break;
                }
                rightButton.setVisibility(position == 2? View.GONE: View.VISIBLE);
                finishButton.setVisibility(position == 2? View.VISIBLE: View.GONE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                finish();
                getSharedPreferences("onboarding", MODE_PRIVATE).edit().putBoolean("onboarding", true).commit();
            }
        });

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private ImageView imageView;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_splash, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            imageView = (ImageView) rootView.findViewById(R.id.image_background);
            String[] arr = getActivity().getResources().getStringArray(R.array.onboarding_strings);
            textView.setText(arr[getArguments().getInt(ARG_SECTION_NUMBER) - 1]);
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0:
                    return "Welcome";
                case 1:
                    return "Help make us better";
                case 2:
                    return "Sign in";
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}
