package com.iot.riff.service.exception;

public class IotException extends RuntimeException {

    public IotException(String message) {
        super(message);
    }

    public IotException(String message, Throwable cause) {
        super(message, cause);
    }
}
