/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.model;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

/**
 *
 * @author kiendt
 */
public class QosGraph extends SsdcEntity<Long> {

    public String graphName;
    public String graphBy;
    public String graphType;
    public Set<Long> graphIndex;
    public Long graphPosition;
    @ApiModelProperty(example = "time by minutes")
    public Long autoRefresh;
    public Long profileId;
    public String graphPeriod;
    public Set<String> graphFormula;

    public void standardObject() {
        if (!Strings.isNullOrEmpty(graphName)) {
            this.graphName = this.graphName.trim();
        }
        if (!Strings.isNullOrEmpty(graphBy)) {
            this.graphBy = this.graphBy.trim();
        }
        if (!Strings.isNullOrEmpty(graphType)) {
            this.graphType = this.graphType.trim();
        }
    }

}
