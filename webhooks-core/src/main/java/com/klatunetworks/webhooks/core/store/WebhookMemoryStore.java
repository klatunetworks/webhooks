package com.klatunetworks.webhooks.core.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.klatunetworks.webhooks.core.Webhook;
import com.klatunetworks.webhooks.core.WebhookResult;

/**
 * A WebhookStore which retries Webhook calls (store in memory) based on the configured backoff function.
 * 
 * @author Dan Simpson
 *
 */
public class WebhookMemoryStore implements WebhookStore, Runnable {

	private static final Logger log = LoggerFactory.getLogger(WebhookMemoryStore.class);

	private static class WebhookRetryDetails {

		public boolean enqueued;
		public long firstAttemptAt;
		public long nextAttemptAt;
		public int numAttempts = 0;

		public WebhookRetryDetails() {
			this.firstAttemptAt = nextAttemptAt = System.currentTimeMillis();
			this.enqueued = false;
		}

		public long numSeconds() {
			return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - firstAttemptAt);
		}

		public void enqueued() {
			this.enqueued = true;
			this.numAttempts++;
		}

		public void reschedule(long nextAttemptAt) {
			this.enqueued = false;
			this.nextAttemptAt = nextAttemptAt;
		}

		public boolean isExhausted() {
			return nextAttemptAt <= 0;
		}
	}

	private final Map<Webhook, WebhookRetryDetails> webhooks = new ConcurrentHashMap<Webhook, WebhookRetryDetails>();
	private final LinkedBlockingQueue<Webhook> outbox = new LinkedBlockingQueue<Webhook>();

	private final Function<Integer, Long> backoffFn;

	public WebhookMemoryStore(Function<Integer, Long> backoffFn) {
		this.backoffFn = backoffFn;
	}

	@Override
	public LinkedBlockingQueue<Webhook> getQueue() {
		return outbox;
	}

	@Override
	public void add(Webhook webhook) {
		if (webhooks.put(webhook, new WebhookRetryDetails()) != null) {
			log.warn("Map overwrite detected, calling code is likely misusing the store.");
		}
	}

	@Override
	public synchronized void update(WebhookResult result) {
		if (result.isSuccessful()) {
			WebhookRetryDetails retry = webhooks.remove(result.getWebhook());
			if (retry != null && retry.numAttempts > 1) {
				log.info("Retry succeeded after {} attempts and {} seconds", retry.numAttempts, retry.numSeconds());
			}
		} else {
			WebhookRetryDetails retry = webhooks.get(result.getWebhook());
			if (retry == null) {
				return;
			}

			retry.reschedule(backoffFn.apply(retry.numAttempts + 1));
			if (retry.isExhausted()) {
				log.warn("Retry failed after {} attempts and {} seconds. Purging.", retry.numAttempts, retry.numSeconds());
				webhooks.remove(result.getWebhook());
			}
		}
	}

	@Override
	/**
	 * Flush the ready-to-retry Webhooks to the queue for the service to consume
	 */
	public void run() {
		long threshold = System.currentTimeMillis();
		webhooks.forEach((webhook, retry) -> {
			if (retry.nextAttemptAt < threshold && !retry.enqueued) {
				retry.enqueued();
				outbox.add(webhook);
			}
		});
	}

	public int getDelayedWebhooks() {
		return webhooks.size();
	}
}
