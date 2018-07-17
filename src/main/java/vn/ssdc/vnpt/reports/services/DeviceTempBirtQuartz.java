package vn.ssdc.vnpt.reports.services;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DeviceTempBirtQuartz implements Job {
    private static final Logger logger = LoggerFactory.getLogger(DeviceTempBirtQuartz.class);

    @Autowired
    public DeviceTempBirtService deviceTempBirtService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        deviceTempBirtService.createDeviceTempBirt();
    }
}
