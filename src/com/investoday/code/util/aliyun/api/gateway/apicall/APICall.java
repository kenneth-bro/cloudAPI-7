package com.investoday.code.util.aliyun.api.gateway.apicall;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.junit.Test;

import com.investoday.code.util.aliyun.api.gateway.Client;
import com.investoday.code.util.aliyun.api.gateway.Request;
import com.investoday.code.util.aliyun.api.gateway.constant.Constants;
import com.investoday.code.util.aliyun.api.gateway.constant.ContentType;
import com.investoday.code.util.aliyun.api.gateway.constant.HttpHeader;
import com.investoday.code.util.aliyun.api.gateway.constant.HttpSchema;
import com.investoday.code.util.aliyun.api.gateway.enums.Method;
import com.investoday.code.util.aliyun.api.gateway.util.MessageDigestUtil;

/**
 * API调用客户端
 * @Description: 请替换APP_KEY,APP_SECRET,HOST,CUSTOM_HEADERS_TO_SIGN_PREFIX为真实配置
 * @author 2016年4月28日 liq
 */
public class APICall {
	//APP KEY
    private static String APP_KEY = "";
    //APP密钥
    private static String APP_SECRET = "";
    //API域名  不含HTTP
    public static String HOST = "";
    //自定义参与签名Header前缀（可选,默认只有"X-Ca-"开头的参与到Header签名）
    private final static List<String> CUSTOM_HEADERS_TO_SIGN_PREFIX = new ArrayList<String>();
    
    //日志
    private static Logger logger = Logger.getLogger(APICall.class);
    
    static {
        CUSTOM_HEADERS_TO_SIGN_PREFIX.add("");
    }

    /**
     *  HTTP GET
     * @Medtod get
     * @author 2016年4月28日 liq
     * @param app_key 
     * @param app_secret
     * @param host 主机域名，不含HTTP
     * @param url	后缀地址
     * @param params 参数
     * @return
     * @throws Exception
     */
    @Test
    public static String get(String app_key, String app_secret, String host, String url, Map<String,String> params) throws Exception {
    	//设置参数
    	APP_KEY = app_key;
    	APP_SECRET = app_secret;
    	HOST = host;
    	params.put("api", "aliyun");
        Set<String> keys = params.keySet();
        Object[] keyStr = keys.toArray();
        for(int i=0; i<keyStr.length; i++){
        	String key = (String) keyStr[i];
        	String val = params.get(key);
        	if(i == 0){
        		url += "?" + key + "=" + val;
        	}else{
        		url += "&" + key + "=" + val;
        	}
        }
        
        Map<String, String> headers = new HashMap<String, String>();
        //（可选）响应内容序列化格式,默认application/json,目前仅支持application/json
        headers.put(HttpHeader.HTTP_HEADER_ACCEPT, "application/json");
        
        //转换为毫秒 - 4分钟
        long millionSeconds = System.currentTimeMillis();
        headers.put(Constants.CA_HEADER_TO_SIGN_PREFIX_SYSTEM + "Timestamp", millionSeconds + "");
        headers.put("X-Ca-Request-Mode", "debug");

        Request request = new Request(Method.GET, HttpSchema.HTTP + HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
        request.setHeaders(headers);
        request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);
        
        Map<String, String> headersMap = request.getHeaders();
//        System.out.println("--------------- Header ---------------");
//        Set<String> headersKeys = headersMap.keySet();
//        for (String string : headersKeys) {
//        	System.out.println(string + ":" + headersMap.get(string));
//		}
//        System.out.println("--------------- Header ---------------");

        //调用服务端
        HttpResponse response = Client.execute(request);
        int responseCode = response.getStatusLine().getStatusCode();
        Header requestIdHeader = response.getFirstHeader("X-Ca-Request-Id");
        if(responseCode != 200){
        	Header errorHeader = response.getFirstHeader("X-Ca-Error-Message");
        	String errorMsg = errorHeader.getValue();
        	String requestIdMsg = requestIdHeader.getValue();
        	logger.error("APICall [" + requestIdMsg + "/" + responseCode + "] Error:" + errorMsg);
        }

        //print(response);
        return getResult(response);
    }

