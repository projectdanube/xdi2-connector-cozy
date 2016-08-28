package xdi2.connector.cozy.contributor;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.google.gson.JsonObject;

import xdi2.core.syntax.XDIAddress;
import xdi2.messaging.target.execution.ExecutionContext;

/**
 * Methods for storing state related to the CozyContributor.
 */
public class CozyContributorExecutionContext {

	private static final String EXECUTIONCONTEXT_KEY_USERS_PER_MESSAGEENVELOPE = CozyContributor.class.getCanonicalName() + "#userspermessageenvelope";

	@SuppressWarnings("unchecked")
	public static Map<String, Map<XDIAddress, JsonObject>> getUsers(ExecutionContext executionContext) {

		return (Map<String, Map<XDIAddress, JsonObject>>) executionContext.getMessageEnvelopeAttribute(EXECUTIONCONTEXT_KEY_USERS_PER_MESSAGEENVELOPE);
	}

	public static Map<XDIAddress, JsonObject> getUser(ExecutionContext executionContext, String key) {

		return getUsers(executionContext).get(key);
	}

	public static void putUser(ExecutionContext executionContext, String key, Map<XDIAddress, JsonObject> value) {

		getUsers(executionContext).put(key, value);
	}

	public static void resetUsers(ExecutionContext executionContext) {

		executionContext.putMessageEnvelopeAttribute(EXECUTIONCONTEXT_KEY_USERS_PER_MESSAGEENVELOPE, new HashMap<String, JSONObject> ());
	}
}
