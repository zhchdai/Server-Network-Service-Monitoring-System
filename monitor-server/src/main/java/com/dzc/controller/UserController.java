package com.dzc.controller;

import com.dzc.entity.RestBean;
import com.dzc.entity.vo.request.ChangePasswordVO;
import com.dzc.entity.vo.request.CreateSubAccountVO;
import com.dzc.entity.vo.request.ModifyEmailVO;
import com.dzc.entity.vo.request.SaveMonitorSettingVO;
import com.dzc.entity.vo.response.SubAccountVO;
import com.dzc.service.AccountService;
import com.dzc.utils.Const;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户（包含子用户）相关", description = "包括更改密码/邮件、子用户创建和删除等操作")
public class UserController {
    @Resource
    AccountService accountService;

    @PostMapping("/change-password")
    @Operation(summary = "更改密码")
    public RestBean<Void> changePassword(@RequestBody @Valid ChangePasswordVO changePasswordVO,
                                         @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        return accountService.changePassword(userId, changePasswordVO.getPassword(), changePasswordVO.getNewPassword()) ?
                RestBean.success() : RestBean.failure(401, "原密码输入错误！");
    }

    @PostMapping("/modify-email")
    @Operation(summary = "更改邮件")
    public RestBean<Void> modifyEmail(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid ModifyEmailVO modifyEmailVO) {
        String result = accountService.modifyEmail(id, modifyEmailVO);
        if (result == null) {
            return RestBean.success();
        } else {
            return RestBean.failure(401, result);
        }
    }

    @PostMapping("/sub/create")
    @Operation(summary = "创建子用户")
    public RestBean<Void> createSubAccount(@RequestBody @Valid CreateSubAccountVO createSubAccountVO) {
        accountService.createSubAccount(createSubAccountVO);
        return RestBean.success();
    }

    @GetMapping("/sub/delete")
    @Operation(summary = "删除子用户")
    public RestBean<Void> deleteSubAccount(int uid, @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        if (uid == userId) {
            return RestBean.failure(401, "非法参数");
        }
        accountService.deleteSubAccount(uid);
        return RestBean.success();
    }

    @GetMapping("/sub/list")
    @Operation(summary = "展示用户的所有子用户")
    public RestBean<List<SubAccountVO>> listSubAccount() {
        return RestBean.success(accountService.listSubAccount());
    }

    @PostMapping("/saveMonitorSettings")
    @Operation(summary = "保存监控设置")
    public RestBean<Void> saveMonitorSettings(@RequestBody SaveMonitorSettingVO saveMonitorSettingVO,
                                              @RequestAttribute(Const.ATTR_USER_ID) int userId){
        accountService.saveAccountMonitorSettings(userId, saveMonitorSettingVO);
        return RestBean.success();
    }
}
