package com.klatunetworks.webhooks.core;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.klatunetworks.webhooks.core.store.WebhookStoreService;

/**
 * Contract for implementing WebhookService classes.
 * 
 * @see WebhookService
 * @see WebhookStoreService
 * 
 * @author Dan Simpson
 *
 */
public interface WebhookServiceContract {

	static final Logger log = LoggerFactory.getLogger(WebhookServiceContract.class);

	public void submit(Webhook webhook, Consumer<WebhookResult> callback);

	public default void submit(String url, String json, Consumer<WebhookResult> callback) {
		submit(new Webhook(url, json), callback);
	}

	public default void submit(Webhook webhook) {
		submit(webhook, (result) -> {
			log.info("Uncaptured: {}", result);
		});
	}

	public default void submit(String url, String json) {
		submit(new Webhook(url, json));
	}

}
