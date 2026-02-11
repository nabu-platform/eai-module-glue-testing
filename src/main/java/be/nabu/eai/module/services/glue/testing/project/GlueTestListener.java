package be.nabu.eai.module.services.glue.testing.project;

import be.nabu.eai.server.Server;
import be.nabu.eai.server.api.ServerListener;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.server.rest.RESTHandler;

public class GlueTestListener implements ServerListener {

	@Override
	public void listen(Server server, HTTPServer httpServer) {
		httpServer.getDispatcher().subscribe(HTTPRequest.class, new RESTHandler("/", GlueTestREST.class, null, server));
	}

}
