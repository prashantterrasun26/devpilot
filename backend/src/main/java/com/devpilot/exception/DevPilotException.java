package com.devpilot.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DevPilotException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public DevPilotException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public DevPilotException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static DevPilotException notFound(String message) {
        return new DevPilotException(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public static DevPilotException unauthorized(String message) {
        return new DevPilotException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static DevPilotException forbidden(String message) {
        return new DevPilotException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static DevPilotException badRequest(String message) {
        return new DevPilotException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static DevPilotException gitHubError(String message) {
        return new DevPilotException(message, HttpStatus.BAD_GATEWAY, "GITHUB_API_ERROR");
    }

    public static DevPilotException aiError(String message) {
        return new DevPilotException(message, HttpStatus.INTERNAL_SERVER_ERROR, "AI_SERVICE_ERROR");
    }
}
