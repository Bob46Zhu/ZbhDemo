package com.ygtest.zbhdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Created by Android Studio.
 * User: ZBH
 * Date: 2019/5/5
 * Time: 20:41
 */
public class FragmentActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    private TextView txt_burn;
    private RadioGroup rd_group;
    private RadioButton rb_burn,rb_my;
    private FirstFragmentActivity firstFragment;
    private SecondFragmentActivity secondFragment;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getSupportActionBar().hide();// 隐藏ActionBar
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//remove notification bar  即全屏
        setContentView(R.layout.usb_fragment);
        bindView();
    }

    //UI组件初始化与事件绑定
    private void bindView(){
        rd_group = findViewById(R.id.menu_button);
        rd_group.setOnCheckedChangeListener(this);

        rb_burn = findViewById(R.id.leftRB);
        rb_my = findViewById(R.id.rightRB);
        rb_burn.setChecked(true);
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (firstFragment != null){
            transaction.hide(firstFragment);
        }
        if (secondFragment != null){
            transaction.hide(secondFragment);
        }
        switch (checkedId){
            case R.id.leftRB:
                if (firstFragment == null){
                    firstFragment = new FirstFragmentActivity(this);
                    transaction.add(R.id.fragment_page,firstFragment);
                }else {
                    transaction.show(firstFragment);
                }
                break;
            case R.id.rightRB:
                if (secondFragment == null){
                    secondFragment = new SecondFragmentActivity(this);
                    transaction.add(R.id.fragment_page,secondFragment);
                } else {
                    transaction.show(secondFragment);
                }
                break;
        }
        transaction.commit();
    }




}
