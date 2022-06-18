package com.nigealm.auth;

import java.io.IOException;


import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

public class AjaxAuthenticationFailureHandler extends
SimpleUrlAuthenticationFailureHandler {
	
	public void onAuthenticationFailure(javax.servlet.http.HttpServletRequest request, 
	        javax.servlet.http.HttpServletResponse response, AuthenticationException exception)
	{

	    try {
	    	response.setContentType("application/json");
			//response.addHeader("Access-Control-Allow-Origin", "*");
	        response.getWriter().print(
	                "{\"login\": \"FAILURE\"}");
	        response.getWriter().flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	

}
