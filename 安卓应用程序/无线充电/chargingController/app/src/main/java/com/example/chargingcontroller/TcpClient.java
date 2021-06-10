package com.example.chargingcontroller;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import com.example.chargingcontroller.Param;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import static android.content.Context.MODE_PRIVATE;

public class TcpClient extends Thread {
    private static final String TAG = "TcpClient";
    private Socket mSocket;
    private String serverIp;
    private int serverPort;
    private byte[] sendBuffer = null;
    private byte[] receiveBuffer;
    private int length = 0;
    private volatile boolean isRun = false;
    private volatile boolean isConnected = false;
    private InputStream mInStream;
    private OutputStream mOutStream;
    private LinkedList<Observer> mObservers;
    private LinkedList<Observer> mPhotoObservers;
    private LinkedList<Observer> mPositionObservers;
    private static TcpClient mTcpClient;
    private long lastTime = 0;
    private Context mContext;


    public TcpClient(String ip, int port) {
        serverIp = ip;
        serverPort = port;
        mObservers = new LinkedList<>();
        mPhotoObservers = new LinkedList<>();
        mPositionObservers = new LinkedList<>();
        mTcpClient = null;
        receiveBuffer = new byte[1024];
    }

    public void setContext(Context con){
        mContext = con;
    }

    public Context getContext(){
        return mContext;
    }

    public void register(Observer v){
        mObservers.offer(v);
    }
    public void registerPhotoObs(Observer v){
        mPhotoObservers.offer(v);
    }

    public void registerPositionObs(Observer v){
        mPositionObservers.offer(v);
    }
    public void unRegister(Observer v){
        mObservers.remove(v);
    }

    public void unRegisterPhotoObs(Observer v){
        mPhotoObservers.remove(v);
    }

    public void unRegisterPositionObs(Observer v){
        mPositionObservers.remove(v);
    }

    public void setServerIp(String ip, int port){
        serverIp = ip;
        serverPort = port;
    }

    public boolean getIsConnected(){
        return isConnected;
    }

    private boolean isConnected(){
        if(mOutStream == null) return false;
        try{
            //发送心跳包
            mOutStream.write(new byte[]{0x7f});
            mOutStream.flush();
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static TcpClient getInstance(){
        if (null == mTcpClient) {
            synchronized (TcpClient.class) {
                if (null == mTcpClient) {
                    mTcpClient = new TcpClient("127.0.0.1", 8080);
                }
            }
        }
        return mTcpClient;
    }

    public void killSelf(){
        isRun = false;
    }

    private void notifyAllObservers(){
        for(Observer v: mObservers){
            v.update(new Observable(), (Boolean)(isConnected));
        }
        Log.d(TAG, "notify all observers!");
    }

    private void notifyPhotoObs(Bitmap bitmap){
        for(Observer v: mPhotoObservers){
            v.update(new Observable(), (Bitmap)bitmap);
        }
        Log.d(TAG, "notify photo observers!");
    }

    private void notifyPositionObs(){
        for(Observer v: mPositionObservers){
            v.update(new Observable(), 0);
        }
        Log.d(TAG, "notify position observers!");
    }

    private boolean connect(){
        try {
            mSocket = new Socket(serverIp, serverPort);
            if(mSocket == null) return false;
            mOutStream = mSocket.getOutputStream();
            mInStream = mSocket.getInputStream();
//            mBufferedInStream = new BufferedInputStream(mInStream);
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
        isConnected = true;
        notifyAllObservers();
        return true;
    }

    public void connect(String ip, int port){
        serverIp = ip;
        serverPort = port;
        new Thread(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        }).start();
    }

    void disConnect(){
        try {
            if (mOutStream != null) {
                mOutStream.close();
                mOutStream = null;
            }
            if (mInStream != null) {
                mInStream.close();
                mInStream = null;
            }
            if(mSocket != null){
                mSocket.close();
                mSocket = null;
            }
//            if(mBufferedInStream != null){
//                mBufferedInStream.close();
//                mBufferedInStream = null;
//            }
            isConnected = false;
            notifyAllObservers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean sendData(Context context, byte[] buff, int len){
        if(!isConnected){
            Toast.makeText(context, "未连接！", Toast.LENGTH_SHORT).show();
            return false;
        }
        sendData(buff, len);
        return true;
    }

    public void sendData(String data){
        try {
            sendBuffer = data.getBytes("utf-8");
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        sendData(sendBuffer, sendBuffer.length);
    }

    public void sendData(byte[] buff, int len){
        sendBuffer = buff;
        this.length = len;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mOutStream == null) return;
                try{
                    mOutStream.write(sendBuffer, 0, length);
                    mOutStream.flush();
                }catch (IOException e){
                    isConnected = false;
                    notifyAllObservers();
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void run() {
        isRun = true;
        int offlineCnt = 0;     //离线检测计数器
        while(isRun){
            if(isConnected && mInStream != null) {
                //todo receive data

                int lenBuff = 0;
                try {
                    lenBuff = mInStream.read(receiveBuffer);
                } catch (IOException e) {
                    isConnected = false;
                    notifyAllObservers();
                    e.printStackTrace();
                }

                System.out.println("收到的数据为： ");
                for(int i = 0; i < lenBuff; ++i){
                    int tmp = ((int)receiveBuffer[i]) & 0xff;
                    String hexString = Integer.toHexString(tmp);
                    System.out.print(hexString + " ");
                }
                System.out.println();

                if(lenBuff > 0) {
                    int[] decodeMsg = CustomProtocol.decode(receiveBuffer, lenBuff);
                    if(decodeMsg != null && decodeMsg.length >= 2){
                        int funcCode = decodeMsg[0];
                    }else{
                        Log.d(TAG, "解码错误！");
                    }
                    offlineCnt = 0;
                }
                else{ //连接已中断
                    ++offlineCnt;
                    if(offlineCnt >= 3) {
                        isConnected = false;
                        notifyAllObservers();
                        offlineCnt = 0;
                        Log.d(TAG, "连接中断！");
                    }
                }
            }
//            if(System.currentTimeMillis() - lastTime > 5000) {  //check the connect status per second.
//                lastTime = System.currentTimeMillis();
//                boolean flag = isConnected();
//                if (isConnected != flag) {
//                    isConnected = flag;
//                    notifyAllObservers();
//                }
//            }
        }
    }
}