    /**
     * HTTP POST 表单
     *
     * @throws Exception
     */
    //@Test
    public void postForm() throws Exception {
        //请求URL
        String url = "/demo/post/form";

        Map<String, String> bodyParam = new HashMap<String, String>();
        bodyParam.put("FormParamKey", "FormParamValue");

        Map<String, String> headers = new HashMap<String, String>();
        //（可选）响应内容序列化格式,默认application/json,目前仅支持application/json
        headers.put(HttpHeader.HTTP_HEADER_ACCEPT, "application/json");
        
        Request request = new Request(Method.POST_FORM, HttpSchema.HTTP + HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
        request.setHeaders(headers);
        request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);
        request.setFormBody(bodyParam);

        //调用服务端
        HttpResponse response = Client.execute(request);

        print(response);
    }

    /**
     * HTTP POST 字符串
     *
     * @throws Exception
     */
    //@Test
    public void postString() throws Exception {
        //请求URL
        String url = "/demo/post/string";
        //Body内容
        String body = "demo string body content";

        Map<String, String> headers = new HashMap<String, String>();
        //（可选）响应内容序列化格式,默认application/json,目前仅支持application/json
        headers.put(HttpHeader.HTTP_HEADER_ACCEPT, "application/json");
        //（可选）Body MD5,服务端会校验Body内容是否被篡改,建议Body非Form表单时添加此Header
        headers.put(HttpHeader.HTTP_HEADER_CONTENT_MD5, MessageDigestUtil.base64AndMD5(body));
        //（POST/PUT请求必选）请求Body内容格式
        headers.put(HttpHeader.HTTP_HEADER_CONTENT_TYPE, ContentType.CONTENT_TYPE_TEXT);

        Request request = new Request(Method.POST_STRING, HttpSchema.HTTP + HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
        request.setHeaders(headers);
        request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);
        request.setStringBody(body);

        //调用服务端
        HttpResponse response = Client.execute(request);

        print(response);
    }

    /**
     * HTTP POST 字节数组
     *
     * @throws Exception
     */
    //@Test
    public void postBytes() throws Exception {
        //请求URL
        String url = "/demo/post/bytes";
        //Body内容
        byte[] bytesBody = "demo bytes body content".getBytes(Constants.ENCODING);

        Map<String, String> headers = new HashMap<String, String>();
        //（可选）响应内容序列化格式,默认application/json,目前仅支持application/json
        headers.put(HttpHeader.HTTP_HEADER_ACCEPT, "application/json");
        //（可选）Body MD5,服务端会校验Body内容是否被篡改,建议Body非Form表单时添加此Header
        headers.put(HttpHeader.HTTP_HEADER_CONTENT_MD5, MessageDigestUtil.base64AndMD5(bytesBody));
        //（POST/PUT请求必选）请求Body内容格式
        headers.put(HttpHeader.HTTP_HEADER_CONTENT_TYPE, ContentType.CONTENT_TYPE_TEXT);

        Request request = new Request(Method.POST_BYTES, HttpSchema.HTTP + HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
        request.setHeaders(headers);
        request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);
        request.setBytesBody(bytesBody);

        //调用服务端
        HttpResponse response = Client.execute(request);

        print(response);
    }

    /**
     * HTTP PUT 表单
     *
     * @throws Exception
     */
    //@Test
    public void putForm() throws Exception {
        //请求URL
        String url = "/demo/put/form";

        Map<String, String> bodyParam = new HashMap<String, String>();
        bodyParam.put("FormParamKey", "FormParamValue");

        Map<String, String> headers = new HashMap<String, String>();
        //（可选）响应内容序列化格式,默认application/json,目前仅支持application/json
        headers.put(HttpHeader.HTTP_HEADER_ACCEPT, "application/json");

        Request request = new Request(Method.PUT_FORM, HttpSchema.HTTP + HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
        request.setHeaders(headers);
        request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);
        request.setFormBody(bodyParam);

        //调用服务端
        HttpResponse response = Client.execute(request);

        print(response);
        
        //提取结果
        getResult(response);
    }

