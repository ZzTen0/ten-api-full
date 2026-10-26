package com.ten.common.service;

/** Resolves an enabled interface by its HTTP method and gateway path. */
public interface InnerInterfaceInfoService {
    Long resolveOnlineInterfaceId(String method, String path);
}
