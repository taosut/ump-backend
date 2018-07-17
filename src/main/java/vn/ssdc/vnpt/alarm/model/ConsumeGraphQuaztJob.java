package vn.ssdc.vnpt.alarm.model;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import vn.ssdc.vnpt.alarm.services.AlarmELKService;

/**
 * Created by THANHLX on 6/29/2017.
 */
public class ConsumeGraphQuaztJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(ConsumeGraphQuaztJob.class);

    @Autowired
    public AlarmELKService alarmElkService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (jobExecutionContext.getPreviousFireTime() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(jobExecutionContext.getPreviousFireTime());
                cal.add(Calendar.MINUTE, -2);
                Date fromDate = cal.getTime();
                String fromDateTime = df.format(fromDate);

                cal.setTime(jobExecutionContext.getFireTime());
                cal.add(Calendar.MINUTE, -2);
                Date endDate = cal.getTime();
                String endDateTime = df.format(endDate);
                logger.info("#CONSUME_ALARM_JOB.................." + fromDateTime + " - " + endDateTime);
                alarmElkService.consumeAlarmData(fromDateTime, endDateTime);
            }

        } catch (ParseException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

//            alarmTypeService.refreshParameter(alarmTypeId);
    }
}
