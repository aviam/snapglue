package com.nigealm.agent;

import com.nigealm.agent.svc.MongoDBAgentServiceImpl;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Gil on 26/04/2016.
 */
public class DummyHTTPServer extends AbstractHandler {
    private static final int PORT = 5555;

    Server server;

    public void startServer(int port) throws Exception {
        server = new Server(port);
        server.setHandler(this);
        server.start();
    }

    public void join() throws InterruptedException {
        server.join();
    }

    @Override
    public void handle(String s, HttpServletRequest request, HttpServletResponse response, int i) throws
            IOException, ServletException {
        Request base_request = (request instanceof Request) ? (Request) request : HttpConnection
                .getCurrentConnection()
                .getRequest();
        base_request.setHandled(true);
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("Done");
        doPost(base_request.getParameter("code"));
    }

    private void doPost(String code) {
        System.out.println(code);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            String url = "https://bitbucket.org/site/oauth2/access_token";
            HttpPost httpPost = new HttpPost(url);
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("client_id", "VZxR4pu5evmHpQC9pv"));
            nvps.add(new BasicNameValuePair("client_secret", "L7qAPjAQr4A5M2uDMGjuUe5zDE95RRmb"));
            nvps.add(new BasicNameValuePair("grant_type", "authorization_code"));
            nvps.add(new BasicNameValuePair("code", code));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
            HttpResponse response = httpClient.execute(httpPost);
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void runMultipleConfigurations() throws Exception {
        DummyHTTPServer server;
        try {
            server = new DummyHTTPServer();
            server.startServer(PORT);
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
