package vn.ssdc.vnpt.devices.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceType;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.vnpt.ssdc.core.ObjectCache;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import javax.ws.rs.NotFoundException;
import java.util.*;

/**
 * Created by vietnq on 11/1/16.
 */
@Service
public class DeviceTypeService extends SsdcCrudService<Long, DeviceType> {
    private static final Logger logger = LoggerFactory.getLogger(DeviceTypeService.class);

    @Autowired
    public DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private ObjectCache ssdcCache;

    @Autowired
    public DeviceTypeService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(DeviceType.class);
    }

    public DeviceType findByPk(String manufacturer, String oui, String productClass) {
        String whereExp = "manufacturer=? and oui=? and product_class=?";
        List<DeviceType> deviceTypes = this.repository.search(whereExp, manufacturer, oui, productClass);
        if (!ObjectUtils.empty(deviceTypes)) {
            return deviceTypes.get(0);
        }
        return null;
    }

    public DeviceType findByPk(String oui, String productClass) {
        String cacheId = "device-type-" + oui + "-" + productClass;
        DeviceType result;
        try {
            result = (DeviceType) ssdcCache.get(cacheId, DeviceType.class);
            if (result != null) {
                return result;
            } else {
                String whereExp = "oui=? and product_class=?";
                List<DeviceType> deviceTypes = this.repository.search(whereExp, oui, productClass);
                if (!ObjectUtils.empty(deviceTypes)) {
                    result = deviceTypes.get(0);
                    ssdcCache.put(cacheId, result, DeviceType.class);
                    return result;
                }
            }
        }
        catch(Exception e) {
            String whereExp = "oui=? and product_class=?";
            List<DeviceType> deviceTypes = this.repository.search(whereExp, oui, productClass);
            if (!ObjectUtils.empty(deviceTypes)) {
                result = deviceTypes.get(0);
                ssdcCache.put(cacheId, result, DeviceType.class);
            }
        }
        return null;
    }


    @Override
    public void beforeDelete(Long id) {
        List<DeviceTypeVersion> deviceTypeVersions = deviceTypeVersionService.findByDeviceType(id);
        if (!ObjectUtils.empty(deviceTypeVersions) && !deviceTypeVersions.isEmpty()) {
            throw new NotFoundException("Object is used.");
        }
    }

    @Override
    public void afterDelete(DeviceType deviceType){
        String cacheId = "device-type-" + deviceType.oui + "-" + deviceType.productClass;
        ssdcCache.remove(cacheId, DeviceType.class);
    }

    public boolean isExisted(Long id, String name, String manufacturer, String oui, String productClass) {
        String whereExp = "id<>? AND name=? AND manufacturer=? AND oui=? AND product_class=?";
        List<DeviceType> deviceTypes = this.repository.search(whereExp, id, name, manufacturer, oui, productClass);
        return !ObjectUtils.empty(deviceTypes) && deviceTypes.isEmpty();
    }

    public List<DeviceType> findByManufacturerAndModelName(String manufacturer, String modelName) {
        List<DeviceType> deviceTypes = new ArrayList<DeviceType>();
        deviceTypes = this.repository.search("model_name=? and manufacturer=?", modelName,manufacturer);
        return deviceTypes;
    }
}
