package com.reliaquest.api.model;

public class ApiResponse<T> {
    private T data;
    private String status;

    public ApiResponse() {}

    // Constructor with arguments (keep this)
    public ApiResponse(String status, T data) {
        this.status = status;
        this.data = data;
    }

    // Getters and setters for data and status

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
