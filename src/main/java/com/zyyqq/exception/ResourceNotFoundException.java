package com.zyyqq.exception;

public class ResourceNotFoundException extends RuntimeException {

    /** 构造资源未找到异常 */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}