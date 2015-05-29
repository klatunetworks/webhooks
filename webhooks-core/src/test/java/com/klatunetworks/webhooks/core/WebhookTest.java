package com.klatunetworks.webhooks.core;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.klatunetworks.webhooks.support.TestCallback;
import com.klatunetworks.webhooks.support.TestWithMockServer;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

public class WebhookTest extends TestWithMockServer {

	@Test
	public void testBuilder() throws IOException {
		Webhook hook = new Webhook(getServerUrl(), "{}");
		Request.Builder builder = hook.toBuilder();
		Assert.assertNotNull(builder);
		Request request = builder.build();
		Assert.assertNotNull(request);
		Assert.assertEquals(getServerUrl(), request.urlString());
		Assert.assertEquals(2l, request.body().contentLength());
		Assert.assertEquals(Webhook.JSON, request.body().contentType());
	}

	@Test(timeout = 1000)
	public void testBasicRequest() throws IOException, InterruptedException {
		server.enqueue(new MockResponse().setResponseCode(200));
		new WebhookService(new OkHttpClient()).submit(getServerUrl(), "['test']", (result) -> {
			Assert.assertEquals(200, result.getResponse().get().code());
		});
		RecordedRequest request = server.takeRequest();
		Assert.assertEquals("['test']", request.getBody().readUtf8());
	}

	@Test(timeout = 1000)
	public void testService() throws IOException {
		server.enqueue(new MockResponse().setResponseCode(200));

		Webhook webhook = new Webhook(getServerUrl(), "{}");
		TestCallback callback = new TestCallback();
		WebhookService service = new WebhookService(new OkHttpClient());

		service.submit(webhook, callback);

		WebhookResult result = callback.take();

		Assert.assertNotNull(result);
		Assert.assertTrue(result.hasResponse());
		Assert.assertTrue(result.isSuccessful());
		Assert.assertFalse(result.hasError());
	}

	@Test(timeout = 1000)
	public void testServiceWithFailure() throws IOException {
		server.enqueue(new MockResponse().setResponseCode(500));
		Webhook webhook = new Webhook(getServerUrl(), "{}");
		TestCallback callback = new TestCallback();
		WebhookService service = new WebhookService(new OkHttpClient());

		service.submit(webhook, callback);
		WebhookResult result = callback.take();
		Assert.assertFalse(result.isSuccessful());
	}

	@Test(timeout = 1000)
	public void testRequest() throws IOException, InterruptedException {
		server.enqueue(new MockResponse().setResponseCode(200));
		Webhook webhook = new Webhook(getServerUrl(), "[1,2,3]");
		webhook.setHeader("Test", "Test");

		WebhookService service = new WebhookService(new OkHttpClient());
		service.submit(webhook);
		RecordedRequest request = server.takeRequest();
		Assert.assertEquals(7l, request.getBodySize());
		Assert.assertEquals("Test", request.getHeader("Test"));
	}
}
