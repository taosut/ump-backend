/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.alarm.model;

import java.util.Set;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

/**
 *
 * @author Admin
 */
public class AlarmGraphs extends SsdcEntity<Long> {

    public String deviceGroups;
    public long startDate;
    public long endDate;
    public String severity;
    public String alarmTypeName;
    public int total;

    public AlarmGraphs() {
        total = 0;
    }

}
