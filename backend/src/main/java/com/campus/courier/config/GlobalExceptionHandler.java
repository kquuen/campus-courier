package com.campus.courier.config;

import com.campus.courier.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 路径参数类型错误（如 /api/order/list 被 /{id} 捕获时 list 无法转为 Long） */
    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleNumberFormat(NumberFormatException e, HttpServletRequest request) {
        log.error("类型转换错误 [{} {}]: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(400, "请求路径参数类型错误");
    }

    /** 404 未找到接口或资源 */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandler(NoHandlerFoundException e, HttpServletRequest request) {
        log.error("接口不存在 [{} {}]", request.getMethod(), request.getRequestURI());
        return Result.fail(404, "接口不存在: " + request.getMethod() + " " + request.getRequestURI());
    }

    /** 405 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                              HttpServletRequest request) {
        log.error("请求方法不支持 [{} {}]", request.getMethod(), request.getRequestURI());
        return Result.fail(405, "请求方法不支持: " + request.getMethod());
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<?> handleMaxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        log.error("文件大小超出限制: {}", e.getMessage());
        return Result.fail(413, "文件大小超出限制，最大支持2MB");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        log.error("参数错误: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(Exception e) {
        log.error("参数校验失败: {}", e.getMessage());
        return Result.fail(400, "参数校验失败");
    }

    /** JSON 解析错误（如日期格式错误） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.error("JSON解析错误: {}", e.getMessage());
        String message = e.getMessage();
        if (message != null && message.contains("LocalDateTime")) {
            return Result.fail(400, "日期格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
        }
        return Result.fail(400, "请求数据格式错误");
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleNullPointer(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.fail("系统内部错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统内部错误，请稍后重试");
    }
}
