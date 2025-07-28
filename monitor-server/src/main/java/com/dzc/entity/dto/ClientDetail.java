package com.dzc.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("db_client_detail")
public class ClientDetail {
    //主机id
    @TableId
    String id;
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
