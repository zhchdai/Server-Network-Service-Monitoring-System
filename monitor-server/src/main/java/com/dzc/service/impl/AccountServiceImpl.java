package com.dzc.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzc.entity.dto.Account;
import com.dzc.entity.dto.AccountMonitorSetting;
import com.dzc.entity.vo.request.*;
import com.dzc.entity.vo.response.GetMonitorSettingVO;
import com.dzc.entity.vo.response.SubAccountVO;
import com.dzc.mapper.AccountMapper;
import com.dzc.mapper.AccountMonitorSettingMapper;
import com.dzc.service.AccountService;
import com.dzc.utils.Const;
import com.dzc.utils.FlowUtils;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    //验证邮件发送冷却时间限制，秒为单位
    @Value("${spring.web.verify.mail-limit}")
    int verifyLimit;

    @Resource
    AmqpTemplate rabbitTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    PasswordEncoder passwordEncoder;

    @Resource
    FlowUtils flow;

    @Resource
    AccountMonitorSettingMapper accountMonitorSettingMapper;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByNameOrEmail(username);
        if (account == null)
            throw new UsernameNotFoundException("用户名或密码错误");
        return User
                .withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    public String registerEmailVerifyCode(String type, String email, String address) {
        synchronized (address.intern()) {
            if (!this.verifyLimit(address))
                return "请求频繁，请稍后再试";
            Random random = new Random();
            int code = random.nextInt(899999) + 100000;
            Map<String, Object> data = Map.of("type", type, "email", email, "code", code);
            rabbitTemplate.convertAndSend(Const.MQ_MAIL, data);
            stringRedisTemplate.opsForValue()
                    .set(Const.VERIFY_EMAIL_DATA + email, String.valueOf(code), 3, TimeUnit.MINUTES);
            return null;
        }
    }


    @Override
    public String resetEmailAccountPassword(EmailResetVO info) {
        String verify = resetConfirm(new ConfirmResetVO(info.getEmail(), info.getCode()));
        if (verify != null) return verify;
        String email = info.getEmail();
        String password = passwordEncoder.encode(info.getPassword());
        boolean update = this.update().eq("email", email).set("password", password).update();
        if (update) {
            this.deleteEmailVerifyCode(email);
        }
        return update ? null : "更新失败，请联系管理员";
    }


    @Override
    public String resetConfirm(ConfirmResetVO info) {
        String email = info.getEmail();
        String code = this.getEmailVerifyCode(email);
        if (code == null) return "请先获取验证码";
        if (!code.equals(info.getCode())) return "验证码错误，请重新输入";
        return null;
    }

    @Override
    public boolean changePassword(int id, String oldPassword, String newPassword) {
        Account account = this.getById(id);
        String password = account.getPassword();
        if (!passwordEncoder.matches(oldPassword, password)) {
            return false;
        }
        this.update(Wrappers.<Account>update().eq("id", id)
                .set("password", passwordEncoder.encode(newPassword)));
        return true;
    }

    @Override
    public void createSubAccount(CreateSubAccountVO createSubAccountVO) {
        Account account = this.findAccountByNameOrEmail(createSubAccountVO.getEmail());
        if (account != null) {
            throw new IllegalArgumentException("该电子邮件已被注册");
        }
        account = this.findAccountByNameOrEmail(createSubAccountVO.getUsername());
        if (account != null) {
            throw new IllegalArgumentException("该用户名已被注册");
        }
        account = new Account(null, createSubAccountVO.getUsername(),
                passwordEncoder.encode(createSubAccountVO.getPassword()), createSubAccountVO.getEmail(),
                Const.ROLE_NORMAL, JSONArray.copyOf(createSubAccountVO.getClients()).toJSONString(), new Date());

        this.save(account);
    }

    @Override
    public void deleteSubAccount(int uid) {
        this.removeById(uid);
    }

    @Override
    public List<SubAccountVO> listSubAccount() {
        return this.list(Wrappers.<Account>query().eq("role", Const.ROLE_NORMAL))
                .stream().map(account -> {
                    return new SubAccountVO(account.getId(), account.getUsername(),
                            account.getEmail(), JSONArray.parse(account.getClients()));
                }).toList();
    }

    @Override
    public String modifyEmail(int id, ModifyEmailVO modifyEmailVO) {
        String code = getEmailVerifyCode(modifyEmailVO.getEmail());
        if (code == null) return "请先获取验证码";
        if (!code.equals(modifyEmailVO.getCode())) return "验证码错误，请重新输入";
        this.deleteEmailVerifyCode(modifyEmailVO.getEmail());
        Account account = this.findAccountByNameOrEmail(modifyEmailVO.getEmail());
        if (account != null && account.getId() != id) return "该邮箱账号已经被其他账号绑定，无法完成操作";
        this.update()
                .set("email", modifyEmailVO.getEmail())
                .eq("id", id)
                .update();
        return null;
    }

    @Override
    public GetMonitorSettingVO getAccountMonitorSettings(String clientId, int userId) {
        QueryWrapper<AccountMonitorSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("client_id", clientId);
        AccountMonitorSetting setting = accountMonitorSettingMapper.selectOne(queryWrapper);
        return setting==null?null:setting.asViewObject(GetMonitorSettingVO.class);
    }

    @Override
    public void saveAccountMonitorSettings(int userId, SaveMonitorSettingVO saveMonitorSettingVO) {
        AccountMonitorSetting settings = saveMonitorSettingVO.asViewObject(AccountMonitorSetting.class);
        settings.setUserId(userId);

        //查询记录是否存在
        QueryWrapper<AccountMonitorSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("client_id", settings.getClientId());
        AccountMonitorSetting existingRecord = accountMonitorSettingMapper.selectOne(queryWrapper);

        if (existingRecord != null) {
            accountMonitorSettingMapper.updateAccountMonitorSetting(settings);
        } else {
            accountMonitorSettingMapper.insert(settings);
        }
    }

    private void deleteEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        stringRedisTemplate.delete(key);
    }

    private String getEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        return stringRedisTemplate.opsForValue().get(key);
    }


    private boolean verifyLimit(String address) {
        String key = Const.VERIFY_EMAIL_LIMIT + address;
        return flow.limitOnceCheck(key, verifyLimit);
    }


    public Account findAccountByNameOrEmail(String text) {
        return this.query()
                .eq("username", text).or()
                .eq("email", text)
                .one();
    }

}
