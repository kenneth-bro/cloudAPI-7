package com.investoday.code.util.aliyun.api.gateway.backend;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.investoday.code.util.aliyun.api.gateway.constant.HttpMethod;

/**
 * 后端签名校验
 * @Description: TODO
 * @author 2016年4月27日 liq
 */
public class BackSignVerify {
	
	private static Logger logger = Logger.getLogger(BackSignVerify.class);
	
	/**
	 * API地址 真实服务器地址
	 */
	private static final String URI = "/Gold/data/data_provider.jsp";
	
	/**
	 * 请求方式
	 */
	private static final String HTTP_METHOD = HttpMethod.GET;
	
	/**
	 * 解析请求数据
	 * @Medtod analysisRequest
	 * @author 2016年4月27日 liq
	 * @param request 请求
	 * @param isBackVerify 是否后端签名校验
	 * @return
	 */
	public static boolean analysisRequest(HttpServletRequest request, boolean isBackVerify){
		if(!isBackVerify){
			return true;
		}
		//获取所有的Header
		Map<String, String> map = new HashMap<String, String>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while(headerNames.hasMoreElements()){
			String key = (String)headerNames.nextElement();
			String value = request.getHeader(key);
			map.put(key, value);
		}
		
		//获取参数
		Map<String, Object> paramMap = new HashMap<String, Object>();
		Enumeration<String> paraNames = request.getParameterNames();
		while(paraNames.hasMoreElements()){
			String name = paraNames.nextElement().toString();
			String value = request.getParameter(name); 
			if(!"sid".equalsIgnoreCase(name)){
				paramMap.put(name, value);
			}
		}
		//校验
		return signVerify(map, paramMap);
	}
	
	/**
	 * 对API网关传递
	 * @Medtod signVerify
	 * @author 2016年4月27日 liq
	 * @param headers Headers参数
	 * @param params  参数
	 * @return
	 */
	public static boolean signVerify(Map<String, String> headerMap, Map<String, Object> params){
		try {
			String uri = URI;
	        String httpMethod = HTTP_METHOD;
	        Map<String, String> headers = new HashMap<String, String>();
	        //为了忽略不同请求传递的header的大小写问题
	        Set<String> keys = headerMap.keySet();
	        for (String key : keys) {
				if("x-ca-proxy-signature".equalsIgnoreCase(key)){
					headers.put(Sign.CA_PROXY_SIGN, headerMap.get(key));
				}
				if("x-forwarded-for".equalsIgnoreCase(key)){
					headers.put("X-Forwarded-For", headerMap.get(key));
				}
				if("x-ca-proxy-signature-secret-key".equalsIgnoreCase(key)){
					headers.put(Sign.CA_PROXY_SIGN_SECRET_KEY, headerMap.get(key));
				}
				if("x-ca-proxy-signature-headers".equalsIgnoreCase(key)){
					headers.put(Sign.CA_PROXY_SIGN_HEADERS, headerMap.get(key));
				}
			}
	        
	        byte[] inputStreamBytes = new byte[]{};

	        String gatewaySign = headers.get(Sign.CA_PROXY_SIGN);

	        String serviceSign = Sign.serviceSign(uri, httpMethod, headers, params, inputStreamBytes);
	        
	        boolean isSame = gatewaySign.equals(serviceSign);
	        if(!isSame){
	        	logger.info("后端签名校验 签名不一致.Headers【" + headerMap + "】Param【" + params +"】");
	        }
	        return isSame;
	        
		} catch (Exception e) {
			logger.error("API网关：后端校验错误!", e);
			return false;
		}
	}
}
