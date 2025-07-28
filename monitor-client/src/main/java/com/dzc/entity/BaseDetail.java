package com.dzc.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BaseDetail {
    //操作系统架构
    String osArch;
    //操作系统名称
    String osName;
    //操作系统版本
    String osVersion;
    //操作系统位数
    int osBit;
    //cpu名称
    String cpuName;
    //cpu核心数
    int cpuCore;
    //内存
    double memory;
    //磁盘容量
    double disk;
    //ip地址
    String ip;
}
