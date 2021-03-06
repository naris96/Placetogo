package com.example.chungwei.placetogo;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.example.chungwei.placetogo.helpers.PreferencesManager;
import com.example.chungwei.placetogo.services.placetogo.IPlacetogoResponse;
import com.example.chungwei.placetogo.services.placetogo.PlacetogoService;
import com.example.chungwei.placetogo.services.placetogo.models.Challenge;
import com.example.chungwei.placetogo.services.placetogo.models.ChallengeResult;

public class ChallengeActivity extends AppCompatActivity {

    //Object Declaration for the layout swipe screen.
    private TextView[] dots;
    private ViewPager viewPager;
    private ChallengeActivity.MyViewPagerAdapter myViewPagerAdapter;
    private PreferencesManager preferencesManager;
    private LinearLayout dotsLayout;

    private int[] backgroundColors = {
            R.drawable.challenge_color1,
            R.drawable.challenge_color2,
            R.drawable.challenge_color3,
            R.drawable.challenge_color4
    };

    private int[] headerStrings = {
            R.string.Challenge1,
            R.string.Challenge2,
            R.string.Challenge3,
            R.string.Challenge4
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_challenge);
        viewPager = findViewById(R.id.view_pager);
        dotsLayout = findViewById(R.id.layoutDots);

        addBottomDots(0);
        changeStatusBarColor();

        Context context = this;

        PlacetogoService placetogoService = new PlacetogoService(this);
        placetogoService.getChallenges(new IPlacetogoResponse<ChallengeResult>() {
            @Override
            public void onResponse(ChallengeResult result) {
                //lock all challenges start form second
                for (int i = 1; i < result.getChallenges().length; i++) {
                    result.getChallenges()[i].setLock(true);
                }

                findViewById(R.id.loading_Layout).setVisibility(View.GONE);
                myViewPagerAdapter = new ChallengeActivity.MyViewPagerAdapter(result);
                viewPager.setOffscreenPageLimit(4 - 1);
                viewPager.setAdapter(myViewPagerAdapter);
                viewPager.addOnPageChangeListener(viewPagerPageChangeListener);
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Connection Error");
                builder.setMessage("We not able to get the challenges data, please try again later.");
                builder.setPositiveButton("Okay", null);
                builder.setOnDismissListener(dialog -> ((AppCompatActivity) context).finish());
                builder.create().show();
            }
        });
    }

    private void addBottomDots(int currentPage) {

        dots = new TextView[headerStrings.length];

        int[] colorsActive = getResources().getIntArray(R.array.array_dot_active);
        int[] colorsInactive = getResources().getIntArray(R.array.array_dot_inactive);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorsInactive[currentPage]);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[currentPage].setTextColor(colorsActive[currentPage]);
        }
    }

    //Method changing notification bar transparent
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public class MyViewPagerAdapter extends PagerAdapter {

        private ChallengeResult challengeResult;

        MyViewPagerAdapter(ChallengeResult challengeResult) {
            this.challengeResult = challengeResult;
        }

        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.challenge_layout, container, false);

            Challenge challenge = challengeResult.getChallenges()[position];
            view.setBackground(getResources().getDrawable(backgroundColors[position]));
            ((TextView) view.findViewById(R.id.challenge_count_TextView)).setText(getResources().getString(headerStrings[position]));
            view.findViewById(R.id.content_ScrollView).setVisibility(challenge.isLock() ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.lock_ImageView).setVisibility(challenge.isLock() ? View.VISIBLE : View.GONE);
            ((TextView) view.findViewById(R.id.title_textView)).setText(challenge.getTitle());
            ((TextView) view.findViewById(R.id.content_textView)).setText(challenge.getContent());
            view.findViewById(R.id.checkIn_Button).setEnabled(!challenge.isComplete());

            Glide.with(view.getContext())
                    .load("https://placetogo.herokuapp.com/" + challenge.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder_gray_24dp)
                    .centerCrop()
                    .into((ImageView) view.findViewById(R.id.challenge_imageView));

            view.findViewById(R.id.checkIn_Button).setOnClickListener(v -> {
                challenge.setComplete(true);
                v.setEnabled(false);

                if (challenge.getNo() < challengeResult.getChallenges().length) {
                    challengeResult.getChallenges()[challenge.getNo()].setLock(false);

                    View nextView = viewPager.getChildAt(challenge.getNo());
                    if (nextView != null) {
                        nextView.findViewById(R.id.content_ScrollView)
                                .setVisibility(challengeResult.getChallenges()[challenge.getNo()].isLock() ? View.GONE : View.VISIBLE);
                        nextView.findViewById(R.id.lock_ImageView)
                                .setVisibility(challengeResult.getChallenges()[challenge.getNo()].isLock() ? View.VISIBLE : View.GONE);
                    }
                }
                Dialog dialog = new Dialog(v.getContext(), android.R.style.Theme_Material_Dialog);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.challenge_complete_popup_layout);
                dialog.setCancelable(false);
                dialog.show();

                Handler mHandler = new Handler();
                Runnable mRunnable = dialog::dismiss;
                mHandler.postDelayed(mRunnable, 2000);
            });

            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return headerStrings.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }

    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        //Changing visibility and button text according to the foreground welcome screen.
        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

}





























