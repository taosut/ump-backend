package vn.ssdc.vnpt.performance.model;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import vn.ssdc.vnpt.performance.sevices.PerformanceSettingService;

/**
 * Created by thangnc on 07-Aug-17.
 */
public class PerformanceQuartzJobRefreshParameter implements Job {

    public static final Logger logger = LoggerFactory.getLogger(PerformanceQuartzJobRefreshParameter.class);

    @Autowired
    private PerformanceSettingService performanceSettingService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();
        logger.info("#PERFORMANCE_REFRESH");
        performanceSettingService.refreshParameter(jdm.getLong("performanceRefreshJobId"));
    }

}
