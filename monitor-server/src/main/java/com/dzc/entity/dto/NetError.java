package com.dzc.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_net_error")
public class NetError {
    @TableId
    int id;
    String ip;
    String type;
    String log;
    Date time;
}
