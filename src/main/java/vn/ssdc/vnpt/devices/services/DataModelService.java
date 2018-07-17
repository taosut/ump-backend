package vn.ssdc.vnpt.devices.services;

import static com.google.common.base.CharMatcher.is;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.*;
import vn.ssdc.vnpt.umpexception.DeviceNotFoundException;
import vn.ssdc.vnpt.umpexception.DuplicationFirmwareVersionException;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

/**
 * Created by kiendt on 1/23/2017.
 */
@Service
public class DataModelService extends SsdcCrudService<Long, DeviceTypeVersion> {

    private static final Logger logger = LoggerFactory.getLogger(DataModelService.class);

    private static final String SOFTWARE_KEY = "summary.softwareVersion";
    private static final String MODELNAME_KEY = "summary.modelName";
    private static final String PRODUCTCLASS_KEY = "_ProductClass";
    private static final String MANUFACTURE_KEY = "_Manufacturer";
    private static final String OUI_KEY = "_OUI";
    private static final String VALUE_KEY = "_value";
    private static final String OBJECT_KEY = "_object";
    private static final String WRIABLE_KEY = "_writable";
    private static final String TYPE_KEY = "_type";
    private static final String INSTANCE_KEY = "_instance";
    private static final String PROFILE_OTHER = "Others";
    private static final String PROFILE_VENDOR = "Vendor";

    public List<String> ignoredParam = Arrays.asList(new String[]{"_id", "_registered", "_deviceId", "_lastInform", "_ip", "_lastBoot", "_lastBootstrap", "_lastConnectionRequest"});
    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private ParameterDetailService parameterDetailService;

    @Autowired
    private Tr069ParameterService tr069ParameterService;

    @Autowired
    private TagService tagService;

