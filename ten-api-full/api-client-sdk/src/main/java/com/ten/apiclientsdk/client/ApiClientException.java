package com.ten.apiclientsdk.client;

/** HTTP failure without retaining the request body or API credentials. */
public class ApiClientException extends IllegalStateException {
    private final int status;
    public ApiClientException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }
}
