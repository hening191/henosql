package com.he.util;

import java.util.regex.Pattern;

/**
 * 文字处理工具
 * @author huaxiaoqiang
* @date 2016年6月15日 上午10:18:47
 */
public class StringUtil {
	
	/**
	 * 字符串脱敏，中间变成****，最小长度3位
	 */
	public static String desensitization(String str) {
		if(isEmpty(str))return "";
		if(str.length() < 3)return str;
		int startIndex = str.length()/3;
		String result = str.substring(0, startIndex);
		for(int n = 0 ; n < startIndex ; n++)result += "*";
		result += str.substring(startIndex*2, str.length());
		return result;
	}
	
	/**
	 * 报文脱毛
	 */
	public static String desensitizationBaoWen(String baowen,String... paras) {
		if(paras == null || paras.length == 0)return baowen;
		for(String para : paras) {
			baowen = desensitizationBaoWen(baowen,para);
		}
		return baowen;
	}
	
	/**
	 * 报文脱毛
	 */
	public static String desensitizationBaoWen(String baowen,String para) {
		try {
			if(StringUtil.isEmpty(baowen,para))return baowen;
			int index = baowen.indexOf(para);
			if(index == -1)return baowen;
			boolean isStart = false;
			String need2DesensitizationStr = "";
			for(int x = index ; x <=index + 30 ; x++) {
				char c =baowen.charAt(x);
				boolean isInt = MathUtil.isInt(c+"");
				if(isInt)isStart = true;
				if(isStart == false)continue;
				if(isInt == false)break;
				need2DesensitizationStr += c;
			}
			if(need2DesensitizationStr.length() < 5)return baowen;
			return baowen.replaceAll(need2DesensitizationStr, desensitization(need2DesensitizationStr));
		}catch (Exception e) {
			e.printStackTrace();
			return baowen;
		}
	}
	
	/**
	 * 判断是不是文件, .后面有文字
	 * @author huaxiaoqiang
	 * @param str 
	 * @return
	* @date 2016年5月17日 上午11:24:35
	 */
	public static boolean isFileName(String str){
		Pattern pattern=Pattern.compile("\\.\\w+", Pattern.CASE_INSENSITIVE);
		return pattern.matcher(str).find();
	}
	
	/**
	 * 正则校验
	 * @author huaxiaoqiang
	 * @param str
	 * @param reg
	 * @return
	* @date 2016年5月17日 下午4:26:15|2016年6月13日 下午2:47:15 修改
	 */
	public static boolean match(String str,String reg){
		Pattern pattern=Pattern.compile(reg);
		return pattern.matcher(str).matches();
	}
	
	/**
	 * 整数类型,最多10位
	 * @author huaxiaoqiang
	 * @param str
	 * @return
	* @date 2016年5月17日 下午5:49:47
	 */
	public static boolean isInteger(String str){
		Pattern pattern=Pattern.compile("\\d{1,10}", Pattern.CASE_INSENSITIVE);
		return pattern.matcher(str).matches();
	}
	
	/**
	 * 是否为空(任意一个为空)
	 * @author huaxiaoqiang
	 * @param strs
	 * @return
	* @date 2016年5月18日 上午9:44:06 修改
	 */
	public static boolean isEmpty(String...strs){
		for(String str : strs)if(str == null || str.equals(""))return true;
		return false; 
	}

	/**
	 * 删除掉最后一位的某个字符
	 * @param s
	 * @param c
	 * @return
	 */
	public static String removeLastChar(String s,char c){
		if(s == null)return "";
		if(s.endsWith(c+"")){
			return s.substring(0,s.length()-1);
		}
		return s;
	}
	
	/**
	 * 整数补0
	 */
	public static String addZero(int i,int length){
		String temp = i+"";
		if(temp.length()>length)return temp;
		int num = length - temp.length();
		for(int n = 0 ; n < num ; n++){
			temp = "0"+temp;
		}
		return temp;
	}
	
	/**
	 * 给字符串补充空格
	 */
	public static String addBlankSpace(String s,int length){
		if(s.length()>=length)return s;
		String result = s;
		for(int i = 0 ; i < length-s.length() ; i++)result += " ";
		return result;
	}

	/**
	 * 首字母转小写
	 */
	public static String toLowerCaseFirstWord(String s) {
		if(s == null)return "";
		if(Character.isLowerCase(s.charAt(0)))
			return s;
		else
			return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
	}

	/**
	 * 首字母转大写
	 */
	public static String toUpperCaseFirstWord(String s) {
		if(s == null)return "";
		if(Character.isUpperCase(s.charAt(0)))
			return s;
		else
			return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
	}

	/**
	 * 校验字符串是否符合表达式
	 * 表达式样例  abc*def*t*t*  其中*可以是n位任意字符
	 */
	public static boolean validateStringFitExpression(String str , String expression){
		if(isEmpty(str,expression))return false;
		if(!expression.contains("*"))return str.equals(expression);
		String[] exps = expression.split("\\*");
		boolean hasLeftStar = expression.startsWith("*");
		boolean hasRightStar = expression.endsWith("*");
		if(!hasLeftStar && !str.startsWith(exps[0]))return false;//处理最左的*
		if(!hasRightStar && !str.endsWith(exps[exps.length - 1]))return false;//处理最右的*
		int p = 0;
		for(int i = 0 ; i < exps.length ; i++){
			System.out.println("从第"+p+"位开始检查是否包含"+exps[i]);
			p = str.indexOf(exps[i],p);
			if(p == -1)return false;
			p += exps[i].length();
		}

		return true;
	}
	
	public static void main(String[] args){
		System.out.println(validateStringFitExpression("abcfactmiddle/ware/userinf/up2chain","*factmiddle/**ware**"));
		//System.out.println("abc/def/gggg/abc/aa".indexOf("abc",0));
	}
	
	
	
}
