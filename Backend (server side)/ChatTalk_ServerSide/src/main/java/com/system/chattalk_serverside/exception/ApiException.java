package com.system.chattalk_serverside.exception;

public class ApiException extends RuntimeException {
    public ApiException( String message ) {
        super(message);
    }
}
