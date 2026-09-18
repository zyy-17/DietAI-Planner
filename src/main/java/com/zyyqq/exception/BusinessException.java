package com.zyyqq.exception;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 获取异常状态码 */
    public int getCode() {
        return code;
    }
}