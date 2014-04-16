package com.example.doctorzhang;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class PeronalCenterActivity extends Activity {

	private Button btnWalkcounter;
	private Button btnHeartrate;
	private Button btnFallwanning;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_peronal_center);
		
		btnWalkcounter = (Button)findViewById(R.id.button_walkcount);
		btnHeartrate = (Button)findViewById(R.id.button_heartrate);
		btnFallwanning = (Button)findViewById(R.id.button_fallwanning);
		
		//�����Ʋ���Activity
		btnWalkcounter.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Intent intent = new Intent();		//����Activity
				intent.setClass(PeronalCenterActivity.this, WlakingCountActivuty.class);;
				startActivity(intent);
			}
		});
		
		//���������ʵ�Activity
		btnHeartrate.setOnClickListener(new OnClickListener() {		
			public void onClick(View v) {
				Intent intent = new Intent();		//����Activity
				intent.setClass(PeronalCenterActivity.this, HeartRateActivity.class);;
				startActivity(intent);
			}
		});
		
		//����������Activity
		btnFallwanning.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Intent intent = new Intent();		//����Activity
				intent.setClass(PeronalCenterActivity.this, FallingActivity.class);;
				startActivity(intent);
			}
		});
		
	}

}
