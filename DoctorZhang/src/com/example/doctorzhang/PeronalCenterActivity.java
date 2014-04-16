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
		
		//开启计步的Activity
		btnWalkcounter.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Intent intent = new Intent();		//启动Activity
				intent.setClass(PeronalCenterActivity.this, WlakingCountActivuty.class);;
				startActivity(intent);
			}
		});
		
		//开启测心率的Activity
		btnHeartrate.setOnClickListener(new OnClickListener() {		
			public void onClick(View v) {
				Intent intent = new Intent();		//启动Activity
				intent.setClass(PeronalCenterActivity.this, HeartRateActivity.class);;
				startActivity(intent);
			}
		});
		
		//开启跌倒的Activity
		btnFallwanning.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				Intent intent = new Intent();		//启动Activity
				intent.setClass(PeronalCenterActivity.this, FallingActivity.class);;
				startActivity(intent);
			}
		});
		
	}

}
