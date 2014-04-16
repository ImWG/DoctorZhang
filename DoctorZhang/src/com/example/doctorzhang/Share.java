package com.example.doctorzhang;

import android.app.Application;

public class Share extends Application {
	private String name = "";					//记录登陆用户名字
	private String id = "";					//记录登陆用户id
	private String level = "";					//记录登陆用户等级
	private boolean isLogin = false;			//是否已经登陆
	private boolean isOnActivity = false;		//Activity是否开启
	
	//全局变量 name
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	//全局变量 id
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	//全局变量 level
	public String getLevel() {
		return level;
	}
	
	public void setLevel(String level) {
		this.level = level;
	}
	
	//全局变量 isLogin
	public boolean getIsLogin() {
		return isLogin;
	}
		
	public void setIsLogin(boolean isLogin) {
		this.isLogin = isLogin;
	}
	
	//全局变量 isOnActivity
	public boolean getIsOnActivity() {
		return isOnActivity;
	}
	
	public void setIsOnActivity(boolean isOnActivity) {
		this.isOnActivity = isOnActivity;
	}
	
}
