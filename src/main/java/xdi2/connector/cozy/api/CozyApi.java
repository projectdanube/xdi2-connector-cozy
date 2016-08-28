package xdi2.connector.cozy.api;


import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import xdi2.core.syntax.XDIAddress;

public class CozyApi {

	private static final Logger log = LoggerFactory.getLogger(CozyApi.class);

	private String token1;
	private String token2;
	private JsonObject meobject;

	public CozyApi() {

	}

	public void init() {

	}

	public void destroy() {

	}

	synchronized public Map<XDIAddress, String> get(String url, String password) {

		log.debug("get()");

		try {

			login(url, password);
			return me(url);
		} catch (Exception ex) {

			throw new RuntimeException("Cannot get: " + ex.getMessage(), ex);
		}
	}

	synchronized private void login(String url, String password) throws Exception {

		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url + "/login");

		post.setEntity(new StringEntity("{\"password\":\"354972f5\",\"authcode\":\"\"}"));

		HttpResponse response = client.execute(post);
		System.out.println(response.getStatusLine());

		this.token1 = response.getHeaders("Set-Cookie")[0].getValue();
		this.token2 = response.getHeaders("Set-Cookie")[1].getValue();
		log.debug("TOKEN1 = " + token1);
		log.debug("TOKEN2 = " + token2);
	}

	synchronized private Map<XDIAddress, String> me(String url) throws Exception {

		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(url + "/apps/contacts/contacts");
		get.setHeader("Cookie", token1 + "; " + token2);

		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != 200) throw new RuntimeException("Unexpected response for user: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
		String body = EntityUtils.toString(response.getEntity(), "UTF-8");

		JsonArray json = new Gson().fromJson(body, JsonArray.class);
		JsonObject json2 = (JsonObject) json.get(0);
		Map<XDIAddress, String> me = new HashMap<XDIAddress, String> ();
		String lastName = json2.get("n").getAsString(); lastName = lastName.substring(0, lastName.indexOf(";")); me.put(XDIAddress.create("<#last><#name>"), lastName);
		String firstName = json2.get("n").getAsString(); firstName = firstName.substring(lastName.length()+1); firstName = firstName.substring(0, firstName.indexOf(";")); me.put(XDIAddress.create("<#first><#name>"), firstName);
		meobject = json2;
		JsonArray json3 = json2.getAsJsonArray("datapoints");
		for (JsonElement json4 : json3) {
			JsonElement name = ((JsonObject) json4).get("name");
			if (name == null || name instanceof JsonNull) continue;

			if (name.getAsString().equals("adr")) {
				JsonArray json5 = ((JsonObject) json4).getAsJsonArray("value");
				String adr = json5.get(2).getAsString();
				me.put(XDIAddress.create("#address<#locality>"), adr.substring(0, adr.indexOf(", ")));
				me.put(XDIAddress.create("#address<#country>"), adr.substring(adr.indexOf(", ")+2));
			}
			if (name.getAsString().equals("email")) {
				me.put(XDIAddress.create("<#email>"), ((JsonObject) json4).get("value").getAsString());
			}
			if (name.getAsString().equals("tel")) {
				me.put(XDIAddress.create("<#tel>"), ((JsonObject) json4).get("value").getAsString());
			}
		}
		log.debug("ME = " + me);

		return me;
	}

	synchronized public void put(String url, Map<XDIAddress, String> user) throws Exception {

		log.debug("put()");

		meobject.addProperty("fn", user.get(XDIAddress.create("<#first><#name>")) + user.get(XDIAddress.create("<#last><#name>")));
		meobject.addProperty("n", user.get(XDIAddress.create("<#last><#name>")) + ";" + user.get(XDIAddress.create("<#first><#name>")) + ";;;");
		JsonArray json3 = meobject.getAsJsonArray("datapoints");
		for (JsonElement json4 : json3) {
			JsonElement name = ((JsonObject) json4).get("name");
			if (name == null || name instanceof JsonNull) continue;

			if (name.getAsString().equals("adr")) {
				JsonArray json5 = new JsonArray();
				json5.add(new JsonPrimitive(""));
				json5.add(new JsonPrimitive(""));
				json5.add(new JsonPrimitive(user.get(XDIAddress.create("#address<#locality>")) + ", " + user.get(XDIAddress.create("#address<#country>"))));
				json5.add(new JsonPrimitive(""));
				json5.add(new JsonPrimitive(""));
				json5.add(new JsonPrimitive(""));
				json5.add(new JsonPrimitive(""));
				((JsonObject) json4).add("value", json5);
			}
			if (name.getAsString().equals("email")) {
				((JsonObject) json4).add("value", new JsonPrimitive(user.get(XDIAddress.create("<#email>"))));
			}
			if (name.getAsString().equals("tel")) {
				((JsonObject) json4).add("value", new JsonPrimitive(user.get(XDIAddress.create("<#tel>"))));
			}
		}

		System.out.println(meobject.toString());
		System.out.println(url + "/apps/contacts/contacts/" + meobject.get("_id").getAsString());

		try {

			HttpClient client = HttpClientBuilder.create().build();
			HttpPut put = new HttpPut(url + "/apps/contacts/contacts/" + meobject.get("_id").getAsString());
			put.setHeader("Cookie", token1 + "; " + token2);
			put.setHeader("Content-Type", "application/json");
			put.setEntity(new StringEntity(meobject.toString()));

			HttpResponse response = client.execute(put);
			if (response.getStatusLine().getStatusCode() != 201) throw new RuntimeException("Unexpected response for user: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
		} catch (Exception ex) {

			throw new RuntimeException("Cannot put: " + ex.getMessage(), ex);
		}
	}
}