    /**
     * HTTP PUT 字符串
     *
     * @throws Exception
     */
   // @Test
    public void putString() throws Exception {
        //请求URL
        String url = "/demo/put/string";
        //Body内容
        String body = "demo string body content";

        Map<String, String> headers = new HashMap<String, String>();
        //（可选）响应内容序列化格式,默认application/json,目前仅支持application/json
        headers.put(HttpHeader.HTTP_HEADER_ACCEPT, "application/json");
        //（可选）Body MD5,服务端会校验Body内容是否被篡改,建议Body非Form表单时添加此Header
        headers.put(HttpHeader.HTTP_HEADER_CONTENT_MD5, MessageDigestUtil.base64AndMD5(body));
        //（POST/PUT请求必选）请求Body内容格式
        headers.put(HttpHeader.HTTP_HEADER_CONTENT_TYPE, ContentType.CONTENT_TYPE_TEXT);

        Request request = new Request(Method.PUT_STRING, HttpSchema.HTTP + HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
        request.setHeaders(headers);
        request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);
        request.setStringBody(body);

        //调用服务端
        HttpResponse response = Client.execute(request);

        print(response);
    }

    /**
     * HTTP PUT 字节数组
     *
     * @throws Exception
     */
    //@Test
    public void putBytesBody() throws Exception {
        //请求URL
        String url = "/demo/put/bytes";
        //Body内容
        byte[] bytesBody = "demo bytes body content".getBytes(Constants.ENCODING);

        Map<String, String> headers = new HashMap<String, String>();
        //（可选）响应内容序列化格式,默认application/json,目前仅支持application/json
        headers.put(HttpHeader.HTTP_HEADER_ACCEPT, "application/json");
        //（可选）Body MD5,服务端会校验Body内容是否被篡改,建议Body非Form表单时添加此Header
        headers.put(HttpHeader.HTTP_HEADER_CONTENT_MD5, MessageDigestUtil.base64AndMD5(bytesBody));
        //（POST/PUT请求必选）请求Body内容格式
        headers.put(HttpHeader.HTTP_HEADER_CONTENT_TYPE, ContentType.CONTENT_TYPE_TEXT);

        Request request = new Request(Method.PUT_BYTES, HttpSchema.HTTP + HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
        request.setHeaders(headers);
        request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);
        request.setBytesBody(bytesBody);

        //调用服务端
        HttpResponse response = Client.execute(request);

        print(response);
    }

    /**
     * HTTP DELETE
     *
     * @throws Exception
     */
    //@Test
    public void delete() throws Exception {
        //请求URL
        String url = "/demo/delete";

        Map<String, String> headers = new HashMap<String, String>();
        //（可选）响应内容序列化格式,默认application/json,目前仅支持application/json
        headers.put(HttpHeader.HTTP_HEADER_ACCEPT, "application/json");

        Request request = new Request(Method.DELETE, HttpSchema.HTTP + HOST + url, APP_KEY, APP_SECRET, Constants.DEFAULT_TIMEOUT);
        request.setHeaders(headers);
        request.setSignHeaderPrefixList(CUSTOM_HEADERS_TO_SIGN_PREFIX);

        //调用服务端
        HttpResponse response = Client.execute(request);

        print(response);
    }

    /**
     * 打印Response
     *
     * @param response
     * @throws IOException
     */
    private void print(HttpResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(response.getStatusLine().getStatusCode()).append(Constants.LF);
        for (Header header : response.getAllHeaders()) {
            sb.append(header.toString()).append(Constants.LF);
        }
        sb.append(readStreamAsStr(response.getEntity().getContent()).trim()).append(Constants.LF);
        System.out.println(sb.toString());
    }
    
    /**
     * 提取结果
     * @Medtod getResult
     * @author 2016年4月27日 liq
     * @param response
     * @return
     * @throws IllegalStateException
     * @throws IOException
     */
    public static String getResult(HttpResponse response) throws IllegalStateException, IOException{
    	return readStreamAsStr(response.getEntity().getContent()).trim();
    }
    
    /**
     * 将流转换为字符串
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static String readStreamAsStr(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WritableByteChannel dest = Channels.newChannel(bos);
        ReadableByteChannel src = Channels.newChannel(is);
        ByteBuffer bb = ByteBuffer.allocate(4096);

        while (src.read(bb) != -1) {
            bb.flip();
            dest.write(bb);
            bb.clear();
        }
        src.close();
        dest.close();
        return new String(bos.toByteArray(), Constants.ENCODING);
    }
}
