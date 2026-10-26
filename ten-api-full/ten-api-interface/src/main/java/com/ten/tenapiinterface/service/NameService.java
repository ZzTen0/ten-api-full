package com.ten.tenapiinterface.service;

import com.ten.apiclientsdk.model.Username;

import javax.servlet.http.HttpServletRequest;

public interface NameService {

    String getName(Username username, HttpServletRequest request);
}
