package com.ten.tenapiinterface.controller;

import com.ten.apiclientsdk.model.Username;
import com.ten.tenapiinterface.service.NameService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 名称api
 */
@RestController
@RequestMapping("/name")
public class NameController {

    @Resource
    private NameService nameService;

    @GetMapping("/ga")
    public String getNameByGET(String name){

        return "GET 你的名字是" + name;
    }


    @PostMapping("/path/{name}")
    public String getNameByPOSTPath(@PathVariable String name){
        return "POST 你的名字是" + name;
    }

    /**
     * POST请求用户名
     * @param username
     * @param request
     * @return
     */
    @PostMapping("/object")
    public String getNameByPOSTJson(@RequestBody Username username, HttpServletRequest request){

        String name = nameService.getName(username,request);

        return name;
    }


}
