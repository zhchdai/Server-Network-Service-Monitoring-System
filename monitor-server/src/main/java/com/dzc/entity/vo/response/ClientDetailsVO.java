package com.dzc.entity.vo.response;

import lombok.Data;

@Data
public class ClientDetailsVO {
    String id;
    String name;
    boolean online;
    String node;
    String location;
    String ip;
    String cpuName;
    String osName;
    String osVersion;
    double memory;
    int cpuCore;
    double disk;
}
