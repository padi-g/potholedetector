package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.OnboardingActivity;

import java.util.ArrayList;

public class SplashActivity extends AppCompatActivity {

    private SplashAdapter splashAdapter;
    private ViewPager mViewPager;
    private LinearLayout pagerIndicator;
    private int dotsCount;
    private ImageView[] dots;
    private Button finishButton;
    private ArrayList<OnBoardingItem> onBoardingItems = new ArrayList<>();
    private int previous_pos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        finishButton = (Button) findViewById(R.id.finish_button);
        mViewPager = (ViewPager) findViewById(R.id.container);
        pagerIndicator = (LinearLayout) findViewById(R.id.viewPagerCountDots);

        loadData();

        splashAdapter = new SplashAdapter(this, onBoardingItems);
        mViewPager.setAdapter(splashAdapter);
        mViewPager.setCurrentItem(0);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dotsCount; ++i) {
                    dots[i].setImageDrawable(ContextCompat.getDrawable(SplashActivity.this, R.drawable.ic_dot_white_24dp));
                }
                dots[position].setImageDrawable(ContextCompat.getDrawable(SplashActivity.this, R.drawable.ic_dot_color_accent_24dp));
                int pos=position+1;

                if(pos==dotsCount && previous_pos==(dotsCount-1))
                    show_animation();
                else if(pos==(dotsCount-1) && previous_pos==dotsCount)
                    hide_animation();

                previous_pos=pos;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSharedPreferences("onboarding", MODE_PRIVATE).edit().putBoolean("onboarding", true).commit();
                Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                startActivity(intent);
            }
        });
        dotsCount = splashAdapter.getCount();
        dots = new ImageView[dotsCount];
        for (int i = 0; i < dotsCount; ++i) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(SplashActivity.this, R.drawable.ic_dot_white_24dp));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(6, 0, 6, 0);
            pagerIndicator.addView(dots[i], params);
        }
        dots[0].setImageDrawable(ContextCompat.getDrawable(SplashActivity.this, R.drawable.ic_dot_color_accent_24dp));
    }

    private void show_animation() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(finishButton, "translationY", -77.0f);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                Log.d("Splash", "setting visible");
                finishButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animator.start();
    }

    private void hide_animation() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(finishButton, "translationY", 100.0f);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {
                finishButton.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }

    private void loadData() {
        //loads data into the OnBoardingItem ArrayList
        int titles[] = new int[]{R.string.first_section_title, R.string.second_section_title, R.string.third_section_title};
        int info[] = new int[]{R.string.first_section_info, R.string.second_section_info, R.string.third_section_info};
        for (int i = 0; i < titles.length; ++i) {
            OnBoardingItem item = new OnBoardingItem();
            item.setTitle(getString(titles[i]));
            item.setDescription(getString(info[i]));
            onBoardingItems.add(item);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_splash, menu);
        return true;
    }
}
