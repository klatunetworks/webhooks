package com.klatunetworks.webhooks.core.store;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.klatunetworks.webhooks.core.Webhook;
import com.klatunetworks.webhooks.core.WebhookResult;

/**
 * Contract for implementing a Webhook store, which the StoreService will use as a data structure for fetching and updating webhooks based
 * on results.
 * 
 * @author Dan Simpson
 *
 */
public interface WebhookStore {

	/**
	 * @return The queue of webhooks that need immediate dispatch
	 */
	public LinkedBlockingQueue<Webhook> getQueue();

	/**
	 * Add a webhook to the store for future dispatch
	 * 
	 * @param webhook
	 */
	public void add(Webhook webhook);

	/**
	 * Update the store with a webhooks result. The store should remove it or reschedule depending on the behavior of the store.
	 * 
	 * @param result
	 */
	public void update(WebhookResult result);

	public static Function<Integer, Long> newConstantBackoffFn(long duration, TimeUnit unit) {
		return (n) -> unit.toMillis(duration);
	}

	public static Function<Integer, Long> newLinearBackoffFn(long duration, TimeUnit unit) {
		return (n) -> unit.toMillis(duration) * (n - 1);
	}

	public static Function<Integer, Long> newExponentialBackoffFn(long duration, TimeUnit unit) {
		return (n) -> unit.toMillis(duration) * (n - 1);
	}

	public static Function<Integer, Long> newRetryLimiter(long max, Function<Integer, Long> backoffFn) {
		return (n) -> n > max ? -1 : backoffFn.apply(n);
	}
}
