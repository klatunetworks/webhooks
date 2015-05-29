package com.klatunetworks.webhooks.support;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import com.klatunetworks.webhooks.core.WebhookResult;

public class TestCallback implements Consumer<WebhookResult> {

	public LinkedBlockingQueue<WebhookResult> queue = new LinkedBlockingQueue<WebhookResult>();

	@Override
	public void accept(WebhookResult result) {
		queue.add(result);
	}

	public WebhookResult take() {
		try {
			return queue.take();
		} catch (InterruptedException e) {
		}
		return null;
	}

	public int size() {
		return queue.size();
	}

}
