package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vn.ssdc.vnpt.selfCare.model.SCBackupFile;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCBackupFileSearchForm;
import vn.vnpt.ssdc.utils.ObjectUtils;
import vn.vnpt.ssdc.utils.StringUtils;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by THANHLX on 11/24/2017.
 */
@Component
public class SelfCareServiceBackupFile {
    @Value("${backupFile.endpoint}")
    private String backupFileEndpoint;

    @Value("${backupFile.url}")
    private String backupFileUrl;

    @Autowired
    private SelfCareServiceDevice selfCareService;

    private RestTemplate restTemplate;

    public SelfCareServiceBackupFile() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
    }

    public SCBackupFile get(String fileId) throws ParseException{
        String url = backupFileEndpoint + "/backupFiles";
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("query", "{\"_id\":\"" + fileId + "\"}");
        queryParams.put("limit", "1");
        String queryString = StringUtils.queryStringFromMap(queryParams);
        if (!ObjectUtils.empty(queryString)) {
            url += "?" + queryString;
        }
        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(url, String.class, queryParams);
        JsonArray jsonArray = new Gson().fromJson(responseEntity.getBody(), JsonArray.class);
        if (jsonArray.size() > 0) {
          return convertToSCBackupFile(jsonArray.get(0).getAsJsonObject());
        } else {
            return null;
        }
    }

    public void delete(String fileId) throws ParseException{
        SCBackupFile sCBackupFile = get(fileId);
        String url = backupFileEndpoint + "/files/" + sCBackupFile.filename;
        this.restTemplate.delete(url);
    }

    public List<SCBackupFile> search(SCBackupFileSearchForm scSCBackupFileSearchForm) throws ParseException{
        ResponseEntity<String> responseEntity = doSearch(scSCBackupFileSearchForm);
        JsonArray jsonArray = new Gson().fromJson(responseEntity.getBody(), JsonArray.class);
        List<SCBackupFile> listFiles = new ArrayList<>();
        if (jsonArray.size() > 0) {
            for (int i=0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                listFiles.add(convertToSCBackupFile(jsonObject));
            }
        }
        return listFiles;
    }

    public int count(SCBackupFileSearchForm scSCBackupFileSearchForm) {
        ResponseEntity<String> responseEntity = doSearch(scSCBackupFileSearchForm);
        return Integer.valueOf(responseEntity.getHeaders().get("totalAll").get(0));
    }

    public ResponseEntity<String> doSearch(SCBackupFileSearchForm scSCBackupFileSearchForm){
        String url = backupFileEndpoint + "/backupFiles";
        Map<String, String> queryParams = new HashMap<String, String>();
        Map<String, Object> mapParam = new HashMap<String, Object>();
        if (!Strings.isNullOrEmpty(scSCBackupFileSearchForm.deviceId)) {
            String[] deviceIds = scSCBackupFileSearchForm.deviceId.split("-");
            mapParam.put("metadata.oui", deviceIds[0]);
            mapParam.put("metadata.productClass", deviceIds[1]);
            mapParam.put("metadata.serialNumber", deviceIds[2]);
        }
        if (!Strings.isNullOrEmpty(scSCBackupFileSearchForm.fileType)) {
            mapParam.put("metadata.fileType", scSCBackupFileSearchForm.fileType);
        }
        if (!Strings.isNullOrEmpty(scSCBackupFileSearchForm.oui)) {
            mapParam.put("metadata.oui", scSCBackupFileSearchForm.oui);
        }
        if (!Strings.isNullOrEmpty(scSCBackupFileSearchForm.productClass)) {
            mapParam.put("metadata.productClass", scSCBackupFileSearchForm.productClass);
        }
        if (!Strings.isNullOrEmpty(scSCBackupFileSearchForm.serialNumber)) {
            mapParam.put("metadata.serialNumber", String.format("/%s/", scSCBackupFileSearchForm.serialNumber));

        }
        if (!Strings.isNullOrEmpty(scSCBackupFileSearchForm.firmwareVersion)) {
            mapParam.put("metadata.version", String.format("/%s/", scSCBackupFileSearchForm.firmwareVersion));

        }
        if (!Strings.isNullOrEmpty(scSCBackupFileSearchForm.modelName)) {
            mapParam.put("metadata.modelName", String.format("/%s/", scSCBackupFileSearchForm.modelName));

        }
        if (!Strings.isNullOrEmpty(scSCBackupFileSearchForm.manufacturer)) {
            mapParam.put("metadata.manufacturer", String.format("/%s/", scSCBackupFileSearchForm.manufacturer));

        }

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dt1.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        if (scSCBackupFileSearchForm.createdFrom != null && scSCBackupFileSearchForm.createdTo != null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$lte", dt1.format(scSCBackupFileSearchForm.createdTo));
            obj.addProperty("$gte", dt1.format(scSCBackupFileSearchForm.createdFrom));
            mapParam.put("uploadDate", obj);
        }

        if (scSCBackupFileSearchForm.createdFrom != null && scSCBackupFileSearchForm.createdTo == null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$gte", dt1.format(scSCBackupFileSearchForm.createdFrom));
            mapParam.put("uploadDate", obj);
        }

        if (scSCBackupFileSearchForm.createdFrom == null && scSCBackupFileSearchForm.createdTo != null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$lte", dt1.format(scSCBackupFileSearchForm.createdTo));
            mapParam.put("uploadDate", obj);
        }
        queryParams.put("query", new Gson().toJson(mapParam));
        queryParams.put("limit", Integer.toString(scSCBackupFileSearchForm.limit));
        queryParams.put("skip", Integer.toString((scSCBackupFileSearchForm.page-1)*scSCBackupFileSearchForm.limit));
        queryParams.put("sort", "{\"uploadDate\":-1}");
        String queryString = StringUtils.queryStringFromMap(queryParams);
        if (!ObjectUtils.empty(queryString)) {
            url += "?" + queryString;
        }
        return this.restTemplate.getForEntity(url, String.class, queryParams);
    }

    public SCBackupFile convertToSCBackupFile(JsonObject jsonObject) throws ParseException{
        SCBackupFile sCBackupFile = new SCBackupFile();
        sCBackupFile.id = jsonObject.get("_id").getAsString();
        sCBackupFile.url = backupFileUrl + "/" + jsonObject.get("filename").getAsString();
        sCBackupFile.fileType = jsonObject.get("metadata").getAsJsonObject().get("fileType").getAsString();
        sCBackupFile.oui = jsonObject.get("metadata").getAsJsonObject().get("oui").getAsString();
        sCBackupFile.productClass = jsonObject.get("metadata").getAsJsonObject().get("productClass").getAsString();
        sCBackupFile.firmwareVersion = jsonObject.get("metadata").getAsJsonObject().get("version").getAsString();
        sCBackupFile.createdTime = jsonObject.get("uploadDate") == null ? null : parseDate(jsonObject.get("uploadDate").getAsString());
        sCBackupFile.filename = jsonObject.get("filename").getAsString();
        sCBackupFile.serialNumber = jsonObject.get("metadata").getAsJsonObject().get("serialNumber").getAsString();
        sCBackupFile.manufacturer = jsonObject.get("metadata").getAsJsonObject().get("manufacturer").getAsString();
        sCBackupFile.modelName = jsonObject.get("metadata").getAsJsonObject().get("modelName").getAsString();
        sCBackupFile.deviceId = sCBackupFile.oui+"-"+sCBackupFile.productClass+"-"+sCBackupFile.serialNumber;
        SCDevice scDevice = selfCareService.getDevice(sCBackupFile.deviceId);
        if(scDevice != null) {
            sCBackupFile.labels = scDevice.labels;
        }
        return sCBackupFile;
    }

    public static Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dt.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        return dt.parse(dateStr);
    }
}
