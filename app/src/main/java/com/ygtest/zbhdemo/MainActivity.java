package com.ygtest.zbhdemo;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import static android.content.Intent.ACTION_VIEW;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_VIEW = "android.intent.action.VIEW";
    private Button btnregister;
    private EditText editname;
    private RadioGroup rad;
    private TextView showTV;
    private TextView filepath;
    private MyReceiver viewRecevier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TAG","onCreate()");
        setContentView(R.layout.activity_main);
        btnregister = findViewById(R.id.btnregister);
        editname = findViewById(R.id.editname);
        filepath = findViewById(R.id.filePath);
        rad = findViewById(R.id.radioGroup);
        btnregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name,sex = "";
                Intent it = new Intent(MainActivity.this,Second_Activity.class);
                name = editname.getText().toString();
                //遍历RadioGroup找出被选中的单选按钮
                for(int i = 0;i < rad.getChildCount();i++)
                {
                    RadioButton rd = (RadioButton)rad.getChildAt(i);
                    if(rd.isChecked())
                    {
                        sex = rd.getText().toString();
                        break;
                    }
                }
                //新建Bundle对象,并把数据写入
                Bundle bd = new Bundle();
                bd.putCharSequence("user",name);
                bd.putCharSequence("sex",sex);
                //将数据包Bundle绑定到Intent上
                it.putExtras(bd);
                startActivity(it);
                //关闭第一个Activity
                finish();
            }
        });

        //广播测试
        IntentFilter viewFilter = new IntentFilter(ACTION_VIEW);
        registerReceiver(viewRecevier,viewFilter);

        /**
         * 从另一个app中读取文件路径
         */
        Intent intent = getIntent();
        String action = intent.getAction();
        if(ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            String path = uri.getScheme();
            Log.d("getScheme",path);
            path = uri.getPath();
            Log.d("getPath",path);
            filepath.setText(path);
        }

    }

//    @Override
//    protected void onStart() {
//        super.onStart();
////        showTV.append("onStart\n");
//        Log.d("TAG","onStart()");
//    }
//
//
//    @Override
//    protected void onPostResume() {
//        super.onPostResume();
////        showTV.append("onPostResume\n");
//        Log.d("TAG","onPostResume()");
//    }
//
//
//    @Override
//    protected void onPause() {
//        super.onPause();
////        showTV.append("onPause\n");
//        Log.d("TAG","onPause()");
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
////        showTV.append("onStop\n");
//        Log.d("TAG","onStop()");
//    }
//
//    @Override
//    protected void onRestart() {
//        super.onRestart();
////        showTV.append("onRestart\n");
//        Log.d("TAG","onRestart()");
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
////        showTV.append("onDestroy\n");
//        Log.d("TAG","onDestroy()");
//    }
}
