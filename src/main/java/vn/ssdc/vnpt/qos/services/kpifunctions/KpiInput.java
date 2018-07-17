/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.services.kpifunctions;

import java.util.Map;

/**
 *
 * @author kiendt
 */
public class KpiInput {

    public String deviceId;
    public Map<String, String> kpiData;
    public String kpiFunction;

    public KpiInput(String deviceId, Map<String, String> kpiData, String kpiFunction) {
        this.deviceId = deviceId;
        this.kpiData = kpiData;
        this.kpiFunction = kpiFunction;
    }

}
