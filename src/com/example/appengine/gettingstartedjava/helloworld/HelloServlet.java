package com.example.appengine.gettingstartedjava.helloworld;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

@SuppressWarnings("serial")
@WebServlet(name = "helloworld", value = "/oidc" )
public class HelloServlet extends HttpServlet {
	
	String authorityBaseUrl = "http://localhost:5000";
	String clientId = "javaClient";
	String secret = "secret";
	String redirectUri = "http://localhost:8080/JavaClient/oidc";
	String responseType = "code";
	String scope = "openid+profile";
	String responseMode = "form_post";
	String grantType = "authorization_code";

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		String url = authorityBaseUrl + "/connect/authorize?"
				+ "client_id=" + clientId
				+ "&client_secret=" + secret
				+ "&redirect_uri=" + redirectUri 
				+ "&response_type=" + responseType
				+ "&scope=" + scope
				+ "&response_mode=" + responseMode;
		
		PrintWriter out = resp.getWriter();
		out.println("<html>");
		out.println("<body>");
		out.println("<a href='" + url + "'>Autenticar</a>");
		out.println("</body>");
		out.println("</html>");
	}
  
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String code = request.getParameter("code");
		
		System.out.println("### RECEIVED CODE");
		System.out.println(code);
		
		JSONObject jsonToken = getToken(code);
		
		String tokenType = jsonToken.getString("token_type");
		String tokenValue = jsonToken.getString("access_token");
		
		JSONObject jsonUserInfo = getUserInfo(tokenType, tokenValue);
		
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<body>");
		out.println("Logged user: " + jsonUserInfo.getString("sub") + " " + jsonUserInfo.getString("name"));
		out.println("</body>");
		out.println("</html>");
	}
	
	private JSONObject getToken(String code) throws IOException{
		
		URL url = new URL(authorityBaseUrl + "/connect/token");
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("grant_type", grantType);
        params.put("redirect_uri", redirectUri);
        params.put("client_id", clientId);
        params.put("client_secret", secret);
        params.put("code", code);

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String,Object> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");

        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);

        Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

        String returnString = "";
        
        for (int c; (c = in.read()) >= 0;) {
        	returnString += (char)c;
        }
        
        System.out.println("### ACCESS TOKEN");
        System.out.println(returnString);
        
        JSONObject json = new JSONObject(returnString);   
        return json;
	}
	
	private JSONObject getUserInfo(String tokenType, String tokenValue) throws IOException{
		
		URL url = new URL(authorityBaseUrl + "/connect/userinfo");
		URLConnection con = url.openConnection();
		con.setRequestProperty("Authorization", tokenType + " " + tokenValue);
		InputStream in = con.getInputStream();
		
		BufferedInputStream bis = new BufferedInputStream(in);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while(result != -1) {
		    buf.write((byte) result);
		    result = bis.read();
		}
		
		String responseString = buf.toString();
		
		System.out.println("### USER INFO");
		System.out.println(responseString);
		
        JSONObject json = new JSONObject(responseString);   
        return json;
	}
}
