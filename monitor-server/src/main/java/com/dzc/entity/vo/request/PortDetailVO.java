package com.dzc.entity.vo.request;

import lombok.Data;

@Data
public class PortDetailVO {
    boolean httpStatus;
    boolean httpsStatus;
    boolean ftpStatus;
    boolean sftpStatus;
    boolean ftpsStatus;
    boolean smtpStatus;
    long timestamp;
}
