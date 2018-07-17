package vn.ssdc.vnpt;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.alarm.model.AlarmDetailQuartzJob;
import vn.ssdc.vnpt.alarm.model.AlarmQuartzJob;
import vn.ssdc.vnpt.alarm.model.MonitoringQuartzJob;
import vn.ssdc.vnpt.logging.model.LoggingUserQuartzJob;

import java.util.Date;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import vn.ssdc.vnpt.alarm.model.ConsumeGraphQuaztJob;

/**
 * Created by THANHLX on 6/29/2017.
 */
@Component
public class QuartzJobLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(QuartzJobLoader.class);

    @Autowired
    private Scheduler scheduler;

    public void run(ApplicationArguments args) {
        initLoggingUserQuartz();
        //initAlarmDetailQuartz();
        //initAlarmQuartz();
        //initMonitoringQuartz();
        //initConsumeGraphJob();
        // tuanha2 run birt device;
        // initDeviceTempBirtQuartz();
    }

//    public void initDeviceTempBirtQuartz() {
//        try {
//            JobKey jobKey = new JobKey("Birt Device Job");
//            if (scheduler.getJobDetail(jobKey) != null) {
//                logger.info("Exist Birt Device Job !");
//            } else {
//                JobDetail job = JobBuilder.newJob(AlarmDetailQuartzJob.class).withIdentity("Birt Device Job").build();
//                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Birt Device Trigger")
//                        .startNow()
//                        .withSchedule(simpleSchedule().withIntervalInHours(12).repeatForever())
//                        .build();
//                scheduler.scheduleJob(job, trigger);
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }

    public void initLoggingUserQuartz() {
        try {
            JobKey jobKey = new JobKey("Logging User Job");
            if (scheduler.getJobDetail(jobKey) != null) {
                logger.info("Exist logging user quartz job");
            } else {
                Date startDate = new Date();
                JobDetail job = JobBuilder.newJob(LoggingUserQuartzJob.class).withIdentity("Logging User Job").build();
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Logging User Trigger")
                        .startAt(startDate)
                        .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void initAlarmDetailQuartz() {
        try {
            JobKey jobKey = new JobKey("Alarm Detail Job");
            if (scheduler.getJobDetail(jobKey) != null) {
                logger.info("Exist alarm detail quartz job");
            } else {
                JobDetail job = JobBuilder.newJob(AlarmDetailQuartzJob.class).withIdentity("Alarm Detail Job").build();
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Alarm Detail Trigger")
                        .startNow()
                        .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void initAlarmQuartz() {
        try {
            JobKey jobKey = new JobKey("Alarm Job");
            if (scheduler.getJobDetail(jobKey) != null) {
                logger.info("Exist alarm quartz job");
            } else {
                JobDetail job = JobBuilder.newJob(AlarmQuartzJob.class).withIdentity("Alarm Job").build();
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Alarm Trigger")
                        .startNow()
                        .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void initMonitoringQuartz() {
        try {
            JobKey jobKey = new JobKey("Monitoring Job");
            if (scheduler.getJobDetail(jobKey) != null) {
                logger.info("Exist monitoring quartz job");
            } else {
                JobDetail job = JobBuilder.newJob(MonitoringQuartzJob.class).withIdentity("Monitoring Job").build();
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Monitoring Trigger")
                        .startNow()
                        .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void initConsumeGraphJob() {
        try {
            JobKey jobKey = new JobKey("ConsumeGraph Job");
            if (scheduler.getJobDetail(jobKey) != null) {
                logger.info("Exist consume quartz job");
            } else {
                JobDetail job = JobBuilder.newJob(ConsumeGraphQuaztJob.class).withIdentity("ConsumeGraph Job").build();
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("ConsumeGraph Trigger")
                        .startNow()
                        .withSchedule(simpleSchedule().withIntervalInSeconds(60).repeatForever())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
