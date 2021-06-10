package com.example.chargingcontroller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.chargingcontroller.R;
import com.example.chargingcontroller.Param;

import java.util.Observable;
import java.util.Observer;

import static android.content.Context.MODE_PRIVATE;
public class wifiConnectFragment extends Fragment implements Observer {
    private EditText et_ipAddr;
    private EditText et_port;
    private Button bt_connect;
    private TextView tv_status;
    private Param mParam;
    private TcpClient mTcpClient;
    private SharedPreferences mSharedPreference;
    private boolean isConnected = false;
    private Handler mHandler;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //todo modify fragment
        View root = inflater.inflate(R.layout.fragment_connect, container, false);

        et_ipAddr = root.findViewById(R.id.et_ipAddr);
        et_port = root.findViewById(R.id.et_port);
        bt_connect = root.findViewById(R.id.bt_connect);
        tv_status = root.findViewById(R.id.tv_status);
        mParam = Param.getInstance();
        mTcpClient = TcpClient.getInstance();
        mTcpClient.setContext(getActivity());
        mTcpClient.register(this);
        mTcpClient.register(mParam);
        isConnected = Param.isTCPConnected;

        if(!isConnected){
            bt_connect.setText("连接");
            tv_status.setText("未连接");
            bt_connect.setBackgroundColor(0xff00ddff);
        }else{
            bt_connect.setText("断开");
            tv_status.setText("已连接");
            bt_connect.setBackgroundColor(0xffcc0000);
        }

        try {
            mSharedPreference = getActivity().getSharedPreferences("wifiConnectSocket", MODE_PRIVATE);
            String _ip = mSharedPreference.getString("IP", "");
            int _port = mSharedPreference.getInt("PORT", 0);
            if (_ip != null && validIpAddr(_ip)) {
                mParam.setServer_ip_addr(_ip);
                mParam.setServer_port(_port);
            }
        }catch (Exception e){
            Log.i("exception: ", "getSharedPrefrence failed!");
        }
        et_ipAddr.setText(mParam.getServer_ip_addr());
        et_port.setText(String.valueOf(mParam.getServer_port()));
        bt_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnected) {
                    String ip_str = et_ipAddr.getText().toString();
                    String port_str = et_port.getText().toString();
                    if (validIpAddr(ip_str)) {
                        mParam.setServer_ip_addr(et_ipAddr.getText().toString());
                        System.out.println("ip地址为：" + mParam.getServer_ip_addr());
                    } else {
                        Toast.makeText(getActivity(), "ip地址不合法", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        mParam.setServer_port(Integer.parseInt(et_port.getText().toString()));
                        Toast.makeText(getActivity(), "ip和port设置成功", Toast.LENGTH_SHORT).show();
                        System.out.println("port为：" + mParam.getServer_port());
                        SharedPreferences.Editor editor = mSharedPreference.edit();
                        editor.putString("IP", mParam.getServer_ip_addr());
                        editor.putInt("PORT", mParam.getServer_port());
                        editor.apply();
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "port不合法", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //                mTcpClient.setServerIp(mParam.getServer_ip_addr(), mParam.getServer_port());
                    mTcpClient.connect(mParam.getServer_ip_addr(), mParam.getServer_port());
                }else{
                    mTcpClient.disConnect();
                    isConnected = false;
                }
            }
        });

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                boolean flag = (Boolean) message.obj;
                if(flag){
                    tv_status.setText("TCP连接成功！");
                    bt_connect.setText("断开");
                    bt_connect.setBackgroundColor(0xffcc0000);
                }else{
                    tv_status.setText("TCP已断开！");
                    bt_connect.setText("连接");
                    bt_connect.setBackgroundColor(0xff00ddff);
                }
                return false;
            }
        });
        return root;
    }

    boolean validIpAddr(String ip){
        String[] lsStr = ip.split("\\.");
        if(lsStr.length != 4) return false;
        int num;
        for(int i = 0; i < 4; ++i){
            try {
                num = Integer.parseInt(lsStr[i]);
            }catch (Exception e){
                return false;
            }
            if(num < 0 || num > 255) return false;
        }
        return true;
    }

    @Override
    public void update(Observable observable, Object o) {
        isConnected = (Boolean)o;
        if(isConnected) {
            if (!mTcpClient.isAlive())
                mTcpClient.start();
        }
        Message msg = new Message();
        msg.obj = o;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onDestroyView() {
        //在fragment销毁的时候进行数据保存等操作
        System.out.println("--------------wifi fragment is been destroyed------------");
        super.onDestroyView();
    }
}
