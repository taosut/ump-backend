package vn.ssdc.vnpt.quartz.services;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.QuartzJobLoader;
import vn.ssdc.vnpt.quartz.model.QrtzTriggers;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Admin on 11/9/2017.
 */
@Service
public class QrtzTriggersService extends SsdcCrudService<String, QrtzTriggers> {
    private static final Logger logger = LoggerFactory.getLogger(QrtzTriggersService.class);
    QuartzJobLoader quartzJobLoader;

    @Autowired
    public QrtzTriggersService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(QrtzTriggers.class);
    }

    public void deleteQuartzJob(String strJob) throws SchedulerException {
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        JobKey jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);
    }

    public void deleteTriger(String strTrigger) throws SchedulerException {
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        TriggerKey triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);
    }

    public void restartTrigger(String strTriggerName){
        if(strTriggerName.equals("Monitoring Trigger")){
            quartzJobLoader.initMonitoringQuartz();
        }
        if(strTriggerName.equals("Alarm Detail Trigger")){
            quartzJobLoader.initAlarmDetailQuartz();
        }
        if(strTriggerName.equals("Logging User Trigger")){
            quartzJobLoader.initLoggingUserQuartz();
        }
        if(strTriggerName.equals("Alarm Trigger")){
            quartzJobLoader.initAlarmQuartz();
        }
        if(strTriggerName.equals("ConsumeGraph Trigger")){
            quartzJobLoader.initConsumeGraphJob();
        }
    }

    public List<QrtzTriggers> connectionToQuartzDB() throws SchedulerException {
        List<QrtzTriggers> lstReturn = new ArrayList<>();

        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        List<String> groups = scheduler.getTriggerGroupNames();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        for (String group : groups) {
            @SuppressWarnings("unchecked")
            GroupMatcher<TriggerKey> groupMatcher = GroupMatcher.groupEquals(group);
            Set<TriggerKey> keys = scheduler.getTriggerKeys(groupMatcher);
            for (TriggerKey key : keys) {
                QrtzTriggers qrtzTriggers = new QrtzTriggers();
                Trigger trigger = scheduler.getTrigger(key);
                qrtzTriggers.triggerName = key.getName().toString();
                qrtzTriggers.triggerGroup = key.getGroup();
                qrtzTriggers.jobName = trigger.getJobKey().getName();
                qrtzTriggers.jobGroup = trigger.getJobKey().getGroup();
                if(trigger.getNextFireTime() != null) {
                    qrtzTriggers.nextFireTime = sdf.format(trigger.getNextFireTime()).toString();
                }
                if(trigger.getPreviousFireTime() != null) {
                    qrtzTriggers.prevFireTime = sdf.format(trigger.getPreviousFireTime()).toString();
                }
                qrtzTriggers.triggerState = scheduler.getTriggerState(key).toString();
                qrtzTriggers.startTime = sdf.format(trigger.getStartTime()).toString();
                if(trigger.getEndTime()!=null){
                    if(trigger.getEndTime().toString()!="0"){
                        qrtzTriggers.endTime = sdf.format(trigger.getEndTime()).toString();
                    }
                }else{
                    qrtzTriggers.endTime = "0";
                }
                lstReturn.add(qrtzTriggers);
            }
        }
        return lstReturn;
    }



}
