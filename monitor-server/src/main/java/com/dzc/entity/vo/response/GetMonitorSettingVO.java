package com.dzc.entity.vo.response;

import lombok.Data;

@Data
public class GetMonitorSettingVO {
    String clientId;
    boolean host;
    boolean http;
    boolean https;
    boolean ftp;
    boolean sftp;
    boolean ftps;
    boolean smtp;
}
