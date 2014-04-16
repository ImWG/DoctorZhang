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
	long timeInterval1 = 20;						//20ms��¼һ��
	double thresholdValue = 8.8;					//Ԥ����ֵ
	double thresholdTotalValue = 120;				//�ۼƷ�ֵ
	double gyroscopeMold = 0;						//���ٶ�ģ
	double gyroscopeTotalMold = 0;					//�ۼƼ��ٶ�ģ
	double[] warningTemp;							//�ۼ�����
	int size = 10;									//�ۼ����鳤��
	int sizeTemp = 0;								//��¼ջ��
	boolean isWraning = false;						//Ԥ������
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
				sizeTemp++;									//ջ���Լ�
				if(sizeTemp == size) {						//�ۼ�������
					for(int i=0; i<size; i++) {
						gyroscopeTotalMold += warningTemp[i];
					}
					if(gyroscopeTotalMold < thresholdTotalValue) {	//2���ж�
						runWarning();						//���б���
					}
					sizeTemp = 0;							//��λ
				}
			}
			handler1.postDelayed(this, timeInterval1);		//�ٿ�����ʱtimeInterval
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
			// event.values float[]������x,y,z�ļ��ٶ�ֵ
			gyroscopeMold = toMold(event.values);				//���÷�������ȡģ
		}
		if((gyroscopeMold < thresholdValue) && (!isWraning)) {	//δwarningʱ���У�1���ж�
			isWraning = true;
			warningTemp = new double[size];					//��ʼ���ۼ�����
			sizeTemp = 0;
			warningTemp[sizeTemp] = gyroscopeMold;
			gyroscopeTotalMold = 0;								//��ʼ���ۼƼ��ٶ�ģ
			sizeTemp++;
		}
	}
	
	
	@Override
	public void onCreate() {
		Log.i(TAG, "3onCreate");
		super.onCreate();
		mySensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		//ע�������
		Sensor sensor = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mySensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
		
    }
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(TAG, "3onStart");
		
		super.onStartCommand(intent, 0 , startId);		//onStart������ʱ�����޸�
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "3onStartCommand");
		handler1.postDelayed(runnable1, timeInterval1);	//ÿtimeInterval1ִ��һ��runnable1
		
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
	
	/****************���б���****************/
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
	
	/**********�� Activity ���²���*********/
	public void updateData(){
		
		Time t=new Time();
		t.setToNow(); // ȡ��ϵͳʱ�䡣
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
		
		Intent intent = new Intent();	//���� Intent ����
		intent.setAction("com.example.doctorzhang.FallingActivity");
		intent.putExtra("time", time);		//�����Ϣ
		sendBroadcast(intent);				//�����㲥
	}

}
