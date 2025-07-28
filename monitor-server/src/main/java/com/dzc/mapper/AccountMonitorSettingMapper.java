package com.dzc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dzc.entity.dto.AccountMonitorSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AccountMonitorSettingMapper extends BaseMapper<AccountMonitorSetting> {
    @Update("""
update db_account_monitor_setting 
set host=#{host},http=#{http},https=#{https},ftp=#{ftp},sftp=#{sftp},ftps=#{ftps},smtp=#{smtp} 
where user_id=#{userId} and client_id=#{clientId}
""")
    void updateAccountMonitorSetting(AccountMonitorSetting accountMonitorSetting);
}
