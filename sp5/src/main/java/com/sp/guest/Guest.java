package com.sp.guest;

public class Guest {
	private int num;
	private String userId;
	private String content;
	private String idAddr;
	private String userName;
	private String created;
	
	public int getNum() {
		return num;
	}
	public void setNum(int num) {
		this.num = num;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getCreated() {
		return created;
	}
	public void setCreated(String created) {
		this.created = created;
	}
	public String getIdAddr() {
		return idAddr;
	}
	public void setIdAddr(String idAddr) {
		this.idAddr = idAddr;
	}
}
