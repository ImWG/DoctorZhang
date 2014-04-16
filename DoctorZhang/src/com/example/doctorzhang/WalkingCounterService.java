package com.example.doctorzhang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.heartrate.FFT;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

public class WalkingCounterService extends Service implements SensorEventListener {

	private static final String TAG = "LocalService";
	//服务端地址
	private String releaseRrcordUrl = "http://192.185.2.37/~penglu2/healthManage/Api.php?s=Index/activity_record";		//发布运动记录url
	private String getRrcordUrl = "http://192.185.2.37/~penglu2/healthManage/Api.php?s=Index/activity_record_list";	//用户运动记录列表url
	
	private IBinder binder=new WalkingCounterService.LocalBinder();
	private String strResult="8888";				//返回的json语句
	private String parseResult="8888";			//分析的值传递
	private String judgeResult="8888";			//返回judge的值
	float [] preCoordinate;
	double currentTime=0,lastTime=0;				//记录时间
	float WALKING_THRESHOLD = 20;
	public static int steps=0;					//记录步数
	boolean isActivityOn = false;					//Activity 是否运行
	boolean isNotReady = true;						//是否已返回数据
	boolean isServiceOn = false;					//Service 是否运行
	long timeInterval1 = 20*60*1000;				//20分钟记录一次
	int size = 512;
	int sizeTemp = 0;								//记录栈顶
	float [] gyroscope = {0,0,0};
	double[] gyroscopeMold;
	float [][] gyroscopeTemp;						//暂存数组栈
	SensorManager mySensorManager;
	
	
	Handler handler1 = new Handler();
	
