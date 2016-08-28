package xdi2.connector.cozy.contributor;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.connector.cozy.api.CozyApi;
import xdi2.connector.cozy.mapping.CozyMapping;
import xdi2.connector.cozy.util.GraphUtil;
import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIStatement;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.operations.GetOperation;
import xdi2.messaging.operations.SetOperation;
import xdi2.messaging.target.MessagingTarget;
import xdi2.messaging.target.Prototype;
import xdi2.messaging.target.contributor.ContributorMount;
import xdi2.messaging.target.contributor.ContributorResult;
import xdi2.messaging.target.contributor.impl.AbstractContributor;
import xdi2.messaging.target.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.execution.ExecutionContext;
import xdi2.messaging.target.execution.ExecutionResult;
import xdi2.messaging.target.impl.graph.GraphMessagingTarget;
import xdi2.messaging.target.interceptor.InterceptorResult;
import xdi2.messaging.target.interceptor.MessageEnvelopeInterceptor;

@ContributorMount(contributorXDIAddresses={"{}#cozy"})
public class CozyContributor extends AbstractContributor implements MessageEnvelopeInterceptor, Prototype<CozyContributor> {

	private static final Logger log = LoggerFactory.getLogger(CozyContributor.class);

	private CozyApi cozyApi;
	private CozyMapping cozyMapping;
	private Graph tokenGraph;

	public CozyContributor() {

		super();

		this.cozyApi = null;
		this.cozyMapping = null;
		this.tokenGraph = null;
	}

	/*
	 * Prototype
	 */

	@Override
	public CozyContributor instanceFor(PrototypingContext prototypingContext) throws Xdi2MessagingException {

		// create new contributor

		CozyContributor contributor = new CozyContributor();

		// set the graph

		contributor.setTokenGraph(this.getTokenGraph());

		// set api and mapping

		contributor.setCozyApi(this.getCozyApi());
		contributor.setCozyMapping(this.getCozyMapping());

		// done

		return contributor;
	}

	/*
	 * Init and shutdown
	 */

	@Override
	public void init(MessagingTarget messagingTarget) throws Exception {

		super.init(messagingTarget);

		if (this.getTokenGraph() == null && messagingTarget instanceof GraphMessagingTarget) this.setTokenGraph(((GraphMessagingTarget) messagingTarget).getGraph()); 
		if (this.getTokenGraph() == null) throw new Xdi2MessagingException("No token graph.", null, null);
	}

	/*
	 * Contributor
	 */

	@Override
	public ContributorResult executeSetOnLiteralStatement(XDIAddress[] contributorXDIAddresses, XDIAddress contributorsXDIAddress, XDIStatement relativeTargetStatement, SetOperation operation, Graph operationResultGraph, ExecutionContext executionContext) throws Xdi2MessagingException {

		XDIAddress cozyUserXDIAddress = contributorXDIAddresses[contributorXDIAddresses.length - 1];

		log.debug("cozyUserXDIAddress: " + cozyUserXDIAddress);

		if (cozyUserXDIAddress.equals("{}#cozy")) return ContributorResult.DEFAULT;

		// retrieve the Cozy user

		Map<XDIAddress, String> user = null;

		try {

			String cozyUrl = GraphUtil.retrieveCozyUrl(CozyContributor.this.getTokenGraph(), cozyUserXDIAddress);
			if (cozyUrl == null) {

				log.warn("No Cozy email for context: " + cozyUserXDIAddress);
				return new ContributorResult(true, false, true);
			}

			String cozyPassword = GraphUtil.retrieveCozyPassword(CozyContributor.this.getTokenGraph(), cozyUserXDIAddress);
			if (cozyPassword == null) {

				log.warn("No Cozy password for context: " + cozyUserXDIAddress);
				return new ContributorResult(true, false, true);
			}

			user = CozyContributor.this.retrieveUser(executionContext, cozyUrl, cozyPassword);
			if (user == null) throw new Exception("No user.");
		} catch (Exception ex) {

			throw new Xdi2MessagingException("Cannot load user data: " + ex.getMessage(), ex, executionContext);
		}

		// modify user

		try {

			String value = relativeTargetStatement.getLiteralData().toString();

			//user.get(relativeTargetStatement.getContextNodeXDIAddress()).addProperty("value", value);
			//CozyContributor.this.cozyApi.put(user);
		} catch (Exception ex) {

			throw new Xdi2MessagingException("Cannot save user data: " + ex.getMessage(), ex, executionContext);
		}

		// done

		return new ContributorResult(true, false, true);
	}

