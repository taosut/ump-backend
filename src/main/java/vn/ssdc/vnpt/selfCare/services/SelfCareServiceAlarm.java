package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.alarm.model.AlarmType;
import vn.ssdc.vnpt.alarm.services.AlarmTypeService;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.selfCare.model.SCAlarmSetting;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmSettingSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCPolicySearchForm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by THANHLX on 12/11/2017.
 */
@Service
public class SelfCareServiceAlarm {

    @Autowired
    AlarmTypeService alarmTypeService;

    @Autowired
    DeviceGroupService deviceGroupService;

    public SCAlarmSetting create(long deviceGroupId, SCAlarmSetting scAlarmSetting) {
        AlarmType alarmType = new AlarmType();
        alarmType = convert(scAlarmSetting, alarmType);
        alarmType.deviceGroups.add(deviceGroupService.get(deviceGroupId));
        alarmType = alarmTypeService.create(alarmType);
        try {
            if (alarmType.notification != null && alarmType.notification == 1) {
                alarmTypeService.createQuartzJob(alarmType.id, alarmType.timeSettings);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return convertToSCAlarmSetting(alarmType);
    }

    public SCAlarmSetting update(Long id, SCAlarmSetting scAlarmSetting) {
        AlarmType alarmType = alarmTypeService.get(id);
        alarmType = alarmTypeService.update(id, convert(scAlarmSetting, alarmType));
        try {
            alarmTypeService.deleteQuartzJob(id);
            alarmTypeService.deleteTriger(id);
            if (alarmType.notification != null && alarmType.notification == 1) {
                alarmTypeService.createQuartzJob(alarmType.id, alarmType.timeSettings);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return convertToSCAlarmSetting(alarmType);
    }

    public List<SCAlarmSetting> search(SCAlarmSettingSearchForm scAlarmSettingSearchForm) {
        List<AlarmType> listAlarmTypes = new ArrayList<>();
        String whereExp = generateQuery(scAlarmSettingSearchForm);
        if (scAlarmSettingSearchForm.page == null || scAlarmSettingSearchForm.limit == null) {
            listAlarmTypes = !Strings.isNullOrEmpty(whereExp) ? alarmTypeService.searchByQuery(whereExp) : alarmTypeService.getAll();
        } else {
            listAlarmTypes = !Strings.isNullOrEmpty(whereExp) ? alarmTypeService.search(scAlarmSettingSearchForm.page - 1, scAlarmSettingSearchForm.limit, whereExp) : alarmTypeService.search(scAlarmSettingSearchForm.page - 1, scAlarmSettingSearchForm.limit);
        }
        List<SCAlarmSetting> listSCAlarmSettings = new ArrayList<>();
        for (AlarmType alarmType : listAlarmTypes) {
            listSCAlarmSettings.add(convertToSCAlarmSetting(alarmType));
        }
        return listSCAlarmSettings;
    }

    public int count(SCAlarmSettingSearchForm scAlarmSettingSearchForm) {
        String whereExp = generateQuery(scAlarmSettingSearchForm);
        return (int) alarmTypeService.countByQuery(whereExp);
    }

    public String generateQuery(SCAlarmSettingSearchForm scAlarmSettingSearchForm) {
        Set<String> listQueries = new HashSet<>();
        if (scAlarmSettingSearchForm.deviceGroupId != null) {
            listQueries.add("device_groups like '%" + scAlarmSettingSearchForm.deviceGroupId + "%'");
        }
        if (scAlarmSettingSearchForm.name != null) {
            listQueries.add("name like '%" + scAlarmSettingSearchForm.name + "%'");
        }
        if (scAlarmSettingSearchForm.severity != null) {
            listQueries.add("severity = '" + scAlarmSettingSearchForm.severity + "'");
        }
        if (scAlarmSettingSearchForm.type != null) {
            listQueries.add("type = '" + scAlarmSettingSearchForm.type + "'");
        }
        if (scAlarmSettingSearchForm.isNotified != null) {
            if (scAlarmSettingSearchForm.isNotified) {
                listQueries.add("notify = 1");
            } else {
                listQueries.add("notify = 0");
            }
        }
        if (scAlarmSettingSearchForm.isMonitored != null) {
            if (scAlarmSettingSearchForm.isMonitored) {
                listQueries.add("monitor = 1");
            } else {
                listQueries.add("monitor = 0");
            }
        }
        if (scAlarmSettingSearchForm.notifyAggregated != null) {
            listQueries.add("notify_aggregated = '" + scAlarmSettingSearchForm.notifyAggregated + "'");
        }
        if (scAlarmSettingSearchForm.monitoringType != null) {
            switch (scAlarmSettingSearchForm.monitoringType) {
                case "Changes":
                    listQueries.add("notification = 2");
                    break;
                case "Passive":
                    listQueries.add("notification = 3");
                    break;
                default:
                    listQueries.add("notification = 1");
                    break;
            }
        }
        return String.join(" AND ", listQueries);
    }

    public AlarmType convert(SCAlarmSetting scAlarmSetting, AlarmType alarmType) {
        alarmType.id = scAlarmSetting.id;
        alarmType.type = scAlarmSetting.type;
        alarmType.name = scAlarmSetting.name;
        alarmType.severity = scAlarmSetting.severity;
        alarmType.notify = scAlarmSetting.isNotified;
        alarmType.monitor = scAlarmSetting.isMonitored;
        alarmType.aggregatedVolume = (int) scAlarmSetting.aggregatedVolume;
        alarmType.notifyAggregated = scAlarmSetting.notifyAggregated;
        alarmType.parameterValues = scAlarmSetting.parameterValues;
        if (scAlarmSetting.monitoringType != null) {
            switch (scAlarmSetting.monitoringType) {
                case "Changes":
                    alarmType.notification = 2;
                    break;
                case "Passive":
                    alarmType.notification = 3;
                    break;
                default:
                    alarmType.notification = 1;
                    break;
            }
        }
        if (alarmType.deviceGroups == null) {
            alarmType.deviceGroups = new HashSet<>();
        }
        alarmType.timeSettings = scAlarmSetting.intervalTime;
        return alarmType;
    }

    public SCAlarmSetting convertToSCAlarmSetting(AlarmType alarmType) {
        SCAlarmSetting scAlarmSetting = new SCAlarmSetting();
        scAlarmSetting.id = alarmType.id;
        scAlarmSetting.type = alarmType.type;
        scAlarmSetting.name = alarmType.name;
        scAlarmSetting.severity = alarmType.severity;
        scAlarmSetting.isNotified = alarmType.notify;
        scAlarmSetting.isMonitored = alarmType.monitor;
        scAlarmSetting.aggregatedVolume = (int) alarmType.aggregatedVolume;
        scAlarmSetting.notifyAggregated = alarmType.notifyAggregated;
        scAlarmSetting.parameterValues = alarmType.parameterValues;
        if (alarmType.notification != null) {
            switch (alarmType.notification) {
                case 2:
                    scAlarmSetting.monitoringType = "Changes";
                    break;
                case 3:
                    scAlarmSetting.monitoringType = "Passive";
                    break;
                default:
                    scAlarmSetting.monitoringType = "Realtime";
                    break;
            }
        }
        scAlarmSetting.intervalTime = alarmType.timeSettings;
        return scAlarmSetting;
    }
}
