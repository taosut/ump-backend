/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import java.util.List;
import java.util.Set;
import vn.ssdc.vnpt.qos.model.QosGraph;

/**
 *
 * @author kiendt
 */
public class SCQosGraph {

    public Long graphId;
    public String graphName;
    public String graphBy;
    public String graphType;
    public Set<Long> graphIndex;
    public Long graphPosition;
    public Long autoRefresh;
    public Long profileId;
    public String graphPeriod;
    public List<List<String>> graphData;

    public Set<String> graphFormula;

    public SCQosGraph() {
    }

    public SCQosGraph(QosGraph qosGraph) {
        this.graphId = qosGraph.id;
        this.graphName = qosGraph.graphName;
        this.graphBy = qosGraph.graphBy;
        this.graphType = qosGraph.graphType;
        this.graphIndex = qosGraph.graphIndex;
        this.graphPosition = qosGraph.graphPosition;
        this.autoRefresh = qosGraph.autoRefresh;
        this.profileId = qosGraph.profileId;
        this.graphPeriod = qosGraph.graphPeriod;
        this.graphFormula = qosGraph.graphFormula;
    }

    public QosGraph convertToQosGraph() {
        QosGraph qosKpi = new QosGraph();
        qosKpi.id = this.graphId;
        qosKpi.graphName = this.graphName;
        qosKpi.graphBy = this.graphBy;
        qosKpi.graphType = this.graphType;
        qosKpi.graphIndex = this.graphIndex;
        qosKpi.graphPosition = this.graphPosition;
        qosKpi.autoRefresh = this.autoRefresh;
        qosKpi.profileId = this.profileId;
        qosKpi.graphPeriod = this.graphPeriod;
        qosKpi.graphFormula = this.graphFormula;
        return qosKpi;
    }

}
