package org.kiy0taka.jenkins.sse;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyInvocation;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.SseFeature;

/**
 * @author Kiyotaka Oku
 */
public class Client {

    private final String jenkinsRoot;
    private final JerseyClient client;
    private String clientId;
    private Cookie cookie;
    private EventSource eventSource;
    private ConcurrentHashMap<String, EventListener> queue = new ConcurrentHashMap<>();
    private ScheduledExecutorService es = Executors.newScheduledThreadPool(1);

    public Client(String jenkinsRoot) {
        this.jenkinsRoot = jenkinsRoot;
        this.client = (JerseyClient) ClientBuilder.newBuilder().register(SseFeature.class).build();
    }

    public Client(String jenkinsRoot, String username, String apiToken) {
        this.jenkinsRoot = jenkinsRoot;
        client = (JerseyClient) ClientBuilder.newBuilder()
                .register(SseFeature.class)
                .register(HttpAuthenticationFeature.basic(username, apiToken))
                .build();
    }

    public void connect(String clientId) {
        this.clientId = clientId;
        connect();
        listen();
        es.scheduleWithFixedDelay(() -> queue.keySet().forEach((key) -> {
            try {
                configure(key, queue.remove(key));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }), 5, 5, TimeUnit.SECONDS);
    }

    public void subscribe(String channelName, EventListener listener) {
        queue.put(channelName, listener);
    }

    private void connect() {
        URI uri = newUriBuilder("sse-gateway/connect").queryParam("clientId", clientId).build();
        Response response = client.target(uri).request().get();
        Iterator<NewCookie> iterator = response.getCookies().values().iterator();
        if (iterator.hasNext()) {
            cookie = iterator.next();
        }
    }

    private void configure(String channelName, EventListener listener) {
        Map<String, String> config = new HashMap<>();
        config.put("jenkins_channel", channelName);
        Map<String, Object> json = new HashMap<>();
        json.put("dispatcherId", clientId);
        json.put("subscribe", Arrays.asList(config));
        client.target(newUriBuilder("sse-gateway/configure")).request().cookie(cookie).post(Entity.json(json));
        eventSource.register(listener, channelName);
    }

    private void listen() {
        javax.ws.rs.client.WebTarget target = new WebTarget(newUriBuilder("sse-gateway/listen").path(clientId), client, cookie);
        eventSource = EventSource.target(target).build();
        eventSource.open();
    }

    private UriBuilder newUriBuilder(String path) {
        return UriBuilder.fromPath(jenkinsRoot).path(path);
    }

    private static class WebTarget extends JerseyWebTarget {

        private Cookie cookie;

        public WebTarget(UriBuilder uriBuilder, JerseyClient client, Cookie cookie) {
            super(uriBuilder, client.getConfiguration());
            this.cookie = cookie;
        }

        @Override
        public JerseyInvocation.Builder request(MediaType... acceptedResponseTypes) {
            JerseyInvocation.Builder result = super.request(acceptedResponseTypes);
            if (cookie != null) {
                result.cookie(cookie);
            }
            return result;
        }
    }
}
