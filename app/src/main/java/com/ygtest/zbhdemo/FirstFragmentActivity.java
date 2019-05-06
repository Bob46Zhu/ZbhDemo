package com.ygtest.zbhdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Android Studio.
 * User: ZBH
 * Date: 2019/5/6
 * Time: 12:13
 */
@SuppressLint("ValidFragment")
public class FirstFragmentActivity extends Fragment implements View.OnClickListener {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String TAG = "YGIAP";

    private TextView tv_filePath;
    private TextView tv_logText;
    private Button bt_openfile;
    private Button bt_burn;

    private String str_filepath;
    private Context mContext;

    //线程相关定义
    private boolean threadcontrol_ct=false;

    private boolean St_Download_Data_Flag=false;
    private boolean Ti_Download_Data_Flag=false;

    //文件操作相关变量定义
    private FileInputStream fis = null;
    int byteLen;//文件大小
    private int count = 0;


    //数据传递变量定义
    private int byteData;
    private byte[] readBuffer =new byte[1];         //读取1字节缓存
    private byte[] sendBuffer = new byte[2048];      //发送2048字节缓存
    private byte[] recvBuffer = new byte[2048]; //读取2048字节缓存
    private String sendData;
    private byte[] head = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
    private int MotorVersionNum;//电机板版本号
    private int LCDVersionNum;//前面板版本号

    //USB设置的Uid Pid
    private int myvid=1155,mypid=22336;

    //Usb相关变量定义
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbInterface[] mUsbInterface;
    private UsbEndpoint[][] mUsbEndpoint = new UsbEndpoint[5][5];
    private UsbDeviceConnection mUsbConnection = null;

    //烧录相关变量定义
    private boolean burn_flag = false;
    STM_DownloadThread stm_downloadThread = null;
    TI_DownloadThread ti_downloadThread = null;


