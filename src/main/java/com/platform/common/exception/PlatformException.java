package com.platform.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PlatformException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String refNo;

    public PlatformException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.refNo = null;
    }

    public PlatformException(String message, HttpStatus status, String errorCode, String refNo) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.refNo = refNo;
    }

    public PlatformException(String message, Throwable cause, HttpStatus status, String errorCode, String refNo) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.refNo = refNo;
    }
}
