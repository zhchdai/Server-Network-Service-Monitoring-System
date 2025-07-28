package com.dzc.entity.vo.response;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RuntimeHistoryVO {
    double disk;
    double memory;
    List<JSONObject> list = new ArrayList<>();
}
