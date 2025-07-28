package com.dzc.entity.vo.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ChangePasswordVO {
    @Length(min = 6,max = 15)
    String password;
    @Length(min = 6,max = 15)
    String newPassword;
}
