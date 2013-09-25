/**
 * 
 */
package com.brif.nix.notifications;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Roy, 2013
 */
public class SapiNotificationsHandler implements NotificationsHandler {

	private String endpoint;

	public SapiNotificationsHandler(String endpoint) {
		this.endpoint = endpoint.concat("/notifications/trigger");
	}

	@Override
	public void notifyMessagesEvent(String email, String eventType,
			Map<String, Object> data, String charset) {
		sendNotification(email, "messages", eventType, data, charset);
	}

	public boolean sendNotification(String email, String entity,
			String eventType, Map<String, Object> notificationAttributes,
			String charset) {
		try {
			sendPost(email, entity, eventType, notificationAttributes, charset);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void sendPost(String email, String entity, String eventType,
			Map<String, Object> notificationAttributes, String charset)
			throws ClientProtocolException, IOException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(this.endpoint);
		httppost.addHeader("Content-Type", "application/json");

		StringEntity stringEntity;
		JSONObject jsonObject = getResultJSON(email, entity, eventType,
				notificationAttributes);
		if (charset != null) {
			stringEntity = new StringEntity(jsonObject.toString(), charset);
		} else {
			stringEntity = new StringEntity(jsonObject.toString());
		}

		httppost.setEntity(stringEntity);
		HttpResponse httpresponse = httpclient.execute(httppost);

		NotifierResponse response = new NotifierResponse(httpresponse);
		if (!response.isFailed()) {
			JSONObject jsonResponse = response.getJsonObject();

			if (jsonResponse == null) {
				// TODO ?
				// throw response.getException();
			}
		}
	}

	private JSONObject getResultJSON(String email, String entity,
			String eventType,

			Map<String, Object> notificationAttributes) {
		final HashMap<String, Object> top = new HashMap<String, Object>(3);
		final JSONObject dataJSON = toJSONObject(notificationAttributes);
		top.put("email", email);
		top.put("entity", entity);
		top.put("type", eventType);
		top.put("data", dataJSON);
		JSONObject jsonObject = toJSONObject(top);
		return jsonObject;
	}

	private JSONObject toJSONObject(Map<String, Object> data) {
		JSONObject jo = new JSONObject();

		try {
			for (Entry<String, Object> entry : data.entrySet()) {
				jo.put(entry.getKey(), entry.getValue());
			}
		} catch (JSONException e) {
			// TODO exception?
		}
		return jo;
	}

	public static class NotifierResponse {

		private HttpResponse mHttpResponse;

		public NotifierResponse(HttpResponse mHttpResponse) {
			this.mHttpResponse = mHttpResponse;
		}

		public JSONObject getJsonObject() {
			try {
				return new JSONObject(EntityUtils.toString(mHttpResponse
						.getEntity()));
			} catch (org.apache.http.ParseException e) {
				return null;
			} catch (JSONException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		}

		public boolean isFailed() {
			return hasConnectionFailed() || hasErrorCode();
		}

		public boolean hasConnectionFailed() {
			return mHttpResponse.getEntity() == null;
		}

		public boolean hasErrorCode() {
			int statusCode = mHttpResponse.getStatusLine().getStatusCode();

			return (statusCode < 200 || statusCode >= 300);
		}
	}

}
