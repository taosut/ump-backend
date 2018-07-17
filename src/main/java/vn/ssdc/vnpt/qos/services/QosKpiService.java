/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.services;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.kafka.model.QosKpiKafka;
import vn.ssdc.vnpt.kafka.services.KafkaService;
import vn.ssdc.vnpt.qos.model.QosKpi;
import vn.ssdc.vnpt.qos.model.searchForm.QosKPISearchForm;
import vn.ssdc.vnpt.umpexception.QosException;
import vn.ssdc.vnpt.utils.CommonService;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.exceptions.EntityNotFoundException;

/**
 *
 * @author kiendt
 */
@Service
public class QosKpiService extends SsdcCrudService<Long, QosKpi> {

    @Autowired
    public QosKpiService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(QosKpi.class);
    }

    @Autowired
    private TagService tagService;

    @Autowired
    private QosGraphService qosGraphService;

    @Autowired
    private CommonService baseService;

    @Autowired
    private KafkaService kafkaService;

    public List<QosKpi> getByProfileId(Long profileId) {
        String whereExp = "profile_id=? ";
        List<QosKpi> qosKpis = this.repository.search(whereExp, profileId);
        return qosKpis;
    }

    public List<QosKpi> getForDashboard() {
        String whereExp = "kpi_position is not null ORDER BY kpi_position";
        List<QosKpi> qosKpis = this.repository.search(whereExp);
        return qosKpis;
    }

    public List<QosKpi> getForSingleDevice(String strIn) {
        String whereExp = "profile_id in (" + strIn + ")";
        List<QosKpi> qosKpis = this.repository.search(whereExp);
        return qosKpis;
    }

    /**
     * do Search
     *
     * @param searchForm
     * @return
     */
    public List<QosKpi> search(QosKPISearchForm searchForm) {
        List<QosKpi> qosKpis = new ArrayList<>();
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
        return qosKpis;
    }

    /**
     * do search absolute
     *
     * @param searchForm
     * @return
     */
    public List<QosKpi> searchAbsolute(QosKPISearchForm searchForm) {
        List<QosKpi> qosKpis = new ArrayList<>();
        Set<String> conditions = generateConditionForSearchAbsolute(searchForm);
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
        return qosKpis;
    }

    /**
     * count total with search condition
     *
     * @param searchForm
     * @return
     */
    public int count(QosKPISearchForm searchForm) {
        searchForm.limit = null;
        searchForm.page = null;
        List<QosKpi> qosKpis = search(searchForm);
        return qosKpis.isEmpty() ? 0 : qosKpis.size();
    }

    /**
     * generate query from search object
     *
     * @param searchForm
     * @return
     */
    private Set<String> generateConditionForSearch(QosKPISearchForm searchForm) {
        Set<String> conditions = new HashSet<>();
        if (!Strings.isNullOrEmpty(searchForm.kpiIndex)) {
            conditions.add(String.format("kpi_index like '%s'", baseService.generateSearchLikeInput(searchForm.kpiIndex)));
        }
        if (!Strings.isNullOrEmpty(searchForm.kpiType)) {
            conditions.add(String.format("kpi_type like '%s'", baseService.generateSearchLikeInput(searchForm.kpiType)));
        }
        if (!Strings.isNullOrEmpty(searchForm.kpiValue)) {
            conditions.add(String.format("kpi_value like '%s'", baseService.generateSearchLikeInput(searchForm.kpiValue)));
        }

        if (searchForm.profileId != null) {
            conditions.add(String.format("profile_id = %s", searchForm.profileId));
        }
        return conditions;
    }

    /**
     * generate query from search object
     *
     * @param searchForm
     * @return
     */
    private Set<String> generateConditionForSearchAbsolute(QosKPISearchForm searchForm) {
        Set<String> conditions = new HashSet<>();
        if (!Strings.isNullOrEmpty(searchForm.kpiIndex)) {
            conditions.add(String.format("kpi_index = '%s'", searchForm.kpiIndex));
        }
        if (!Strings.isNullOrEmpty(searchForm.kpiType)) {
            conditions.add(String.format("kpi_type like '%s'", baseService.generateSearchLikeInput(searchForm.kpiType)));
        }
        if (!Strings.isNullOrEmpty(searchForm.kpiValue)) {
            conditions.add(String.format("kpi_value like '%s'", baseService.generateSearchLikeInput(searchForm.kpiValue)));
        }

        if (searchForm.profileId != null) {
            conditions.add(String.format("profile_id = %s", searchForm.profileId));
        }
        return conditions;
    }

    /**
     * validate input qosKpi
     *
     * @param qosKpi
     * @param query
     * @param index
     * @param limit
     * @return qos_kpi_profile_not_exist qos_kpi_type_invalid_format
     * qos_kpi_value_not_allow_null qos_kpi_index_not_allow_null
     * qos_kpi_index_invalid_length qos_kpi_index_duplicate_name
     */
    public void validate(QosKpi qosKpi) throws QosException {
        try {
            if (tagService.get(qosKpi.profileId) == null) {
                throw new QosException("qos_kpi_profile_not_exist");
            }
        } catch (EntityNotFoundException e) {
            throw new QosException("qos_kpi_profile_not_exist");
        }

//        List<String> KpiTypeAllows = Arrays.asList(new String[]{"Device", "Device Group"});
//        if (!baseService.validateAllowValue(qosKpi.kpiType, KpiTypeAllows)) {
//            throw new QosException("qos_kpi_type_invalid_format");
//        }
        if (!baseService.validateNotNullString(qosKpi.kpiValue)) {
            throw new QosException("qos_kpi_value_not_allow_null");
        }
        if (!baseService.validateNotNullString(qosKpi.kpiIndex)) {
            throw new QosException("qos_kpi_index_not_allow_null");
        }

        if (!baseService.validateLengthInput(qosKpi.kpiIndex, 1, 100)) {
            throw new QosException("qos_kpi_index_invalid_length");
        }
        Set<String> conditions = new HashSet<>();
        if (!Strings.isNullOrEmpty(qosKpi.kpiIndex)) {
            conditions.add(String.format("kpi_index = '%s'", qosKpi.kpiIndex));
        }
        if (qosKpi.profileId != null) {
            conditions.add(String.format("profile_id = %s", qosKpi.profileId));
        }
        if (qosKpi.id != null) {
            conditions.add(String.format("id != %s", qosKpi.id));
        }
        String query = String.join(" AND ", conditions);
        if (!findByQuery(query).isEmpty()) {
            throw new QosException("qos_kpi_index_duplicate_name");
        }
    }

    public List<QosKpi> findByQuery(String query, Integer index, Integer limit) {
        return this.repository.search(query, new PageRequest(index, limit)).getContent();
    }

    public Page<QosKpi> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }

    public List<QosKpi> findByQuery(String query) {
        return this.repository.search(query);
    }

    @Override
    public void beforeUpdate(Long id, QosKpi entity) {
        entity.standardObject();
        validate(entity);
    }

    @Override
    public void beforeCreate(QosKpi entity) {
        entity.standardObject();
        validate(entity);
    }

    @Override
    public void afterDelete(QosKpi entity) {
        sendMessage(new QosKpiKafka(QosKpiKafka.TYPE_DELETE, entity));
    }

    @Override
    public void afterUpdate(QosKpi oldEntity, QosKpi newEntity) {
        sendMessage(new QosKpiKafka(QosKpiKafka.TYPE_UPDATE, newEntity));
    }

    @Override
    public void afterCreate(QosKpi entity) {
        sendMessage(new QosKpiKafka(QosKpiKafka.TYPE_CREATE, entity));
    }

    private void sendMessage(QosKpiKafka qosKafka) {
        kafkaService.sendToQosKpiTopic(qosKafka.toMessage());
    }

    public List<QosKpi> findByProfileIds(Set<Long> profileIds) {
        Set<String> ids = new HashSet<>();
        for (Long id : profileIds) {
            ids.add(String.valueOf(id));
        }
        String input = String.join(",", ids);
        String query = String.format("profile_id in (%s)", input);
        return findByQuery(query);
    }

}