    private Handler mHandler = new Handler() {
        public void handleMessage (Message msg) {

            switch(msg.what){
                case 1:
                    tv_logText.setText("正在烧录前面板......\n");
                    break;
                case 2:
                    tv_logText.append("连接成功\n");
                    break;
                case 3:
                    tv_logText.append("文件大小："+byteLen+"\n");
                    break;
                case 4:
                    tv_logText.append("\n烧录完成\n");
                    bt_burn.setEnabled(true);
                    bt_openfile.setEnabled(true);
                    mUsbConnection.close();
                    break;
                case 5:
                    tv_logText.append("正在烧录");
                    break;
                case 6:
                    tv_logText.append(".");
                    break;
                case 7:
                    tv_logText.setText("正在烧录电机板.....\n");
                    break;
                case 8:
                    tv_logText.append("电机板版本号："+MotorVersionNum+".");
                    break;
                case 9:
                    tv_logText.append(MotorVersionNum+"\n");
                    tv_logText.append("前面板版本号："+LCDVersionNum+".");
                    break;
                case 10:
                    tv_logText.append(LCDVersionNum+"\n");
                    break;
                case 11:
                    int result = msg.what;
                    count += result;
                    if(count == 13){
                        tv_logText.setText("download succes\n");
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @SuppressLint("ValidFragment")
    public FirstFragmentActivity(Context context){
        this.mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.first_fragment,container,false);
        UI_init(view);//初始化UI界面

        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        Usb_Start();
        /**
         * 注册两个广播信号，监听USB设备插入和拔出
         */
        IntentFilter usbFilter = new IntentFilter(ACTION_USB_ATTACHED);
        usbFilter.addAction(ACTION_USB_DETACHED);
        getActivity().registerReceiver(mUsbReceiver,usbFilter);//USB拔出，注册广播

        /**
         * 烧录线程标志位状态
         * */
        threadcontrol_ct=true;
        if(stm_downloadThread!=null){//设置stm32烧录线程为空
            stm_downloadThread=null;
        }

        if (ti_downloadThread != null){//设置TI烧录线程为空
            ti_downloadThread = null;
        }


        return view;
    }

    private void Usb_Start() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Log.e(TAG, "设备列表大小  = " + deviceList.size());
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            mUsbDevice = deviceIterator.next();
            Log.i(TAG, "vid: " + mUsbDevice.getVendorId() + "\t pid: " + mUsbDevice.getProductId());
            if(mUsbDevice.getVendorId()==myvid&&mUsbDevice.getProductId()==mypid){
                break;
            }
        }
        if(mUsbDevice!=null&&mUsbDevice.getVendorId()==myvid&&mUsbDevice.getProductId()==mypid){
            tv_logText.setText("找到设备\n设备VID："+ mUsbDevice.getVendorId()+"\n" +
                    "\t pid: " + mUsbDevice.getProductId()+"\n");
        }
        else{
            tv_logText.setText("未找到设备\n");
            //finish();
            return;
        }

        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        if(mUsbManager.hasPermission(mUsbDevice)){

        }
        else{
            mUsbManager.requestPermission(mUsbDevice, pi);
        }

        if(mUsbManager.hasPermission(mUsbDevice)){
            tv_logText.append("设备拥有权限\n");
        }
        else{
            tv_logText.append("设备无权限\n");
        }

        tv_logText.append("设备名称："+mUsbDevice.getDeviceName()+"\n");

        tv_logText.append("接口数为:"+mUsbDevice.getInterfaceCount()+"\n");

        mUsbInterface=new UsbInterface[mUsbDevice.getInterfaceCount()];
        for(int i=0;i<mUsbDevice.getInterfaceCount();i++){
            mUsbInterface[i]=mUsbDevice.getInterface(i);
            tv_logText.append("接口"+i+"的端点数为："+mUsbInterface[i].getEndpointCount()+"\n");
            for(int j=0;j<mUsbInterface[i].getEndpointCount();j++){
                mUsbEndpoint[i][j]=mUsbInterface[i].getEndpoint(j);
                if(mUsbEndpoint[i][j].getDirection()==0 ){
                    tv_logText.append("端点"+j+"的数据方向为输出\n");
                }
                else{
                    tv_logText.append("端点"+j+"的数据方向为输入\n");
                }
            }
        }
    }

    private void UI_init(View view){
        tv_filePath = view.findViewById(R.id.file_path);
        tv_logText = view.findViewById(R.id.log_text);
        bt_openfile = view.findViewById(R.id.open_Btn);
        bt_burn = view.findViewById(R.id.program_Btn);

        bt_burn.setOnClickListener(this);
        bt_openfile.setOnClickListener(this);

        tv_filePath.setMovementMethod(ScrollingMovementMethod.getInstance());//设置文件目录文本框为可以滚动
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.program_Btn:
                if (burn_flag && !tv_filePath.getText().toString().isEmpty()){
                    String i = str_filepath.substring(str_filepath.length()-10);
                    bt_burn.setEnabled(false);
                    if (i.equals("F2800$.bin")){
                        stm_downloadThread= new STM_DownloadThread();
                        stm_downloadThread.start();
                        St_Download_Data_Flag = true;
                    } else if (i.equals("F2000$.bin")){
                        ti_downloadThread = new TI_DownloadThread();
                        ti_downloadThread.start();
                        Ti_Download_Data_Flag = true;
                    }else {
                        tv_logText.setText("请打开正确的烧录文件\n");
                    }
                } else {
                    tv_logText.setText("请检查设备是否插入和烧录文件");
                }

                break;
            case R.id.open_Btn:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("*/*");//无类型限制
                intent.setType("application/octet-stream");//只能选项二进制文件
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,1);
                break;
        }
    }


    /**
     * 广播事件，监听广播信号
     * */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_USB_ATTACHED)){//当广播收到USB插入的信号时
                Usb_Start();
                burn_flag = true;
                bt_burn.setEnabled(true);
                PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                if(mUsbManager.hasPermission(mUsbDevice)){
                }
                else{
                    mUsbManager.requestPermission(mUsbDevice, pi);
                }

                if(mUsbManager.hasPermission(mUsbDevice)){
                    tv_logText.append("设备拥有权限\n");
                }
                else{
                    tv_logText.append("设备无权限\n");
                }
                showToast("有USB设备接入");

            }else if (action.equals(ACTION_USB_DETACHED)) {  //当USB插出时的信号
                St_Download_Data_Flag = false;
                Ti_Download_Data_Flag = false;
                if (mUsbConnection != null){
                    mUsbConnection.close();
                }
                //program_btn.setEnabled(false);
                burn_flag = false;
                tv_logText.setText("请插入设备");
                showToast("USB设备拔出");
            }
        }
    };


    /**
     * 吐司
     * */
    public void showToast(String str){
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }

    /**
     * 从打开的系统程序返回文件路径函数
     * */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())){//使用第三方应用打开
                str_filepath = uri.getPath();
                tv_filePath.setText(str_filepath);
                showToast(str_filepath);
                return ;
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){//4.4以后的系统
                str_filepath = getPath(mContext,uri);
                tv_filePath.setText(str_filepath);
                showToast(str_filepath);
            } else { //4.4以下系统使用
                str_filepath = getRealPathFromURI(uri);
                tv_filePath.setText(str_filepath);
                showToast(str_filepath);
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = mContext.getContentResolver().query(contentUri,proj,null,null,null);
        if (null != cursor && cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }

    /*
     * 专为android 4.4设计的从Uri获取文件的绝对路径，以前的方法已不好使
     * */

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getPath (final Context context, final Uri uri){
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        //文件阅读器
        if (isKitKat && DocumentsContract.isDocumentUri(context,uri)){
            //读取外部存储
            if (isExternalStorageDocument(uri)){
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)){
                    return Environment.getExternalStorageDirectory()+"/"+split[1];
                }
            }
            //DownLoads Provider
            else if (isDownloadDocument(uri)){
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_download"),
                        Long.valueOf(id));
                return getDataColumn(context,contentUri,null,null);
            }
            //Media Provider
            else if (isMideaDocument(uri)){
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contenUri = null;
                if ("image".equals(type)){
                    contenUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)){
                    contenUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)){
                    contenUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context,contenUri,selection,selectionArgs);
            }
        }
        //MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())){
            return getDataColumn(context,uri,null,null);
        }
        //File
        else if ("file".equalsIgnoreCase(uri.getScheme())){
            return uri.getPath();
        }
        return null;
    }

    /*
     * @param uri The Uri to check
     * @return Whether the Uri authority is MediaProvider
     * */
    public boolean isMideaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /*
     * @param uri The Uri to check
     * @return Whether the Uri authority is ExternalStorageProvider.
     * */
    public boolean isExternalStorageDocument(Uri uri){
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /*
     * @param uri The Uri to check
     * @return Whether the Uri authority is DownloadsProvider
     * */
    public boolean isDownloadDocument(Uri uri){
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /*
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris,and other file-based ContentProviders.
     *
     * @param context            The context.
     * @param uri                The Uri query.
     * @param selection          (Optional) Filter used in the query.
     * @param selectionArgs      (Optional) Selection arguments used in the query.
     * @return                   The value of the _data column,which is typically a file path.
     * */
    public String getDataColumn(Context context,Uri uri,String selection,String[] selectionArgs){
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try{
            cursor = context.getContentResolver().query(uri,projection,selection,selectionArgs,null);
            if(cursor != null && cursor.moveToFirst()){
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 烧录前面板线程
     * */
    class STM_DownloadThread extends Thread{
        @Override
        public void destroy() {
            // TODO Auto-generated method stub
            super.destroy();
        }
        public STM_DownloadThread(){
            if(mUsbConnection!=null){
                mUsbConnection.close();
            }
            mUsbConnection = mUsbManager.openDevice(mUsbDevice);
            mUsbConnection.claimInterface(mUsbInterface[1], true);
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            int datalength;
            while(threadcontrol_ct){
                while(St_Download_Data_Flag){
                    St_Download_Data_Flag=false;
                    sendData= "5";
                    mHandler.obtainMessage(1).sendToTarget();
                    Log.d(TAG,"正在烧录前面板......");
                    byte[] mytmpbyte=sendData.getBytes();

                    while(true) {
                        int b = mUsbConnection.bulkTransfer(mUsbEndpoint[1][0], mytmpbyte, mytmpbyte.length, 100);
                        Log.d(TAG,"发送了"+b+"字节");
//                        connection.bulkTransfer(endpoint[1][0], mytmpbyte, mytmpbyte.length, 30);
                        datalength = mUsbConnection.bulkTransfer(mUsbEndpoint[1][1], recvBuffer, recvBuffer.length, 30);
                        if (datalength != 0){
                            Log.d(TAG,"连接成功");
                            mHandler.obtainMessage(2).sendToTarget();
                            break;
                        } else {
                            Log.e(TAG,"接收握手信号失败");
                            break;
                        }
                    }


                    //打开文件
                    File file = new File(str_filepath);
                    try {
                        fis = new FileInputStream(file);
                        byteLen = fis.available();//获取文件的大小
                        mHandler.obtainMessage(3).sendToTarget();

                        head[0] = (byte) (byteLen >> 24); //将文件大小放到22个字节的前4个字节中
                        head[1] = (byte) (byteLen >> 16);
                        head[2] = (byte) (byteLen >> 8);
                        head[3] = (byte) byteLen;

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Looper.prepare();
                    int headlen = mUsbConnection.bulkTransfer(mUsbEndpoint[1][0],head,head.length,30);//发送22个字节给前面板
                    //showToast("发送了"+headlen+"个字节"); //调试用
                    //mHandler.obtainMessage(7).sendToTarget();//调试用
                    datalength = mUsbConnection.bulkTransfer(mUsbEndpoint[1][1], recvBuffer,recvBuffer.length, 5000);
                    // showToast("收到字节数："+datalength); //调试用
                    int proFileSize = 0;//
                    int readfilelen = 0; //读取到的sendBuffer缓存的字节数
                    mHandler.obtainMessage(5).sendToTarget();
                    while (datalength == 2)
                    {
//                        int datapos = 0;
//                        int readfilelen = 0;
                        try {
                            //mHandler.obtainMessage(2).sendToTarget();
                            readfilelen = fis.read(sendBuffer,0,2048);
                            // fis.mark(datapos);
                            // showToast("读取到文件大小为："+fis.read(sendBuffer));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        datalength = mUsbConnection.bulkTransfer(mUsbEndpoint[1][0], sendBuffer, readfilelen, 100);
                        //showToast("发送的字节数为：" + datalength);//调试用
                        mHandler.obtainMessage(6).sendToTarget();
                        Log.d(TAG,".");
                        datalength = mUsbConnection.bulkTransfer(mUsbEndpoint[1][1], recvBuffer, recvBuffer.length, 100);
                        //mHandler.obtainMessage(7).sendToTarget();
                        //showToast("校检和" + datalength + "字节");
                        proFileSize = readfilelen + proFileSize;//烧录文件的大小
                        if(readfilelen < 2048){//最后一次读取到的文件小于2048时说明已读取完文件
                            try {
                                fis.close();//关闭文件
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mHandler.obtainMessage(4).sendToTarget();
                            showToast("烧录完成");
                            //stm_downloadThread.destroy();//使用这个销毁会中止APP
                            break;
                        }
                    }
                    Looper.loop();
                }
            }
        }
    }


    /**
     * 烧录电机板线程
     * */
    class TI_DownloadThread extends Thread{
        @Override
        public void destroy() {
            // TODO Auto-generated method stub
            super.destroy();
        }
        public TI_DownloadThread(){
            if(mUsbConnection!=null){
                mUsbConnection.close();
            }
            mUsbConnection = mUsbManager.openDevice(mUsbDevice);
            mUsbConnection.claimInterface(mUsbInterface[1], true);
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            int datalength;
            int readfilelen = 0; //读取到的sendBuffer缓存的字节数
            while(threadcontrol_ct){
                if(Ti_Download_Data_Flag){
                    Ti_Download_Data_Flag=false;
                    byte connectFlag= 'A';
                    byte sciLevelFalg = 0;
                    int ReadTxCount = 0;
                    int wordData = 0;
                    int blockLen = 0;
                    //打开文件
                    mHandler.obtainMessage(7).sendToTarget();
                    File file = new File(str_filepath);
                    try {
                        fis = new FileInputStream(file);
                        byteLen = fis.available();//获取文件的大小
                        mHandler.obtainMessage(3).sendToTarget();
                        Log.d(TAG,"烧录文件大小："+byteLen);
                        readfilelen = fis.read(sendBuffer,0,22);
                        sciLevelFalg = sendBuffer[2];
                        Log.d(TAG,"读取到22个字节,第三个字节为:"+sciLevelFalg);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    switch (sciLevelFalg){
                        case 0x00:
                            connectFlag = 'A';//兼容历史，只能使用这个字符
                            break;
                        case (byte) 0xA0:
                            connectFlag = (byte) 0xA0;
                            break;
                        case (byte) 0xA1:
                            connectFlag = (byte) 0xA1;
                            break;
                        case (byte) 0xA2:
                            connectFlag = (byte) 0xA2;
                            break;
                        default:
                            connectFlag = 'A';
                            break;
                    }
                    //byte[] mytmpbyte=sendData.getBytes();



                    while(true) {
                        int i = mUsbConnection.bulkTransfer(mUsbEndpoint[1][0], new byte[]{connectFlag}, 1, 50);
                        Log.d(TAG,"发送了："+i+"个字节");
                        datalength = mUsbConnection.bulkTransfer(mUsbEndpoint[1][1], recvBuffer, recvBuffer.length, 200);//读取到9个字节
                        if (datalength == 9 && recvBuffer[0] == connectFlag){

                            Log.d(TAG,"收到9个字节\n连接成功");

                            mHandler.obtainMessage(2).sendToTarget();
                            MotorVersionNum = recvBuffer[1];
                            MotorVersionNum &= 0xff;
                            mHandler.obtainMessage(8).sendToTarget();
                            MotorVersionNum = recvBuffer[3];
                            MotorVersionNum &= 0xff;
                            LCDVersionNum = recvBuffer[5];
                            LCDVersionNum &= 0xff;
                            mHandler.obtainMessage(9).sendToTarget();
                            LCDVersionNum = recvBuffer[7];
                            LCDVersionNum &= 0xff;
                            mHandler.obtainMessage(10).sendToTarget();
                            Log.d("YGDM:",">The version of Motor Control MCU"+recvBuffer[1]+recvBuffer[2]+recvBuffer[3]+recvBuffer[4]);
                            Log.d("YGDM:",">The version of LCD Display MCU"+recvBuffer[5]+recvBuffer[6]+recvBuffer[7]+recvBuffer[8]);
                            break;
                        }
                    }

                    Looper.prepare();

                    mUsbConnection.bulkTransfer(mUsbEndpoint[1][0],sendBuffer,readfilelen,30);//发送22个字节
                    //showToast("发送了"+headlen+"个字节"); //调试用
                    //mHandler.obtainMessage(7).sendToTarget();//调试用
                    datalength = mUsbConnection.bulkTransfer(mUsbEndpoint[1][1], recvBuffer, recvBuffer.length, 10000);//接收2字节 校检和
                    //等待擦除Flash
                    Log.e(TAG, "收到发送22字节的校检字节"+String.valueOf(datalength));
                    // showToast("收到字节数："+datalength); //调试用
                    int proFileSize = 0;//

                    if (datalength == 2)
                    {
                        mHandler.obtainMessage(5).sendToTarget();
                        while (true) {
                            try {
                                //mHandler.obtainMessage(2).sendToTarget();
                                // readfilelen = fis.read(sendBuffer,0,1);
                                readfilelen = fis.read(readBuffer);
                                // fis.mark(datapos);
                                // showToast("读取到文件大小为："+fis.read(sendBuffer));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            sendBuffer[blockLen++] = readBuffer[0];
                            if (ReadTxCount == 0x00){
                                wordData = readBuffer[0];
                                wordData &= 0xff;
                            } else if (ReadTxCount == 0x01){
                                byteData =readBuffer[0];
                                byteData &= 0xff;
                                wordData |= (byteData << 8);
                            }
                            ReadTxCount++;
                            if(readfilelen == -1){
                                wordData = 0x0000;
                                byteData = 0x0000;
                                byte[] mFinish = {0,0};
                                datalength = mUsbConnection.bulkTransfer(mUsbEndpoint[1][0],mFinish , 2, 50);
                                mHandler.obtainMessage(4).sendToTarget();
                                //ti_downloadThread.destroy();//使用这个销毁会中止APP
                                Log.d(TAG,"最后发送2个字节，让前面板的接收程序知道已经烧录完成");
                                showToast("烧录完成");
                                break;
                            }else if (ReadTxCount == 2 * (wordData + 3)){
                                mUsbConnection.bulkTransfer(mUsbEndpoint[1][0],sendBuffer , blockLen, 1000);
                                Log.d(TAG+"2x3","blockLen"+blockLen+"\nReadTxCount"+ReadTxCount);
                                blockLen = 0;
                                datalength = mUsbConnection.bulkTransfer(mUsbEndpoint[1][1], recvBuffer, recvBuffer.length, 3000);
                                if (datalength != 2)
                                {
                                    Log.e("WARNNING_2x3:","校检和失败");
                                } else {
                                    Log.d(TAG,"2x3");
                                }
                                mHandler.obtainMessage(6).sendToTarget();
                                wordData = 0x0000;
                                byteData = 0x0000;
                                ReadTxCount = 0x00;
                            } else if ((ReadTxCount -6) % 2048 == 0 && ReadTxCount > 6){
                                mUsbConnection.bulkTransfer(mUsbEndpoint[1][0],sendBuffer , blockLen, 1000);
                                Log.d(TAG+"0x800","blockLen"+blockLen+"\nReadTxCount"+ReadTxCount);
                                blockLen = 0;
                                datalength = mUsbConnection.bulkTransfer(mUsbEndpoint[1][1], recvBuffer, recvBuffer.length, 3000);
                                Log.e(TAG+"0x800", String.valueOf(datalength));
                                if (datalength != 2)
                                {
                                    Log.e("WARNNING0x800","校检和失败");
                                } else {
                                    Log.d("process",".");
                                }
                                mHandler.obtainMessage(6).sendToTarget();
                            } else if (blockLen == 2048){
                                mUsbConnection.bulkTransfer(mUsbEndpoint[1][0],sendBuffer , blockLen, 3000);
                                Log.d(TAG,"烧录2048字节");
                                mHandler.obtainMessage(6).sendToTarget();
                                blockLen = 0;
                            }
                            //mHandler.obtainMessage(6).sendToTarget();
                        }
                    } else {
                        showToast("烧录失败，接收22字节校检失败");
                        Log.e(TAG,"接收发送22字节的校检失败");
                    }
                    //  mHandler.obtainMessage(3).sendToTarget();
                    Looper.loop();
                }
            }
        }
    }


}
