package com.system.chattalk_serverside.exception;

public class ExpiredJwtException extends RuntimeException {
    public ExpiredJwtException( String message ) {
        super(message);
    }
}
