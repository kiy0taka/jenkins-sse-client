# Jenkins SSE Client

Java Client for [Jenkins SSE Gateway Plugin](https://github.com/jenkinsci/sse-gateway-plugin).

## Usage

### Connect
```java
Client client = new Client("http://your-jenkins-server", "user", "api-token or password");
client.connect("clientId");
```

### Subscribe Job Events
```java
client.subscribe("job", event -> {
    Map data = event.readData(Map.class);
    System.out.println(data);
    ...
});
```

See also [Events](https://github.com/jenkinsci/pubsub-light-module/blob/master/src/main/java/org/jenkinsci/plugins/pubsub/Events.java) and [EventProps](https://github.com/jenkinsci/pubsub-light-module/blob/master/src/main/java/org/jenkinsci/plugins/pubsub/EventProps.java).

### Subscribe Pipeline Events
```java
client.subscribe("pipeline", event -> {
    Map data = event.readData(Map.class);
    System.out.println(data);
    ...
});
```

See also [PipelineEventChannel](https://github.com/jenkinsci/blueocean-plugin/blob/master/blueocean-events/src/main/java/io/jenkins/blueocean/events/PipelineEventChannel.java).
