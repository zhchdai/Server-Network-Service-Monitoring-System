package com.dzc.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PortDetail {
    boolean httpStatus;
    boolean httpsStatus;
    boolean ftpStatus;
    boolean sftpStatus;
    boolean ftpsStatus;
    boolean smtpStatus;
    long timestamp;
}