    @Autowired
    public DataModelService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(DeviceTypeVersion.class);
    }

    @Autowired
    private Tr069ProfileService tr069ProfileService;

    @Value("${tmpDir}")
    private String tmpDir;

    public void setAcsClient(AcsClient acsClient) {
        this.acsClient = acsClient;
    }

    /**
     * get Infor of deviceId from genies
     *
     * @param deviceId
     * @return
     */
    public JsonObject getInforDevice(String deviceId, List<String> listProjections) {
        Map<String, String> acsQuery = new HashMap<String, String>();
        acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
        if (listProjections != null) {
            List<String> listProcessProjections = new ArrayList<>();
            for (String projection : listProjections) {
                listProcessProjections.add(projection);
                int indexOf = projection.indexOf(".");
                while (indexOf >= 0) {
                    String parameterPath = projection.substring(0, (indexOf + 1));
                    if (!listProjections.contains(parameterPath)) {
                        if (!listProcessProjections.contains(parameterPath + "_object")) {
                            listProcessProjections.add(parameterPath + "_object");
                        }
                        if (!listProcessProjections.contains(parameterPath + "_writable")) {
                            listProcessProjections.add(parameterPath + "_writable");
                        }
                        if (!listProcessProjections.contains(parameterPath + "_timestamp")) {
                            listProcessProjections.add(parameterPath + "_timestamp");
                        }
                    }
                    indexOf = projection.indexOf(".", indexOf + 1);
                }
            }
            acsQuery.put("projection", StringUtils.join(listProcessProjections, ","));
        }
        JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
        if (arrayTmpObject.size() > 0) {
            return arrayTmpObject.get(0).getAsJsonObject();
        }
        throw new DeviceNotFoundException("Cannot find infor about deviceId " + deviceId);
    }

    /**
     * API export XML for data model
     *
     * @param deviceTypeVersionId
     */
    public String exportDataModelJson(Long deviceTypeVersionId) {
        String strReturn = "ERROR EXPORT !";
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.get(deviceTypeVersionId);

        Map<String, String> mapData = new HashMap<String, String>();
        Map<String, ParameterDetail> listParameter = parameterDetailService.findByDeviceTypeVersion(deviceTypeVersionId);
        for (String key : listParameter.keySet()) {
            ParameterDetail parameter = listParameter.get(key);
            String strValue;
            if (!"object".equals(parameter.dataType)) {
                strValue = "[" + parameter.access + ",\"" + parameter.defaultValue + "\"," + "\"xsd:" + parameter.dataType + "\"]";
            } else {
                strValue = "[" + parameter.access + "]";
            }
            mapData.put(key, strValue);
        }

        String strTimeCreated = String.valueOf(System.currentTimeMillis());
        File jsonFile = new File(tmpDir + "/" + deviceTypeVersion.modelName + "_" + strTimeCreated + ".json");

        List sortedKeys = new ArrayList(mapData.keySet());
        Collections.sort(sortedKeys);

        try {
            if (jsonFile.createNewFile()) {
                FileOutputStream fos = new FileOutputStream(jsonFile);
                PrintWriter pw = new PrintWriter(fos);
                pw.println("{");
                for (int index = 0; index < sortedKeys.size(); index++) {
                    if (index == (sortedKeys.size() - 1)) {
                        pw.println("\"" + sortedKeys.get(index) + "\"" + " : " + mapData.get(sortedKeys.get(index)));
                    } else {
                        pw.println("\"" + sortedKeys.get(index) + "\"" + " : " + mapData.get(sortedKeys.get(index)) + ",");
                    }
                }
                pw.println("}");
                pw.flush();
                pw.close();
                fos.close();
            }
            strReturn = jsonFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            strReturn += e;
        }
        return strReturn;
    }

    public String exportDataModelXML(Long deviceTypeVersionId) {
        String strReturn = "ERROR EXPORT ! ";
        try {
            //1st Create XML
            Document document = DocumentHelper.createDocument();
            // for datamodel
            //Set Root
//            Element xmlDocument = document.addElement("dm:document");
            Element root = document.addElement("model");
            //2st Get All Object Parameter Detail With deviceTypeVersionId
            List<ParameterDetail> listObject = parameterDetailService.getAllObject(deviceTypeVersionId);
            //3st Get Parameter
            for (int intIndex = 0; intIndex < listObject.size(); intIndex++) {
                ParameterDetail Object = listObject.get(intIndex);
                String strPath = Object.path;

                Element eObject = root.addElement("object").addAttribute("name", strPath);
                //After get Object then get All Their Parameter
                List<ParameterDetail> listParameter = parameterDetailService.getAllParameter(deviceTypeVersionId, strPath);
                for (int intIndexParameter = 0; intIndexParameter < listParameter.size(); intIndexParameter++) {
                    ParameterDetail parameter = listParameter.get(intIndexParameter);
                    if (!"object".equalsIgnoreCase(parameter.dataType)) {
                        //Process cut string parameter
                        // kiendt -> chuyen tu dang InternetGatewayDevice.LANDevice.1.Hosts.Host. sang  InternetGatewayDevice.LANDevice.{i}.Hosts.Host.
                        String strParamter = parameter.path;
                        String access = parameter.access;
                        Element eParameter = eObject.addElement("parameter");
                        if (!Strings.isNullOrEmpty(strParamter)) {
                            eParameter.addAttribute("name", strParamter.substring(strPath.length(), strParamter.length()).replaceAll("\\.\\d+\\.", ".{i}."));
                        }
                        if (!Strings.isNullOrEmpty(access)) {
                            eParameter.addAttribute("access", access);
                        }
                        //
                        if (parameter.description != null) {
                            Element eDes = eParameter.addElement("description");
                            eDes.addText(parameter.description);
                        }
                        //
                        Element eSyntax = eParameter.addElement("syntax");
                        if (!"".equals(parameter.defaultValue) && parameter.defaultValue != null) {
                            Element eDataType = eSyntax.addElement(parameter.dataType);
                            eDataType.addElement("defaultValue").addText(parameter.defaultValue);
                        } else {
                            eSyntax.addElement(parameter.dataType);
                        }
                    }
                }
            }
            // for profile
            List<Tag> tags = tagService.getListProfilesOfVersion(deviceTypeVersionId);
            for (Tag tag : tags) {
                // loc danh sach parameter de phan loai cac parameter co chung parentobject
                Map<String, List<Parameter>> mapsObjectInProfile = new HashMap<>();
                for (Map.Entry<String, Parameter> entry : tag.parameters.entrySet()) {
                    if (mapsObjectInProfile.containsKey(entry.getValue().tr069ParentObject)) {
                        List<Parameter> parametes = mapsObjectInProfile.get(entry.getValue().tr069ParentObject);
                        parametes.add(entry.getValue());
                        mapsObjectInProfile.put(entry.getValue().tr069ParentObject, parametes);
                    } else {
                        List<Parameter> parametes = new ArrayList<>();
                        parametes.add(entry.getValue());
                        mapsObjectInProfile.put(entry.getValue().tr069ParentObject, parametes);
                    }
                }
                //
                Element profileElement = root.addElement("profile");
                if (tag.name.contains(":InternetGatewayDevice")) {
                    profileElement.addAttribute("name", tag.name.split(":InternetGatewayDevice")[0]);
                } else if (tag.name.contains(":Device")) {
                    profileElement.addAttribute("name", tag.name.split(":Device")[0]);
                } else {
                    profileElement.addAttribute("name", tag.name);
                }
                for (Map.Entry<String, List<Parameter>> map : mapsObjectInProfile.entrySet()) {
                    Element objectElement = profileElement.addElement("object").addAttribute("ref", map.getKey());
                    for (Parameter parameter : map.getValue()) {
                        objectElement.addElement("parameter").addAttribute("ref", parameter.shortName);
                    }
                }
            }

            // Pretty print the document to System.out
            OutputFormat format = OutputFormat.createPrettyPrint();
            String strTimeCreated = String.valueOf(System.currentTimeMillis());
            File xmlFile = new File(tmpDir + "/datamodel_" + strTimeCreated + ".xml");
//            File xmlFile = new File("D://test.xml");
            if (xmlFile.createNewFile()) {
                XMLWriter output = new XMLWriter(new FileWriter(xmlFile), format);
                output.write(document);
                output.close();
            }
            strReturn = xmlFile.getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            logger.error("{}", e);
//            e.printStackTrace();
            strReturn += e;
        } catch (IOException e) {
            logger.error("{}", e);
            strReturn += e;
        }
        return strReturn;
    }

    public void importDataModelXML(InputStream input) throws IOException, IOException, DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(input);

    }

    public Set<Parameter> getProfileOfDevices(String deviceId, Long tagId) {
        Set<Parameter> result = new LinkedHashSet<Parameter>();
        Tag tag = tagService.get(tagId);
        List<String> listTr069Names = new ArrayList<String>();
        List<String> listProjections = new ArrayList<>();
        for (Map.Entry<String, Parameter> entry : tag.parameters.entrySet()) {
            String tr069Name = entry.getValue().tr069Name;
            if (tr069Name.contains("{i}")) {
                String tmpTr069Name = tr069Name.substring(0, tr069Name.indexOf("{i}"));
                if (!listProjections.contains(tmpTr069Name)) {
                    listProjections.add(tmpTr069Name);
                }
            } else {
                if (!listProjections.contains(tr069Name)) {
                    listProjections.add(tr069Name);
                }
            }
            if (!listTr069Names.contains(tr069Name)) {
                listTr069Names.add(tr069Name);
            }
            int indexOf = tr069Name.indexOf(".");
            while (indexOf >= 0) {
                String parameterPath = tr069Name.substring(0, (indexOf + 1));
                if (!listTr069Names.contains(parameterPath)) {
                    listTr069Names.add(parameterPath);
                }
                indexOf = tr069Name.indexOf(".", indexOf + 1);
            }
        }
        JsonObject body = getInforDevice(deviceId, listProjections);
        Map<String, ParameterDetail> listParameters = findParameterDetailProfile(body, listTr069Names);
        Map<String, Parameter> parameters = parameterDetailService.convertToMapParameter(listParameters);
        for (Map.Entry<String, Parameter> parameterEntry : parameters.entrySet()) {
            result.add(parameterEntry.getValue());
        }
        return result;
    }

    /**
     * parse root infor to get list parameter details
     *
     * @param body
     * @return
     */
    public Map<String, ParameterDetail> findParameterDetailProfile(JsonObject body, List<String> listTr069Names) {
        Map<String, ParameterDetail> mapParam = new HashMap<String, ParameterDetail>();
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject() && !entry.getKey().contains("summary")) {
                ParameterDetail parameter = new ParameterDetail();
                if (body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY) != null) {
                    parameter.access = String.valueOf(body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY).getAsBoolean());
                }
                if (body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY).getAsBoolean()) {
                    parameter.defaultValue = "";
                    parameter.rule = "";
                    parameter.path = entry.getKey() + ".";
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = "object";
                    parameter.shortName = entry.getKey();
                    if (listTr069Names.contains(parameter.tr069Name)) {
                        mapParam.put(parameter.path, parameter);
                        loopFindParameterDetailProfile(mapParam, entry.getValue().getAsJsonObject(), entry.getKey() + ".", listTr069Names);
                    }
                }
            }
        }
        return mapParam;
    }

    private void loopFindParameterDetailProfile(Map<String, ParameterDetail> mapParam, JsonObject body, String key, List<String> listTr069Names) {
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject()) {
                // if param is a object
                ParameterDetail parameter = new ParameterDetail();
                if (body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY) != null) {
                    parameter.access = String.valueOf(body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY).getAsBoolean());
                }
                if (body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY).getAsBoolean() == true) {
                    parameter.path = key + entry.getKey() + ".";
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = "object";
                    parameter.defaultValue = "";
                    parameter.shortName = entry.getKey();
                    parameter.parentObject = key;
                    parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
                    if (listTr069Names.contains(parameter.tr069Name)) {
                        mapParam.put(parameter.path, parameter);
                        loopFindParameterDetailProfile(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".", listTr069Names);
                    }
                } else if (body.get(entry.getKey()).getAsJsonObject().get(INSTANCE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(INSTANCE_KEY).getAsBoolean() == true) {
                    parameter.path = key + entry.getKey() + ".";
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = "object";
                    parameter.defaultValue = "";
                    parameter.shortName = entry.getKey();
                    parameter.parentObject = key;
                    parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
                    parameter.instance = true;
                    if (listTr069Names.contains(parameter.tr069Name)) {
                        mapParam.put(parameter.path, parameter);
                        loopFindParameterDetailProfile(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".", listTr069Names);
                    }
                } else if (body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString() != null) {
                    if (body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY) != null
                            && !body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).isJsonNull()
                            && body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString() != null) {
                        parameter.setValue(body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString());
                        parameter.defaultValue = body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString();
                    }
                    parameter.path = key + entry.getKey();
                    Tr069Parameter tr069Parameter = tr069ParameterService.searchByPath(parameter.path);
                    if (tr069Parameter != null) {
                        parameter.rule = tr069Parameter.rule;
                    }
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString().contains("xsd") ? body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString().replaceAll("xsd:", "") : body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString();
                    parameter.shortName = entry.getKey();
                    parameter.parentObject = key;
                    parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
                    if (listTr069Names.contains(parameter.tr069Name)) {
                        mapParam.put(parameter.path, parameter);
                        loopFindParameterDetailProfile(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".", listTr069Names);
                    }
                }
            }
        }
    }

    /**
     * create new deviceTypeVersion and create parameterDetail
     *
     * @param deviceId
     */
    public void cloneDataModel(String deviceId, String cloneDeviceId) {
        try {
            logger.info("Clone data model of {} to {}", cloneDeviceId, deviceId);
            JsonObject body = getInforDevice(deviceId, null);
            DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
            JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
            String modelName = body.get(MODELNAME_KEY) != null ? body.get(MODELNAME_KEY).getAsJsonObject().get(VALUE_KEY).getAsString() : "";
            String firmwareVersion = body.get(SOFTWARE_KEY) != null ? body.get(SOFTWARE_KEY).getAsJsonObject().get(VALUE_KEY).getAsString() : "";
            String productClass = inforObject.get(PRODUCTCLASS_KEY) != null ? inforObject.get(PRODUCTCLASS_KEY).getAsString() : renderUnknowName();
            String manufacture = inforObject.get(MANUFACTURE_KEY) != null ? inforObject.get(MANUFACTURE_KEY).getAsString() : renderUnknowName();
            String oui = inforObject.get(OUI_KEY).getAsString() != null ? inforObject.get(OUI_KEY).getAsString() : renderUnknowName();
            DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
            if (currenDeviceType == null) {
                // new deviceType
                DeviceType deviceType = new DeviceType();
                deviceType.manufacturer = manufacture;
                deviceType.oui = oui;
                deviceType.productClass = productClass;
                deviceType.name = productClass + "_" + manufacture + "_" + oui;
                deviceType.modelName = modelName;
                currenDeviceType = deviceTypeService.create(deviceType);
            }

            if (deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion) != null) {
                throw new DuplicationFirmwareVersionException("firmwareVersion " + firmwareVersion + " existed!!");
            }
            deviceTypeVersion.id = null;
            deviceTypeVersion.deviceTypeId = currenDeviceType.id;
            deviceTypeVersion.firmwareVersion = firmwareVersion;
            deviceTypeVersion.modelName = modelName;
            deviceTypeVersion.productClass = productClass;
            deviceTypeVersion.oui = oui;
            deviceTypeVersion.manufacturer = manufacture;
            DeviceTypeVersion currentDeviceTypeVersion = new DeviceTypeVersion();
            currentDeviceTypeVersion = deviceTypeVersionService.create(deviceTypeVersion);

            JsonObject cloneBody = getInforDevice(cloneDeviceId, null);
            Map<String, ParameterDetail> cloneMapParam = parseParameterDetail(cloneBody);
            Map<String, ParameterDetail> mapParam = parseParameterDetail(body);
            for (Map.Entry<String, ParameterDetail> entry : cloneMapParam.entrySet()) {
                if (!mapParam.containsKey(entry.getKey())) {
                    mapParam.put(entry.getKey(), entry.getValue());
                }
            }
            currentDeviceTypeVersion.parameters = parameterDetailService.convertToMapParameter(mapParam);

            Map<String, Tag> mapDiagnostic = new HashMap<>();
            if (!deviceTypeVersionService.checkExistSimilarDeviceTypeVersion(currenDeviceType.id, currentDeviceTypeVersion.id)) {
                mapDiagnostic = createProfile(currentDeviceTypeVersion, mapParam);
            } else {
                DeviceTypeVersion similarDeviceTypeVersion = getSimilarDeviceTypeVersion(currenDeviceType, mapParam);
                mapDiagnostic = createProfileFromSimilarDeviceTypeVersion(currentDeviceTypeVersion, mapParam, similarDeviceTypeVersion);
            }

            currentDeviceTypeVersion.diagnostics = mapDiagnostic;
            deviceTypeVersionService.update(currentDeviceTypeVersion.id, currentDeviceTypeVersion);
            createDataModel(currentDeviceTypeVersion, mapParam);
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    /**
     * create new deviceTypeVersion and create parameterDetail
     *
     * @param deviceId
     */
    public void addDataModel(String deviceId) {
        try {
            logger.info("Add data model for {}", deviceId);
            JsonObject body = getInforDevice(deviceId, null);
            DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
            JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
            String modelName = body.get(MODELNAME_KEY) != null ? body.get(MODELNAME_KEY).getAsJsonObject().get(VALUE_KEY).getAsString() : "";
            String firmwareVersion = body.get(SOFTWARE_KEY) != null ? body.get(SOFTWARE_KEY).getAsJsonObject().get(VALUE_KEY).getAsString() : "";
            String productClass = inforObject.get(PRODUCTCLASS_KEY) != null ? inforObject.get(PRODUCTCLASS_KEY).getAsString() : renderUnknowName();
            String manufacture = inforObject.get(MANUFACTURE_KEY) != null ? inforObject.get(MANUFACTURE_KEY).getAsString() : renderUnknowName();
            String oui = inforObject.get(OUI_KEY).getAsString() != null ? inforObject.get(OUI_KEY).getAsString() : renderUnknowName();
            DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
            Map<String, ParameterDetail> mapParam;
            if (currenDeviceType == null) {
                // new deviceType
                DeviceType deviceType = new DeviceType();
                deviceType.manufacturer = manufacture;
                deviceType.oui = oui;
                deviceType.productClass = productClass;
                deviceType.name = productClass + "_" + manufacture + "_" + oui;
                deviceType.modelName = modelName;
                currenDeviceType = deviceTypeService.create(deviceType);
            }

            if (deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion) != null) {
                throw new DuplicationFirmwareVersionException("firmwareVersion " + firmwareVersion + " existed!!");
            }
            deviceTypeVersion.id = null;
            deviceTypeVersion.deviceTypeId = currenDeviceType.id;
            deviceTypeVersion.firmwareVersion = firmwareVersion;
            deviceTypeVersion.modelName = modelName;
            deviceTypeVersion.productClass = productClass;
            deviceTypeVersion.oui = oui;
            deviceTypeVersion.manufacturer = manufacture;
            DeviceTypeVersion currentDeviceTypeVersion = new DeviceTypeVersion();
            currentDeviceTypeVersion = deviceTypeVersionService.create(deviceTypeVersion);

            mapParam = parseParameterDetail(body);
            currentDeviceTypeVersion.parameters = parameterDetailService.convertToMapParameter(mapParam);

            Map<String, Tag> mapDiagnostic = new HashMap<>();
            if (!deviceTypeVersionService.checkExistSimilarDeviceTypeVersion(currenDeviceType.id, currentDeviceTypeVersion.id)) {
                mapDiagnostic = createProfile(currentDeviceTypeVersion, mapParam);
            } else {
                DeviceTypeVersion similarDeviceTypeVersion = getSimilarDeviceTypeVersion(currenDeviceType, mapParam);
                mapDiagnostic = createProfileFromSimilarDeviceTypeVersion(currentDeviceTypeVersion, mapParam, similarDeviceTypeVersion);
            }

            currentDeviceTypeVersion.diagnostics = mapDiagnostic;
            deviceTypeVersionService.update(currentDeviceTypeVersion.id, currentDeviceTypeVersion);
            createDataModel(currentDeviceTypeVersion, mapParam);
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    /**
     * create data model for one devicetypeVersion
     *
     * @param deviceTypeVersion
     * @param mapParam
     */
    public void createDataModel(DeviceTypeVersion deviceTypeVersion, Map<String, ParameterDetail> mapParam) {
        // this code takes long time to run -> need optimize
        for (Map.Entry<String, ParameterDetail> entry : mapParam.entrySet()) {
            entry.getValue().deviceTypeVersionId = deviceTypeVersion.id;
            entry.getValue().id = null;
            parameterDetailService.create(entry.getValue());
        }
    }

    /**
     * Xoa cac parameter thuoc setting_profile nhung khong cos trong profile
     * dinh clone
     *
     *
     * @return
     */
    private String removeParameterNotBelongSettingProfile(String profileSettingDetail, Map<String, Parameter> parameters) {
        try {
            JsonArray finalSubProfiles = new JsonArray();
            JsonArray subProfiles = new Gson().fromJson(profileSettingDetail, JsonArray.class);
            for (int i = 0; i < subProfiles.size(); i++) {
                JsonObject subprofile = subProfiles.get(i).getAsJsonObject();
                JsonArray parameterInSubProfiles = new Gson().fromJson(subprofile.get("parameters"), JsonArray.class);
                subprofile.remove("parameters");
                subprofile.add("parameters", checkParameterInSubProfileBelongProfileBeforeClone(parameterInSubProfiles, parameters));
                finalSubProfiles.add(subprofile);
            }
            return new Gson().toJson(finalSubProfiles);
        } catch (Exception e) {
            logger.error("removeParameterNotBelongSettingProfile ", e);
        }
        return null;
    }

    /**
     * tra lai map parameter thuoc subprofile sau khi da xoa cac parameter khong
     * nam trong datamodel truoc khi clone
     *
     * @param parameterInSubProfiles
     * @param parameters
     * @return
     */
    private JsonArray checkParameterInSubProfileBelongProfileBeforeClone(JsonArray parameterInSubProfiles, Map<String, Parameter> parameters) {
        JsonArray finalParametersInSubProfile = new JsonArray();

        for (int i = 0; i < parameterInSubProfiles.size(); i++) {
            JsonObject parameter = parameterInSubProfiles.get(i).getAsJsonObject();
            // convert path thanh dang A.{i}.B.{i}.C de so sanh voi tr069parameter trong parameter cua datamodel
            String tr069PathFromParameterInSubProfile = parameter.get("path").getAsString().replaceAll("\\.\\d+\\.", ".{i}.");
            boolean isBelong = false;
            for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
                String tr069FromParameterInNewProfile = entry.getValue().tr069Name;
                if (tr069PathFromParameterInSubProfile.equals(tr069FromParameterInNewProfile)) {
                    isBelong = true;
                }
            }
            if (isBelong) {
                finalParametersInSubProfile.add(parameter);
            }
        }
        return finalParametersInSubProfile;
    }

    /**
     * create profile for one devicetypeVersion
     *
     * @param deviceTypeVersion
     * @param mapParam
     */
    public Map<String, Tag> createProfileFromSimilarDeviceTypeVersion(DeviceTypeVersion deviceTypeVersion, Map<String, ParameterDetail> mapParam, DeviceTypeVersion similarDeviceTypeVersion) {
        List<Tag> listProfilesOfVersion = tagService.getListProfilesOfVersion(similarDeviceTypeVersion.id);
        Map<String, Tag> listProfiles = new HashMap<>();
        for (Tag similarProfile : listProfilesOfVersion) {
            Map<String, Parameter> parameters = new HashMap<>();
            Map<String, Parameter> similarParameters = similarProfile.parameters;

            for (Map.Entry<String, Parameter> similarParameterEntry : similarParameters.entrySet()) {
                if (mapParam.containsKey(similarParameterEntry.getKey())) {
                    Parameter parameterClone = parameterDetailService.convertToParameter(mapParam.get(similarParameterEntry.getKey()));
                    parameters.put(similarParameterEntry.getKey(), parameterClone);
                }
            }

            if (parameters.size() > 0) {
                Tag profile = new Tag();
                profile.name = similarProfile.name;
                profile.deviceTypeVersionId = deviceTypeVersion.id;
                profile.parameters = parameters;
                profile.assigned = 0;
                profile.assignedGroup = "PROFILE";
                profile.rootTagId = null;
//                profile.synchronize = similarProfile.synchronize;
                profile.id = null;
                // clone setting profile

                profile.profileSetting = similarProfile.profileSetting;
                profile.correspondingModule = similarProfile.correspondingModule;
                if (!Strings.isNullOrEmpty(similarProfile.subProfileSetting)) {
                    profile.subProfileSetting = removeParameterNotBelongSettingProfile(similarProfile.subProfileSetting, parameters);
                }
                Tag profile1 = tagService.create(profile);
                listProfiles.put(profile1.name, profile1);
                for (Map.Entry<String, Parameter> parameterEntry : profile1.parameters.entrySet()) {
                    if (mapParam.get(parameterEntry.getKey()).profile == null) {
                        mapParam.get(parameterEntry.getKey()).profile = new HashSet<String>();
                    }
                    mapParam.get(parameterEntry.getKey()).profile.add(profile.id.toString());
                }
            }
        }

        Map<String, Tag> diagnostics = new HashMap<>();
        for (Map.Entry<String, Tag> entry : similarDeviceTypeVersion.diagnostics.entrySet()) {
            if (listProfiles.containsKey(entry.getKey())) {
                diagnostics.put(entry.getKey(), listProfiles.get(entry.getKey()));
            }
        }
        return diagnostics;
    }

    /**
     * create profile for one devicetypeVersion
     *
     * @param deviceTypeVersion
     * @param mapParam
     */
    public Map<String, Tag> createProfile(DeviceTypeVersion deviceTypeVersion, Map<String, ParameterDetail> mapParam) {
        // list profile standard tr069
        Map<String, Tag> listProfile = new ConcurrentHashMap<String, Tag>();
        // below tr069 but not below profile of tr069
        Tag profileOther = tagService.generateProfileOther(PROFILE_OTHER, deviceTypeVersion);
        // not below tr069
        Tag profileVendor = tagService.generateProfileOther(PROFILE_VENDOR, deviceTypeVersion);
        // map diagnostics data
        Map<String, Tag> listDiagnostics = new HashMap<String, Tag>();

        for (Map.Entry<String, ParameterDetail> entry : mapParam.entrySet()) {
            // if paramaeter is tr069 standard
            Tr069Parameter tr069Parameter = tr069ParameterService.isTr069ParameterStandard(entry.getValue().tr069Name);
            if (tr069Parameter != null) {
                String profileNames = tr069Parameter.profileNames;
                if (profileNames.isEmpty() || "".equals(profileNames)) {
                    profileOther.parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                } else {
                    // if profileNames la mot chuoi cac profile
                    if (profileNames.contains(",")) {
                        List<String> lstConstProfile = Arrays.asList(profileNames.split(","));
                        for (String profile : lstConstProfile) {
                            if (!listProfile.containsKey(profile)) {
                                tagService.generateProfile(listProfile, profile, deviceTypeVersion);
                            }
                            for (Map.Entry<String, Tag> tmp : listProfile.entrySet()) {
                                if (tmp.getKey().equalsIgnoreCase(profile) && !entry.getValue().dataType.equals("object")) {
                                    tmp.getValue().parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                                    listProfile.put(tmp.getKey(), tmp.getValue());
                                    break;
                                }
                            }
                        }

                    } else {
                        if (!listProfile.containsKey(profileNames)) {
                            tagService.generateProfile(listProfile, profileNames, deviceTypeVersion);
                        }
                        for (Map.Entry<String, Tag> tmp : listProfile.entrySet()) {
                            if (tmp.getKey().equalsIgnoreCase(profileNames) && !entry.getValue().dataType.equals("object")) {
                                tmp.getValue().parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                                listProfile.put(tmp.getKey(), tmp.getValue());
                                break;
                            }
                        }
                    }
                }
            } else {
                // put to list no standard
                if (!entry.getValue().dataType.equals("object")) {
                    profileVendor.parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                }
            }
        }

        List<Tr069Profile> diagnosticsProfile = tr069ProfileService.getProfileIsDiagnostics();
        for (Map.Entry<String, Tag> tmp : listProfile.entrySet()) {
            boolean blIsDianostics = false;
            for (int dpIndex = 0; dpIndex < diagnosticsProfile.size(); dpIndex++) {
                Tr069Profile profileTemp = diagnosticsProfile.get(dpIndex);
                if (tmp.getKey().equals(profileTemp.name)) {
                    blIsDianostics = true;
                }
            }
            if (blIsDianostics) {
                listDiagnostics.put(tmp.getKey(), tmp.getValue());
            }
        }
        //
        listProfile.put("PROFILE_OTHER", profileOther);
        listProfile.put("PROFILE_VENDOR", profileVendor);
        // create profile standard, profile other and profile vendor
        for (Map.Entry<String, Tag> tmp : listProfile.entrySet()) {
            Tag tag = tagService.create(tmp.getValue());
            for (String key : mapParam.keySet()) {
                if (mapParam.get(key).profile == null) {
                    mapParam.get(key).profile = new HashSet<String>();
                }
                if (tag.parameters.containsKey(mapParam.get(key).path)) {
                    if (mapParam.get(key).profile != null && !mapParam.get(key).profile.contains(tag.id.toString())) {
                        mapParam.get(key).profile.add(tag.id.toString());
                    }
                }
            }
        }
        return listDiagnostics;
    }

    /**
     * render unknow name in case product, manufacture, oui == null
     *
     * @return
     */
    private String renderUnknowName() {
        return "UNKNOWN_" + System.currentTimeMillis();
    }

    /**
     * count duplicate parameter between 2 datamodel
     *
     * @param currentDataModel
     * @param tmpVersionDataModel
     * @return
     */
    private int countDuplicateParameter(Map<String, ParameterDetail> currentDataModel, Map<String, ParameterDetail> tmpVersionDataModel) {
        int count = 0;
        for (String key : currentDataModel.keySet()) {
            if (tmpVersionDataModel.containsKey(key)) {
                ++count;
            }
        }
        return count;
    }

    /**
     * process compare to parse parameter deleted, parameter duplicated,
     * paraemter added
     *
     * @param currentDataModel
     * @param tmpVersionDataModel
     * @return
     */
    private Map<String, ParameterDetail> processCompare(Map<String, ParameterDetail> currentDataModel, Map<String, ParameterDetail> tmpVersionDataModel) {
        for (Map.Entry<String, ParameterDetail> entry : currentDataModel.entrySet()) {
            if (tmpVersionDataModel.containsKey(entry.getKey())) {
                // update default value
                tmpVersionDataModel.get(entry.getKey()).defaultValue = entry.getValue().defaultValue;
                tmpVersionDataModel.put(entry.getKey(), tmpVersionDataModel.get(entry.getKey()));
            } else if (!tmpVersionDataModel.containsKey(entry.getKey())) {
                // new key
                tmpVersionDataModel.put(entry.getKey(), entry.getValue());
            }
            Iterator it = tmpVersionDataModel.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry item = (Map.Entry<String, ParameterDetail>) it.next();
                if (!currentDataModel.containsKey(item.getKey())) {
                    it.remove();
                }
            }
        }
        return tmpVersionDataModel;
    }

    /**
     * @param currenDeviceType
     * @param currentDataModel
     * @return
     */
    public DeviceTypeVersion getSimilarDeviceTypeVersion(DeviceType currenDeviceType, Map<String, ParameterDetail> currentDataModel) {
        List<DeviceTypeVersion> listDeviceTypeVersions = deviceTypeVersionService.findByDeviceType(currenDeviceType.id);
        int maxParameterComplicate = 0;
        DeviceTypeVersion result = null;
        for (DeviceTypeVersion deviceTypeVersion : listDeviceTypeVersions) {
            Map<String, ParameterDetail> tmpVersionDataModel = parameterDetailService.findByDeviceTypeVersion(deviceTypeVersion.id);
            int max = countDuplicateParameter(currentDataModel, tmpVersionDataModel);
            if (max > maxParameterComplicate) {
                maxParameterComplicate = max;
                result = deviceTypeVersion;
            }
        }
        return result;
    }

    /**
     * parse root infor to get list parameter details
     *
     * @param body
     * @return
     */
    public Map<String, ParameterDetail> parseParameterDetail(JsonObject body) {
        Map<String, ParameterDetail> mapParam = new HashMap<String, ParameterDetail>();
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject() && !entry.getKey().contains("summary")) {
                ParameterDetail parameter = new ParameterDetail();
                if (body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY) != null) {
                    parameter.access = String.valueOf(body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY).getAsBoolean());
                }
                if (body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY).getAsBoolean()) {
                    parameter.defaultValue = "";
                    parameter.rule = "";
                    parameter.path = entry.getKey() + ".";
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = "object";
                    parameter.shortName = entry.getKey();
                    mapParam.put(entry.getKey() + ".", parameter);
                    loop(mapParam, entry.getValue().getAsJsonObject(), entry.getKey() + ".");
                }
            }
        }
        return mapParam;
    }

    /**
     * loop to go through object
     *
     * @param mapParam
     * @param body
     * @param key
     */
    private void loop(Map<String, ParameterDetail> mapParam, JsonObject body, String key) {
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject()) {
                // if param is a object
                ParameterDetail parameter = new ParameterDetail();
                processInLoop(entry, body, parameter, key, mapParam);
            }
        }
    }

    /**
     * break code from loop to decreate complication
     *
     * @param entry
     * @param body
     * @param parameter
     * @param key
     * @param mapParam
     */
    private void processInLoop(Map.Entry<String, JsonElement> entry, JsonObject body, ParameterDetail parameter, String key, Map<String, ParameterDetail> mapParam) {
        if (body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY) != null) {
            parameter.access = String.valueOf(body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY).getAsBoolean());
        }
        if (body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY).getAsBoolean() == true) {
            parameter.path = key + entry.getKey() + ".";
            parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
            parameter.dataType = "object";
            parameter.defaultValue = "";
            parameter.shortName = entry.getKey();
            parameter.parentObject = key;
            parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
            mapParam.put(key + entry.getKey() + ".", parameter);
            loop(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".");
        } else if (body.get(entry.getKey()).getAsJsonObject().get(INSTANCE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(INSTANCE_KEY).getAsBoolean() == true) {
            parameter.path = key + entry.getKey() + ".";
            parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
            parameter.dataType = "object";
            parameter.defaultValue = "";
            parameter.shortName = entry.getKey();
            parameter.parentObject = key;
            parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
            parameter.instance = true;
            mapParam.put(key + entry.getKey() + ".", parameter);
            loop(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".");
        } else if (body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString() != null) {
            if (body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY) != null && !body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).isJsonNull() && body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString() != null) {
                parameter.setValue(body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString());
                parameter.defaultValue = body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString();
            }
            parameter.path = key + entry.getKey();
            Tr069Parameter tr069Parameter = tr069ParameterService.searchByPath(parameter.path);
            if (tr069Parameter != null) {
                parameter.rule = tr069Parameter.rule;
            }
            parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
            parameter.dataType = body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString().contains("xsd") ? body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString().replaceAll("xsd:", "") : body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString();
            parameter.shortName = entry.getKey();
            parameter.parentObject = key;
            parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
            mapParam.put(key + entry.getKey(), parameter);
            loop(mapParam, entry.getValue().getAsJsonObject(), key + "." + entry.getKey());
        }
    }

    private String getFirmwareVersion(JsonObject jsonObject) {
        if (jsonObject.get("InternetGatewayDevice") != null) {
            return jsonObject.get("InternetGatewayDevice").getAsJsonObject().get("DeviceInfo").getAsJsonObject().get("ModemFirmwareVersion").getAsJsonObject().get(VALUE_KEY).getAsString();
        } else if (jsonObject.get("Device") != null) {
            return jsonObject.get("Device").getAsJsonObject().get("DeviceInfo").getAsJsonObject().get("HardwareVersion").getAsJsonObject().get(VALUE_KEY).getAsString();
        }
        return null;
    }

    public Map<String, ParameterDetail> getMapPrameterFromDevice(String deviceId) {
        Map<String, ParameterDetail> map = new HashMap<>();
        JsonObject body = getInforDevice(deviceId, null);
        map = parseParameterDetail(body);
        return map;
    }

}
