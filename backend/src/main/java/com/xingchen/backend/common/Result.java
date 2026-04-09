package com.xingchen.backend.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = "success";
        result.data = data;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = message;
        result.data = data;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = "success";
        result.timestamp = System.currentTimeMillis();
        return result;
    }
}
