package com.example.doctorzhang;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.heartrate.FFT;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class HeartRateActivity extends Activity implements SurfaceHolder.Callback,PreviewCallback {

	private Button btnHeartRateON;
	private Button btnHeartRateOFF;
	private TextView tvHeartRate;				//心率数文字
	private ListView lvRecordList;
	
	MySQLiteHelper mh;		//声明数据库辅助类
	SQLiteDatabase db;		//数据库对象
	SimpleAdapter listItemAdapter;
	SurfaceHolder surfaceHolder;
    Camera camera;
    Bitmap myBitmap;
    Visualizer visualizer;
	
    private int size = 32;							//记录的数组大小
	private long timeInterval1 = 20;				//20ms记录一次
	//private long timeInterval2 = 1*1000;			//1000ms记录一次
	private double Pixel_R =0;							//三原色的像素值
	private double pixelBuffer_R[];					//Pixel_R暂存记录
	private double pixelBuffer_temp[];					//Pixel临时暂存记录
	private double baket[];
	private double baketTemp[];
	private double currentTime=0,lastTime=0;				//记录时间
	private double simpleTime=0;
	private boolean captruePic = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_heart_rate);
		
		btnHeartRateON = (Button)findViewById(R.id.button_HeartRateON);
		btnHeartRateOFF = (Button)findViewById(R.id.button_HeartRateOFF);
		tvHeartRate = (TextView) findViewById(R.id.textView_Counter);
		lvRecordList = (ListView) this.findViewById(R.id.listView_record_list);
		SurfaceView view = (SurfaceView) findViewById(R.id.surface_view);
		
		view.getHolder().addCallback(this);
	    pixelBuffer_R = new double[size];
		pixelBuffer_temp = new double[size];
		baket = new double[size];
		baketTemp = new double[2*size];
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		final PowerManager.WakeLock mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		
		mh = new MySQLiteHelper(this,MySQLiteHelper.DB_NAME,null,1);
		db = mh.getWritableDatabase();
		
		creatList();
		//添加并且显示
		lvRecordList.setAdapter(listItemAdapter);
		
		/****************开启测心率的button**************/
		btnHeartRateON.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Toast.makeText(HeartRateActivity.this,"开始测量" , Toast.LENGTH_SHORT).show();
				turnLightOn(camera);
				mWakeLock.acquire();
				currentTime = System.currentTimeMillis();		//记录时间
				//启动定时器
				handler1.postDelayed(runnable1, timeInterval1);	//每timeInterval1执行一次runnable1
				//handler2.postDelayed(runnable2, timeInterval2);	//每timeInterval2执行一次runnable2
			}
		});
		
		/****************关闭测心率的button**************/
		btnHeartRateOFF.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Toast.makeText(HeartRateActivity.this,"停止测量" , Toast.LENGTH_SHORT).show();
				turnLightOff(camera);
				mWakeLock.release();
				//停止定时器
				handler1.removeCallbacks(runnable1);
				//handler2.removeCallbacks(runnable2);
			}
		});
	}
    
	protected void creatList() {
		
		Cursor cursor = db.query(MySQLiteHelper.TABLE_NAME_HEARTRATE, new String[]{}, 
				null , null, null, null, null);
		
		//生成动态数组，加入数据  
		ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
		
		while(cursor.moveToNext()) {
			HashMap<String, Object> map = new HashMap<String, Object>();		//添加map
			map = new HashMap<String, Object>();
			map.put("time", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.TIME2)));
			map.put("rate", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.RATE)));
			listItem.add(map);
		}
		
		//生成适配器的Item和动态数组对应的元素  
		listItemAdapter = new SimpleAdapter(this,listItem,	//数据源   
				R.layout.heart_rate_list,						//ListItem的XML实现  
				//动态数组与ImageItem对应的子项          
				new String[] {"rate","time"},   
				//ImageItem的XML文件里面的一个ImageView,两个TextView ID  
				new int[] { R.id.heart_rate, R.id.time }
		);
	}

	@Override
	public void onPreviewFrame(byte[] arg0, Camera arg1) {
		if(captruePic) {		//Bitmap保存门控
			Camera.Parameters parameters = arg1.getParameters();
			int imageFormat = parameters.getPreviewFormat();
			int w = parameters.getPreviewSize().width;
			int h = parameters.getPreviewSize().height;
			Rect rect=new Rect(0,0,w,h);
			YuvImage yuvImg = new YuvImage(arg0,imageFormat,w,h,null);
			try {
				ByteArrayOutputStream outputstream = new ByteArrayOutputStream();   
				yuvImg.compressToJpeg(rect, 100, outputstream);    
				myBitmap = BitmapFactory.decodeByteArray(outputstream.toByteArray(), 0, outputstream.size());
				camera.startPreview();
			}catch (Exception e) {
				Log.i("HeartRate", "ExceptionBitmap");
			}
			captruePic = false;
		}
		
		//Log.i("HeartRate", "data:"+myBitmap.getPixel(h/2, w/2));	//Bitmap取样
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
		/*
		try{  
			camera = Camera.open();  
			camera.setPreviewDisplay(holder);  
			Parameters params = camera.getParameters();  
			params.setPreviewSize(40, 30);
			camera.setParameters(params);  
			camera.startPreview() ;  
			camera.setPreviewCallback(this);  
		}catch(Exception e){  
			e.printStackTrace();  
		}
		*/
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try{  
			camera = Camera.open();  
			camera.setPreviewDisplay(holder);  
			Parameters params = camera.getParameters();  
			params.setPreviewSize(40, 30);
			camera.setParameters(params);  
			camera.startPreview() ;  
			camera.setPreviewCallback(this);  
		}catch(Exception e){  
			e.printStackTrace();  
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//注销camera
		if(camera != null) camera.release();
		camera = null;
	}
	
    /****************打开FlashLight****************/
	
	public static void turnLightOn(Camera mCamera) {
		if (mCamera == null) {
			return;
		}
		Parameters parameters = mCamera.getParameters();
		if (parameters == null) {
			return;
		}
		List<String> flashModes = parameters.getSupportedFlashModes();
		// Check if camera flash exists
		if (flashModes == null) {
			// Use the screen as a flashlight (next best thing)
			return;
		}
		String flashMode = parameters.getFlashMode();
		if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
			// Turn on the flash
			if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
				parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
				mCamera.setParameters(parameters);
			} else {
				Log.e("FlashLight", "FLASH_MODE_ON not supported");
			}
		}
	}
    
	/****************关闭FlashLight****************/
	
	public static void turnLightOff(Camera mCamera) {
		if (mCamera == null) {
			return;
		}
		Parameters parameters = mCamera.getParameters();
		if (parameters == null) {
			return;
		}
		List<String> flashModes = parameters.getSupportedFlashModes();
		String flashMode = parameters.getFlashMode();
		// Check if camera flash exists
		if (flashModes == null) {
			return;
		}
		if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
			// Turn off the flash
			if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
				parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
				mCamera.setParameters(parameters);
			} else {
				Log.e("FlashLight", "FLASH_MODE_OFF not supported");
			}
		}
	}
    
    /****************定时器1****************/
    
    Handler handler1 = new Handler();
	
	Runnable runnable1 = new Runnable(){
	@Override
		public void run() {
		/*
			currentTime = System.currentTimeMillis();		//记录时间
			simpleTime = currentTime - lastTime;
			lastTime = currentTime;							//重新计时
		*/
		
		/*
			//暂存averageSimpleTime数组 k=10
			for(int k=9; k>0; k--) {
				averageSimpleTime[k] = averageSimpleTime[k-1];
			}
			averageSimpleTime[0] = currentTime;
		*/	
			//复位
			Pixel_R = 0;
			//int t = 0;
			//统计pixel RGB
			for (int i=(int) (0.25*myBitmap.getHeight()); i<0.5*myBitmap.getHeight(); i++) {
				for (int j=(int) (0.25*myBitmap.getHeight()); j<0.5*myBitmap.getHeight(); j++) {
					Pixel_R += (int) ((myBitmap.getPixel(i, j) & 0x00ff0000) >> 16);	//取高两位
				}
			}
			//暂存pixel数组 k=size
			for(int k=size-1; k>0; k--) {
				pixelBuffer_temp[k] = pixelBuffer_temp[k-1];
			}
			pixelBuffer_temp[0] = Pixel_R;
			captruePic = true;		//允许更新Bitmap

			if(pixelBuffer_temp[size-1] == 0) {				//预存数组栈不满

				Log.i("HeartRate", "GoOn");
				handler1.postDelayed(this, timeInterval1);		//再开启定时timeInterval1

			} else {
				lastTime = System.currentTimeMillis();		//记录时间
				pixelBuffer_R = pixelBuffer_temp;	//赋值
				
				//FFT运算测试
				FFT fft =new FFT();
				
				try {
					baketTemp = fft.myFFT(pixelBuffer_R);				//输出fft
					for(int a=0; a<size; a++) {
						baket[a] = (int) Math.sqrt((Math.pow(baketTemp[2*a], 2)+Math.pow(baketTemp[2*a+1], 2)));	//输出幅度谱
					}
					//baket[0]=0;										//直流频率置0
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				/*
				for (int u=0; u<size; u++) {
					//Log.i("HeartRate", "BAKET:" + baket[u]);			//测试fft 显示幅度谱
					baket[u] = (int) (30*Math.log10(baket[u]));			//对数转换
				}
				*/
				/**************find最大值***************/
				double max = baket[(int)(size/10)];
				int dex = (int)(size/10);
				for(int i=(int)(size/10);i<(int)(size/4);i++){					//只找符合正常范围最大值
					if(baket[i] > max) {
						max=baket[i];
						dex=i;
					}
					if(i==(int)(size/10-1)) {
						//Log.i("HeartRate", "max:"+max+" ,dex:"+dex );
					}
				}
				simpleTime = lastTime - currentTime;
				tvHeartRate.setText(String.format("%3.2f", (60000*dex)/(simpleTime)));
				
				Toast.makeText(HeartRateActivity.this,"已停止测量" , Toast.LENGTH_SHORT).show();
				turnLightOff(camera);
				//停止定时器
				handler1.removeCallbacks(runnable1);
				camera.stopPreview();
				camera.release();
				camera = null;
			}
			
		}
	};

	@Override  
	protected void onDestroy() {  
		// TODO Auto-generated method stub  
		super.onDestroy(); 
		camera.stopPreview();
		camera.release();
		camera = null;
	}
}
