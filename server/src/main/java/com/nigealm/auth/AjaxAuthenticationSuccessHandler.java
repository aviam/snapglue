package com.nigealm.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import com.google.gson.Gson;
import com.nigealm.usermanagement.UserEntity;
import com.nigealm.usermanagement.UserManagementDao;
import com.nigealm.usermanagement.UserManagementServiceImpl;

public class AjaxAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler
{

	@Autowired
	private UserManagementDao userManagementDao;

	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth)
			throws IOException, ServletException
	{
		response.setHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));
        response.setContentType("application/json");
        String sessionID=request.getSession().getId().toString();
		
		JSONObject json1 = new JSONObject();
		JSONObject json2 = new JSONObject();
		JSONObject json3 = new JSONObject();
        String name=auth.getName();
    
		//UserEntity userEntity = userManagementDao.getUserByName(name);
		JSONObject userJsonObject=userManagementDao.getUserByName("username",name);
		
		//System.out.println(userEntity.getUsername());
		//Gson gson=new Gson();
		
        
		try {
		    json1.put("user", (userJsonObject == null) ? "" : userJsonObject.toString());
			json2.put("login", "SUCCESS");
			json3.put("SESSIONID",sessionID );
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
 
		//System.out.println("json2: " + json2);
 
		JSONObject mergedJSON1 = mergeJSONObjects(json2, json1);
		JSONObject mergedJSON2 = mergeJSONObjects(json3, mergedJSON1);

		//response.getWriter().print("{\"login\": \"SUCCESS\"}");
		response.getWriter().print(mergedJSON2);
		response.getWriter().flush();

	}
	
	
	
	public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) {
		JSONObject mergedJSON = new JSONObject();
		try {
			mergedJSON = new JSONObject(json1, JSONObject.getNames(json1));
			for (String crunchifyKey : JSONObject.getNames(json2)) {
				mergedJSON.put(crunchifyKey, json2.get(crunchifyKey));
			}
 
		} catch (JSONException e) {
			throw new RuntimeException("JSON Exception" + e);
		}
		return mergedJSON;
	}
}
