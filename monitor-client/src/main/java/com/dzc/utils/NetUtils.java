package com.dzc.utils;

import com.alibaba.fastjson2.JSONObject;
import com.dzc.entity.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@Component
public class NetUtils {
    private final HttpClient client = HttpClient.newHttpClient();

    @Lazy
    @Resource
    ConnectionConfig connectionConfig;

    public boolean registerToServer(String address,String token){
        log.info("正在向服务器注册，请稍后...");
        Response response = this.doGet("/register", address, token);
        System.out.println(response);
        if(response.success()){
            log.info("客户端注册已完成！");
        }else {
            log.error("客户端注册失败：{}",response.message());
        }
        return response.success();
    }

    private Response doGet(String url){
        return this.doGet(url,connectionConfig.getAddress(),connectionConfig.getToken());
    }

    private Response doGet(String url, String address, String token){
        try {
            HttpRequest request = HttpRequest.newBuilder().GET()
                    .uri(new URI(address+"/monitor"+url))
                    .header("Authorization",token)
                    .build();
            HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
            return JSONObject.parseObject(response.body()).to(Response.class);
        }catch (Exception e){
            log.error("在发起服务端请求时出现问题",e);
            return Response.errorResponse(e);
        }
    }

    public void updateBaseDetails(BaseDetail baseDetail){
        Response response = this.doPost("/detail", baseDetail);
        if(response.success()){
            log.info("系统基本信息已更新完成");
        }else {
            log.error("系统基本信息更新失败：{}",response.message());
        }
    }

    public void updateRuntimeDetails(RuntimeDetail runtimeDetail){
        Response response = this.doPost("/runtime", runtimeDetail);
        if(!response.success()){
            log.warn("更新运行时状态时，接收到服务端的异常相应内容：{}",response.message());
        }
    }

    public void updatePortStatus(PortDetail portDetail){
        Response response = this.doPost("/port", portDetail);
        if(!response.success()){
            log.warn("更新端口状态异常，接收到服务端的异常相应内容：{}",response.message());
        }
    }

    private Response doPost(String url, Object data){
        try{
            String rawData = JSONObject.from(data).toString();
            HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(rawData))
                    .uri(new URI(connectionConfig.getAddress()+"/monitor"+url))
                    .header("Authorization",connectionConfig.getToken())
                    .header("Content-Type","application/json")
                    .build();
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
            return JSONObject.parseObject(response.body()).to(Response.class);
        }catch (Exception e){
            log.error("在发起服务端请求时出现问题",e);
            return Response.errorResponse(e);
        }
    }
}
