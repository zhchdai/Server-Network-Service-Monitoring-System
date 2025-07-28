package com.dzc.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RuntimeDetail {
    //时间戳
    long timestamp;
    //cpu使用量
    double cpuUsage;
    //内存使用量
    double memoryUsage;
    //磁盘使用量
    double diskUsage;
    //网络上传速度
    double networkUpload;
    //网络下载速度
    double networkDownload;
    //磁盘读取速度
    double diskRead;
    //磁盘写入速度
    double diskWrite;
}
