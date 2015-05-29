package com.klatunetworks.webhooks.core;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.klatunetworks.webhooks.core.store.WebhookMemoryStore;
import com.klatunetworks.webhooks.core.store.WebhookStoreService;
import com.klatunetworks.webhooks.support.TestWithMockServer;
import com.squareup.okhttp.mockwebserver.MockResponse;

public class WebhookStoreTest extends TestWithMockServer {

	@Test(timeout = 1000)
	public void testServiceWithMemoryStore() throws IOException, InterruptedException {
		Webhook webhook = webhook();

		WebhookMemoryStore store = new WebhookMemoryStore((n) -> n > 5 ? -1 : 1l);
		server.enqueue(new MockResponse().setResponseCode(503));
		server.enqueue(new MockResponse().setResponseCode(503));
		server.enqueue(new MockResponse().setResponseCode(503));
		server.enqueue(new MockResponse().setResponseCode(503));
		server.enqueue(new MockResponse().setResponseCode(200));

		WebhookStoreService service = new WebhookStoreService(new WebhookService(), store);
		service.submit(webhook);

		Assert.assertEquals(0, store.getQueue().size());
		Assert.assertEquals(1, store.getDelayedWebhooks());

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
		executor.scheduleAtFixedRate(store, 0, 1, TimeUnit.MILLISECONDS);
		executor.submit(service);
		
		for (int i = 0; i < 5; i++) {
			server.takeRequest();
		}

		executor.shutdown();
		
		Assert.assertEquals(0, store.getQueue().size());
		Assert.assertEquals(0, store.getDelayedWebhooks());
	}

}
