package com.dzc.entity.vo.response;

import com.alibaba.fastjson2.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubAccountVO {
    int id;
    String username;
    String email;
    JSONArray clientList;
}
