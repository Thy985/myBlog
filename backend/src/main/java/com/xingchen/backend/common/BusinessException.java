package com.xingchen.backend.common;

/**
 * 业务异常 - 继承 exception.BusinessException 以便被 GlobalExceptionHandler 统一处理
 * @deprecated 请使用 {@link com.xingchen.backend.exception.BusinessException}
 */
@Deprecated
public class BusinessException extends com.xingchen.backend.exception.BusinessException {

    public BusinessException(Integer code, String message) {
        super(code, message);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
