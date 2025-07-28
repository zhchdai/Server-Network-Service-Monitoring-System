package com.dzc.controller;

import com.dzc.entity.RestBean;
import com.dzc.entity.dto.Account;
import com.dzc.entity.vo.request.*;
import com.dzc.entity.vo.response.*;
import com.dzc.service.AccountService;
import com.dzc.service.ClientService;
import com.dzc.utils.Const;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitor")
@Tag(name = "监控相关", description = "包含监控主机的一系列相关操作")
public class MonitorController {
    @Resource
    ClientService clientService;

    @Resource
    AccountService accountService;

    @GetMapping("/list")
    @Operation(summary = "返回用户拥有权限的所有主机的预览信息")
    public RestBean<List<ClientPreviewVO>> listAllClient(@RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                         @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        List<ClientPreviewVO> clients = clientService.listClients(userId, accountService.getById(userId).getEmail());
        if (this.isAdminAccount(userRole)) {
            return RestBean.success(clients);
        } else {
            List<String> ids = this.accountAccessClients(userId);
            return RestBean.success(clients.stream()
                    .filter(vo -> ids.contains(vo.getId()))
                    .toList());
        }

    }

    @GetMapping("/simple-list")
    @Operation(summary = "返回用户拥有权限的所有主机的简略信息")
    public RestBean<List<ClientSimpleVO>> simpleClientList(@RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.isAdminAccount(userRole)) {
            return RestBean.success(clientService.listSimpleDetails());
        } else {
            return RestBean.noPermission();
        }
    }

    @PostMapping("/rename")
    @Operation(summary = "重命名主机")
    public RestBean<Void> renameClient(@RequestBody @Valid RenameClientVO renameClientVO,
                                       @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                       @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userId, userRole, renameClientVO.getId())) {
            clientService.renameClient(renameClientVO);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }
    }

    @PostMapping("/node")
    @Operation(summary = "重命名节点")
    public RestBean<Void> renameNode(@RequestBody @Valid RenameNodeVO renameNodeVO,
                                     @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                     @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userId, userRole, renameNodeVO.getId())) {
            clientService.renameNode(renameNodeVO);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }

    }

    @GetMapping("/details")
    @Operation(summary = "返回用户拥有权限的所有主机的详细信息")
    public RestBean<ClientDetailsVO> details(@RequestParam String clientId,
                                             @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                             @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userId, userRole, clientId)) {
            return RestBean.success(clientService.getClientDetails(clientId));
        } else {
            return RestBean.noPermission();
        }

    }

    @GetMapping("/runtime-history")
    @Operation(summary = "返回主机运行时数据的历史信息")
    public RestBean<RuntimeHistoryVO> runtimeDetailsHistory(@RequestParam String clientId,
                                                            @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                            @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userId, userRole, clientId)) {
            return RestBean.success(clientService.clientRuntimeDetailsHistory(clientId));
        } else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/runtime-now")
    @Operation(summary = "返回主机运行时数据的实时信息")
    public RestBean<RuntimeDetailVO> runtimeDetailsNow(@RequestParam String clientId,
                                                       @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                       @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userId, userRole, clientId)) {
            return RestBean.success(clientService.clientRuntimeDetailsNow(clientId));
        } else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/port")
    @Operation(summary = "返回端口状态信息")
    public RestBean<PortDetailVO> portDetails(@RequestParam String clientId,
                                              @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                              @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userId, userRole, clientId)) {
            return RestBean.success(clientService.clientPortDetails(clientId));
        } else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/getMonitorSettings")
    @Operation(summary = "获取监控设置")
    public RestBean<GetMonitorSettingVO> getMonitorSettings(@RequestParam String clientId,
                                                             @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        return RestBean.success(accountService.getAccountMonitorSettings(clientId, userId));
    }

    @GetMapping("/register")
    @Operation(summary = "返回注册主机所需的token")
    public RestBean<String> registerToken(@RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.isAdminAccount(userRole)) {
            return RestBean.success(clientService.registerToken());
        } else {
            return RestBean.noPermission();
        }
    }

    @GetMapping("/delete")
    @Operation(summary = "删除主机")
    public RestBean<String> deleteClient(@RequestParam String clientId,
                                         @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.isAdminAccount(userRole)) {
            clientService.deleteClient(clientId);
            return RestBean.success();
        } else {
            return RestBean.noPermission();
        }

    }

    private List<String> accountAccessClients(int uid) {
        Account account = accountService.getById(uid);
        return account.getClientList();
    }

    private boolean isAdminAccount(String role) {
        role = role.substring(5);
        return Const.ROLE_ADMIN.equals(role);
    }

    private boolean permissionCheck(int uid, String role, String clientId) {
        if (this.isAdminAccount(role)) {
            return true;
        }
        return this.accountAccessClients(uid).contains(clientId);
    }
}
