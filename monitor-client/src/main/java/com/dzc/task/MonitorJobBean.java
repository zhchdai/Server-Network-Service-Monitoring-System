package com.dzc.task;

import com.dzc.entity.PortDetail;
import com.dzc.entity.RuntimeDetail;
import com.dzc.utils.MonitorUtils;
import com.dzc.utils.NetUtils;
import jakarta.annotation.Resource;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MonitorJobBean extends QuartzJobBean {
    @Resource
    MonitorUtils monitorUtils;

    @Resource
    NetUtils netUtils;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        RuntimeDetail runtimeDetail = monitorUtils.monitorRuntimeDetail();
        netUtils.updateRuntimeDetails(runtimeDetail);
        PortDetail portDetail = monitorUtils.monitorPortStatus();
        netUtils.updatePortStatus(portDetail);
    }
}
