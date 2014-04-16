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
	private TextView tvCounter;				//步数计数文字
	private TextView tvMeter;					//距离计数文字
	private ListView lvRecordList;
	private ImageView iv;
	
	public static int activitySteps = 0 ;
	
	MySQLiteHelper mh;		//声明数据库辅助类
	SQLiteDatabase db;		//数据库对象
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
		//动态注册BroadcastReceiver
		registerReceiver(myReceiver, filter);
		
		creatList();
		//添加并且显示
		lvRecordList.setAdapter(listItemAdapter);
		
		/****************开启计步的button**************/
		btnWalkcounterON.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {	
				startCounterService();
				Toast.makeText(WlakingCountActivuty.this,"开始计步" , Toast.LENGTH_SHORT).show();
			}
		});
		
		/****************关闭计步的button**************/
		btnWalkcounterOFF.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Toast.makeText(WlakingCountActivuty.this,"停止计步" , Toast.LENGTH_SHORT).show();
				stopCounterService();
			}
		});
		
	}
	
	/***************************传感器Receiver*****************************/
	
	public class StepUpdateReceiver extends BroadcastReceiver{
		//定义一个继承自 BroadcastReceiver 的内部类 StepUpdateReceiver 来接受传感器的信息
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();					//获得 Bundle
			activitySteps = bundle.getInt("step");				//读取步数
			tvCounter.setText(String.valueOf(activitySteps)+"步");	//显示当前步数
			tvMeter.setText(String.valueOf(String.format("%4.1f",activitySteps*0.4)+"米"));		//显示行走距离
			//drawBar();				//刷新Bar
		}
	}
	
	protected void creatList() {
		
		Cursor cursor = db.query(MySQLiteHelper.TABLE_NAME_STEP, new String[]{}, 
				null , null, null, null, null);
		
		//生成动态数组，加入数据  
		ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
		
		while(cursor.moveToNext()) {
			HashMap<String, Object> map = new HashMap<String, Object>();		//添加map
			map = new HashMap<String, Object>();
			map.put("time", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.TIME1)));
			map.put("adress", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.ADRESS)));
			map.put("step", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.STEP)));
			listItem.add(map);
		}
		
		//生成适配器的Item和动态数组对应的元素  
		listItemAdapter = new SimpleAdapter(this,listItem,	//数据源   
				R.layout.record_list,						//ListItem的XML实现  
				//动态数组与ImageItem对应的子项          
				new String[] {"adress","time","step"},   
				//ImageItem的XML文件里面的一个ImageView,两个TextView ID  
				new int[] { R.id.adress, R.id.time, R.id.step }
		);
	}

	/***********************画图显示**********************/
	
	public void drawBar() {
		Bitmap newb = Bitmap.createBitmap( 700, 700, Config.ARGB_8888 );
		Canvas canvasTemp = new Canvas( newb );
		//canvasTemp.drawColor(Color.GRAY);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.GRAY);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		String familyName ="宋体";
		Typeface font = Typeface.create(familyName,Typeface.BOLD);
		paint.setTypeface(font);
		paint.setTextSize(20);
		
		MySQLiteHelper mh_step = new MySQLiteHelper(this,MySQLiteHelper.TABLE_NAME_STEP,null,1);
		final SQLiteDatabase db_step = mh_step.getWritableDatabase();
		Cursor cursor_step = db_step.query(MySQLiteHelper.TABLE_NAME_STEP, new String[] { "id","step" }, null, null, null, null, "id desc" );
		cursor_step.moveToFirst();
		
		int flagCounter = 0;
		boolean flagOn = true;
		// 将光标移动到下一行，从而判断该结果集是否还有下一条数据，如果有则返回true，没有则返回false  
		while (cursor_step.moveToNext() & flagOn) {
			
			if(flagCounter == 0) {
				cursor_step.moveToPrevious();
			}
			flagCounter = flagCounter+1 ;					//画Bar计数器
			if(flagCounter > 10) {
				flagOn = false;
				flagCounter = 0;
			}
			int perStep = cursor_step.getInt(cursor_step.getColumnIndex("step"));
			canvasTemp.drawRect(200, 100 + flagCounter*30 , 250 + perStep, 120 + flagCounter*30 , paint);	//画PerStepBar
			canvasTemp.drawText(cursor_step.getString(cursor_step.getColumnIndex("time")),0,115 + flagCounter*30,paint);
			paint.setColor(Color.BLACK);
			canvasTemp.drawText(cursor_step.getString(cursor_step.getColumnIndex("step")),200,115 + flagCounter*30,paint);
			paint.setColor(Color.GRAY);
		}
		canvasTemp.drawText("当前步数",0,85,paint);
		canvasTemp.drawRect(200, 70, 200 + activitySteps, 90 , paint);	//画CurrentStepBar
		paint.setColor(Color.BLACK);
		canvasTemp.drawText(String.valueOf(activitySteps),150,85,paint);
		paint.setColor(Color.GRAY);
		iv.setImageBitmap(newb);
		cursor_step.close();
		mh_step.close();
	}
	
	/****************启动CounterService****************/
	
	private void startCounterService(){
		Intent intent=new Intent(this,WalkingCounterService.class);
		startService(intent);
    }
    
    /****************停止CounterService****************/
	
    private void stopCounterService(){
         Intent intent=new Intent(this,WalkingCounterService.class);
         stopService(intent);
    }
    
}
