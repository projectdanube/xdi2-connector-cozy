package xdi2.connector.cozy.util;

import xdi2.core.Graph;
import xdi2.core.syntax.XDIAddress;

public class GraphUtil {

	private GraphUtil() { }

	public static String retrieveCozyUrl(Graph graph, XDIAddress cozyUserXDIAddress) {

/*		XDIAddress contextNodeXDIAddress = XDIAddress.create("" + CozyMapping.XDI_ADD_MEECO_CONTEXT + cozyUserXDIAddress + XDISecurityConstants.XDI_ADD_OAUTH_TOKEN);

		LiteralNode literalNode = graph.getDeepLiteralNode(contextNodeXDIAddress);

		return literalNode == null ? null : literalNode.getLiteralDataString();*/
		return "https://markus.cozycloud.cc";
	}

	public static String retrieveCozyPassword(Graph graph, XDIAddress cozyUserXDIAddress) {

	/*	XDIAddress contextNodeXDIAddress = XDIAddress.create("" + CozyMapping.XDI_ADD_MEECO_CONTEXT + cozyUserXDIAddress + XDISecurityConstants.XDI_ADD_OAUTH_TOKEN);

		LiteralNode literalNode = graph.getDeepLiteralNode(contextNodeXDIAddress);

		return literalNode == null ? null : literalNode.getLiteralDataString();*/
		return "354972f5";
	}
}
