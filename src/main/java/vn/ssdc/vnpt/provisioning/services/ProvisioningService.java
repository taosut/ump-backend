package vn.ssdc.vnpt.provisioning.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.*;
import vn.ssdc.vnpt.devices.services.*;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.services.SubscriberDeviceService;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.*;


@Service
public class ProvisioningService extends SsdcCrudService<Long, Tag> {


    public static final int ACTION_PRESET_CREATEORUPDATE = 1;
    public static final int ACTION_PRESET_DELETE = 0;

    private static final String KEY_PRESET = "preset_";

    @Autowired
    public AcsClient acsClient;

    @Autowired
    public DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    public DeviceTypeService deviceTypeService;

    @Autowired
    public TagService tagService;

    @Autowired
    public SubscriberDeviceService subscriberDeviceService;

    @Autowired
    public ParameterDetailService parameterDetailService;

    @Autowired
    private Tr069ParameterService tr069ParameterService;

    @Autowired
    public ProvisioningService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Tag.class);
    }

    public void createProvisioningTasks(String deviceId) {
        Map<String, String> acsQuery = new HashMap<String, String>();
        acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
        JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
        if (arrayTmpObject.size() > 0) {
            JsonObject body = arrayTmpObject.get(0).getAsJsonObject();
            JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
            String productClass = inforObject.get("_ProductClass") != null ? inforObject.get("_ProductClass").getAsString() : "";
            String oui = inforObject.get("_OUI").getAsString() != null ? inforObject.get("_OUI").getAsString() : "";
            String firmwareVersion = body.get("summary.softwareVersion") != null ? body.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString() : "";
            DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
            if (currenDeviceType != null) {
                DeviceTypeVersion currentDeviceTypeVersion = deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion);
                if (currentDeviceTypeVersion != null) {
                    List<Tag> listProvisioningTags = tagService.getProvisioningTagByDeviceTypeVersionId(currentDeviceTypeVersion.id);
                    Map<String, Object> parameterValues = new HashMap<String, Object>();
                    Map<String, Map<String, String>> mapAddObject = new HashMap<String, Map<String, String>>();
                    for (Tag provisioningTag : listProvisioningTags) {
                        Map<String, Parameter> parameterMap = provisioningTag.parameters;
                        List<String> listPath = new ArrayList<String>(provisioningTag.parameters.keySet());
                        String parameters = StringUtils.join(listPath, ",");
                        ResponseEntity<String> responseEntity = acsClient.getDevice(deviceId, parameters);
                        String responseEntityBody = responseEntity.getBody();
                        List<Device> devices = Device.fromJsonString(responseEntityBody, provisioningTag.parameters.keySet());
                        if (devices.size() > 0) {
                            Map<String, String> listParametersOfDevice = devices.get(0).parameters;
                            //List of change parameters
                            for (Map.Entry<String, Parameter> entry : parameterMap.entrySet()) {
                                String path = entry.getKey();
                                Parameter parameter = entry.getValue();
                                String valueOfDevice = listParametersOfDevice.get(path);
                                String provisioningValue = getProvisioningValue(deviceId, parameter);
                                if (provisioningValue != null) {
                                    if (valueOfDevice != null) {
                                        parameterValues.put(path, provisioningValue);
                                    }
                                    else{
                                        String tr069Path = tr069ParameterService.convertToTr069Param(path);
                                        if(tr069Path.lastIndexOf("{i}")>0) {
                                            int lastIndex = tr069Path.lastIndexOf("{i}") + 4;
                                            String shortName = tr069Path.substring(lastIndex);
                                            String tmpObjectName = path.substring(0, path.length()-shortName.length()-1);
                                            if(tmpObjectName.lastIndexOf(".")>0) {
                                                String objectName = tmpObjectName.substring(0, tmpObjectName.lastIndexOf("."));
                                                Map<String, String> mapParameterValues = new HashMap<>();
                                                if(mapAddObject.containsKey(objectName)){
                                                    mapParameterValues = mapAddObject.get(objectName);
                                                }
                                                if(!mapParameterValues.containsKey(shortName)){
                                                    mapParameterValues.put(shortName, provisioningValue);
                                                }
                                                mapAddObject.put(objectName,mapParameterValues);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    for (Map.Entry<String, Map<String, String>> entry : mapAddObject.entrySet())
                    {
                        acsClient.addObject(deviceId, entry.getKey(), entry.getValue(), true);
                    }
                    if (parameterValues.size() > 0) {
                        acsClient.setParameterValues(deviceId, parameterValues, true);
                    }
                }
            }
        }
    }

    public String getProvisioningValue(String deviceId, Parameter parameter) {
        if (parameter.useSubscriberData != null && parameter.useSubscriberData == 1) {
            List<Subscriber> subscribers = subscriberDeviceService.findByDeviceId(deviceId);
            if (subscribers.size() > 0) {
                Subscriber subscriber = subscribers.get(0);
                String subscriberDataKey = parameter.subscriberData;
                if (subscriber.subscriberData.get(subscriberDataKey) != null) {
                    return subscriber.subscriberData.get(subscriberDataKey);
                }
            }
        }
        if (parameter.value != null && !parameter.value.isEmpty()) {
            return parameter.value;
        }
        return parameter.defaultValue;
    }
}
