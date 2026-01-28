package com.iot.riff.mqtt;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;

@Singleton
public class IotMqttConfig {
    private final String host;
    private final int port;
    private final String password;

    public IotMqttConfig(@Property(name = "mqtt.host") String host,
            @Property(name = "mqtt.port") int port,
            @Property(name = "mqtt.password") String password) {

        this.host = host;
        this.port = port;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }
}
