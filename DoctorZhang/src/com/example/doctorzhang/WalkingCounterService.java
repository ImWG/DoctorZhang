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
	//����˵�ַ
	private String releaseRrcordUrl = "http://192.185.2.37/~penglu2/healthManage/Api.php?s=Index/activity_record";		//�����˶���¼url
	private String getRrcordUrl = "http://192.185.2.37/~penglu2/healthManage/Api.php?s=Index/activity_record_list";	//�û��˶���¼�б�url
	
	private IBinder binder=new WalkingCounterService.LocalBinder();
	private String strResult="8888";				//���ص�json���
	private String parseResult="8888";			//������ֵ����
	private String judgeResult="8888";			//����judge��ֵ
	float [] preCoordinate;
	double currentTime=0,lastTime=0;				//��¼ʱ��
	float WALKING_THRESHOLD = 20;
	public static int steps=0;					//��¼����
	boolean isActivityOn = false;					//Activity �Ƿ�����
	boolean isNotReady = true;						//�Ƿ��ѷ�������
	boolean isServiceOn = false;					//Service �Ƿ�����
	long timeInterval1 = 20*60*1000;				//20���Ӽ�¼һ��
	int size = 512;
	int sizeTemp = 0;								//��¼ջ��
	float [] gyroscope = {0,0,0};
	double[] gyroscopeMold;
	float [][] gyroscopeTemp;						//�ݴ�����ջ
	SensorManager mySensorManager;
	
	
	Handler handler1 = new Handler();
	
	Runnable runnable1 = new Runnable(){
	@Override
		public void run() {
		// TODO Auto-generated method stub
		Share share = (Share)getApplicationContext();	//��ȡȫ�ֱ���ʵ��
		uploadData();
		if(share.getIsLogin()) {
			uploadToOnline();
		}
		handler1.postDelayed(this, timeInterval1);		//�ٿ�����ʱtimeInterval
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
		//ע�������
		Sensor sensor = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mySensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
		
    }
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(TAG, "1onStart");
		super.onStartCommand(intent, 0 , startId);		//onStart������ʱ�����޸�
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "1onStartCommand");
		isServiceOn = true;
		//���ܽ��̵ĵ�����Ϣ
		if(isServiceOn){
			//������ʱ��
			handler1.postDelayed(runnable1, timeInterval1);	//ÿtimeInterval1ִ��һ��runnable1
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() { 
		Log.i(TAG, "1onDestroy");
		mySensorManager.unregisterListener(this);
		mySensorManager = null;
		//ֹͣ��ʱ��
		handler1.removeCallbacks(runnable1);
		
		super.onDestroy();
	}
	
	//����������̳�Binder
	public class LocalBinder extends Binder{
		//���ر��ط���
		WalkingCounterService getService(){
			return WalkingCounterService.this;
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	
	//�����������仯����ø÷���
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			// event.values float[]������x,y,z
			analyseData(event.values);//���÷�����������
			gyroscope = event.values;
			//save(event.values);
		} 
	}
	
	//�����������������м���
	public void analyseData(float[] values){
		//��ȡ��ǰʱ��
		currentTime=System.currentTimeMillis();
		//ÿ�� 200MS ȡ���ٶ�����ǰһ�����бȽ�
		if(currentTime - lastTime >200){
			if(preCoordinate == null){//��δ�������
				preCoordinate = new float[3];
				for(int i=0;i<3;i++){
					preCoordinate[i] = values[i];
				}
			} else{					//��¼��ԭʼ����Ļ����ͽ��бȽ�
				int angle = calculateAngle(values,preCoordinate);
				if(angle >= WALKING_THRESHOLD){
					steps++;		//��������
					updateData();	//���²���
				}
				for(int i=0;i<3;i++){
					preCoordinate[i]=values[i];
				}
			}
			lastTime = currentTime;	//���¼�ʱ
		}
	}
		
	//�����������������ٶ�ʸ���нǵķ���
	public int calculateAngle(float[] newPoints,float[] oldPoints){
		int angle=0;
		float vectorProduct=0;		//������
		float newMold=0;			//��������ģ
		float oldMold=0;			//��������ģ
		for(int i=0;i<3;i++){
			vectorProduct += newPoints[i]*oldPoints[i];
			newMold += newPoints[i]*newPoints[i];
			oldMold += oldPoints[i]*oldPoints[i];
		}
		newMold = (float)Math.sqrt(newMold);
		oldMold = (float)Math.sqrt(oldMold);
		//����нǵ�����
		float cosineAngle=(float)(vectorProduct/(newMold*oldMold));
		//ͨ������ֵ��Ƕ�
		float fangle = (float)Math.toDegrees(Math.acos(cosineAngle));
		angle = (int)fangle;
		return angle; //���������ļн�
	}
	
	//�������� Activity ���²���
	public void updateData(){
		Intent intent = new Intent();	//���� Intent ����
		intent.setAction("com.example.doctorzhang.WlakingCountActivuty");
		intent.putExtra("step", steps);	//��Ӳ���
		MySQLiteHelper mh = new MySQLiteHelper(this,MySQLiteHelper.DB_NAME,null,1);
		SQLiteDatabase db = mh.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.STEP, steps);
		db.update(MySQLiteHelper.TABLE_NAME_STEP, values, "time=?", new String[] { "0" });
		sendBroadcast(intent);			//�����㲥
	}
	
	
	//�����������ݿ��в�������߹��Ĳ���
	public void  uploadData(){
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
		
		MySQLiteHelper mh = new MySQLiteHelper(this,MySQLiteHelper.DB_NAME,null,1);
		SQLiteDatabase db = mh.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.TIME1, time);
		values.put(MySQLiteHelper.STEP, steps);
		db.insert(MySQLiteHelper.TABLE_NAME_STEP, null , values);
		steps = 0;				//��������
		db.close();				//�ر����ݿ�
		mh.close();
	}
	
	/******************�ϴ����ݵ������*******************/
	
	public void  uploadToOnline(){
		Share share = (Share)getApplicationContext();	//��ȡȫ�ֱ���ʵ��
		//��ȡ���µ�����
		MySQLiteHelper mh = new MySQLiteHelper(this,MySQLiteHelper.DB_NAME,null,1);
		final SQLiteDatabase db = mh.getWritableDatabase();
		Cursor cursor = db.query(MySQLiteHelper.TABLE_NAME_STEP, new String[] { "time","step" }, null, null, null, null, "id desc" );
		cursor.moveToFirst();
		String login_id = share.getId();
		String measure_time = cursor.getString(cursor.getColumnIndex("time"));
		String data_string = cursor.getString(cursor.getColumnIndex("step"));
		ReleaseRrcordJSON(login_id ,"1" ,data_string ,measure_time);
	}
	
	
	/******************�洢����������*******************/
	public void save(float[] values)
	{
		
		try {
		/*************�洢���ֻ�����***********
			FileOutputStream outStream=this.openFileOutput("GyroscopeData.txt",Context.MODE_APPEND);
			for(int i=0;i<3;i++){
				outStream.write((String.valueOf(values[i]) + "\t\t").getBytes());
			}
			outStream.write(("\n").getBytes());
			outStream.close();
		*************************************/
			
		/************�洢���ֻ�SD��***********/
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File sdCardDir = Environment.getExternalStorageDirectory();//��ȡSDCardĿ¼
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
	
	/******************�洢����������*******************/
	public void saveToFFT(double[] values)
	{
		
		try {
		/************�洢���ֻ�SD��***********/
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File sdCardDir = Environment.getExternalStorageDirectory();//��ȡSDCardĿ¼
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
	
	/******************�洢����������*******************/
	public void saveToData(double[] values)
	{
		
		try {
		/************�洢���ֻ�SD��***********/
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			File sdCardDir = Environment.getExternalStorageDirectory();//��ȡSDCardĿ¼
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
	
	/***********************�����˶���¼��JSON/POST**********************/
  	
	public void  ReleaseRrcordJSON(final String login_id ,
									final String doctor_id ,
									final String data_string ,
									final String measure_time) {
  		
		new Thread() {
  			@Override
  			public void run() {	//�����߳�
  			HttpPost request = new HttpPost(releaseRrcordUrl);
  			try {
  				/***************post JSON*******************
  				// �ȷ�װһ�� JSON ����
  				JSONObject param = new JSONObject();
  				param.put("name",name);
  				param.put("birthday",birthday);
  				param.put("height",height);
  				param.put("weight",weight);
  				param.put("tel",tel);
  				// �󶨵����� Entry
  				StringEntity se = new StringEntity(param.toString());
  				showJSON = param.toString();				//��ʾJSON
  				request.setEntity(se);
  				********************************************/
  				
  				/***************http post*******************/
  				List <NameValuePair> params=new ArrayList<NameValuePair>();
  				params.add(new BasicNameValuePair("login_id",login_id));
  				params.add(new BasicNameValuePair("doctor_id",doctor_id));
  				params.add(new BasicNameValuePair("data_string",data_string));
  				params.add(new BasicNameValuePair("measure_time",measure_time));
  				//����HTTP request
  				request.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));
  				//ȡ��HTTP response
  				HttpResponse httpResponse = new DefaultHttpClient().execute(request);
  				if(httpResponse.getStatusLine().getStatusCode()==200){  
  		            try {
  		            	//��ȡ���������ع�����json�ַ�������
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
	
	/***********************�����˶���¼��Json���ݽ���**********************/
  	
  	public String parseRegJson(String JSON) { 
  		try {
  			JSONObject jsonObject = new JSONObject(JSON);
  			parseResult = jsonObject.getString("judge").toString();
  		} catch (JSONException ex) {  
  		    // �쳣�������  
  			Log.i(TAG, "Json parse error");
  		}
  		return parseResult;
  	}
  	
  	public double[] toMold(float gyroscopeTemp[][]) { 
  		gyroscopeMold = new double[2*size];			//������ģ
		for(int k=0; k<size; k++) {
			float addMold=0;
			for(int i=0;i<3;i++){
				addMold += gyroscopeTemp[k][i]*gyroscopeTemp[k][i];
			}
			gyroscopeMold[2*k]=(double) Math.sqrt(addMold);
			gyroscopeMold[2*k+1]=0.0;		//�������ֶ�Ϊ0
		}
		return gyroscopeMold;
  	}
  	
  	public double[] toFilter(double filterData[]) {
  		int Wp = 120;
  		for(int i=Wp-1; i<size*2-Wp+1; i++) {
  			filterData[i] = 0;		//�����ͨ�˲���
  		}
		return filterData;
  	}
}
