package com.ieps.exception;

/**
 * Created by ljw
 */
public class CustomException extends Exception {
    private String message;

    public CustomException(String message) {
        this.message = message;
    }

    public CustomException(String message, String message1) {
        super(message);
        this.message = message1;
    }

    public CustomException(String message, Throwable cause, String message1) {
        super(message, cause);
        this.message = message1;
    }

    public CustomException(Throwable cause, String message) {
        super(cause);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
