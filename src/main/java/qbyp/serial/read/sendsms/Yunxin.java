package qbyp.serial.read.sendsms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import qbyp.serial.read.serialPort.SerialTool;
import qbyp.serial.read.util.PropertiesReader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class Yunxin {
	public static void main(String[] args) throws Exception {
		//System.out.println(sendCode("18902284520"));
//		JSONArray params=new JSONArray();
//		params.add("18902284520");
//		params.add("广州天河");
//		System.out.println(sendMsgCommon("18902284520", params));
//		
//		
		
		String jsonString="{\"ewType\":\"CIRCEL\",\"ringId\":\"SgCkBoadFX9FoUJJ25pnbp\","
				+ "\"tel\":\"18902284520\",\"message\":\"w谢莉红车主，您的二轮摩托车（女）:粤已离开设防区域，防盗器供电正常，在2018-01-19 23:17:09进入“蕉岭车站广场”，如有被盗情况，请及时报警！\","
				+ "\"params\":\"[\\\"谢莉红\\\",\\\"二轮摩托车（女）\\\",\\\"粤\\\",\\\"正常\\\",\\\"2018-01-19 23:17:09\\\",\\\"蕉岭车站广场\\\"]\"}";
		JSONObject json = JSONObject
				.parseObject(jsonString);
		String tel = json.getString("tel");
		String message = json.getString("message");
		String ewType = json.getString("ewType");
		String params_str = json.getString("params");
		JSONArray params = JSONArray
				.parseArray(params_str);
		String is_success = "success";
		System.out.println("tel:"+tel);
		System.out.println("params:"+params);
		System.out.println("ewType:"+ewType);
		is_success = Yunxin.sendMsg(tel, params,
					ewType);
	}

	private static Logger logger = Logger.getLogger(Yunxin.class);
	
	private static final String SERVER_SENDCODE_URL = "https://api.netease.im/sms/sendcode.action";// 发送验证码的请求路径URL
	
	private static final String APP_KEY = PropertiesReader.getProperty("APP_KEY");// 网易云信分配的账号
	private static final String APP_SECRET = PropertiesReader.getProperty("APP_SECRET");// 网易云信分配的密钥
	private static final String NONCE = PropertiesReader.getProperty("NONCE");// 随机数
	
//	private static final String APP_KEY = "09b561986eb58694fec86a430e288676";// 网易云信分配的账号
//	private static final String APP_SECRET = "f4a7566d6c0e";// 网易云信分配的密钥
//	private static final String NONCE = "123456";// 随机数
	
	private static final String SERVER_SENDTEMPLATE_URL = "https://api.netease.im/sms/sendtemplate.action";// 发送验证码的请求路径URL
	private static final String TEMPLATEID = PropertiesReader.getProperty("TEMPLATEID");//模板编号(由客户顾问配置之后告知开发者)
	

	public static String sendCode(String phone) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost post = new HttpPost(SERVER_SENDCODE_URL);

		String curTime = String.valueOf((new Date().getTime() / 1000L));
		String checkSum = CheckSumBuilder.getCheckSum(APP_SECRET, NONCE,
				curTime);

		// 设置请求的header
		post.addHeader("AppKey", APP_KEY);
		post.addHeader("Nonce", NONCE);
		post.addHeader("CurTime", curTime);
		post.addHeader("CheckSum", checkSum);
		post.addHeader("Content-Type",
				"application/x-www-form-urlencoded;charset=utf-8");

		// 设置请求参数
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("mobile", phone));

		post.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));

		// 执行请求
		HttpResponse response = httpclient.execute(post);
		String responseEntity = EntityUtils.toString(response.getEntity(),
				"utf-8");

		// 判断是否发送成功，发送成功返回true
		String code = JSON.parseObject(responseEntity).getString("code");
		if (code.equals("200")) {
			return "success";
		}
		return "error";
	}
	
	/**
	 * 发送通知类短信
	 * @param phone 手机号码
	 * @param params 短信参数列表，用于依次填充模板，JSONArray格式，每个变量长度不能超过30字，如["xxx","yyy"];对于不包含变量的模板，不填此参数表示模板即短信全文内容
	 * @param ewType 预警类型
	 * @return
	 * @throws IOException
	 */
    public static String sendMsg(String phone,JSONArray params,String ewType)  throws IOException{
    	try {
    		CloseableHttpClient httpclient = HttpClients.createDefault();
    		HttpPost post = new HttpPost(SERVER_SENDTEMPLATE_URL);

    		String curTime = String.valueOf((new Date().getTime() / 1000L));
    		String checkSum = CheckSumBuilder.getCheckSum(APP_SECRET, NONCE,
    				curTime);

    		// 设置请求的header
    		post.addHeader("AppKey", APP_KEY);
    		post.addHeader("Nonce", NONCE);
    		post.addHeader("CurTime", curTime);
    		post.addHeader("CheckSum", checkSum);
    		post.addHeader("Content-Type",
    				"application/x-www-form-urlencoded;charset=utf-8");

    		// 设置请求参数
    		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    		JSONArray phoneJson=new JSONArray();
    		phoneJson.add(phone);
    		
    		nameValuePairs.add(new BasicNameValuePair("mobiles", phoneJson.toJSONString()));
    		logger.info("ewType:"+ewType);
    		logger.info("PropertiesReader.getProperty(TEMPLATEID):"+PropertiesReader.getProperty("TEMPLATEID_"+ewType));
    		if(ewType==null || ewType.equals("")){
    			return "error";
    		}else{
    			nameValuePairs.add(new BasicNameValuePair("templateid", PropertiesReader.getProperty("TEMPLATEID_"+ewType)));
    		}
    		//nameValuePairs.add(new BasicNameValuePair("templateid", TEMPLATEID));
    		
    		nameValuePairs.add(new BasicNameValuePair("params", params.toJSONString()));
    		post.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));

    		// 执行请求
    		HttpResponse response = httpclient.execute(post);
    		String responseEntity = EntityUtils.toString(response.getEntity(),
    				"utf-8");

    		// 判断是否发送成功，发送成功返回true
    		String code = JSON.parseObject(responseEntity).getString("code");
    		logger.info("code:"+code);
    		if (code.equals("200")) {
    			return "success";
    		}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("sendMsg:", e);
		}
    	
		return "error";
    }
    
    /**
	 * 发送通知类短信
	 * @param phone 手机号码
	 * @param params 短信参数列表，用于依次填充模板，JSONArray格式，每个变量长度不能超过30字，如["xxx","yyy"];对于不包含变量的模板，不填此参数表示模板即短信全文内容
	 * @return
	 * @throws IOException
	 */
    public static String sendMsgCommon(String phone,JSONArray params)  throws IOException{
    	CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost post = new HttpPost(SERVER_SENDTEMPLATE_URL);

		String curTime = String.valueOf((new Date().getTime() / 1000L));
		String checkSum = CheckSumBuilder.getCheckSum(APP_SECRET, NONCE,
				curTime);

		// 设置请求的header
		post.addHeader("AppKey", APP_KEY);
		post.addHeader("Nonce", NONCE);
		post.addHeader("CurTime", curTime);
		post.addHeader("CheckSum", checkSum);
		post.addHeader("Content-Type",
				"application/x-www-form-urlencoded;charset=utf-8");

		// 设置请求参数
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		JSONArray phoneJson=new JSONArray();
		phoneJson.add(phone);
		
		nameValuePairs.add(new BasicNameValuePair("mobiles", phoneJson.toJSONString()));
		nameValuePairs.add(new BasicNameValuePair("templateid", TEMPLATEID));
		
		nameValuePairs.add(new BasicNameValuePair("params", params.toJSONString()));

		post.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));

		// 执行请求
		HttpResponse response = httpclient.execute(post);
		String responseEntity = EntityUtils.toString(response.getEntity(),
				"utf-8");

		// 判断是否发送成功，发送成功返回true
		String code = JSON.parseObject(responseEntity).getString("code");
		if (code.equals("200")) {
			return "success";
		}
		return "error";
    }
}
