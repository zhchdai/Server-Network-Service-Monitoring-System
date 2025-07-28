package com.dzc.entity.vo.response;

import lombok.Data;

@Data
public class ClientPreviewVO {
    String id;
    boolean online;
    String name;
    String location;
    String osName;
    String osVersion;
    String ip;
    String cpuName;
    int cpuCore;
    double memory;
    double cpuUsage;
    double memoryUsage;
    double networkUpload;
    double networkDownload;
}
