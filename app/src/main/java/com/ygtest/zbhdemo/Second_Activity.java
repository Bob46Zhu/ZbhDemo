package com.ygtest.zbhdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * Created by Android Studio.
 * User: ZBH
 * Date: 2019/5/4
 * Time: 19:06
 */
public class Second_Activity extends AppCompatActivity {


    private TextView txtshow;
    private String name;
    private String sex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        txtshow = findViewById(R.id.txtshow);
        //获得Intent对象,并且用Bundle出去里面的数据
        Intent it = getIntent();
        Bundle bd = it.getExtras();

        //按键值的方式取出Bundle中的数据
        name = bd.getCharSequence("user").toString();
        sex = bd.getCharSequence("sex").toString();
        txtshow.setText("尊敬的"+name + " " + sex + "士"+"恭喜你,注册成功~");
    }
}
