package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SplashAdapter extends PagerAdapter {
    private Context context;
    private ArrayList<OnBoardingItem> items;
    private TextView title, info;

    public SplashAdapter(Context context, ArrayList<OnBoardingItem> items) {
        this.context = context;
        this.items = items;
    }
    public int getCount() {
        return items.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.onboarding_item_layout, container, false);
        OnBoardingItem item = items.get(position);
        title = (TextView) itemView.findViewById(R.id.onboarding_text_title);
        info = (TextView) itemView.findViewById(R.id.onboarding_text_info);
        title.setText(item.getTitle());
        info.setText(item.getDescription());
        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
    }
}
