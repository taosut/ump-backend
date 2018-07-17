package vn.ssdc.vnpt.alarm.services;

import io.searchbox.client.JestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.alarm.model.AlarmDetails;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.List;

/**
 * Created by Admin on 6/8/2017.
 */
@Service
public class AlarmDetailsService extends SsdcCrudService<Long, AlarmDetails> {

    private static final Logger logger = LoggerFactory.getLogger(AlarmDetailsService.class);

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    public AlarmDetailsService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(AlarmDetails.class);
    }

    public List<AlarmDetails> findByMonitoring(Long alarmTypeId, String fromDate, String toDate) {
        String whereExp = " alarm_type_id = ? and created >= ? and created < ? ";
        List<AlarmDetails> lstAlarmDetail = this.repository.search(whereExp, alarmTypeId, fromDate, toDate);
        return lstAlarmDetail;
    }

    public List<AlarmDetails> getAlarmDetailById(Long alarmTypeId) {
        List<AlarmDetails> alarmDetailsList = this.repository.search("alarm_type_id = ?", alarmTypeId);
        return alarmDetailsList;
    }

    public List<AlarmDetails> checkAlarmDetailExits(long alarmTypeId, String deviceId, long raised) {
        List<AlarmDetails> alarmDetailsList = this.repository.search("alarm_type_id = ? "
                + " and device_id = ? and raised = ? ", alarmTypeId, deviceId, raised);
        return alarmDetailsList;
    }

}
