/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.kafka.model;

import com.google.gson.Gson;
import vn.ssdc.vnpt.qos.model.QosKpi;

/**
 *
 * @author kiendt
 */
public class QosKpiKafka {

    public static final String TYPE_CREATE = "CREATE";
    public static final String TYPE_UPDATE = "UPDATE";
    public static final String TYPE_DELETE = "DELETE";

    public String type;
    public QosKpi qosKpi;

    public QosKpiKafka(String type, QosKpi qosKpi) {
        this.type = type;
        this.qosKpi = qosKpi;
    }

    public String toMessage() {
        return new Gson().toJson(this);
    }

}
