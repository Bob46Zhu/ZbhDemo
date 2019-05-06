package com.ygtest.zbhdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import static android.content.Intent.ACTION_VIEW;

public class MyReceiver extends BroadcastReceiver {

    private String path;
    private Context mContent;
    public void MyReceiver(Context context){
        mContent = context;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        String action = intent.getAction();
        context = mContent;
        if (action.equals(ACTION_VIEW)){
            Uri uri = intent.getData();
            path = uri.getScheme();
           // Log.d("getScheme",path);
            path = uri.getPath();
            Log.d("getPath",path);
        }
    }
}
