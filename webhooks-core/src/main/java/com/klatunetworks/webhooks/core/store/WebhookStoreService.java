package com.klatunetworks.webhooks.core.store;

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.klatunetworks.webhooks.core.Webhook;
import com.klatunetworks.webhooks.core.WebhookServiceContract;

/**
 * A Webhook service which leverages a store for managing the state of webhooks. The run method will block indefinitely, so an instance of
 * this class should be submitted to an executor service.
 * 
 * @author Dan Simpson
 *
 */
public class WebhookStoreService implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(WebhookStoreService.class);

	private final WebhookServiceContract service;
	private final WebhookStore store;

	public WebhookStoreService(WebhookServiceContract service, WebhookStore store) {
		this.service = service;
		this.store = store;
	}

	/**
	 * Submit a webhook for future callout
	 * 
	 * @param webhook
	 */
	public void submit(Webhook webhook) {
		store.add(webhook);
	}

	@Override
	public void run() {
		LinkedBlockingQueue<Webhook> queue = store.getQueue();
		while (true) {
			try {
				service.submit(queue.take(), result -> store.update(result));
			} catch (InterruptedException e) {
				log.warn("Thread interrupted.  Exiting.");
				return;
			}
		}

	}
}
