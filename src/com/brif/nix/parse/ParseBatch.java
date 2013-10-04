package com.brif.nix.parse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The <b>ParseBatch</b> is a utility to aggregate more than one operation in
 * the Parse cloud.
 * 
 * <p>
 * The basic workflow for batch() is to push() operations.
 * 
 * @author Roy, 2013
 * 
 */
public class ParseBatch {

	enum Method {
		POST, PUT, DELETE, GET;
	}

	List<JSONObject> requests = new ArrayList<JSONObject>(2);

	public boolean push(Method method, String path, Map<String, Object> data) {
		JSONObject jo = new JSONObject();

		try {
			jo.put("method", method.name());
			jo.put("path", path);
			if (data != null && data.size() > 0)
				jo.put("body", transfromToJSONObject(data));

		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}

		requests.add(jo);

		return true;

	}

	protected boolean pushByClass(Method method, String className,
			Map<String, Object> data) {
		return push(method, Parse.getParseAPIShortUrlClass() + className, data);
	}

	protected boolean pushByClassObject(Method method, String className,
			String objectId, Map<String, Object> data) {
		return push(method, Parse.getParseAPIShortUrlClass() + className + "/"
				+ objectId, data);
	}

	public void delete(String className, String objectId) {
		pushByClassObject(Method.DELETE, className, objectId, null);
	}

	private static JSONObject transfromToJSONObject(Map<String, Object> data)
			throws JSONException {
		JSONObject jo = new JSONObject();
		for (Map.Entry<String, Object> o : data.entrySet()) {
			jo.put(o.getKey(), o.getValue());
		}
		return jo;
	}

	public void batch() throws ParseException, JSONException,
			ClientProtocolException, IOException {

		this.batch(null);
	}

	public void batch(String charset) throws ParseException, JSONException,
			ClientProtocolException, IOException {
		for (int i=0;i < requests.size(); i+=50) {
			postBatch(charset, i);
		}
	}

	private void postBatch(String charset, int i) throws JSONException,
			UnsupportedEncodingException, IOException, ClientProtocolException,
			ParseException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(Parse.getParseAPIUrlBatch());
		httppost.addHeader("X-Parse-Application-Id", Parse.getApplicationId());
		httppost.addHeader("X-Parse-REST-API-Key", Parse.getRestAPIKey());
		httppost.addHeader("Content-Type", "application/json");

		JSONObject jo = new JSONObject();
		jo.put("requests", requests.subList(i, Math.min(i+49, requests.size())));

		StringEntity stringEntity = null;
		if (charset != null) {
			stringEntity = new StringEntity(jo.toString(), charset);
		} else {
			stringEntity = new StringEntity(jo.toString());
		}

		httppost.setEntity(stringEntity);
		HttpResponse httpresponse = httpclient.execute(httppost);

		ParseResponse response = new ParseResponse(httpresponse);

		if (response.isFailed()) {
			throw response.getException();
		}
	}

}
