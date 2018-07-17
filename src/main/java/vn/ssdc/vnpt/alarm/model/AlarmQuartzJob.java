package vn.ssdc.vnpt.alarm.model;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import vn.ssdc.vnpt.alarm.services.AlarmService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Admin on 6/28/2017.
 */
public class AlarmQuartzJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(AlarmQuartzJob.class);

    @Autowired
    public AlarmService alarmService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            if (jobExecutionContext.getPreviousFireTime() != null) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (jobExecutionContext.getPreviousFireTime() != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(jobExecutionContext.getPreviousFireTime());
                    cal.add(Calendar.SECOND, -10);
                    Date fromDate = cal.getTime();
                    String fromDateTime = df.format(fromDate);

                    cal.setTime(jobExecutionContext.getFireTime());
                    cal.add(Calendar.SECOND, -10);
                    Date endDate = cal.getTime();
                    String endDateTime = df.format(endDate);
                    logger.info("#ALARM_QUARTZ: " + fromDateTime + " - " + endDateTime);
                    alarmService.processAlarm(fromDateTime, endDateTime);
                }
            }
        } catch (ParseException e) {
            logger.error("AlarmQuartzJob execute", e);
        }
    }
}
