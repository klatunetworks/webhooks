package com.klatunetworks.webhooks.core;

import java.util.Optional;

import com.squareup.okhttp.Response;

/**
 * The result of a Webhook call
 * 
 * @author Dan Simpson
 *
 */
public class WebhookResult {

	private final Webhook webhook;
	private long latency = 0;

	private Optional<Response> response = Optional.empty();
	private Optional<Throwable> error = Optional.empty();
	private Optional<String> body = Optional.empty();

	public WebhookResult(Webhook webhook) {
		super();
		this.webhook = webhook;
	}

	/**
	 * @return the webhook
	 */
	public Webhook getWebhook() {
		return webhook;
	}

	/**
	 * @return true if there is a HTTP response
	 */
	public boolean hasResponse() {
		return response.isPresent();
	}

	/**
	 * @return true if there was an exception in the process of invoking the webhook
	 */
	public boolean hasError() {
		return error.isPresent();
	}

	/**
	 * @param response
	 *          the response to set
	 */
	public void setResponse(Response response) {
		this.response = Optional.of(response);
	}

	/**
	 * @param error
	 *          the error to set
	 */
	public void setError(Throwable error) {
		this.error = Optional.of(error);
	}

	/**
	 * @param body
	 *          the body to set
	 */
	public void setBody(String body) {
		this.body = Optional.of(body);
	}

	/**
	 * Generate a string summary of the result
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();

		if (hasResponse()) {
			builder.append("Webhook complete with response code ");
			builder.append(response.get().code());
			if (!response.get().isSuccessful()) {
				builder.append(" (failure)");
			}
		} else {
			builder.append("Webhook failed");
		}

		if (hasError()) {
			builder.append(" with error ");
			builder.append(error.get().getMessage());
		}

		builder.append(" (");
		builder.append(latency);
		builder.append("ms)");

		return builder.toString();
	}

	/**
	 * @return The response body
	 */
	public Optional<String> getBody() {
		return body;
	}

	/**
	 * @return the response
	 */
	public Optional<Response> getResponse() {
		return response;
	}

	/**
	 * @return the error
	 */
	public Optional<Throwable> getError() {
		return error;
	}

	/**
	 * Mark the latency for the result
	 * 
	 * @param latency
	 */
	public void setLatency(long latency) {
		this.latency = latency;
	}

	/**
	 * @return the latency
	 */
	public long getLatency() {
		return latency;
	}

	/**
	 * 
	 * @return true if there was a resposne with a successful status code
	 */
	public boolean isSuccessful() {
		return response.map(v -> v.isSuccessful()).orElse(false);
	}

}