	Runnable runnable1 = new Runnable(){
	@Override
		public void run() {
		// TODO Auto-generated method stub
		Share share = (Share)getApplicationContext();	//获取全局变量实例
		uploadData();
		if(share.getIsLogin()) {
			uploadToOnline();
		}
		handler1.postDelayed(this, timeInterval1);		//再开启定时timeInterval
		}
	};
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	@Override
	public void onCreate() {
		Log.i(TAG, "1onCreate");
		super.onCreate();
		mySensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		//注册监听器
		Sensor sensor = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mySensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
		
    }
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(TAG, "1onStart");
		super.onStartCommand(intent, 0 , startId);		//onStart方法过时，已修改
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "1onStartCommand");
		isServiceOn = true;
		//接受进程的调试信息
		if(isServiceOn){
			//启动定时器
			handler1.postDelayed(runnable1, timeInterval1);	//每timeInterval1执行一次runnable1
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() { 
		Log.i(TAG, "1onDestroy");
		mySensorManager.unregisterListener(this);
		mySensorManager = null;
		//停止定时器
		handler1.removeCallbacks(runnable1);
		
		super.onDestroy();
	}
	
	//定义内容类继承Binder
	public class LocalBinder extends Binder{
		//返回本地服务
		WalkingCounterService getService(){
			return WalkingCounterService.this;
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	
	//传感器发生变化后调用该方法
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			// event.values float[]保存了x,y,z
			analyseData(event.values);//调用方法分析数据
			gyroscope = event.values;
			//save(event.values);
		} 
	}
	
	//方法：分析参数进行计算
	public void analyseData(float[] values){
		//获取当前时间
		currentTime=System.currentTimeMillis();
		//每隔 200MS 取加速度力和前一个进行比较
		if(currentTime - lastTime >200){
			if(preCoordinate == null){//还未存过数据
				preCoordinate = new float[3];
				for(int i=0;i<3;i++){
					preCoordinate[i] = values[i];
				}
			} else{					//记录了原始坐标的话，就进行比较
				int angle = calculateAngle(values,preCoordinate);
				if(angle >= WALKING_THRESHOLD){
					steps++;		//步数增加
					updateData();	//更新步数
				}
				for(int i=0;i<3;i++){
					preCoordinate[i]=values[i];
				}
			}
			lastTime = currentTime;	//重新计时
		}
	}
		
	//方法：计算两个加速度矢量夹角的方法
	public int calculateAngle(float[] newPoints,float[] oldPoints){
		int angle=0;
		float vectorProduct=0;		//向量积
		float newMold=0;			//新向量的模
		float oldMold=0;			//旧向量的模
		for(int i=0;i<3;i++){
			vectorProduct += newPoints[i]*oldPoints[i];
			newMold += newPoints[i]*newPoints[i];
			oldMold += oldPoints[i]*oldPoints[i];
		}
		newMold = (float)Math.sqrt(newMold);
		oldMold = (float)Math.sqrt(oldMold);
		//计算夹角的余弦
		float cosineAngle=(float)(vectorProduct/(newMold*oldMold));
		//通过余弦值求角度
		float fangle = (float)Math.toDegrees(Math.acos(cosineAngle));
		angle = (int)fangle;
		return angle; //返回向量的夹角
	}
	
	//方法：向 Activity 更新步数
	public void updateData(){
		Intent intent = new Intent();	//创建 Intent 对象
		intent.setAction("com.example.doctorzhang.WlakingCountActivuty");
		intent.putExtra("step", steps);	//添加步数
		MySQLiteHelper mh = new MySQLiteHelper(this,MySQLiteHelper.DB_NAME,null,1);
		SQLiteDatabase db = mh.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.STEP, steps);
		db.update(MySQLiteHelper.TABLE_NAME_STEP, values, "time=?", new String[] { "0" });
		sendBroadcast(intent);			//发出广播
	}
	
	
	//方法：向数据库中插入今日走过的步数
	public void  uploadData(){
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
		
		MySQLiteHelper mh = new MySQLiteHelper(this,MySQLiteHelper.DB_NAME,null,1);
		SQLiteDatabase db = mh.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.TIME1, time);
		values.put(MySQLiteHelper.STEP, steps);
		db.insert(MySQLiteHelper.TABLE_NAME_STEP, null , values);
		steps = 0;				//步数清零
		db.close();				//关闭数据库
		mh.close();
	}
	
	/******************上传数据到服务端*******************/
	
	public void  uploadToOnline(){
		Share share = (Share)getApplicationContext();	//获取全局变量实例
		//获取最新的数据
		MySQLiteHelper mh = new MySQLiteHelper(this,MySQLiteHelper.DB_NAME,null,1);
		final SQLiteDatabase db = mh.getWritableDatabase();
		Cursor cursor = db.query(MySQLiteHelper.TABLE_NAME_STEP, new String[] { "time","step" }, null, null, null, null, "id desc" );
		cursor.moveToFirst();
		String login_id = share.getId();
		String measure_time = cursor.getString(cursor.getColumnIndex("time"));
		String data_string = cursor.getString(cursor.getColumnIndex("step"));
		ReleaseRrcordJSON(login_id ,"1" ,data_string ,measure_time);
	}
	
	
	/******************存储传感器数据*******************/
	public void save(float[] values)
	{
		
		try {
		/*************存储在手机本地***********
			FileOutputStream outStream=this.openFileOutput("GyroscopeData.txt",Context.MODE_APPEND);
			for(int i=0;i<3;i++){
				outStream.write((String.valueOf(values[i]) + "\t\t").getBytes());
			}
			outStream.write(("\n").getBytes());
			outStream.close();
		*************************************/
			
		/************存储在手机SD卡***********/
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File sdCardDir = Environment.getExternalStorageDirectory();//获取SDCard目录
			File saveFile = new File(sdCardDir,"GyroscopeData.txt");
			FileOutputStream outStream = new FileOutputStream(saveFile,true);
			for(int i=0;i<3;i++){
				outStream.write((String.valueOf(values[i]) + "\t\t").getBytes());
			}
			outStream.write(("\n").getBytes());
			outStream.close();
			
	    }
		/************************************/
			
		} catch (FileNotFoundException e) {
			return;
		}
		catch (IOException e){
			return ;
		}
	}
	
	/******************存储传感器数据*******************/
	public void saveToFFT(double[] values)
	{
		
		try {
		/************存储在手机SD卡***********/
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File sdCardDir = Environment.getExternalStorageDirectory();//获取SDCard目录
			File saveFile = new File(sdCardDir,"GyroscopeMoldFilter.txt");
			FileOutputStream outStream = new FileOutputStream(saveFile,true);
			for(int i=0;i<2*size;i++){
				outStream.write((String.format("%2.6f", values[i]) + "\t").getBytes());
			}
			outStream.write(("\n").getBytes());
			outStream.close();
			
	    }
		/************************************/
			
		} catch (FileNotFoundException e) {
			return;
		}
		catch (IOException e){
			return ;
		}
	}
	
	/******************存储传感器数据*******************/
	public void saveToData(double[] values)
	{
		
		try {
		/************存储在手机SD卡***********/
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File sdCardDir = Environment.getExternalStorageDirectory();//获取SDCard目录
			File saveFile = new File(sdCardDir,"GyroscopeMoldData.txt");
			FileOutputStream outStream = new FileOutputStream(saveFile,true);
			for(int i=0;i<2*size;i++){
				outStream.write((String.format("%2.6f", values[i]) + "\t").getBytes());
			}
			outStream.write(("\n").getBytes());
			outStream.close();
			
	    }
		/************************************/
			
		} catch (FileNotFoundException e) {
			return;
		}
		catch (IOException e){
			return ;
		}
	}
	
	/***********************发布运动记录的JSON/POST**********************/
  	
	public void  ReleaseRrcordJSON(final String login_id ,
									final String doctor_id ,
									final String data_string ,
									final String measure_time) {
  		
		new Thread() {
  			@Override
  			public void run() {	//启动线程
  			HttpPost request = new HttpPost(releaseRrcordUrl);
  			try {
  				/***************post JSON*******************
  				// 先封装一个 JSON 对象
  				JSONObject param = new JSONObject();
  				param.put("name",name);
  				param.put("birthday",birthday);
  				param.put("height",height);
  				param.put("weight",weight);
  				param.put("tel",tel);
  				// 绑定到请求 Entry
  				StringEntity se = new StringEntity(param.toString());
  				showJSON = param.toString();				//显示JSON
  				request.setEntity(se);
  				********************************************/
  				
  				/***************http post*******************/
  				List <NameValuePair> params=new ArrayList<NameValuePair>();
  				params.add(new BasicNameValuePair("login_id",login_id));
  				params.add(new BasicNameValuePair("doctor_id",doctor_id));
  				params.add(new BasicNameValuePair("data_string",data_string));
  				params.add(new BasicNameValuePair("measure_time",measure_time));
  				//发出HTTP request
  				request.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));
  				//取得HTTP response
  				HttpResponse httpResponse = new DefaultHttpClient().execute(request);
  				if(httpResponse.getStatusLine().getStatusCode()==200){  
  		            try {
  		            	//读取服务器返回过来的json字符串数据
  		            	strResult = EntityUtils.toString(httpResponse.getEntity());
  		            	judgeResult = parseRegJson(strResult);
  		            	if(judgeResult.equals("1")) {
  		            		Log.i(TAG, "Upload Success!");
  		            	} else if(judgeResult.equals("0")) {
  		            		Log.i(TAG, "Upload Fail !");
  		            	}
  		            	} catch (IllegalStateException e) {
  		            		e.printStackTrace();
  		            		Log.i(TAG, "IllegalStateException");
  		            	} catch (IOException e) {
  		            		e.printStackTrace();
  		            		Log.i(TAG, "IOException");
  		            	}
  		            }
  			} catch (UnsupportedEncodingException e2) {
  				e2.printStackTrace();
  				Log.i(TAG, "UnsupportedEncodingException");
  			} catch (ClientProtocolException e3) {
  				e3.printStackTrace();
  				Log.i(TAG, "ClientProtocolException");
  			} catch (IOException e4) {
  				e4.printStackTrace();
  				Log.i(TAG, "IOException");
  			}
  			isNotReady = false;
  			super.run();
  			}
  		}.start();
  	}
	
	/***********************发布运动记录的Json数据解析**********************/
  	
  	public String parseRegJson(String JSON) { 
  		try {
  			JSONObject jsonObject = new JSONObject(JSON);
  			parseResult = jsonObject.getString("judge").toString();
  		} catch (JSONException ex) {  
  		    // 异常处理代码  
  			Log.i(TAG, "Json parse error");
  		}
  		return parseResult;
  	}
  	
  	public double[] toMold(float gyroscopeTemp[][]) { 
  		gyroscopeMold = new double[2*size];			//向量的模
		for(int k=0; k<size; k++) {
			float addMold=0;
			for(int i=0;i<3;i++){
				addMold += gyroscopeTemp[k][i]*gyroscopeTemp[k][i];
			}
			gyroscopeMold[2*k]=(double) Math.sqrt(addMold);
			gyroscopeMold[2*k+1]=0.0;		//虚数部分都为0
		}
		return gyroscopeMold;
  	}
  	
  	public double[] toFilter(double filterData[]) {
  		int Wp = 120;
  		for(int i=Wp-1; i<size*2-Wp+1; i++) {
  			filterData[i] = 0;		//理想低通滤波器
  		}
		return filterData;
  	}
}
