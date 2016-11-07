package com.investoday.test;

import java.util.HashMap;
import java.util.Map;

import com.investoday.code.util.aliyun.api.gateway.apicall.APICall;

public class APITest {
	public static void main(String[] args) {
		try{
			Map<String, String> param = new HashMap<String, String>();
			//必传参数
			param.put("api", "aliyun");
			//接口参数(更多参数请赋值给改Map)
			param.put("page", "stock_data");
			//凭证Key
			String appKey = "********";	
			//凭证secret
			String appSecret = "************";
			//调用地址(目前有华南--深圳、华东--杭州、华北--北京3个服务地址)
			/**
			 * 数据服务地址
			 * 华南(深圳) api.hn.investoday.net
			 * 华东(杭州) api.hd.investoday.net
			 * 华北(北京) api.hb.investoday.net
			 */
			String host = "api.hn.investoday.net";
			//固定不变
			String url = "/service";
			String dataJson = "";
			dataJson = APICall.get(appKey, appSecret, host, url, param);
			System.out.println("data:" + dataJson);
		}catch(Exception e){
			System.out.println("error");
		}
	}
}
