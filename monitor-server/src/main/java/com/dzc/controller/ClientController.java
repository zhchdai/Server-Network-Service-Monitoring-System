package com.dzc.controller;

import com.dzc.entity.RestBean;
import com.dzc.entity.dto.Client;
import com.dzc.entity.vo.request.ClientDetailVO;
import com.dzc.entity.vo.request.PortDetailVO;
import com.dzc.entity.vo.request.RuntimeDetailVO;
import com.dzc.service.ClientService;
import com.dzc.utils.Const;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitor")
@Tag(name = "客户端相关", description = "包括注册主机、获取主机详细信息、获取主机运行时数据的功能")
public class ClientController {
    @Resource
    ClientService clientService;

    @GetMapping("/register")
    @Operation(summary = "注册主机")
    public RestBean<Void> registerClient(@RequestHeader("Authorization") String token) {
        return clientService.verifyAndRegister(token)?
                RestBean.success():RestBean.failure(401,"客户端注册失败，请检查token是否正确");
    }

    @PostMapping("/detail")
    @Operation(summary = "获取主机详细信息")
    public RestBean<Void> updateClientDetails(@RequestAttribute(Const.ATTR_CLIENT) Client client,
                                              @RequestBody @Valid ClientDetailVO clientDetailVO) {
        clientService.updateClientDetail(client, clientDetailVO);
        return RestBean.success();
    }

    @PostMapping("/runtime")
    @Operation(summary = "获取主机运行时数据")
    public RestBean<Void> updateRuntimeDetails(@RequestAttribute(Const.ATTR_CLIENT) Client client,
                                               @RequestBody @Valid RuntimeDetailVO runtimeDetailVO) {
        clientService.updateRunTimeDetail(client, runtimeDetailVO);
        return RestBean.success();
    }

    @PostMapping("/port")
    @Operation(summary = "获取主机端口状态")
    public RestBean<Void> updatePortStatus(@RequestAttribute(Const.ATTR_CLIENT) Client client,
                                           @RequestBody @Valid PortDetailVO portDetailVO) {
        clientService.updatePortDetail(client, portDetailVO);
        return RestBean.success();
    }
}
