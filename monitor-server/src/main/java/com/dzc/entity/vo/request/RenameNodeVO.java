package com.dzc.entity.vo.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class RenameNodeVO {
    String id;
    @Length(min = 1,max = 10)
    String node;
}
