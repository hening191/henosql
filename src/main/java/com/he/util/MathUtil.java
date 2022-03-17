package com.he.util;

import java.math.BigDecimal;

/**
 * 数学计算工具类
 * @author wangwei
 */
public class MathUtil {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	}
	
	public static double keep2decimal (double d){
		return new BigDecimal(d).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	public static double keep1decimal (double d){
		return new BigDecimal(d).setScale(1,BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 是否整数
	 */
	public static boolean isInt(String s) {
		try {
			Integer.valueOf(s);
			return true;
		}catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * 随机一个a-z、A-Z、0-9的字符
	 */
	public static String random_azAZ09(){
		String[] azAZ = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z",
					"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z",
					"0","1","2","3","4","5","6","7","8","9"};
		return azAZ[random(0,61)];
	}
	
	/**
	 * 随机一个固定长度的字符串,由阿拉伯数字和大小写字母组成
	 */
	public static String random_azAZ09(int length){
		String result = "";
		for(int n=0;n<length;n++)result += random_azAZ09();
		return result;
	}

	/**
	 * 随机一个a-z、0-9的字符
	 */
	public static String random_az09(){
		String[] azAZ = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z",
				"0","1","2","3","4","5","6","7","8","9"};
		return azAZ[random(0,35)];
	}

	/**
	 * 随机一个固定长度的字符串,由阿拉伯数字和小写字母组成
	 */
	public static String random_az09(int length){
		String result = "";
		for(int n=0;n<length;n++)result += random_az09();
		return result;
	}

	/**
	 * 随机一个固定长度的字符串,由阿拉伯数字组成
	 */
	public static String random_09(int length){
		String result = "";
		for(int n=0;n<length;n++)result += random(0,9);
		return result;
	}
	
	/**
	 * 获取[start,end]的随机数
	 * @param start 含
	 * @param end 含
	 * @return
	 */
	public static int random(int start,int end){
		return (int)((Math.random()*(end-start+1))+start);
	}

	/**
	 * 保留1位小数
	 * @param num
	 * @return
	 */
	public static String keepDeimalsToString(double num){
		// 保留几位小数
		double data = 3.07222;
		//利用字符串格式化的方式实现四舍五入,保留1位小数
		String result = String.format("%.1f",num);
		//1代表小数点后面的位数, 不足补0。 f代表数据是浮点类型。保留2位小数就是“%.2f”，依此累推。
		return result;
	}
}
