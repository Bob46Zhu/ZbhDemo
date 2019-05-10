package com.ygtest.zbhdemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.annotation.LongDef;
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
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import javax.net.ssl.HttpsURLConnection;

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

    private String netaddr="http://120.236.175.244:3122/";
    private String netaddr1="http://ww2.sinaimg.cn/large/7a8aed7bgw1evshgr5z3oj20hs0qo0vq.jpg";
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
            Download(netaddr);
            if (!et_addr.equals(null)){

                //Download(et_addr.getText().toString());
            } else{
                showToast("请输入正确网址");
            }
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

//    private  boolean Download2(String url){
//
//        HttpPost
//
//        return false;
//    }


    private boolean Download(String url){
        Bundle bundle = new Bundle();
        Message message = new Message();
        try {
            //打开连接
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            //打开文件输入流
            conn.setRequestMethod("GET");
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 401){
                Log.e("ERROR:401","未经授权");
            }
            InputStream is = conn.getInputStream();
            //获取文件长度
            int contentLength = conn.getContentLength();
            Log.d("DOWNLOAD","文件长度="+contentLength);
            //创建文件夹MyDownLoad，在存储卡下
            String dirName = Environment.getExternalStorageDirectory()+"/storage/emulated/0/";
            //下载后的文件名
            String fileName = dirName+"556621.pdf";
            File file1 = new File(fileName);
            if (file1.exists()){
                Log.e("文件------","文件已经存在!");
                return fileIsExists(fileName);
            } else {
                //创建字节流
                byte[] bs = new byte[1024];
                int len;
                OutputStream os = new FileOutputStream(fileName);
                //写数据
                while((len = is.read(bs))!=-1){
                    os.write(bs,0,len);
                }
                //完成后关闭流
                Log.e("文件不存在","下载成功");
                os.close();
                is.close();
            }
            message.setData(bundle);
            mHandler.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }



    public boolean fileIsExists(String fileName) {
        try{
            File f = new File(fileName);
            if (!f.exists()){
                return false;
            }
        } catch (Exception e){
            return false;
        }
        return true;
    }


    /**
     * 打印吐司
     * */
    public void showToast(String str){
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }

}
