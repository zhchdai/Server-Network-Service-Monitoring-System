package com.dzc.entity.vo.request;

import com.dzc.entity.BaseData;
import lombok.Data;

@Data
public class SaveMonitorSettingVO implements BaseData {
    String clientId;
    boolean host;
    boolean http;
    boolean https;
    boolean ftp;
    boolean sftp;
    boolean ftps;
    boolean smtp;
}
