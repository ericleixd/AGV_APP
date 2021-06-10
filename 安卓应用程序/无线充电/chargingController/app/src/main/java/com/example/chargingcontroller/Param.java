package com.example.chargingcontroller;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.Image;
import android.os.Environment;

//import com.example.agvcontroller.ui.utils.CarPose;

import java.util.Observable;
import java.util.Observer;

public class Param extends Application implements Observer {
    @Override
    public void update(Observable observable, Object o) {
        isTCPConnected = (Boolean)o;
    }

    public static enum DIR{
        STOP, UP, LEFT, DOWN, RIGHT
    };
    private volatile static double MAX_SPEED;
    public volatile static int RockerRadiusBig;
    public volatile static int RockerRadiusSmall;
    private volatile static String Server_ip_addr;
    private volatile static int Server_port;
    public volatile static double TURN_ANGLE = 15.0D;
    //    public volatile static CarPose currentCarPos;
    private volatile static DIR direction = DIR.STOP;
    public volatile static Bitmap mapGraph = null;
    private volatile static Param pParam = null;
    private volatile static double speed = 0.0;
    public volatile static boolean isTCPConnected = false;
    public volatile static PointF targetPos;
    public volatile static Double targetDirection;
    @SuppressLint("SdCardPath")
    public static String storagePath;
    public static String graphSavePath;
    public static int maxNumOfHistoryMap = 10; //历史地图最多的数量

    //需要修改
    public volatile static int photoChannelOccupiedId = 0;   //photo trans channel occupied by id: id of activity or fragment; 0: spare

    private Param(){
        init();
    }
    public static Param getInstance()
    {
        if (pParam == null)
        {
            synchronized(Param.class) {
                if(pParam == null) {
                    pParam = new Param();
//                    localParam.init();
                }
            }
        }
        return pParam;
    }
    public void decreaseSpeed(double paramDouble)
    {
        speed = Math.max(speed - paramDouble, 0.0D);
    }

    public DIR getDirection()
    {
        return direction;
    }

    public double getMaxSpeed()
    {
        return MAX_SPEED;
    }

    public String getServer_ip_addr()
    {
        return Server_ip_addr;
    }

    public int getServer_port()
    {
        return Server_port;
    }

    public double getSpeed()
    {
        return speed;
    }

    public void increaseSpeed(double paramDouble)
    {
        this.setSpeed(speed + paramDouble);
    }

    public void init()
    {
        Server_ip_addr = "192.168.1.101";
        Server_port = 8080;
        MAX_SPEED = 2.0D;
        speed = 0.0D;
        direction = DIR.STOP;
//        currentCarPos = null;
    }

    public void setDirection(DIR paramDIR)
    {
        direction = paramDIR;
    }

    public void setMaxSpeed(double paramDouble)
    {
        MAX_SPEED = paramDouble;
    }

    public void setServer_ip_addr(String paramString)
    {
        Server_ip_addr = paramString;
    }

    public void setServer_port(int paramInt)
    {
        Server_port = paramInt;
    }

    public void setSpeed(double paramDouble)
    {
        speed = Math.max(-MAX_SPEED, Math.min(paramDouble, MAX_SPEED));
    }

    public void speedReset()
    {
        speed = 0.0D;
    }
}
