package vn.ssdc.vnpt.performance.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.performance.sevices.PerformanceSettingService;
import vn.ssdc.vnpt.utils.StringUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by thangnc on 27-Jun-17.
 */
public class PerformanceQuartzJob implements Job {

    public static final Logger logger = LoggerFactory.getLogger(PerformanceQuartzJob.class);
    public static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PerformanceSettingService performanceSettingService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if(jobExecutionContext.getPreviousFireTime() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(jobExecutionContext.getPreviousFireTime());
            cal.add(Calendar.SECOND, -10);
            Date fromDate = cal.getTime();
            String fromDateTime = df.format(fromDate);

            cal.setTime(jobExecutionContext.getFireTime());
            cal.add(Calendar.SECOND, -10);
            Date endDate = cal.getTime();
            String endDateTime = df.format(endDate);

            JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();
            Long performanceJobId = jdm.getLong("performanceJobId");
            logger.info("#PERFORMANCE_STATISTICS_QUARTZ: " + fromDateTime + " - " + endDateTime);
            performanceSettingService.statiticsData(performanceJobId, fromDateTime, endDateTime);
        }
    }
}
