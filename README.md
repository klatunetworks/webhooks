## webhooks

A java library with opinions and constructs for webhooks. It works as a simple asynchronous http client or a retrying service with backoff strategies.

[![Build Status](https://travis-ci.org/klatunetworks/webhooks.svg?branch=master)](https://travis-ci.org/klatunetworks/webhooks)

##### Opinions

* Webhooks are POST requests with JSON bodies
* Webhooks succeed on 2xx responses
* Webhooks fail on 4xx+ responses
* Webhooks may or may not require retries

##### Core Dependencies

* [okhttp][okhttp]
* [metrics-core][metrics]
* slf4j
* java 1.8

##### Minimal Examples

```java
// Create a new service with default http client
WebhookService service = new WebhookService();

// Post a small json body to the domain.  Fire and forget.
service.submit("https://mydomain.com/path", "['json']")

// Invoke callback on completion
service.submit(url, json, (result) -> {
  // do something with WebhookResult
});
```

The above is ideal for best-effort webhooks.  The underlying HTTP client will attempt to retry the request on failure, with alternative routes if available. See [okhttp][okhttp] for more info

##### Custom headers

```java
Webhook webhook = new Webhook(url, json);
webhook.setHeader("Authorization", "Basic ...");
service.submit(webhook, callback);
```

##### dropwizard-metrics integration

You can supply the bottom line webhook service with a metrics registry to track the following:

* webhook.errors: Exceptions
* webhook.complete: Completed webhooks
* webhook.complete.success: Successful webhooks (HTTP)
* webhook.complete.error: Failed webhooks (HTTP)
* webhook.latency > Histogram of request latency

```java
new WebhookService(new OkHttpClient(), new MetricRegistry());
```

##### Retries, Backoff, Limits, etc

Included in core is a WebhookMemoryStore which will schedule and retry webhooks on failure.  Tune with care. The example below uses a backoff function which limits the attempts to 5 (return <= 0 to stop retrying) and increases the next request by (attempt * 5000).

```java
WebhookStore store = new WebhookMemoryStore((attempt) -> {
  return attempt > 5 ? -1l : ((n - 1) * 5000l);
});

WebhookStoreService service = new WebhookStoreService(new WebhookService(), store);

// Submit both to a executor to run in the backround
ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
executor.scheduleAtFixedRate(store, 0, 5, TimeUnit.MILLISECONDS);
executor.submit(service);
```

Quick backoff function helpers:

```java
WebhookStore.newConstantBackoffFn(30, TimeUnit.SECONDS);
WebhookStore.newLinearBackoffFn(30, TimeUnit.SECONDS);
WebhookStore.newExponentialBackoffFn(30, TimeUnit.SECONDS);
WebhookStore.newRetryLimiter(30, WebhookStore.newLinearBackoffFn(30, TimeUnit.SECONDS));
```

##### Custom Stores

In order to properly handle reliable webhooks in a distributed system, I suggest implementing your own WebhookStore and using the WebhookStoreService as your interface.

[okhttp]: https://github.com/square/okhttp
[metrics]: https://github.com/dropwizard/metrics