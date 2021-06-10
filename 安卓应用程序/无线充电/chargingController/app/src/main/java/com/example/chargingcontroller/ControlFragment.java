package com.example.chargingcontroller;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.chargingcontroller.Param;
import com.example.chargingcontroller.R;
import com.example.chargingcontroller.TcpClient;
import com.example.chargingcontroller.CRC;

public class ControlFragment extends Fragment implements View.OnTouchListener {
    private ImageButton bt_up;
    private ImageButton bt_down;
    private ImageButton bt_left;
    private ImageButton bt_right;
    private ImageButton bt_stop;
    private Param mParam;
    private TcpClient mTcpClient;

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_control, container, false);

        bt_up = root.findViewById(R.id.bt_up);
        bt_down = root.findViewById(R.id.bt_down);
        bt_left = root.findViewById(R.id.bt_left);
        bt_right = root.findViewById(R.id.bt_right);
        bt_stop = root.findViewById(R.id.bt_stop);
        mParam = Param.getInstance();
        mTcpClient = TcpClient.getInstance();

        bt_up.setOnTouchListener(this);
        bt_down.setOnTouchListener(this);
        bt_left.setOnTouchListener(this);
        bt_right.setOnTouchListener(this);
        bt_stop.setOnTouchListener(this);

        return root;
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            int id = v.getId();
            if(id == bt_stop.getId()){
                mParam.speedReset();
                sendCmd(0, 0);
                System.out.println("stop");
            }else if(id == bt_up.getId() || id == bt_down.getId()){
                if(id == bt_up.getId()){
                    mParam.increaseSpeed(0.5);
                }else{
                    mParam.increaseSpeed(-0.5);
                }
                //todo --there is a bug!!
                int speedToSend = (int)(Math.abs(mParam.getSpeed()) * 100);
                if(mParam.getSpeed() > 0.0){
                    sendCmd(0x01, speedToSend);
                }else{
                    sendCmd(0x02, speedToSend);
                }
            }else if(id == bt_left.getId() || id == bt_right.getId()){
                int angleToSend = (int)(Param.TURN_ANGLE * 100);
                if(id == bt_left.getId()){
                    sendCmd(0x03, angleToSend);
                }else{
                    sendCmd(0x04, angleToSend);
                }
            }
            v.setBackgroundColor(0xff7f7f7f);
//            Vibrator vibrator = (Vibrator)this.getSystemService(VIBRATOR_SERVICE);
//            vibrator.vibrate(50);
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            v.setBackgroundColor(0xffffffff);
            if(v.getId() == R.id.bt_left){ //方向键按下旋转，抬起后停止旋转
                sendCmd(0x03, 0);
            } else if(v.getId() == R.id.bt_right){
                sendCmd(0x04, 0);
            }
        }
        return false;
    }

    public void sendCmd(int cmdId, int data){
        byte[] mBuffer = new byte[8];
        mBuffer[0] = (byte)0xff;
        mBuffer[1] = (byte)0xfe;
        if(cmdId == 0){
            mBuffer[2] = 0;
            mBuffer[3] = 0;
            mTcpClient.sendData(mBuffer, 4);
        }else{
            mBuffer[2] = (byte)cmdId;
            mBuffer[3] = 2;
            mBuffer[4] = (byte)((data >> 8) & 0xff);
            mBuffer[5] = (byte)(data & 0xff);
            int crc = CRC.CRC16_Calc(mBuffer, 6);
            mBuffer[6] = (byte)((crc >> 8) & 0xff);
            mBuffer[7] = (byte)(crc & 0xff);
            mTcpClient.sendData(mBuffer, 8);
        }
    }
}
