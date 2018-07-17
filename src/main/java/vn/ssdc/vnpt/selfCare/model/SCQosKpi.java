/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import com.google.common.base.Strings;
import java.util.Set;
import vn.ssdc.vnpt.qos.model.QosKpi;
import vn.ssdc.vnpt.qos.model.QosKpiThreshold;

/**
 *
 * @author kiendt
 */
public class SCQosKpi {

    public Long kpiId;
    public String kpiIndex;
    public String kpiValue;
    public String kpiType;
    public String kpiMeasure;
    public Long profileId;

    public Set<QosKpiThreshold> kpiThreshold;
    public Set<String> kpiFormula;

    public Long kpiPosition;

    private String kpiValueType;

    public SCQosKpi() {
    }

    public SCQosKpi(QosKpi qosKpi) {
        this.kpiId = qosKpi.id;
        this.kpiIndex = qosKpi.kpiIndex;
        this.kpiValue = qosKpi.kpiValue;
        this.kpiType = qosKpi.kpiType;
        this.kpiMeasure = qosKpi.kpiMeasure;
        this.profileId = qosKpi.profileId;
        this.kpiThreshold = qosKpi.kpiThreshold;
        this.kpiFormula = qosKpi.kpiFormula;
        this.kpiPosition = qosKpi.kpiPosition;
        this.kpiValueType = qosKpi.getKpiChartType();
    }

    public void standardObject() {
        if (!Strings.isNullOrEmpty(kpiIndex)) {
            this.kpiIndex = this.kpiIndex.trim();
        }
        if (!Strings.isNullOrEmpty(kpiValue)) {
            this.kpiValue = this.kpiValue.trim();
        }
        if (!Strings.isNullOrEmpty(kpiType)) {
            this.kpiType = this.kpiType.trim();
        }
        if (!Strings.isNullOrEmpty(kpiMeasure)) {
            this.kpiMeasure = this.kpiMeasure.trim();
        }
    }

    public QosKpi convertToQosKpi() {
        QosKpi qosKpi = new QosKpi();
        qosKpi.id = this.kpiId;
        qosKpi.kpiIndex = this.kpiIndex;
        qosKpi.kpiValue = this.kpiValue;
        qosKpi.kpiType = this.kpiType;
        qosKpi.kpiMeasure = this.kpiMeasure;
        qosKpi.profileId = this.profileId;
        qosKpi.kpiFormula = this.kpiFormula;
        qosKpi.kpiThreshold = this.kpiThreshold;
        qosKpi.kpiPosition = this.kpiPosition;
        qosKpi.setKpiChartType();
        return qosKpi;
    }

}
