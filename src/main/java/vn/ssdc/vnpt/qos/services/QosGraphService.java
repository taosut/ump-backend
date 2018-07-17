/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.services;

import com.google.common.base.Strings;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.qos.model.QosKpiDataELK;
import vn.ssdc.vnpt.qos.model.QosGraph;
import vn.ssdc.vnpt.qos.model.QosKpi;
import vn.ssdc.vnpt.qos.model.searchForm.QosGraphDataSearchForm;
import vn.ssdc.vnpt.qos.model.searchForm.QosGraphSearchForm;
import vn.ssdc.vnpt.umpexception.QosException;
import vn.ssdc.vnpt.utils.CommonService;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.exceptions.EntityNotFoundException;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

/**
 *
 * @author kiendt
 */
@Service
public class QosGraphService extends SsdcCrudService<Long, QosGraph> {

    private static final Logger logger = LoggerFactory.getLogger(QosGraphService.class);

    @Autowired
    private QosELKService qosELKService;

    @Autowired
    public QosGraphService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(QosGraph.class);
    }

    @Autowired
    private TagService tagService;

    @Autowired
    private QosKpiService qosKpiService;

    @Autowired
    private CommonService baseService;

    public List<QosGraph> getByProfileId(Long profileId) {
        String whereExp = "profile_id = ? ";
        List<QosGraph> qosKpis = this.repository.search(whereExp, profileId);
        return qosKpis;
    }

    /**
     * do Search
     *
     * @param searchForm
     * @return
     */
    public List<QosGraph> search(QosGraphSearchForm searchForm) {
        List<QosGraph> qosKpis = new ArrayList<>();
        if (!Strings.isNullOrEmpty(searchForm.deviceId)) {
            try {
                qosKpis = qosELKService.getQosGraphByDeviceId(searchForm.deviceId);
            } catch (ParseException ex) {
                logger.error("search {}", ex.getMessage());
            }
        } else {
            Set<String> conditions = generateConditionForSearch(searchForm);
            if (searchForm.limit != null && searchForm.page != null) {
                if (!conditions.isEmpty()) {
                    String query = String.join(" AND ", conditions);
                    qosKpis = findByQuery(query, searchForm.page - 1, searchForm.limit);
                } else {
                    qosKpis = getPage(searchForm.page - 1, searchForm.limit).getContent();
                }
            } else {
                if (!conditions.isEmpty()) {
                    String query = String.join(" AND ", conditions);
                    qosKpis = findByQuery(query);
                } else {
                    qosKpis = getAll();
                }
            }
        }

        return qosKpis;
    }

    /**
     * count total with search condition
     *
     * @param searchForm
     * @return
     */
    public int count(QosGraphSearchForm searchForm) {
        searchForm.limit = null;
        searchForm.page = null;
        List<QosGraph> qosKpis = search(searchForm);
        return qosKpis.isEmpty() ? 0 : qosKpis.size();
    }

    /**
     * generate query from search object
     *
     * @param searchForm
     * @return
     */
    private Set<String> generateConditionForSearch(QosGraphSearchForm searchForm) {
        Set<String> conditions = new HashSet<>();

        if (!Strings.isNullOrEmpty(searchForm.graphName)) {
            conditions.add(String.format("graph_name like '%s'", baseService.generateSearchLikeInput(searchForm.graphName)));
        }
        if (!Strings.isNullOrEmpty(searchForm.graphBy)) {
            conditions.add(String.format("graph_by like '%s'", baseService.generateSearchLikeInput(searchForm.graphBy)));
        }
        if (!Strings.isNullOrEmpty(searchForm.graphType)) {
            conditions.add(String.format("graph_type like '%s'", baseService.generateSearchLikeInput(searchForm.graphType)));
        }
        if (searchForm.profileId != null) {
            conditions.add(String.format("profile_id = %s", searchForm.profileId));
        }
        if (searchForm.graphIndex != null) {
            conditions.add(String.format("graph_index like '%s'", searchForm.graphIndex));
        }
        return conditions;
    }

    /**
     * validate input qos graph
     *
     * @param qosGraph
     * @throws QosException
     */
    public void validate(QosGraph qosGraph) throws QosException {
        try {
            if (tagService.get(qosGraph.profileId) == null) {
                throw new QosException("qos_graph_profile_not_exist");
            }
        } catch (EntityNotFoundException e) {
            throw new QosException("qos_graph_profile_not_exist");
        }

        if (!baseService.validateNotNullString(qosGraph.graphName)) {
            throw new QosException("qos_graph_name_not_allow_null");
        }
        if (!baseService.validateLengthInput(qosGraph.graphName, 1, 50)) {
            throw new QosException("qos_graph_name_invalid_length");
        }

        List<String> listTypeAllow = Arrays.asList(new String[]{"pie_chart", "column_chart", "line_chart", "get_stats", "table_list"});
        if (!baseService.validateAllowValue(qosGraph.graphType, listTypeAllow)) {
            throw new QosException("qos_graph_type_invalid_format");
        }
        List<String> listGraphByAllow = Arrays.asList(new String[]{"Device", "Device Group"});
        if (!baseService.validateAllowValue(qosGraph.graphBy, listGraphByAllow)) {
            throw new QosException("qos_graph_by_invalid_format");
        }

        if (qosGraph.graphPosition == null) {
            throw new QosException("qos_graph_position_not_allow_null");
        }
        String query = String.format("id in (%s) AND profile_id = %s", qosGraph.graphIndex.toString().replace("[", "").replace("]", ""), qosGraph.profileId);
        List<QosKpi> qosList = qosKpiService.findByQuery(query);
        if (qosList != null && qosGraph.graphIndex != null && qosList.size() != qosGraph.graphIndex.size()) {
            throw new QosException("qos_graph_index_invalid");
        }
    }

    public List<QosGraph> findByProfileId(Long profileId) {
        String query = String.format("profile_id = %s", profileId);
        return findByQuery(query);
    }

    public List<QosGraph> findByProfileIds(Set<Long> profileIds) {
        Set<String> ids = new HashSet<>();
        for (Long id : profileIds) {
            ids.add(String.valueOf(id));
        }
        String input = String.join(",", ids);
        String query = String.format("profile_id in (%s)", input);
        return findByQuery(query);
    }

    public List<QosGraph> findByQuery(String query, Integer index, Integer limit) {
        return this.repository.search(query, new PageRequest(index, limit)).getContent();
    }

    public Page<QosGraph> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }

    public List<QosGraph> findByQuery(String query) {
        return this.repository.search(query);
    }

    @Override
    public void beforeDelete(Long id) {
    }

    @Override
    public void beforeUpdate(Long id, QosGraph entity) {
        entity.standardObject();
        validate(entity);
    }

    @Override
    public void beforeCreate(QosGraph entity) {
        entity.standardObject();
        validate(entity);
    }

    SimpleDateFormat formatDateByYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat formatDateByMonth = new SimpleDateFormat("yyyy-MM");
    SimpleDateFormat formatDateByHour = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat formatDateByDay = new SimpleDateFormat("yyyy-MM-dd");

    public List<List<String>> hanldleData(QosGraph qosGraph, QosGraphDataSearchForm qosGraphDataSearchForm) {
        List<List<String>> setReturn = new ArrayList<>();
        if (qosGraph.graphPeriod.equalsIgnoreCase("d")) {
            setReturn = handleDataPerDay(qosGraphDataSearchForm, qosGraph);
        } else if (qosGraph.graphPeriod.equalsIgnoreCase("h")) {
            setReturn = handleDataPerHour(qosGraphDataSearchForm, qosGraph);
        } else if (qosGraph.graphPeriod.equalsIgnoreCase("m")) {
            setReturn = handleDataPerMonth(qosGraphDataSearchForm, qosGraph);
        } else if (qosGraph.graphPeriod.equalsIgnoreCase("y")) {
            setReturn = handleDataPerYear(qosGraphDataSearchForm, qosGraph);
        }
        return setReturn;
    }

    public List<List<String>> handleDataPerYear(QosGraphDataSearchForm qosGraphDataSearchForm, QosGraph qosGraph) {
        List<List<String>> setReturn = new ArrayList<>();
        try {
            List<Date> lstData = getTimeBetween(qosGraphDataSearchForm.fromDate, qosGraphDataSearchForm.toDate, "y");
            Set<Long> lstKpi = qosGraph.graphIndex;

            List<String> strData = new ArrayList<>();
            strData.add("Per Year");

            for (Long kpiId : lstKpi) {
                strData.add(qosKpiService.get(kpiId).kpiIndex);
            }

            setReturn.add(strData);

            for (Date date : lstData) {
                qosGraphDataSearchForm.fromDate = getDayOfTheYear(date, "min");
                qosGraphDataSearchForm.toDate = getDayOfTheYear(date, "max");
                strData = new ArrayList<>();
                strData.add(formatDateByYear.format(date));
                strData.addAll(addDataToList(lstKpi, qosGraphDataSearchForm));
                setReturn.add(strData);
            }

        } catch (Exception ex) {
            logger.error("QosGraphEndpoint error convert date : " + ex.toString());
        }

        return setReturn;
    }

    public List<List<String>> handleDataPerMonth(QosGraphDataSearchForm qosGraphDataSearchForm, QosGraph qosGraph) {
        List<List<String>> setReturn = new ArrayList<>();
        try {
            List<Date> lstData = getTimeBetween(qosGraphDataSearchForm.fromDate, qosGraphDataSearchForm.toDate, "m");
            Set<Long> lstKpi = qosGraph.graphIndex;

            List<String> strData = new ArrayList<>();
            strData.add("Per Month");

            for (Long kpiId : lstKpi) {
                strData.add(qosKpiService.get(kpiId).kpiIndex);
            }

            setReturn.add(strData);

            for (Date date : lstData) {
                qosGraphDataSearchForm.fromDate = getDayOfTheMonth(date, "min");
                qosGraphDataSearchForm.toDate = getDayOfTheMonth(date, "max");
                strData = new ArrayList<>();
                strData.add(formatDateByMonth.format(date));
                strData.addAll(addDataToList(lstKpi, qosGraphDataSearchForm));
                setReturn.add(strData);
            }

        } catch (Exception ex) {
            logger.error("QosGraphEndpoint error convert date : " + ex.toString());
        }
        return setReturn;
    }

    public Date getDayOfTheYear(Date date, String strType) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if ("min".equalsIgnoreCase(strType)) {
            c.set(Calendar.DAY_OF_YEAR, c.getActualMinimum(Calendar.DAY_OF_YEAR));
        } else if ("max".equalsIgnoreCase(strType)) {
            c.set(Calendar.DAY_OF_YEAR, c.getActualMaximum(Calendar.DAY_OF_YEAR));
        }
        Date lastDayOfMonth = c.getTime();
        return lastDayOfMonth;
    }

    public Date getDayOfTheMonth(Date date, String strType) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if ("min".equalsIgnoreCase(strType)) {
            c.set(Calendar.DAY_OF_MONTH, c.getActualMinimum(Calendar.DAY_OF_MONTH));
        } else if ("max".equalsIgnoreCase(strType)) {
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        Date lastDayOfMonth = c.getTime();
        return lastDayOfMonth;
    }

    public List<List<String>> handleDataPerHour(QosGraphDataSearchForm qosGraphDataSearchForm, QosGraph qosGraph) {
        List<List<String>> setReturn = new ArrayList<>();
        try {
            List<Date> lstData = getTimeBetween(qosGraphDataSearchForm.fromDate, qosGraphDataSearchForm.toDate, "h");
            Set<Long> lstKpi = qosGraph.graphIndex;

            List<String> strData = new ArrayList<>();
            strData.add("Per Hour");

            for (Long kpiId : lstKpi) {
                strData.add(qosKpiService.get(kpiId).kpiIndex);
            }

            setReturn.add(strData);

            for (Date date : lstData) {
                qosGraphDataSearchForm.fromDate = oneHourBack(date);
                qosGraphDataSearchForm.toDate = date;

                strData = new ArrayList<>();
                strData.add(formatDateByHour.format(date));

                strData.addAll(addDataToList(lstKpi, qosGraphDataSearchForm));

                setReturn.add(strData);
            }

        } catch (Exception ex) {
            logger.error("QosGraphEndpoint error convert date : " + ex.toString());
        }
        return setReturn;
    }

    public List<List<String>> handleDataPerDay(QosGraphDataSearchForm qosGraphDataSearchForm, QosGraph qosGraph) {
        List<List<String>> setReturn = new ArrayList<>();
        try {
            List<Date> lstDateBetween = getTimeBetween(qosGraphDataSearchForm.fromDate, qosGraphDataSearchForm.toDate, "d");
            Set<Long> lstKpi = qosGraph.graphIndex;

            List<String> strData = new ArrayList<>();
            strData.add("Per Day");

            for (Long kpiId : lstKpi) {
                strData.add(qosKpiService.get(kpiId).kpiIndex);
            }

            setReturn.add(strData);

            for (Date date : lstDateBetween) {
                qosGraphDataSearchForm.fromDate = beginOfDay(date);
                qosGraphDataSearchForm.toDate = endOfDay(date);

                strData = new ArrayList<>();
                strData.add(formatDateByDay.format(date));
                strData.addAll(addDataToList(lstKpi, qosGraphDataSearchForm));
                setReturn.add(strData);
            }
        } catch (Exception ex) {
            logger.error("QosGraphEndpoint error convert date : " + ex.toString());
        }
        return setReturn;
    }

    public List<String> addDataToList(Set<Long> lstKpi, QosGraphDataSearchForm qosGraphDataSearchForm) {
        List<String> lstReturn = new ArrayList<>();
        for (Long kpiId : lstKpi) {
            List<QosKpiDataELK> lstDataPerHour = qosELKService.getDataELKLastRecordInTime(qosGraphDataSearchForm, kpiId);
            if (!lstDataPerHour.isEmpty()) {
                lstReturn.add(lstDataPerHour.get(0).value.toString());
            } else {
                lstReturn.add(null);
            }
        }
        return lstReturn;
    }

    public Date oneHourBack(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR, -1);
        Date oneHourBack = cal.getTime();
        return oneHourBack;
    }

    public List<Date> getTimeBetween(Date startDate, Date endDate, String strType) {
        List<Date> datesInRange = new ArrayList<>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startDate);

        Calendar endCalendar = new GregorianCalendar();
        endCalendar.setTime(endDate);

        while (calendar.before(endCalendar)) {
            Date result = calendar.getTime();
            datesInRange.add(result);
            if ("h".equalsIgnoreCase(strType)) {
                calendar.add(Calendar.HOUR, 1);
            } else if ("d".equalsIgnoreCase(strType)) {
                calendar.add(Calendar.DATE, 1);
            } else if ("m".equalsIgnoreCase(strType)) {
                calendar.add(Calendar.MONTH, 1);
            } else if ("y".equalsIgnoreCase(strType)) {
                calendar.add(Calendar.YEAR, 1);
            }
        }
        if ("h".equalsIgnoreCase(strType) || "d".equalsIgnoreCase(strType)) {
            datesInRange.add(endDate);
        }
        return datesInRange;
    }

    public Date beginOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    public Date endOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        return cal.getTime();
    }

}
