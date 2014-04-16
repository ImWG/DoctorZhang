package com.example.doctorzhang;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class WlakingCountActivuty extends Activity {

	private Button btnWalkcounterON;
	private Button btnWalkcounterOFF;
	private TextView tvCounter;				//������������
	private TextView tvMeter;					//�����������
	private ListView lvRecordList;
	private ImageView iv;
	
	public static int activitySteps = 0 ;
	
	MySQLiteHelper mh;		//�������ݿ⸨����
	SQLiteDatabase db;		//���ݿ����
	SimpleAdapter listItemAdapter;
	StepUpdateReceiver myReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_walkingcounter);
		
		btnWalkcounterON = (Button)findViewById(R.id.button_WalkcounterON);
		btnWalkcounterOFF = (Button)findViewById(R.id.button_WalkcounterOFF);
		tvCounter = (TextView) findViewById(R.id.textView_Counter);
		tvMeter = (TextView) findViewById(R.id.textView_Meter);
		iv = (ImageView)findViewById(R.id.imageView_map);
		lvRecordList = (ListView) this.findViewById(R.id.listView_record_list);
		
		mh = new MySQLiteHelper(this,MySQLiteHelper.TABLE_NAME_STEP,null,1);
		db = mh.getWritableDatabase();
		
		myReceiver = new StepUpdateReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.example.doctorzhang.WlakingCountActivuty");
		//��̬ע��BroadcastReceiver
		registerReceiver(myReceiver, filter);
		
		creatList();
		//��Ӳ�����ʾ
		lvRecordList.setAdapter(listItemAdapter);
		
		/****************�����Ʋ���button**************/
		btnWalkcounterON.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {	
				startCounterService();
				Toast.makeText(WlakingCountActivuty.this,"��ʼ�Ʋ�" , Toast.LENGTH_SHORT).show();
			}
		});
		
		/****************�رռƲ���button**************/
		btnWalkcounterOFF.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Toast.makeText(WlakingCountActivuty.this,"ֹͣ�Ʋ�" , Toast.LENGTH_SHORT).show();
				stopCounterService();
			}
		});
		
	}
	
	/***************************������Receiver*****************************/
	
	public class StepUpdateReceiver extends BroadcastReceiver{
		//����һ���̳��� BroadcastReceiver ���ڲ��� StepUpdateReceiver �����ܴ���������Ϣ
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();					//��� Bundle
			activitySteps = bundle.getInt("step");				//��ȡ����
			tvCounter.setText(String.valueOf(activitySteps)+"��");	//��ʾ��ǰ����
			tvMeter.setText(String.valueOf(String.format("%4.1f",activitySteps*0.4)+"��"));		//��ʾ���߾���
			//drawBar();				//ˢ��Bar
		}
	}
	
	protected void creatList() {
		
		Cursor cursor = db.query(MySQLiteHelper.TABLE_NAME_STEP, new String[]{}, 
				null , null, null, null, null);
		
		//���ɶ�̬���飬��������  
		ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
		
		while(cursor.moveToNext()) {
			HashMap<String, Object> map = new HashMap<String, Object>();		//���map
			map = new HashMap<String, Object>();
			map.put("time", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.TIME1)));
			map.put("adress", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.ADRESS)));
			map.put("step", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.STEP)));
			listItem.add(map);
		}
		
		//������������Item�Ͷ�̬�����Ӧ��Ԫ��  
		listItemAdapter = new SimpleAdapter(this,listItem,	//����Դ   
				R.layout.record_list,						//ListItem��XMLʵ��  
				//��̬������ImageItem��Ӧ������          
				new String[] {"adress","time","step"},   
				//ImageItem��XML�ļ������һ��ImageView,����TextView ID  
				new int[] { R.id.adress, R.id.time, R.id.step }
		);
	}

	/***********************��ͼ��ʾ**********************/
	
	public void drawBar() {
		Bitmap newb = Bitmap.createBitmap( 700, 700, Config.ARGB_8888 );
		Canvas canvasTemp = new Canvas( newb );
		//canvasTemp.drawColor(Color.GRAY);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.GRAY);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		String familyName ="����";
		Typeface font = Typeface.create(familyName,Typeface.BOLD);
		paint.setTypeface(font);
		paint.setTextSize(20);
		
		MySQLiteHelper mh_step = new MySQLiteHelper(this,MySQLiteHelper.TABLE_NAME_STEP,null,1);
		final SQLiteDatabase db_step = mh_step.getWritableDatabase();
		Cursor cursor_step = db_step.query(MySQLiteHelper.TABLE_NAME_STEP, new String[] { "id","step" }, null, null, null, null, "id desc" );
		cursor_step.moveToFirst();
		
		int flagCounter = 0;
		boolean flagOn = true;
		// ������ƶ�����һ�У��Ӷ��жϸý�����Ƿ�����һ�����ݣ�������򷵻�true��û���򷵻�false  
		while (cursor_step.moveToNext() & flagOn) {
			
			if(flagCounter == 0) {
				cursor_step.moveToPrevious();
			}
			flagCounter = flagCounter+1 ;					//��Bar������
			if(flagCounter > 10) {
				flagOn = false;
				flagCounter = 0;
			}
			int perStep = cursor_step.getInt(cursor_step.getColumnIndex("step"));
			canvasTemp.drawRect(200, 100 + flagCounter*30 , 250 + perStep, 120 + flagCounter*30 , paint);	//��PerStepBar
			canvasTemp.drawText(cursor_step.getString(cursor_step.getColumnIndex("time")),0,115 + flagCounter*30,paint);
			paint.setColor(Color.BLACK);
			canvasTemp.drawText(cursor_step.getString(cursor_step.getColumnIndex("step")),200,115 + flagCounter*30,paint);
			paint.setColor(Color.GRAY);
		}
		canvasTemp.drawText("��ǰ����",0,85,paint);
		canvasTemp.drawRect(200, 70, 200 + activitySteps, 90 , paint);	//��CurrentStepBar
		paint.setColor(Color.BLACK);
		canvasTemp.drawText(String.valueOf(activitySteps),150,85,paint);
		paint.setColor(Color.GRAY);
		iv.setImageBitmap(newb);
		cursor_step.close();
		mh_step.close();
	}
	
	/****************����CounterService****************/
	
	private void startCounterService(){
		Intent intent=new Intent(this,WalkingCounterService.class);
		startService(intent);
    }
    
    /****************ֹͣCounterService****************/
	
    private void stopCounterService(){
         Intent intent=new Intent(this,WalkingCounterService.class);
         stopService(intent);
    }
    
}
