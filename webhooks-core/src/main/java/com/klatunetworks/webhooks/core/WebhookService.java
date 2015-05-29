package com.klatunetworks.webhooks.core;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import com.codahale.metrics.MetricRegistry;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * Base service for posting Webhook events to external web services. All requests are Asynchronous and invoke a Consumer<WebhookResult> on
 * completion, regardless of outcome.
 * 
 * @author Dan Simpson
 *
 */
public class WebhookService implements WebhookServiceContract {

	private final class ForwardingCallback implements Callback {

		private final long created;
		private final Webhook webhook;
		private final Consumer<WebhookResult> callback;

		public ForwardingCallback(Webhook webhook, Consumer<WebhookResult> callback) {
			super();
			this.created = System.currentTimeMillis();
			this.webhook = webhook;
			this.callback = callback;
		}

		@Override
		public void onFailure(Request request, IOException exception) {
			WebhookResult result = new WebhookResult(webhook);
			result.setLatency(System.currentTimeMillis() - created);
			result.setError(exception);
			callback.accept(result);

			// track metrics
			metrics.ifPresent((m) -> {
				m.meter("webhook.error").mark();
				m.histogram("webhook.latency").update(result.getLatency());
			});
		}

		@Override
		public void onResponse(Response response) {
			WebhookResult result = new WebhookResult(webhook);
			result.setLatency(System.currentTimeMillis() - created);
			result.setResponse(response);
			try {
				result.setBody(response.body().string());
			} catch (IOException error) {
				result.setError(error);
			}
			callback.accept(result);

			// track metrics for request
			metrics.ifPresent((m) -> {
				m.meter("webhook.complete").mark();
				if (result.isSuccessful()) {
					m.meter("webhook.complete.success").mark();
				} else {
					m.meter("webhook.complete.error").mark();
				}
				m.histogram("webhook.latency").update(result.getLatency());
			});
		}
	}

	private final Optional<MetricRegistry> metrics;
	private final OkHttpClient client;

	/**
	 * Create a new WebhookService with default http client
	 */
	public WebhookService() {
		this(getSharedClient());
	}

	/**
	 * Build a Webhook service with a given HTTP client
	 * 
	 * @param client
	 */
	public WebhookService(OkHttpClient client) {
		this(client, null);
	}

	public WebhookService(OkHttpClient client, MetricRegistry metrics) {
		super();
		this.client = client;
		this.metrics = Optional.ofNullable(metrics);
	}

	/**
	 * Submit a webhook to the service for execution in the future. On completion, the consumer will be fed the result.
	 * 
	 * @param webhook
	 *          the webhook object with webhook target information
	 * @param callback
	 *          the callback consumer
	 */
	public void submit(Webhook webhook, Consumer<WebhookResult> callback) {
		client.newCall(webhook.toBuilder().build()).enqueue(new ForwardingCallback(webhook, callback));
	}

	private static OkHttpClient sharedClient;

	private static synchronized OkHttpClient getSharedClient() {
		if (sharedClient == null) {
			sharedClient = new OkHttpClient();
		}
		return sharedClient;
	}

}
