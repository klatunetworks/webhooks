package com.klatunetworks.webhooks.core;

import java.util.HashMap;
import java.util.Map;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

/**
 * 
 * Webhook request class.
 * 
 * @author Dan Simpson
 *
 */
public class Webhook {

	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	private String url;
	private String json;
	private Map<String, String> headers = new HashMap<String, String>();

	public Webhook() {
	}

	public Webhook(String url, String json) {
		this.url = url;
		this.json = json;
	}

	/**
	 * Build webhook request with url, json and headers
	 * 
	 * @param url
	 * @param json
	 * @param headers
	 */
	public Webhook(String url, String json, Map<String, String> headers) {
		this.url = url;
		this.json = json;
		this.headers = headers;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * 
	 * @return a Request.Builder for okhttp
	 */
	protected Request.Builder toBuilder() {
		return new Request.Builder().url(url).headers(Headers.of(headers)).post(RequestBody.create(JSON, json));
	}

	/**
	 * Add or update a header for the webhook request.
	 * 
	 * @param name
	 *          of the header
	 * @param value
	 *          of the header
	 */
	public void setHeader(String name, String value) {
		headers.put(name, value);
	}
}
