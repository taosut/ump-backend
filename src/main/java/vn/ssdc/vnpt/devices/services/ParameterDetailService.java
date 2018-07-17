package vn.ssdc.vnpt.devices.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.ParameterDetail;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kiendt on 2/6/2017.
 */
@Service
public class ParameterDetailService extends SsdcCrudService<Long, ParameterDetail> {

    @Autowired
    Tr069ParameterService tr069ParameterService;

    @Autowired
    DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    TagService tagService;

    @Autowired
    public ParameterDetailService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(ParameterDetail.class);
    }

    //getAllObject with DeviceTypeVersionId
    public List<ParameterDetail> getAllObject(Long deviceTypeVersionId) {
        String whereExp = "data_type='object' and device_type_version_id=? order by path";
        List<ParameterDetail> lParameterDetail = this.repository.search(whereExp, deviceTypeVersionId);
        return lParameterDetail;
    }

    //getAllParameter with DeviceTypeVersionId and Parent Object
    public List<ParameterDetail> getAllParameter(Long deviceTypeVersionId, String strParentObject) {
        String whereExp = " parent_object=? and device_type_version_id=? order by path";
        List<ParameterDetail> lParameterDetail = this.repository.search(whereExp, strParentObject, deviceTypeVersionId);
        return lParameterDetail;
    }

    public Map<String, ParameterDetail> findByDeviceTypeVersion(Long deviceTypeVersionId) {
        StringBuilder sb = new StringBuilder("device_type_version_id=?");
        List<ParameterDetail> parameterDetailList = this.repository.search(sb.toString(), deviceTypeVersionId);
        Map<String, ParameterDetail> map = new HashMap<String, ParameterDetail>();
        for (ParameterDetail tmp : parameterDetailList) {
            map.put(tmp.path, tmp);
        }
        return map;
    }

    public Parameter convertToParameter(ParameterDetail detail) {
        Parameter parameter = new Parameter();
        parameter.shortName = detail.shortName;
        parameter.tr069Name = detail.tr069Name;
        parameter.path = detail.path;
        parameter.rule = detail.rule;
        parameter.dataType = detail.dataType;
        parameter.defaultValue = detail.defaultValue;
        parameter.access = detail.access;
        parameter.parentObject = detail.parentObject;
        parameter.instance = detail.instance;
        parameter.tr069ParentObject = detail.tr069ParentObject;
        return parameter;
    }

    /**
     * convert from map Parameter to ParameterDetail
     *
     * @param map
     * @return
     */
    public Map<String, Parameter> convertToMapParameter(Map<String, ParameterDetail> map) {
        Map<String, Parameter> mapResult = new HashMap<String, Parameter>();
        for (Map.Entry<String, ParameterDetail> entry : map.entrySet()) {
            ParameterDetail entity = entry.getValue();
            Parameter parameter = new Parameter();
            parameter.defaultValue = entity.defaultValue;
            parameter.dataType = entity.dataType;
            parameter.rule = entity.rule;
            parameter.shortName = entity.shortName;
            parameter.path = entity.path;
            parameter.tr069Name = entity.tr069Name;
            parameter.access = entity.access;
            parameter.instance = entity.instance;
            parameter.tr069ParentObject = entity.tr069ParentObject;
            parameter.parentObject = entity.parentObject;
            parameter.value = entity.getValue();
            mapResult.put(entry.getKey(), parameter);
        }
        return mapResult;
    }

    public List<ParameterDetail> findByDeviceTypeVersion2(Long deviceTypeVersionId) {
        return this.repository.search("device_type_version_id=?", deviceTypeVersionId);
    }

    public List<ParameterDetail> suggestName(List<String> listDeviceTypeVersionId) {
        List<ParameterDetail> listResult = new ArrayList<ParameterDetail>();
        for (String tmp : listDeviceTypeVersionId) {
            String query = "device_type_version_id = ?";
            listResult.addAll(this.repository.search(query, Long.valueOf(tmp)));
        }
        return listResult;
    }

    public ParameterDetail findByParams(String path, Long deviceTypeVersionId)  {

        ParameterDetail parameterDetail = null;

        String whereExp = "path=? AND device_type_version_id=?";
        List<ParameterDetail> list = this.repository.search(whereExp, path, deviceTypeVersionId);
        if (list.size() > 0) {
            parameterDetail = list.get(0);
        }

        return parameterDetail;
    }

    public List<ParameterDetail> findForProvisioning(String strPath) {
        String whereExp = " tr069_parent_object = (SELECT tr069_name FROM parameter_details WHERE path = ? LIMIT 1) + '{i}.' AND  access = 'true' ";
        List<ParameterDetail> list = this.repository.search(whereExp, strPath);
        return list;
    }

    public List<ParameterDetail> findParameters() {
        String whereExp = "data_type<>'object' AND access = 'true' GROUP BY path";
        return this.repository.search(whereExp);
    }

    public ParameterDetail getByTr069Name(String tr069Name, long deviceTypeVersionId) {
        String whereExp = "tr069_name=? and device_type_version_id=?";
        List<ParameterDetail> list = this.repository.search(whereExp, tr069Name, deviceTypeVersionId);
        if (!ObjectUtils.empty(list)) {
            return list.get(0);
        }
        return null;
    }

    public List<ParameterDetail> findByTr069name(String tr069Name) {
        String whereExp = "tr069_name=?";
        return this.repository.search(whereExp, tr069Name);
    }

    public void deleteByDeviceTypeVersionId(Long deviceTypeVersionId) {
        String whereExp = " device_type_version_id=?";
        List<ParameterDetail> parameterDetails = this.repository.search(whereExp, deviceTypeVersionId);
        if (parameterDetails.size() > 0) {
            for (ParameterDetail parameterDetail : parameterDetails) {
                this.repository.delete(parameterDetail);
            }
        }
    }

    public void deleteSingleParamerter(Long deviceTypeVersionId, String parameter) {
        String whereExp = " device_type_version_id=? and path=?";
        List<ParameterDetail> parameterDetails = this.repository.search(whereExp, deviceTypeVersionId, parameter);
        if (!parameterDetails.isEmpty()) {
            for (ParameterDetail parameterDetail : parameterDetails) {
                this.repository.delete(parameterDetail);
            }
        }
    }

    public List<ParameterDetail> findByDraytek(String deviceTypeVersionId) {
        String whereExp = "device_type_version_id = ? and access = 'true'";
        List<ParameterDetail> lParameterDetail = this.repository.search(whereExp, deviceTypeVersionId);
        return lParameterDetail;
    }

    public ParameterDetail createNewParameter(Long deviceTypeVersionId, Long tagId, String path, String defaultValue, String dataType, String access) {
        ParameterDetail parameterDetail = new ParameterDetail();
        parameterDetail.defaultValue = defaultValue;
        parameterDetail.dataType = dataType;
        parameterDetail.access = access;
        parameterDetail.deviceTypeVersionId = deviceTypeVersionId;
        parameterDetail.profile.add(String.valueOf(tagId));
        String currentPath = "";
        String[] subPaths = path.split("\\.");
        for (String subPath : subPaths) {
            currentPath += subPath;
            if (!currentPath.equals(path)) {
                // Create parent object
                currentPath += ".";
                createParentObject(deviceTypeVersionId, currentPath);
            } else {
                // Create parameter
                parameterDetail.path = path;
                parameterDetail.tr069Name = tr069ParameterService.convertToTr069Param(path);
                parameterDetail.setShortName();
                parameterDetail.setParentObject();
                parameterDetail.tr069ParentObject = tr069ParameterService.convertToTr069Param(parameterDetail.parentObject);
                parameterDetail.instance = false;
                create(parameterDetail);
                break;
            }
        }

        return parameterDetail;
    }

    private void createParentObject(Long deviceTypeVersionId, String path) {
        // Check exist
        if (findByParams(path, deviceTypeVersionId) == null) {
            ParameterDetail parameterDetail = new ParameterDetail();
            parameterDetail.access = String.valueOf(true);
            parameterDetail.deviceTypeVersionId = deviceTypeVersionId;
            parameterDetail.path = path;
            parameterDetail.tr069Name = tr069ParameterService.convertToTr069Param(path);
            parameterDetail.dataType = "object";
            parameterDetail.defaultValue = "";
            parameterDetail.setShortName();
            parameterDetail.setParentObject();
            parameterDetail.tr069ParentObject = tr069ParameterService.convertToTr069Param(parameterDetail.parentObject);
            parameterDetail.instance = parameterDetail.tr069Name.lastIndexOf(".{i}.") > -1 && parameterDetail.tr069Name.lastIndexOf(".{i}.") + ".{i}.".length() == parameterDetail.tr069Name.length();
            create(parameterDetail);
        }
    }
    

}
