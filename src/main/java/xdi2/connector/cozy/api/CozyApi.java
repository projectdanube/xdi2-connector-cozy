package xdi2.connector.cozy.api;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import xdi2.core.syntax.XDIAddress;

public class CozyApi {

	private static final Logger log = LoggerFactory.getLogger(CozyApi.class);

	private String token;
	private String meid;

	public CozyApi() {

	}

	public void init() {

	}

	public void destroy() {

	}

	synchronized public Map<XDIAddress, JsonObject> get(String email, String password) {

		log.debug("get()");

		try {

			login(email, password);
			meid();
			return me();
		} catch (Exception ex) {

			throw new RuntimeException("Cannot get: " + ex.getMessage(), ex);
		}
	}

	synchronized public void put(Map<XDIAddress, JsonObject> user) {

		log.debug("put()");

		try {

			HttpClient client = HttpClientBuilder.create().build();
			HttpPut put = new HttpPut("https://api.meeco.me/v2/story/story_items/" + meid + ".json");
			put.setHeader("Authorization", "Bearer " + token);

			JsonArray json3 = new JsonArray();
			for (Entry<XDIAddress, JsonObject> entry : user.entrySet()) {
				json3.add(entry.getValue());
			}
			JsonObject json2 = new JsonObject();
			json2.add("slots_attributes", json3);
			json2.addProperty("name", "man");
			json2.addProperty("label", "Markus");
			json2.addProperty("ordinal", "4");
			JsonObject json = new JsonObject();
			json.add("story_item", json2);
			put.setEntity(new StringEntity(json.toString()));
			//put.setEntity(new InputStreamEntity(MeecoApi.class.getResourceAsStream("test")));
			put.setHeader("Content-Type", "application/json; charset=UTF-8");
			log.debug("USER = " + json.toString());

			HttpResponse response = client.execute(put);
			if (response.getStatusLine().getStatusCode() != 200) throw new RuntimeException("Unexpected response for user: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
		} catch (Exception ex) {

			throw new RuntimeException("Cannot put: " + ex.getMessage(), ex);
		}
	}

	synchronized private void login(String email, String password) throws Exception {

		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("https://api.meeco.me/v2/session/login");

		List<NameValuePair> pairs = new ArrayList<NameValuePair> ();
		pairs.add(new BasicNameValuePair("grant_type", "password"));
		pairs.add(new BasicNameValuePair("email", email));
		pairs.add(new BasicNameValuePair("password", password));
		post.setEntity(new UrlEncodedFormEntity(pairs));

		HttpResponse response = client.execute(post);
		if (response.getStatusLine().getStatusCode() != 201) throw new RuntimeException("Unexpected response for user: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());

		this.token = response.getHeaders("Set-Cookie")[0].getValue().substring("token=".length(), response.getHeaders("Set-Cookie")[0].getValue().indexOf(';'));
		log.debug("TOKEN = " + token);
	}

	/*	private void user() throws Exception {

		HttpGet get = new HttpGet("https://api.meeco.me/v2/user");
		get.setHeader("Authorization", "Bearer " + token);

		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != 200) throw new RuntimeException("Unexpected response for user: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
		String body = EntityUtils.toString(response.getEntity(), "UTF-8");
	}*/

	synchronized private void meid() throws Exception {

		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet("https://api.meeco.me/v2/story/story_items.json");
		get.setHeader("Authorization", "Bearer " + token);

		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != 200) throw new RuntimeException("Unexpected response for user: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
		String body = EntityUtils.toString(response.getEntity(), "UTF-8");

		JsonObject json = new Gson().fromJson(body, JsonObject.class);
		JsonArray json2 = json.getAsJsonArray("story_items");
		for (JsonElement json3 : json2) {
			if (((JsonObject) json3).get("me").getAsBoolean() == true) {
				meid = ((JsonObject) json3).get("id").getAsString();
				break;
			}
		}
		log.debug("MEID = " + meid);
	}

	synchronized private Map<XDIAddress, JsonObject> me() throws Exception {

		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet("https://api.meeco.me/v2/story/story_items/" + meid + ".json");
		get.setHeader("Authorization", "Bearer " + token);

		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != 200) throw new RuntimeException("Unexpected response for user: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
		String body = EntityUtils.toString(response.getEntity(), "UTF-8");

		JsonObject json = new Gson().fromJson(body, JsonObject.class);
		Map<XDIAddress, JsonObject> me = new HashMap<XDIAddress, JsonObject> ();
		JsonArray json2 = json.getAsJsonArray("slots");
		for (JsonElement json3 : json2) {
			JsonElement name = ((JsonObject) json3).get("name");
			if (name == null || name instanceof JsonNull) continue;

			if (name.getAsString().equals("slots_attributes_city")) me.put(XDIAddress.create("#address<#locality>"), (JsonObject) json3);
			if (name.getAsString().equals("slots_attributes_country")) me.put(XDIAddress.create("#address<#country>"), (JsonObject) json3);
			if (name.getAsString().equals("slots_attributes_email")) me.put(XDIAddress.create("<#email>"), (JsonObject) json3);
			if (name.getAsString().equals("full_name")) {

				me.put(XDIAddress.create("<#first><#name>"), (JsonObject) json3);
				me.put(XDIAddress.create("<#last><#name>"), (JsonObject) json3);
			}
		}
		log.debug("ME = " + me);

		return me;
	}
}
