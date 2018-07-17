package vn.ssdc.vnpt.reports.services;

import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceType;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.reports.model.DeviceTempBirt;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.text.ParseException;
import java.util.List;
import java.util.Set;

@Service
public class DeviceTempBirtService extends SsdcCrudService<Long, DeviceTempBirt> {
    private static final Logger logger = LoggerFactory.getLogger(DeviceTempBirtService.class);

    @Autowired
    private SelfCareServiceDevice selfCareService;

    @Autowired
    public DeviceTempBirtService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(DeviceTempBirt.class);
    }

    @Autowired
    private Scheduler scheduler;

    @Autowired
    public DeviceTypeService deviceTypeService;

    public void createDeviceTempBirt() {
        SCDeviceSearchForm searchParameter = new SCDeviceSearchForm();
        int UpdateDevice = 0;
        int CreateDevice = 0;
        try {
            //1.Get All The Device
            List<SCDevice> lstDevice = selfCareService.searchDevice(searchParameter);
            if(!lstDevice.isEmpty()){
                for(SCDevice deviceTemp: lstDevice){
                    DeviceTempBirt dtbTemp = new DeviceTempBirt();
                    dtbTemp.serialNumber = deviceTemp.serialNumber;
                    dtbTemp.manufacturer = deviceTemp.manufacturer;
                    dtbTemp.productClass = deviceTemp.productClass;
                    dtbTemp.oui = deviceTemp.oui;
                    dtbTemp.firmwareVersion = deviceTemp.firmwareVersion;

                    Set<String> setLabel = deviceTemp.labels;

                    //2.Get Model Name
                    DeviceType deviceType = deviceTypeService.findByPk(deviceTemp.manufacturer, deviceTemp.oui, deviceTemp.productClass);
                    dtbTemp.modelName = deviceType.modelName;

                    if(setLabel!=null){
                        for (String s : setLabel) {
                            dtbTemp.label = s;

                            //3.Create Device Temp
                            List<DeviceTempBirt> lstDeviceTempBirt = checkDevice(dtbTemp.serialNumber,dtbTemp.manufacturer,dtbTemp.productClass,dtbTemp.oui);
                            if(!lstDeviceTempBirt.isEmpty()){
                                DeviceTempBirt deviceTempBirt = lstDeviceTempBirt.get(0);
                                dtbTemp.id = deviceTempBirt.id;
                                this.update(dtbTemp.id,dtbTemp);
                                logger.info("Update Device : " + dtbTemp.serialNumber + " - "+ dtbTemp.manufacturer
                                        + " - " + dtbTemp.productClass + " - " + dtbTemp.oui);
                                UpdateDevice++;
                            }else{
                                this.create(dtbTemp);
                                logger.info("Create Device : " + dtbTemp.serialNumber + " - " +dtbTemp.manufacturer
                                        + " - " + dtbTemp.productClass + " - " + dtbTemp.oui);
                                CreateDevice++;
                            }
                        }
                    }
                }

                logger.info("Total Update : " + UpdateDevice + ", Total Create : " + CreateDevice);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    public List<DeviceTempBirt> checkDevice(String serialNumber, String manufacturer,String productClass ,String oui) {
        String whereExp = "serial_number=? and manufacturer=? and product_class = ? and  oui = ?";
        return this.repository.search(whereExp,serialNumber, manufacturer,productClass,oui);
    }
}
