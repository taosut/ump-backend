package vn.ssdc.vnpt.qos.model;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author kiendt
 */
import com.google.common.base.Strings;
import java.util.Set;
import vn.ssdc.vnpt.qos.services.kpifunctions.ExpressionLogic;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

public class QosKpi extends SsdcEntity<Long> {

    public String kpiIndex;
    public String kpiValue;
    public String kpiType;
    public String kpiMeasure;
    public Long profileId;

    public Set<QosKpiThreshold> kpiThreshold;
    public Set<String> kpiFormula;

    public Long kpiPosition;

    private String kpiChartType;

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

    public String getKpiChartType() {
        return kpiChartType;
    }

    public void setKpiChartType() {
        if (!Strings.isNullOrEmpty(this.kpiValue) && this.kpiValue.contains("=IF")) {
            this.kpiChartType = "table";
        } else {
            this.kpiChartType = "line";
        }
    }

}
