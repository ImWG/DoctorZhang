package com.example.doctorzhang;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteHelper extends SQLiteOpenHelper{
	
	public static final String DB_NAME = "dz_db";
	
	public static final String TABLE_NAME_LINK = "link_table";
	public static final String NAME = "name";
	public static final String TEL = "tel";
	
	public static final String TABLE_NAME_STEP = "step_table";
	public static final String TIME1 = "time";
	public static final String STEP = "step";
	public static final String ADRESS = "adress";
	
	public static final String TABLE_NAME_HEARTRATE = "heartrate_table";
	public static final String TIME2 = "time";
	public static final String RATE = "rate";
	
	public static final String TABLE_NAME_FALLING = "falling_table";
	public static final String TIME3 = "time";
	public static final String TYPE = "type";
	public static final String LINKMAN = "linkman";
	public static final String LINKTEL = "tel";
	
	public MySQLiteHelper(Context context, String name,CursorFactory factory,int version){//构造器
		super(context, name, factory, version);
	}
	
	public void onCreate(SQLiteDatabase db) {
		db.execSQL ("create table if not exists " 
					+ TABLE_NAME_STEP  + "("			//创建数据库表1
					+ TIME1 +" char primary key,"
					+ STEP +" integer,"
					+ ADRESS + " char)");
		ContentValues values1 = new ContentValues();		//添加数据
		values1.put(MySQLiteHelper.TIME1, 0);
		values1.put(MySQLiteHelper.STEP, 0);
		values1.put(MySQLiteHelper.ADRESS, 0);
		db.insert(MySQLiteHelper.TABLE_NAME_STEP,MySQLiteHelper.TIME1, values1);
		
		db.execSQL ("create table if not exists " 
				+ TABLE_NAME_HEARTRATE  + "("			//创建数据库表2
				+ TIME2 +" char primary key,"
				+ RATE + " integer)");
		ContentValues values2 = new ContentValues();		//添加数据
		values2.put(MySQLiteHelper.TIME2, 0);
		values2.put(MySQLiteHelper.RATE, 0);
		db.insert(MySQLiteHelper.TABLE_NAME_HEARTRATE,MySQLiteHelper.TIME2, values2);
		
		db.execSQL ("create table if not exists " 
				+ TABLE_NAME_FALLING  + "("			//创建数据库表3
				+ TIME3 +" char primary key,"
				+ TYPE +" integer,"
				+ LINKMAN +" char,"
				+ LINKTEL + " char)");
		ContentValues values3 = new ContentValues();		//添加数据
		values3.put(MySQLiteHelper.TIME3, 0);
		values3.put(MySQLiteHelper.TYPE, 0);
		values3.put(MySQLiteHelper.LINKMAN, 0);
		values3.put(MySQLiteHelper.LINKTEL, 0);
		db.insert(MySQLiteHelper.TABLE_NAME_FALLING,MySQLiteHelper.TIME3, values3);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}//对 onUpgrade 方法的重写
}
