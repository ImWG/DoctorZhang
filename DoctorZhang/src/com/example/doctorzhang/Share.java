package com.example.doctorzhang;

import android.app.Application;

public class Share extends Application {
	private String name = "";					//��¼��½�û�����
	private String id = "";					//��¼��½�û�id
	private String level = "";					//��¼��½�û��ȼ�
	private boolean isLogin = false;			//�Ƿ��Ѿ���½
	private boolean isOnActivity = false;		//Activity�Ƿ���
	
	//ȫ�ֱ��� name
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	//ȫ�ֱ��� id
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	//ȫ�ֱ��� level
	public String getLevel() {
		return level;
	}
	
	public void setLevel(String level) {
		this.level = level;
	}
	
	//ȫ�ֱ��� isLogin
	public boolean getIsLogin() {
		return isLogin;
	}
		
	public void setIsLogin(boolean isLogin) {
		this.isLogin = isLogin;
	}
	
	//ȫ�ֱ��� isOnActivity
	public boolean getIsOnActivity() {
		return isOnActivity;
	}
	
	public void setIsOnActivity(boolean isOnActivity) {
		this.isOnActivity = isOnActivity;
	}
	
}