	@Override
	public ContributorResult executeGetOnAddress(XDIAddress[] contributorXDIAddresses, XDIAddress contributorsXDIAddress, XDIAddress relativeTargetAddress, GetOperation operation, Graph operationResultGraph, ExecutionContext executionContext) throws Xdi2MessagingException {

		XDIAddress cozyUserXDIAddress = contributorXDIAddresses[contributorXDIAddresses.length - 1];

		log.debug("GET cozyUserXDIAddress: " + cozyUserXDIAddress);

		if (cozyUserXDIAddress.equals("{}#cozy")) return ContributorResult.DEFAULT;

		// retrieve the Cozy user

		Map<XDIAddress, String> user = null;

		try {

			String cozyEmail = GraphUtil.retrieveCozyUrl(CozyContributor.this.getTokenGraph(), cozyUserXDIAddress);
			if (cozyEmail == null) {

				log.warn("No Cozy email for context: " + cozyUserXDIAddress);
				return new ContributorResult(true, false, true);
			}

			String cozyPassword = GraphUtil.retrieveCozyPassword(CozyContributor.this.getTokenGraph(), cozyUserXDIAddress);
			if (cozyPassword == null) {

				log.warn("No Cozy password for context: " + cozyUserXDIAddress);
				return new ContributorResult(true, false, true);
			}

			user = CozyContributor.this.retrieveUser(executionContext, cozyEmail, cozyPassword);
			if (user == null) throw new Exception("No user.");
		} catch (Exception ex) {

			throw new Xdi2MessagingException("Cannot load user data: " + ex.getMessage(), ex, executionContext);
		}

		// add the user to the response

		if (user != null) {

			ContextNode contextNode = operationResultGraph.setDeepContextNode(contributorsXDIAddress);

			for (Entry<XDIAddress, String> entry : user.entrySet()) {

				String value = entry.getValue();

				contextNode.setDeepContextNode(entry.getKey()).setLiteralString(value);
			}
		}

		// done

		return new ContributorResult(true, false, true);
	}

	/*
	 * MessageEnvelopeInterceptor
	 */

	@Override
	public InterceptorResult before(MessageEnvelope messageEnvelope, ExecutionContext executionContext, ExecutionResult executionResult) throws Xdi2MessagingException {

		CozyContributorExecutionContext.resetUsers(executionContext);

		return InterceptorResult.DEFAULT;
	}

	@Override
	public InterceptorResult after(MessageEnvelope messageEnvelope, ExecutionContext executionContext, ExecutionResult executionResult) throws Xdi2MessagingException {

		return InterceptorResult.DEFAULT;
	}

	@Override
	public void exception(MessageEnvelope messageEnvelope, ExecutionContext executionContext, ExecutionResult executionResult, Exception ex) {

	}

	/*
	 * Helper methods
	 */

	private Map<XDIAddress, String> retrieveUser(ExecutionContext executionContext, String url, String password) throws IOException, JSONException {

		Map<XDIAddress, String> user = CozyContributorExecutionContext.getUser(executionContext, url);

		if (user == null) {

			user = this.cozyApi.get(url, password);

			CozyContributorExecutionContext.putUser(executionContext, url, user);
		}

		return user;
	}

	/*
	 * Getters and setters
	 */

	public CozyApi getCozyApi() {

		return this.cozyApi;
	}

	public void setCozyApi(CozyApi cozyApi) {

		this.cozyApi = cozyApi;
	}

	public CozyMapping getCozyMapping() {

		return this.cozyMapping;
	}

	public void setCozyMapping(CozyMapping cozyMapping) {

		this.cozyMapping = cozyMapping;
	}

	public Graph getTokenGraph() {

		return this.tokenGraph;
	}

	public void setTokenGraph(Graph tokenGraph) {

		this.tokenGraph = tokenGraph;
	}
}
