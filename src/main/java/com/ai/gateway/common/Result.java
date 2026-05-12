package com.ai.gateway.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果类（泛型）
 * <p>
 * 本类用于封装所有API接口的响应数据，提供统一的响应格式。
 * 采用泛型设计，可以承载任意类型的响应数据。
 * </p>
 *
 * <p><b>响应格式示例：</b></p>
 * <pre>{@code
 * // 成功响应
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": { ... },
 *   "timestamp": 1234567890
 * }
 *
 * // 失败响应
 * {
 *   "code": 400,
 *   "message": "参数错误",
 *   "data": null,
 *   "timestamp": 1234567890
 * }
 * }</pre>
 *
 * <p><b>设计优势：</b></p>
 * <ul>
 *   <li>统一响应格式，便于前端统一处理</li>
 *   <li>泛型设计，支持任意类型的响应数据</li>
 *   <li>包含时间戳，便于调试和问题追踪</li>
 *   <li>提供静态工厂方法，简化使用</li>
 * </ul>
 *
 * @param <T> 响应数据的类型
 * @author AI Gateway Platform
 * @version 1.0.0
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     * <p>
     * 用途：标识请求的处理结果
     * 常见值：
     *   - 200: 成功
     *   - 400: 参数错误
     *   - 401: 未授权
     *   - 403: 禁止访问
     *   - 404: 资源不存在
     *   - 500: 服务器内部错误
     * </p>
     */
    private Integer code;

    /**
     * 响应消息
     * <p>
     * 用途：人类可读的结果描述
     * 成功时："操作成功"、"注册成功"等
     * 失败时："参数错误"、"余额不足"等具体错误信息
     * </p>
     */
    private String message;

    /**
     * 响应数据
     * <p>
     * 用途：承载业务数据
     * 类型：泛型，可以是任意类型（User、List、Map等）
     * 成功时：包含实际的业务数据
     * 失败时：通常为null
     * </p>
     */
    private T data;

    /**
     * 时间戳
     * <p>
     * 用途：记录响应生成的时间（毫秒级Unix时间戳）
     * 作用：
     *   1. 便于前端显示请求时间
     *   2. 用于调试和问题追踪
     *   3. 检测网络延迟
     * </p>
     */
    private Long timestamp;

    /**
     * 默认构造函数
     * <p>
     * 自动设置时间戳为当前时间
     * </p>
     */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 全参构造函数
     *
     * @param code    响应码
     * @param message 响应消息
     * @param data    响应数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应（无数据）
     * <p>
     * 适用场景：操作成功但不需要返回数据
     * 例如：删除操作、更新操作等
     * </p>
     *
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 创建成功响应（带数据）
     * <p>
     * 适用场景：查询操作，需要返回数据
     * 例如：查询用户信息、查询列表等
     * </p>
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 创建成功响应（自定义消息+数据）
     * <p>
     * 适用场景：需要自定义成功消息的场景
     * 例如："注册成功"、"充值成功"等
     * </p>
     *
     * @param message 自定义成功消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 创建失败响应（默认错误）
     * <p>
     * 使用默认的错误码和错误消息
     * </p>
     *
     * @param <T> 数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    /**
     * 创建失败响应（自定义消息）
     * <p>
     * 适用场景：需要自定义错误消息的场景
     * 例如："用户名已存在"、"余额不足"等
     * </p>
     *
     * @param message 自定义错误消息
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /**
     * 创建失败响应（自定义响应码和消息）
     * <p>
     * 适用场景：需要精确控制错误码和错误消息的场景
     * 例如：区分不同类型的参数错误
     * </p>
     *
     * @param code    自定义错误码
     * @param message 自定义错误消息
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 创建失败响应（使用ResultCode枚举）
     * <p>
     * 适用场景：使用预定义的错误码和消息
     * 优点：统一管理错误码，避免硬编码
     * </p>
     *
     * @param resultCode 错误码枚举
     * @param <T>        数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }
}
