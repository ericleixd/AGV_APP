package com.example.chargingcontroller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * A simple {@link Fragment} subclass.
 * Use the
 * create an instance of this fragment.
 */
public class ChargingFragment extends Fragment {

    Switch switch_charging_mode;
    ToggleButton tb_charging_OnOff;
    EditText et__i_ref;
    EditText et__u_ref;
    double I_Ref;
    double U_Ref;
    String temp; // 用来保存EditText的数据
    private byte[] mBuffer;
    private TcpClient mTcpClient;
    char chargingMode;// I U N

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_charging, container, false);
        et__u_ref = root.findViewById(R.id.et_u_ref);
        et__i_ref = root.findViewById(R.id.et_i_ref);
        switch_charging_mode = root.findViewById(R.id.switch_charging_mode);
        tb_charging_OnOff = root.findViewById(R.id.tb_charging_OnOff);
        mTcpClient = TcpClient.getInstance();

        chargingMode = 'N';

        mBuffer = new byte[10];
        mBuffer[0] = (byte)0xff; //消息头
        mBuffer[1] = (byte)0xfe; //消息头
        mBuffer[2] = 0x12; // 功能码
        mBuffer[3] = 3; //数据长度

        tb_charging_OnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    if (switch_charging_mode.isChecked()){
                        // The switch is enabled（right）
                        // 开始充电--恒流模式
                        showMessage(getActivity(),"This is I mode",1);
                        chargingMode = 'I';
                        temp = et__i_ref.getText().toString().trim();// trim 出除空格
                        I_Ref = Double.parseDouble(temp); // 把输入的数据用double保存
                        sendCmd(chargingMode,I_Ref);
                        showMessage(getActivity(),"This is "+ chargingMode + " mode.  I_ref = " + String.valueOf(I_Ref),1);
                    }else {
                        // The switch is disabled(left)
                        // 开始充电--恒压模式
                        showMessage(getActivity(),"This is U mode",1);
                        chargingMode = 'U';
                        temp = et__u_ref.getText().toString().trim();
                        U_Ref = Double.parseDouble(temp);
                        sendCmd(chargingMode,U_Ref);
                        showMessage(getActivity(),"This is "+ chargingMode + " mode.  U_ref = " + String.valueOf(U_Ref),1);
                    }
                } else {
                    // The toggle is disabled
                    // 停止充电
                    chargingMode = 'N';
                    temp = "0.00"; // 为了格式统一这个参数没有意义
                    U_Ref = Double.parseDouble(temp);
                    I_Ref = Double.parseDouble(temp);
                    sendCmd(chargingMode,U_Ref);
                    showMessage(getActivity(),"This is "+ chargingMode + " mode.  U_ref = " + String.valueOf(U_Ref) + " I_ref = " +String.valueOf(I_Ref),1);
                }
            }
        });
        return root;
    }
// 把要发送的信息打在屏幕上
    private void showMessage(Context context, String Message, int isLong){
        Toast.makeText(context,Message,isLong).show();
    }
// 和工控机进行TCP通讯，把充电模式和对应的参数发送到工控机，并采用CRC校验
    void sendCmd(char _chargingMode, double _voltageOrCurrent){
//        为了减小数据长度，从Double转换到int，乘100是为了保留小数点后两位，在ROS中读取这个数据后需要除100
        int voltageOrCurrent = (int)(_voltageOrCurrent*100);
        mBuffer[4] = (byte)(_chargingMode);
        mBuffer[5] = (byte)((voltageOrCurrent >> 8) & 0xff);
        mBuffer[6] = (byte)(voltageOrCurrent & 0xff);
        try{
            // CRC校验采用16位校验码，使用查表法
            short crc = CRC.CRC16_Calc(mBuffer, 7);
            mBuffer[7] = (byte)((crc >> 8) & 0xff);
            mBuffer[8] = (byte)(crc & 0xff);
            // 使用TCP把消息发出去
            mTcpClient.sendData(mBuffer, 9);
        }catch (Exception e){
            System.out.println("-----socket thread exception-----");
        }
    }


}