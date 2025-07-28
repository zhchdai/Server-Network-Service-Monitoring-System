package com.dzc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzc.entity.dto.Account;
import com.dzc.entity.vo.request.*;
import com.dzc.entity.vo.response.GetMonitorSettingVO;
import com.dzc.entity.vo.response.SubAccountVO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface AccountService extends IService<Account>, UserDetailsService {
    Account findAccountByNameOrEmail(String text);

    String registerEmailVerifyCode(String type, String email, String address);

    String resetEmailAccountPassword(EmailResetVO info);

    String resetConfirm(ConfirmResetVO info);

    boolean changePassword(int id, String oldPassword, String newPassword);

    void createSubAccount(CreateSubAccountVO createSubAccountVO);

    void deleteSubAccount(int uid);

    List<SubAccountVO> listSubAccount();

    String modifyEmail(int id, ModifyEmailVO modifyEmailVO);

    GetMonitorSettingVO getAccountMonitorSettings(String clientId, int userId);

    void saveAccountMonitorSettings(int userId, SaveMonitorSettingVO saveMonitorSettingVO);
}
