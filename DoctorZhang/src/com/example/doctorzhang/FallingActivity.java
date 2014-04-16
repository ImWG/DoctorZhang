package com.example.doctorzhang;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FallingActivity extends Activity {

	private TextView tvFallingCount;				//跌倒数显示
	private ListView lvRecordList;
	
	String fallingTime;
	long timeInterval1 = 1*60*1000;				//1分钟报警
	
	MySQLiteHelper mh;		//声明数据库辅助类
	SQLiteDatabase db;		//数据库对象
	SimpleAdapter listItemAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_falling);
		
		tvFallingCount = (TextView) findViewById(R.id.textView_fall_count);
		lvRecordList = (ListView) this.findViewById(R.id.listView_record_list);
		
		mh = new MySQLiteHelper(this,MySQLiteHelper.DB_NAME,null,1);
		db = mh.getWritableDatabase();
		
		creatList();
		//添加并且显示
		lvRecordList.setAdapter(listItemAdapter);
		
	}
	
	/***************************传感器Receiver*****************************/
	
	public class StepUpdateReceiver extends BroadcastReceiver{
		//定义一个继承自 BroadcastReceiver 的内部类 StepUpdateReceiver 来接受传感器的信息
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();					//获得 Bundle
			fallingTime = bundle.getString("time");				//读取跌倒时间
			handler1.postDelayed(runnable1, timeInterval1);		//进行1分钟报警
			infoDialog();
		}
	}

	/****************************报警定时器*********************************/
	
	Handler handler1 = new Handler();
	
	Runnable runnable1 = new Runnable(){
	@Override
		public void run() {
		// TODO Auto-generated method stub
		
		}
	};
	
	/***********************信息对话框**********************/
	
	protected void infoDialog() {
		LayoutInflater inflater = LayoutInflater.from(this);
		View DialogView = inflater.inflate ( R.layout.sos_dialog, null);
		AlertDialog.Builder builder = new Builder(FallingActivity.this);
		builder	.setTitle("警报！")
				.setIcon(android.R.drawable.ic_dialog_info)
				.setView( DialogView);
		
		TextView tvSOSLinkman, tvSOSAdress;
		EditText etSOSContent;
		
		tvSOSLinkman = (TextView) DialogView.findViewById(R.id.textView_linkman);				//控件注册
		tvSOSAdress = (TextView) DialogView.findViewById(R.id.textView_adress);
		etSOSContent = (EditText) DialogView.findViewById(R.id.editText_content);
		
		builder.setNeutralButton("拨打电话",  new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});

		builder.setNeutralButton("发送信息",  new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		
		builder.setNegativeButton("取消警报", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		
		builder.show();
	}
	
	/****************************list生成*********************************/
	
	protected void creatList() {
		
		Cursor cursor = db.query(MySQLiteHelper.TABLE_NAME_FALLING, new String[]{}, 
				null , null, null, null, null);
		
		//生成动态数组，加入数据  
		ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
		
		while(cursor.moveToNext()) {
			HashMap<String, Object> map = new HashMap<String, Object>();		//添加map
			map = new HashMap<String, Object>();
			map.put("time", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.TIME3)));
			map.put("type", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.TYPE)));
			map.put("linkman", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.LINKMAN)));
			map.put("tel", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.LINKTEL)));
			listItem.add(map);
		}
		
		//生成适配器的Item和动态数组对应的元素  
		listItemAdapter = new SimpleAdapter(this,listItem,	//数据源   
				R.layout.heart_rate_list,						//ListItem的XML实现  
				//动态数组与ImageItem对应的子项          
				new String[] {"time","type","linkman","tel"},   
				//ImageItem的XML文件里面的一个ImageView,两个TextView ID  
				new int[] { R.id.type, R.id.time, R.id.linkman , R.id.tel,}
		);
	}
}
