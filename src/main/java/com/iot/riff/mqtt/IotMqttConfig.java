package com.iot.riff.mqtt;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;

@Singleton
public class IotMqttConfig {
    private final String host;
    private final int port;
    private final String secretPath;

    public IotMqttConfig(@Property(name = "mqtt.host") String host,
            @Property(name = "mqtt.port") int port,
            @Property(name = "mqtt.secretPath") String secretPath) {

        this.host = host;
        this.port = port;
        this.secretPath = secretPath;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getSecretPath() {
        return secretPath;
    }
}
