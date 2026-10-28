package com.ten.apiclientsdk.client;

/**
 * SDK HTTP 调用失败异常。
 *
 * <p>仅保留 HTTP 状态码和受控错误说明，不持有请求体或调用凭据。</p>
 */
public class ApiClientException extends IllegalStateException {
    /**
     * 上游接口返回的 HTTP 状态码。
     */
    private final int status;

    /**
     * 创建接口调用异常。
     *
     * @param status 上游 HTTP 状态码
     * @param message 上游受控错误说明或通用失败提示
     */
    public ApiClientException(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * 获取上游 HTTP 状态码。
     *
     * @return 上游 HTTP 状态码
     */
    public int getStatus() {
        return status;
    }
}
