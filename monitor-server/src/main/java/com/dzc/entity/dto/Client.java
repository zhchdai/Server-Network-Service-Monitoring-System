package com.dzc.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dzc.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
@TableName("db_client")
public class Client implements BaseData {
    @TableId
    String id;
    String name;
    String token;
    String node;
    Date registerTime;
}
