package com.investoday.code.util.aliyun.api.gateway.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.servlet.http.Cookie;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BestMatchSpecFactory;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import com.investoday.code.util.aliyun.api.gateway.apicall.APICall;

/**
 * HttpClient连接池
 * @Description: TODO
 * @author 2016年6月30日 liq
 */
public class HttpPool {
	
	private static Logger logger = Logger.getLogger(HttpPool.class);
	
	/**
	 * 全局连接
	 */
	private static CloseableHttpClient httpClientUse;
	
	/**
	 * 初始化域名
	 */
	private static final String HOST = "http://" + APICall.HOST;
	
	static{
		initPool();
	}
	
	//初始化配置
	private static void config(HttpRequestBase httpRequestBase, Map<String, String> headers){
		//设置请求头
		for (Map.Entry<String, String> e : headers.entrySet()) {
			 httpRequestBase.addHeader(e.getKey(), e.getValue());
	    }
	      
	      //配置请求超时设置
	      RequestConfig requestConfig = RequestConfig.custom()
	    		  .setConnectTimeout(30000)
	    		  .setConnectionRequestTimeout(30000)
	    		  .setSocketTimeout(30000)
	    		  .build();
	      httpRequestBase.setConfig(requestConfig);
	}
	
	//初始化连接池
	@SuppressWarnings("deprecation")
	public static void initPool(){
		ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
		LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
		//注册http请求和https请求
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", plainsf)
				.register("https", sslsf)
				.build();
		//连接池管理器
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		//最大连接数增加到200
		cm.setMaxTotal(20000);
		//每个路由基础的连接增加到20
		cm.setDefaultMaxPerRoute(2000);
		
		//将目标主机的最大连接数增加到50
		HttpHost localhost = new HttpHost(HOST,8080);
		cm.setMaxPerRoute(new HttpRoute(localhost), 50000);
		
		//请求重试处理
		HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
			@Override
			public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
				//如果已经重试了5次，则放弃
				if(executionCount >= 5){
					return false;
				}
				
				// 如果服务器丢掉了连接，那么就重试
				if(exception instanceof NoHttpResponseException){
					return true;
				}
				
				// 不要重试SSL握手异常
				if(exception instanceof SSLHandshakeException){
					return false;
				}
				
				// 超时  
				if (exception instanceof InterruptedIOException) {                 
                    return false;  
                }  
				
				// 目标服务器不可达     
                if (exception instanceof UnknownHostException) {              
                    return false;  
                }  
                
                // 连接被拒绝     
                if (exception instanceof ConnectTimeoutException) {              
                    return false;  
                }  
                
                // ssl握手异常              
                if (exception instanceof SSLException) {      
                    return false;  
                }  
                  
                HttpClientContext clientContext = HttpClientContext.adapt(context);  
                HttpRequest request = clientContext.getRequest();  
                
                // 如果请求是幂等的，就再次尝试  
                if (!(request instanceof HttpEntityEnclosingRequest)) {                   
                    return true;  
                }  
				return false;
			}
		};
		
		BasicCookieStore cookieStore = new BasicCookieStore(); 
		CookieSpecProvider easySpecProvider = new CookieSpecProvider() {
			public CookieSpec create(HttpContext context) {
				return new BrowserCompatSpec() {
					public void validate(Cookie cookie, CookieOrigin origin)
							throws MalformedCookieException {
						// Oh, I am easy
					}
				};
			}

		};
		Registry<CookieSpecProvider> r = RegistryBuilder  
				.<CookieSpecProvider> create()  
				.register(CookieSpecs.BEST_MATCH, new BestMatchSpecFactory())  
				.register(CookieSpecs.BROWSER_COMPATIBILITY,  
				new BrowserCompatSpecFactory())  
				.register("easy", easySpecProvider).build(); 
		
		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(cm)
				.setRetryHandler(httpRequestRetryHandler)
				.setDefaultCookieStore(cookieStore)
				.setDefaultCookieSpecRegistry(r)
				.build();
		
			
		//将注册对象赋值给全局
		httpClientUse = httpClient;
		
		
		logger.info("域名[" + HOST +"]连接池初始化 Success.");
	}
	
	
	public static HttpResponse getResponse(String url, Map<String, String> headers){
        try {
        	HttpGet httpget = new HttpGet(url);  
            config(httpget, headers);
			CloseableHttpResponse response = httpClientUse.execute(httpget, HttpClientContext.create());
			return response;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}

