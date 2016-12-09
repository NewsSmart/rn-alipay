package com.alipay;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Map;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.util.Log;


import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.Promise;

import com.alipay.sdk.app.PayTask;

public class RNAlipayModule extends ReactContextBaseJavaModule {
	// 商户PID
	//public static final String PARTNER = "";
	// 商户收款账号
	//public static final String SELLER = "";
	// 商户私钥，pkcs8格式
	//public static final String RSA_PRIVATE = "";
	// 支付宝公钥
	//public static final String RSA_PUBLIC = "";
	//private static final int SDK_PAY_FLAG = 1;
	//private static final int SDK_CHECK_FLAG = 2;

	private final ReactApplicationContext mReactContext;

	public RNAlipayModule(ReactApplicationContext reactContext) {
		super(reactContext);
		mReactContext = reactContext;
  	}
  	
	@Override
  	public String getName() {
    	return "RNAlipay";
  	}

  	@ReactMethod
  	public void pay(ReadableMap options, Promise promise) {

        String privateKey = options.getString("privateKey");
        String app_id = options.getString("app_id");
        String seller_id = options.getString("seller_id");
        String outTradeNO = options.getString("outTradeNO");
        String subject = options.getString("subject");
        String body = options.getString("body");
        String notifyURL = options.getString("notifyURL");

        String total_amount;
        if (options.getType("total_amount") == ReadableType.Number) {
             total_amount = Double.toString(options.getDouble("total_amount"));
        } else {
             total_amount = options.getString("total_amount");
        }

        String timeout_express = options.getString("timeout_express");
        String showURL = options.getString("showURL");

		if (TextUtils.isEmpty(app_id) || TextUtils.isEmpty(privateKey) || TextUtils.isEmpty(seller_id)) {

		    promise.reject("需要配置APPID | RSA_PRIVATE| SELLERID");

			return;
		}
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
	String timestampTemp = df.format(new Date());
		String orderInfo = getOrderInfo(app_id, seller_id, outTradeNO, subject, body, total_amount, timeout_express, showURL, notifyURL,timestampTemp);

		/**
		 * 特别注意，这里的签名逻辑需要放在服务端，切勿将私钥泄露在代码中！
		 */
		String sign = sign(orderInfo, privateKey);
		String orderInfoTemp = "";

        try {
            /**
             * 仅需对sign 做URL编码
             */
            	sign = URLEncoder.encode(sign, "UTF-8");
		// 签约合作者身份ID
		orderInfoTemp = "app_id=" + URLEncoder.encode(app_id,"UTF-8");
		String biz_content = "{\"timeout_express\":\""+timeout_express+"\",\"seller_id\":\""+seller_id+"\",\"product_code\":\"QUICK_MSECURITY_PAY\","
					+"\"total_amount\":\""+total_amount+"\",\"subject\":\""+subject+"\",\"body\":\""+body+"\",\"out_trade_no\":\""
					+outTradeNO+"\"}";
		orderInfoTemp += ("&biz_content=" + URLEncoder.encode(biz_content,"UTF-8"));
		// 参数编码， 固定值
		orderInfoTemp += ("&charset="+URLEncoder.encode("utf-8","UTF-8"));
		// 参数编码， 固定值
		orderInfoTemp += ("&format="+URLEncoder.encode("json","UTF-8"));

		// 服务接口名称， 固定值
		orderInfoTemp += ("&method="+URLEncoder.encode("alipay.trade.app.pay","UTF-8"));

		// 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
		// orderInfo += "&paymethod=\"expressGateway\"";

		orderInfoTemp += ("&notify_url=" + URLEncoder.encode(notifyURL,"UTF-8"));

		orderInfoTemp += ("&sign_type=" + URLEncoder.encode("RSA","UTF-8"));
		
		orderInfoTemp += ("&timestamp=" + URLEncoder.encode(timestampTemp,"UTF-8"));
		orderInfoTemp += ("&version=" + URLEncoder.encode("1.0","UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        /**
         * 完整的符合支付宝参数规范的订单信息
         */
        final String payInfo = orderInfoTemp + "&sign=" + sign ;

        System.out.println(payInfo);

		PayTask alipay = new PayTask(getCurrentActivity());
		Map<String, String> result = alipay.payV2(payInfo, true);
		Log.i("msp", result.toString());
		//cb.invoke(result);
		//promise.resolve(result);
		@SuppressWarnings("unchecked")
		PayResult payResult = new PayResult((Map<String, String>) result);
		/**
		 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
		 */
		String resultInfo = payResult.getResult();// 同步返回需要验证的信息
		String resultStatus = payResult.getResultStatus();
		// 判断resultStatus 为9000则代表支付成功
		if (TextUtils.equals(resultStatus, "9000")) {
			// 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
			promise.resolve("支付成功");
		} else {
			// 该笔订单真实的支付结果，需要依赖服务端的异步通知。
			promise.resolve("支付失败");
			
		}
    }
  	/**
	 * create the order info. 创建订单信息
	 * 
	 */
	public String getOrderInfo(
	    String app_id,
	    String seller_id,
	    String outTradeNO,
	    String subject,
	    String body,
	    String total_amount,
	    String timeout_express,
	    String showURL,
	    String notifyURL,
	    String timestampTemp
	) {
		String orderInfo = "";
		
		// 签约合作者身份ID
		//orderInfo = "app_id=" + "\"" + app_id+ "\"";
		//String biz_content = "{\"timeout_express\":\""+timeout_express+"\",\"seller_id\":\""+seller_id+"\",\"product_code\":\"QUICK_MSECURITY_PAY\","
					//+"\"total_amount\":\""+total_amount+"\",\"subject\":\""+subject+"\",\"body\":\""+body+"\",\"out_trade_no\":\""
					//+outTradeNO+"\"}";
		//orderInfo += ("&biz_content="+"\"" + biz_content + "\"");
		// 参数编码， 固定值
		//orderInfo += ("&charset=\"utf-8\"");
		// 参数编码， 固定值
		//orderInfo += ("&format=\"json\"");

		// 服务接口名称， 固定值
		//orderInfo += ("&method=\"alipay.trade.app.pay\"");

		// 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
		// orderInfo += "&paymethod=\"expressGateway\"";

		//orderInfo += ("&notify_url=\"" + notifyURL+ "\"");

		//orderInfo += ("&sign_type=\"RSA\"");
		//SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		//orderInfo += ("&timestamp=\"" + df.format(new Date())+ "\"");
		//orderInfo += ("&version=\"1.0\"");
		
		
		
		// 签约合作者身份ID
		orderInfo = "app_id=" +  app_id;
		String biz_content = "{\"timeout_express\":\""+timeout_express+"\",\"seller_id\":\""+seller_id+"\",\"product_code\":\"QUICK_MSECURITY_PAY\","
					+"\"total_amount\":\""+total_amount+"\",\"subject\":\""+subject+"\",\"body\":\""+body+"\",\"out_trade_no\":\""
					+outTradeNO+"\"}";
		orderInfo += ("&biz_content="+ biz_content);
		// 参数编码， 固定值
		orderInfo += ("&charset=utf-8");
		// 参数编码， 固定值
		orderInfo += ("&format=json");

		// 服务接口名称， 固定值
		orderInfo += ("&method=alipay.trade.app.pay");

		// 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
		// orderInfo += "&paymethod=\"expressGateway\"";

		orderInfo += ("&notify_url=" + notifyURL);

		orderInfo += ("&sign_type=RSA");
		
		orderInfo += ("&timestamp=" + timestampTemp);
		orderInfo += ("&version=1.0");
		
		
		


		return orderInfo;
	}

	/**
	 * sign the order info. 对订单信息进行签名
	 * 
	 * @param content
	 *            待签名订单信息
	 */
	public String sign(String content, String rsaPrivate) {
		return SignUtils.sign(content, rsaPrivate);
	}
	/**
	 * get the sign type we use. 获取签名方式
	 * 
	 */
	public String getSignType() {
		return "sign_type=\"RSA\"";
	}
}
