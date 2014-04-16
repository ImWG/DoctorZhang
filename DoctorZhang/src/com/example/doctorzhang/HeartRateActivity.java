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
	private TextView tvHeartRate;				//����������
	private ListView lvRecordList;
	
	MySQLiteHelper mh;		//�������ݿ⸨����
	SQLiteDatabase db;		//���ݿ����
	SimpleAdapter listItemAdapter;
	SurfaceHolder surfaceHolder;
    Camera camera;
    Bitmap myBitmap;
    Visualizer visualizer;
	
    private int size = 32;							//��¼�������С
	private long timeInterval1 = 20;				//20ms��¼һ��
	//private long timeInterval2 = 1*1000;			//1000ms��¼һ��
	private double Pixel_R =0;							//��ԭɫ������ֵ
	private double pixelBuffer_R[];					//Pixel_R�ݴ��¼
	private double pixelBuffer_temp[];					//Pixel��ʱ�ݴ��¼
	private double baket[];
	private double baketTemp[];
	private double currentTime=0,lastTime=0;				//��¼ʱ��
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
		//��Ӳ�����ʾ
		lvRecordList.setAdapter(listItemAdapter);
		
		/****************���������ʵ�button**************/
		btnHeartRateON.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Toast.makeText(HeartRateActivity.this,"��ʼ����" , Toast.LENGTH_SHORT).show();
				turnLightOn(camera);
				mWakeLock.acquire();
				currentTime = System.currentTimeMillis();		//��¼ʱ��
				//������ʱ��
				handler1.postDelayed(runnable1, timeInterval1);	//ÿtimeInterval1ִ��һ��runnable1
				//handler2.postDelayed(runnable2, timeInterval2);	//ÿtimeInterval2ִ��һ��runnable2
			}
		});
		
		/****************�رղ����ʵ�button**************/
		btnHeartRateOFF.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Toast.makeText(HeartRateActivity.this,"ֹͣ����" , Toast.LENGTH_SHORT).show();
				turnLightOff(camera);
				mWakeLock.release();
				//ֹͣ��ʱ��
				handler1.removeCallbacks(runnable1);
				//handler2.removeCallbacks(runnable2);
			}
		});
	}
    
	protected void creatList() {
		
		Cursor cursor = db.query(MySQLiteHelper.TABLE_NAME_HEARTRATE, new String[]{}, 
				null , null, null, null, null);
		
		//���ɶ�̬���飬��������  
		ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
		
		while(cursor.moveToNext()) {
			HashMap<String, Object> map = new HashMap<String, Object>();		//���map
			map = new HashMap<String, Object>();
			map.put("time", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.TIME2)));
			map.put("rate", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.RATE)));
			listItem.add(map);
		}
		
		//������������Item�Ͷ�̬�����Ӧ��Ԫ��  
		listItemAdapter = new SimpleAdapter(this,listItem,	//����Դ   
				R.layout.heart_rate_list,						//ListItem��XMLʵ��  
				//��̬������ImageItem��Ӧ������          
				new String[] {"rate","time"},   
				//ImageItem��XML�ļ������һ��ImageView,����TextView ID  
				new int[] { R.id.heart_rate, R.id.time }
		);
	}

	@Override
	public void onPreviewFrame(byte[] arg0, Camera arg1) {
		if(captruePic) {		//Bitmap�����ſ�
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
		
		//Log.i("HeartRate", "data:"+myBitmap.getPixel(h/2, w/2));	//Bitmapȡ��
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
		//ע��camera
		if(camera != null) camera.release();
		camera = null;
	}
	
    /****************��FlashLight****************/
	
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
    
	/****************�ر�FlashLight****************/
	
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
    
    /****************��ʱ��1****************/
    
    Handler handler1 = new Handler();
	
	Runnable runnable1 = new Runnable(){
	@Override
		public void run() {
		/*
			currentTime = System.currentTimeMillis();		//��¼ʱ��
			simpleTime = currentTime - lastTime;
			lastTime = currentTime;							//���¼�ʱ
		*/
		
		/*
			//�ݴ�averageSimpleTime���� k=10
			for(int k=9; k>0; k--) {
				averageSimpleTime[k] = averageSimpleTime[k-1];
			}
			averageSimpleTime[0] = currentTime;
		*/	
			//��λ
			Pixel_R = 0;
			//int t = 0;
			//ͳ��pixel RGB
			for (int i=(int) (0.25*myBitmap.getHeight()); i<0.5*myBitmap.getHeight(); i++) {
				for (int j=(int) (0.25*myBitmap.getHeight()); j<0.5*myBitmap.getHeight(); j++) {
					Pixel_R += (int) ((myBitmap.getPixel(i, j) & 0x00ff0000) >> 16);	//ȡ����λ
				}
			}
			//�ݴ�pixel���� k=size
			for(int k=size-1; k>0; k--) {
				pixelBuffer_temp[k] = pixelBuffer_temp[k-1];
			}
			pixelBuffer_temp[0] = Pixel_R;
			captruePic = true;		//�������Bitmap

			if(pixelBuffer_temp[size-1] == 0) {				//Ԥ������ջ����

				Log.i("HeartRate", "GoOn");
				handler1.postDelayed(this, timeInterval1);		//�ٿ�����ʱtimeInterval1

			} else {
				lastTime = System.currentTimeMillis();		//��¼ʱ��
				pixelBuffer_R = pixelBuffer_temp;	//��ֵ
				
				//FFT�������
				FFT fft =new FFT();
				
				try {
					baketTemp = fft.myFFT(pixelBuffer_R);				//���fft
					for(int a=0; a<size; a++) {
						baket[a] = (int) Math.sqrt((Math.pow(baketTemp[2*a], 2)+Math.pow(baketTemp[2*a+1], 2)));	//���������
					}
					//baket[0]=0;										//ֱ��Ƶ����0
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				/*
				for (int u=0; u<size; u++) {
					//Log.i("HeartRate", "BAKET:" + baket[u]);			//����fft ��ʾ������
					baket[u] = (int) (30*Math.log10(baket[u]));			//����ת��
				}
				*/
				/**************find���ֵ***************/
				double max = baket[(int)(size/10)];
				int dex = (int)(size/10);
				for(int i=(int)(size/10);i<(int)(size/4);i++){					//ֻ�ҷ���������Χ���ֵ
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
				
				Toast.makeText(HeartRateActivity.this,"��ֹͣ����" , Toast.LENGTH_SHORT).show();
				turnLightOff(camera);
				//ֹͣ��ʱ��
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
