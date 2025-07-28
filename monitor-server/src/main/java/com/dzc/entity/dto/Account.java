package com.dzc.entity.dto;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dzc.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
@TableName("db_account")
@AllArgsConstructor
public class Account implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String username;
    String password;
    String email;
    String role;
    String clients;
    Date registerTime;

    public List<String> getClientList() {
        if(clients==null){
            return Collections.emptyList();
        }
        return JSONArray.parse(clients).toList(String.class);
    }
}
