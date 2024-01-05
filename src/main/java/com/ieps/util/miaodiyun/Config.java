package com.ieps.util.miaodiyun;

/**
 * 系统常量
 */
//TODO:这个包好像是验证码短信用的，在毕设中删除这个功能就行了，防止花别人的钱
public class Config {
	/**
	 * url前半部分
	 */
	public static final String BASE_URL = "https://api.miaodiyun.com/20150822";
//	public static final String BASE_URL = "https://api.miaodiyun.com";

	/**
	 * 开发者注册后系统自动生成的账号，可在官网登录后查看
	 */
	public static final String ACCOUNT_SID = "ab078f85fcf24cc6902f902f5dbfc1b7";

	/**
	 * 开发者注册后系统自动生成的TOKEN，可在官网登录后查看
	 */
	public static final String AUTH_TOKEN = "737a08444ddf4e968833b259a760f242";

	/**
	 * 响应数据类型, JSON或XML
	 */
	public static final String RESP_DATA_TYPE = "json";
}