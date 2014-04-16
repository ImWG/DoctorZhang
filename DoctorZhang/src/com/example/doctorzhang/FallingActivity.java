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

	private TextView tvFallingCount;				//��������ʾ
	private ListView lvRecordList;
	
	String fallingTime;
	long timeInterval1 = 1*60*1000;				//1���ӱ���
	
	MySQLiteHelper mh;		//�������ݿ⸨����
	SQLiteDatabase db;		//���ݿ����
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
		//��Ӳ�����ʾ
		lvRecordList.setAdapter(listItemAdapter);
		
	}
	
	/***************************������Receiver*****************************/
	
	public class StepUpdateReceiver extends BroadcastReceiver{
		//����һ���̳��� BroadcastReceiver ���ڲ��� StepUpdateReceiver �����ܴ���������Ϣ
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();					//��� Bundle
			fallingTime = bundle.getString("time");				//��ȡ����ʱ��
			handler1.postDelayed(runnable1, timeInterval1);		//����1���ӱ���
			infoDialog();
		}
	}

	/****************************������ʱ��*********************************/
	
	Handler handler1 = new Handler();
	
	Runnable runnable1 = new Runnable(){
	@Override
		public void run() {
		// TODO Auto-generated method stub
		
		}
	};
	
	/***********************��Ϣ�Ի���**********************/
	
	protected void infoDialog() {
		LayoutInflater inflater = LayoutInflater.from(this);
		View DialogView = inflater.inflate ( R.layout.sos_dialog, null);
		AlertDialog.Builder builder = new Builder(FallingActivity.this);
		builder	.setTitle("������")
				.setIcon(android.R.drawable.ic_dialog_info)
				.setView( DialogView);
		
		TextView tvSOSLinkman, tvSOSAdress;
		EditText etSOSContent;
		
		tvSOSLinkman = (TextView) DialogView.findViewById(R.id.textView_linkman);				//�ؼ�ע��
		tvSOSAdress = (TextView) DialogView.findViewById(R.id.textView_adress);
		etSOSContent = (EditText) DialogView.findViewById(R.id.editText_content);
		
		builder.setNeutralButton("����绰",  new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});

		builder.setNeutralButton("������Ϣ",  new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		
		builder.setNegativeButton("ȡ������", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		
		builder.show();
	}
	
	/****************************list����*********************************/
	
	protected void creatList() {
		
		Cursor cursor = db.query(MySQLiteHelper.TABLE_NAME_FALLING, new String[]{}, 
				null , null, null, null, null);
		
		//���ɶ�̬���飬��������  
		ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
		
		while(cursor.moveToNext()) {
			HashMap<String, Object> map = new HashMap<String, Object>();		//���map
			map = new HashMap<String, Object>();
			map.put("time", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.TIME3)));
			map.put("type", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.TYPE)));
			map.put("linkman", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.LINKMAN)));
			map.put("tel", cursor.getString(cursor.getColumnIndex(MySQLiteHelper.LINKTEL)));
			listItem.add(map);
		}
		
		//������������Item�Ͷ�̬�����Ӧ��Ԫ��  
		listItemAdapter = new SimpleAdapter(this,listItem,	//����Դ   
				R.layout.heart_rate_list,						//ListItem��XMLʵ��  
				//��̬������ImageItem��Ӧ������          
				new String[] {"time","type","linkman","tel"},   
				//ImageItem��XML�ļ������һ��ImageView,����TextView ID  
				new int[] { R.id.type, R.id.time, R.id.linkman , R.id.tel,}
		);
	}
}
