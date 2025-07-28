package com.dzc.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dzc.entity.BaseData;
import lombok.Data;

@TableName("db_account_monitor_setting")
@Data
public class AccountMonitorSetting implements BaseData {
    int userId;
    String clientId;
    boolean host;
    boolean http;
    boolean https;
    boolean ftp;
    boolean sftp;
    boolean ftps;
    boolean smtp;
}
