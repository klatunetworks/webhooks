package com.klatunetworks.webhooks.support;

import org.junit.After;
import org.junit.Before;

import com.klatunetworks.webhooks.core.Webhook;
import com.squareup.okhttp.mockwebserver.MockWebServer;

public class TestWithMockServer {

	protected MockWebServer server;

	@Before
	public void setUp() throws Exception {
		server = new MockWebServer();
		server.start();
	}

	@After
	public void tearDown() throws Exception {
		server.shutdown();
	}

	public String getServerUrl() {
		return server.getUrl("/test").toString();
	}

	public Webhook webhook() {
		return new Webhook(getServerUrl(), "['json']");
	}
}
