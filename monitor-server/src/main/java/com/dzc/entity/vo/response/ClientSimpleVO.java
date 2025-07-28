package com.dzc.entity.vo.response;

import lombok.Data;

@Data
public class ClientSimpleVO {
    String id;
    String name;
    String location;
    String osName;
    String osVersion;
    String ip;
}
