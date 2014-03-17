package org.chatminou.mpdcontrol.cover.provider;

import static android.text.TextUtils.isEmpty;
import android.net.http.AndroidHttpClient;
import android.util.Log;

import org.chatminou.mpdcontrol.MainApplication;
import org.chatminou.mpdcontrol.cover.CoverManager;
import org.chatminou.mpdcontrol.cover.ICoverRetriever;
import org.chatminou.mpdcontrol.helpers.CoverAsyncHelper;
import org.chatminou.mpdcontrol.tools.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class AbstractWebCover implements ICoverRetriever 
{

    private final String USER_AGENT = "MpdControl/0.0.0";
    private final static boolean DEBUG = CoverManager.DEBUG;

    protected void d(String a, String b) { if (DEBUG) Log.d(MainApplication.TAG, a + ": " + b); }
    protected void w(String a, String b) { Log.w(MainApplication.TAG, a + ": " + b); }
    protected void e(String a, String b) { Log.e(MainApplication.TAG, a + ": " + b); }

    protected String executeGetRequest(String _request) 
    {
    	HttpGet httpGet;
    	String request;
    	String response;

    	request = _request.replace(" ", "%20");
        if (DEBUG) 
        {
        	d(getName(), "Http request : " + request);
        }
        httpGet = new HttpGet(request);
        response = executeRequest(httpGet);

        if (request != null && !httpGet.isAborted()) 
        {
            httpGet.abort();
        }
        return response;
    }

    protected String executeGetRequestWithConnection(String request) 
    {

        URL url = CoverManager.buildURLForConnection(request);
        HttpURLConnection connection = CoverManager.getHttpConnection(url);
        BufferedReader br = null;
        String result = null;
        String line;

        if (!CoverManager.urlExists(connection)) 
        {
            return null;
        }

        /** TODO: After minSdkVersion="19" use try-with-resources here. */
        try 
        {
            InputStream inputStream = connection.getInputStream();
            br = new BufferedReader(new InputStreamReader(inputStream));
            line = br.readLine();
            do 
            {
                result += line;
                line = br.readLine();
            } while(line != null);
        } 
        catch (Exception ex) 
        {
            e(CoverAsyncHelper.class.getSimpleName() + ": Failed to execute cover get request.", ex.toString());
        } 
        finally 
        {
            if (connection != null) 
            {
                connection.disconnect();
            }
            if (br != null) 
            {
                try 
                {
                    br.close();
                } catch (IOException ex) 
                {
                    e("Failed to close the buffered reader.", ex.toString());
                }
            }
        }
        return result;
    }
    
    protected String executePostRequest(String url, String request) 
    {
        HttpPost httpPost = null;
        String result = null;

        try 
        {
            prepareRequest();
            httpPost = new HttpPost(url);
            if (DEBUG) d(getName(), "Http request : " + request);
            httpPost.setEntity(new StringEntity(request));
            result = executeRequest(httpPost);
        } 
        catch (UnsupportedEncodingException e) 
        {
            e(getName(), "Cannot build the HTTP POST : " + e);
            result = "";
        } 
        finally 
        {
            if (request != null && httpPost != null && !httpPost.isAborted()) 
            {
                httpPost.abort();
            }
        }
        return result;
    }
    
    protected String executeRequest(HttpRequestBase request) {

        AndroidHttpClient client = prepareRequest();
        StringBuilder builder = new StringBuilder();
        HttpResponse response;
        StatusLine statusLine;
        int statusCode;
        HttpEntity entity;
        InputStream content;
        BufferedReader reader;
        String line;

        try 
        {
            response = client.execute(request);
            statusLine = response.getStatusLine();
            statusCode = statusLine.getStatusCode();

            if(CoverManager.urlExists(statusCode)) 
            {
                entity = response.getEntity();
                content = entity.getContent();
                reader = new BufferedReader(new InputStreamReader(content));
                while ((line = reader.readLine()) != null) 
                {
                    builder.append(line);
                }
            } 
            else 
            {
                w(getName(), "Failed to download cover : HTTP status code : " + statusCode);
            }
        } 
        catch (Exception e) 
        {
            e(getName(), "Failed to download cover :" + e);
        } 
        finally 
        {
            if (client != null) 
            {
                client.close();
            }
        }
        if (DEBUG) d(getName(), "Http response : " + builder);
        return builder.toString();
    }

    public boolean isCoverLocal() {
        return false;
    }

    protected AndroidHttpClient prepareRequest() 
    {
        AndroidHttpClient client = AndroidHttpClient.newInstance(USER_AGENT);
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 5000);
        HttpConnectionParams.setSoTimeout(client.getParams(), 5000);
        return client;
    }
}
