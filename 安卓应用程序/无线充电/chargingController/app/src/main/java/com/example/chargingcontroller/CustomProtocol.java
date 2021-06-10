package com.example.chargingcontroller;


import android.util.Log;

import com.example.chargingcontroller.CRC;


public class CustomProtocol {
    private static final String TAG = "CustomProtocol";
    public static final int TARGET_POS_DIR = 0x06;
    public static final int TARGET_POS = 0x07;
    public static final int TYPE_IMAGE = 0x10;
    public static final int TYPE_PATH = 0x11;
    public static int[] decode(byte[] buff, int len){
        if(buff == null || len < 4 || buff[0] != (byte)0xff || buff[1] != (byte)0xfe){
            Log.d(TAG, "数据不合格!");
            return null;
        }
        int funcCode = buff[2];
        int dataLen = buff[3];
        short crc = 0;
        crc |= (((short)buff[len - 2]) & 0xff);
        crc <<= 8;
        crc |= (((short)buff[len - 1]) & 0xff);
        if(crc != CRC.CRC16_Calc(buff, len - 2)){
            Log.d(TAG, "CRC计算不一致！ 接收出来的CRC为： " + crc + ", 计算出来的CRC为： " + CRC.CRC16_Calc(buff, len - 2));
            return null;
        }
        int[] res;
        switch (funcCode){
            case TARGET_POS:

                break;
            case TARGET_POS_DIR:
                if(len != 12){
                    Log.d(TAG, "数据长度不合格！");
                    return null;
                }
                res = new int[4];
                res[0] = TARGET_POS_DIR;
                for(int i = 0; i < 3; ++i){
                    res[i+1] = ((int)buff[4 + (i<<1)]) & 0xff;
                    res[i+1] = (res[i+1] << 8) | (((int)buff[5+(i<<1)]) & 0xff);
                }
                return res;
            case TYPE_IMAGE:
                if(len != 10){
                    Log.d(TAG, "数据长度不合格！");
                    return null;
                }

                res = new int[2];
                res[0] = TYPE_IMAGE;
                res[1] = 0;
                for(int i = 4; i < 8; ++i){
                    res[1] <<= 8;
                    res[1] |= (((int)buff[i]) & 0xff);
                }
                return res;
            case TYPE_PATH:

                break;
        }
        return null;
    }
    public static int[] decode(byte[] buff){
        return decode(buff, buff.length);
    }

    public static void sendSpeedCmd(TcpClient mTcpClient, int cmdId, int data){
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
