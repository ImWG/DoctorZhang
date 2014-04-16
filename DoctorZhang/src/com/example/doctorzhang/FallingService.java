package com.example.doctorzhang;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

public class FallingService extends Service implements SensorEventListener {

	private static final String TAG = "FallService";
	long timeInterval1 = 20;						//20ms记录一次
	double thresholdValue = 8.8;					//预警阀值
	double thresholdTotalValue = 120;				//累计阀值
	double gyroscopeMold = 0;						//加速度模
	double gyroscopeTotalMold = 0;					//累计加速度模
	double[] warningTemp;							//累计数组
	int size = 10;									//累计数组长度
	int sizeTemp = 0;								//记录栈顶
	boolean isWraning = false;						//预警开关
	float[] gyroscope = {0,0,0};
	MediaPlayer myMediaPlayer;
	SensorManager mySensorManager;
	
	Handler handler1 = new Handler();
	

	Runnable runnable1 = new Runnable(){
	@Override
		public void run() {
			// TODO Auto-generated method stub
			if(isWraning) {
				warningTemp[sizeTemp] = gyroscopeMold;
				sizeTemp++;									//栈顶自加
				if(sizeTemp == size) {						//累计数组满
					for(int i=0; i<size; i++) {
						gyroscopeTotalMold += warningTemp[i];
					}
					if(gyroscopeTotalMold < thresholdTotalValue) {	//2次判定
						runWarning();						//进行报警
					}
					sizeTemp = 0;							//复位
				}
			}
			handler1.postDelayed(this, timeInterval1);		//再开启定时timeInterval
		}
	};
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			// event.values float[]保存了x,y,z的加速度值
			gyroscopeMold = toMold(event.values);				//调用方法数据取模
		}
		if((gyroscopeMold < thresholdValue) && (!isWraning)) {	//未warning时进行，1次判定
			isWraning = true;
			warningTemp = new double[size];					//初始化累计数组
			sizeTemp = 0;
			warningTemp[sizeTemp] = gyroscopeMold;
			gyroscopeTotalMold = 0;								//初始化累计加速度模
			sizeTemp++;
		}
	}
	
	
	@Override
	public void onCreate() {
		Log.i(TAG, "3onCreate");
		super.onCreate();
		mySensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		//注册监听器
		Sensor sensor = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mySensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
		
    }
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(TAG, "3onStart");
		
		super.onStartCommand(intent, 0 , startId);		//onStart方法过时，已修改
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "3onStartCommand");
		handler1.postDelayed(runnable1, timeInterval1);	//每timeInterval1执行一次runnable1
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() { 
		Log.i(TAG, "3onDestroy");
		
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public double toMold(float values[]) { 
		double gyroscopeMold = 0;
		for(int i=0; i<3; i++) {
			gyroscopeMold += values[i]*values[i];
		}
		gyroscopeMold = Math.sqrt(gyroscopeMold);
		return gyroscopeMold;
  	}
	
	/****************进行报警****************/
	private void runWarning() {
		// TODO Auto-generated method stub
		myMediaPlayer =MediaPlayer.create(getBaseContext(), R.raw.fall);
		myMediaPlayer.setVolume(1.0f, 1.0f);
		myMediaPlayer.start();
		myMediaPlayer.stop();
		myMediaPlayer.release();
		updateData();
		Log.i(TAG, "Warning");
	}
	
	/**********向 Activity 更新步数*********/
	public void updateData(){
		
		Time t=new Time();
		t.setToNow(); // 取得系统时间。
		int year = t.year;
		int month = t.month;
		int date = t.monthDay;
		int hour = t.hour; // 0-23
		int minute = t.minute;
		int second = t.second;
		String time = 	String.valueOf(year) + "-" +
						String.valueOf(month) + "-" +
						String.valueOf(date) + " " +
						String.valueOf(hour) + ":" +
						String.valueOf(minute) + ":" +
						String.valueOf(second) ;
		
		Intent intent = new Intent();	//创建 Intent 对象
		intent.setAction("com.example.doctorzhang.FallingActivity");
		intent.putExtra("time", time);		//添加信息
		sendBroadcast(intent);				//发出广播
	}

}
