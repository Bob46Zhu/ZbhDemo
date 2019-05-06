package com.ygtest.zbhdemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Created by Android Studio.
 * User: ZBH
 * Date: 2019/5/6
 * Time: 12:18
 */
@SuppressLint("ValidFragment")
public class SecondFragmentActivity extends Fragment implements View.OnClickListener {

    private TextView tv2;
    private EditText et_addr;
    private Button bt_download;


    private Context mContext;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            tv2.setText(data.getString("file"));
        }
    };
    Runnable download = new Runnable() {
        @Override
        public void run() {
            Download(et_addr.getText().toString());
        }
    };

    @SuppressLint("ValidFragment")
    public SecondFragmentActivity(Context context){
        this.mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.second_fragment,container,false);
        UI_init(view);

        return view;
    }

    private void UI_init(View view){
        tv2 = view.findViewById(R.id.firstFragment);
        et_addr = view.findViewById(R.id.et_addr);
        bt_download = view.findViewById(R.id.bt_netdownload);

        bt_download.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_netdownload:
                new Thread(download).start();
                break;
        }
    }

    private void Download(String url){
        BufferedReader buffer;
        StringBuilder sb = new StringBuilder();
        String line = "";
        Bundle bundle = new Bundle();
        Message message = new Message();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            buffer = new BufferedReader((new InputStreamReader(conn.getInputStream())));
            byte[] bf = new byte[1024];
            while((line=buffer.readLine())!=null){
                sb.append(line);
            }
            String content = URLDecoder.decode(sb.toString(),"UTF-8");
            Log.i("file",content);

            bundle.putString("file",content);
            message.setData(bundle);
            mHandler.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
