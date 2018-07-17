package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import vn.ssdc.vnpt.selfCare.model.SCFile;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCFileSearchForm;
import vn.vnpt.ssdc.utils.ObjectUtils;
import vn.vnpt.ssdc.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by THANHLX on 11/24/2017.
 */
@Component
public class SelfCareServiceFile {

    @Value("${file.endpoint}")
    private String fileEndpoint;

    @Value("${file.url}")
    private String fileUrl;

    private RestTemplate restTemplate;

    public SelfCareServiceFile() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
    }

    public SCFile get(String fileId) throws ParseException {
        String url = fileEndpoint + "/files";
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
            return convertToFile(jsonArray.get(0).getAsJsonObject());
        } else {
            return null;
        }
    }

    public SCFile getByName(String fileName) throws ParseException {
        String url = fileEndpoint + "/files";
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("query", "{\"filename\":\"" + fileName + "\"}");
        queryParams.put("limit", "1");
        String queryString = StringUtils.queryStringFromMap(queryParams);
        if (!ObjectUtils.empty(queryString)) {
            url += "?" + queryString;
        }
        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(url, String.class, queryParams);
        JsonArray jsonArray = new Gson().fromJson(responseEntity.getBody(), JsonArray.class);
        if (jsonArray.size() > 0) {
            return convertToFile(jsonArray.get(0).getAsJsonObject());
        } else {
            return null;
        }
    }

    public SCFile update(String id, String fileName, String oui, String productClass, String manufacturer, String modelName, String firmwareVersion, InputStream file, String fileUrl, String username, String password, Boolean isBasicFirmware, String md5, String size) throws ParseException, IOException {
        SCFile scFile = get(id);
        HttpHeaders headers = new HttpHeaders();
        headers.add("filetype", "1 Firmware Upgrade Image");
        headers.add("manufacturer", manufacturer);
        headers.add("modelName", modelName);
        headers.add("version", firmwareVersion);
        headers.add("oui", oui);
        headers.add("productclass", productClass);
        if (fileUrl != null) {
            headers.add("fileUrl", fileUrl);
        }
        if (username != null) {
            headers.add("username", username);
        }
        if (password != null) {
            headers.add("password", password);
        }
        if (isBasicFirmware != null) {
            headers.add("isBasicFirmware", isBasicFirmware.toString());
        }
        if (md5 != null) {
            headers.add("md5", md5);
        }
        if (size != null) {
            headers.add("size", size);
        }
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        byte[] bytes = null;
        if(file != null) {
            bytes = IOUtils.toByteArray(file);
        }
        if (bytes != null && bytes.length == 0) {
            String url = fileEndpoint + "/update-files/" + scFile.fileId;
            try {
                URI uri = new URI(url);
                RequestEntity<MultiValueMap<String, Object>> requestEntity = new RequestEntity<>(headers, HttpMethod.PUT, uri);
                ResponseEntity<String> responseEntity = this.restTemplate.exchange(requestEntity, String.class);
                if (responseEntity.getStatusCodeValue() != 200 && responseEntity.getStatusCodeValue() != 201) {
                    return null;
                }
                return get(id);
            } catch (Exception e) {
                return null;
            }
        } else {
            headers.add("uploadFileName", fileName);
            String url = fileEndpoint + "/files/" + scFile.fileId;
            try {
                URI uri = new URI(url);
                HttpEntity<byte[]> requestEntity = new HttpEntity<byte[]>(bytes, headers);
                ResponseEntity<String> responseEntity = this.restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
                if (responseEntity.getStatusCodeValue() != 200 && responseEntity.getStatusCodeValue() != 201) {
                    return null;
                }
                return get(id);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public SCFile create(String fileName, String oui, String productClass, String manufacturer, String modelName, String firmwareVersion, InputStream file, String fileUrl, String username, String password, Boolean isBasicFirmware, String md5, String size) {
        String fileId = UUID.randomUUID().toString();
        String url = fileEndpoint + "/files/" + fileId;
        HttpHeaders headers = new HttpHeaders();
        headers.add("filetype", "1 Firmware Upgrade Image");
        headers.add("manufacturer", manufacturer);
        headers.add("modelName", modelName);
        headers.add("version", firmwareVersion);
        headers.add("oui", oui);
        headers.add("productclass", productClass);
        headers.add("uploadFileName", fileName);
        if (fileUrl != null) {
            headers.add("fileUrl", fileUrl);
        }
        if (username != null) {
            headers.add("username", username);
        }
        if (password != null) {
            headers.add("password", password);
        }
        if (md5 != null) {
            headers.add("md5", md5);
        }
        if (size != null) {
            headers.add("size", size);
        }
        if (isBasicFirmware != null) {
            headers.add("isBasicFirmware", isBasicFirmware.toString());
        }
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        try {
            URI uri = new URI(url);
            ResponseEntity<String> responseEntity = null;
            if (file != null) {
                HttpEntity<byte[]> requestEntity = new HttpEntity<byte[]>(IOUtils.toByteArray(file), headers);
                responseEntity = this.restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            } else {
                RequestEntity<MultiValueMap<String, Object>> requestEntity = new RequestEntity<>(headers, HttpMethod.PUT, uri);
                responseEntity = this.restTemplate.exchange(requestEntity, String.class);
            }
            if (responseEntity.getStatusCodeValue() != 200 && responseEntity.getStatusCodeValue() != 201) {
                return null;
            }
            return getByName(fileId);
        } catch (Exception e) {
            return null;
        } finally{
            try {
                file.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    public void delete(String fileId) throws ParseException {
        SCFile sCFile = get(fileId);
        String url = fileEndpoint + "/files/" + sCFile.fileId;
        this.restTemplate.delete(url);
    }

    public List<SCFile> search(SCFileSearchForm scFileSearchForm) throws ParseException {
        ResponseEntity<String> responseEntity = doSearch(scFileSearchForm);
        JsonArray jsonArray = new Gson().fromJson(responseEntity.getBody(), JsonArray.class);
        List<SCFile> listSCFiles = new ArrayList<>();
        if (jsonArray.size() > 0) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                listSCFiles.add(convertToFile(jsonObject));
            }
        }
        return listSCFiles;
    }

    public int count(SCFileSearchForm scFileSearchForm) {
        ResponseEntity<String> responseEntity = doSearch(scFileSearchForm);
        return Integer.valueOf(responseEntity.getHeaders().get("totalAll").get(0));
    }

    public ResponseEntity<String> doSearch(SCFileSearchForm scFileSearchForm) {
        String url = fileEndpoint + "/files";
        Map<String, String> queryParams = new HashMap<String, String>();
        Map<String, Object> mapParam = new HashMap<String, Object>();
        if (!Strings.isNullOrEmpty(scFileSearchForm.fileType)) {
            mapParam.put("metadata.fileType", scFileSearchForm.fileType);
        }
        if (!Strings.isNullOrEmpty(scFileSearchForm.oui)) {
            mapParam.put("metadata.oui", scFileSearchForm.oui);
        }
        if (!Strings.isNullOrEmpty(scFileSearchForm.productClass)) {
            mapParam.put("metadata.productClass", scFileSearchForm.productClass);
        }
        if (!Strings.isNullOrEmpty(scFileSearchForm.manufacturer)) {
            mapParam.put("metadata.manufacturer", scFileSearchForm.manufacturer);
        }
        if (!Strings.isNullOrEmpty(scFileSearchForm.modelName)) {
            mapParam.put("metadata.modelName", scFileSearchForm.modelName);
        }
        if (!Strings.isNullOrEmpty(scFileSearchForm.firmwareVersion)) {
            mapParam.put("metadata.version", String.format("/%s/", scFileSearchForm.firmwareVersion));
        }
        if (!Strings.isNullOrEmpty(scFileSearchForm.fileName)) {
            mapParam.put("metadata.uploadFileName", String.format("/%s/", scFileSearchForm.fileName));
        }
        if (scFileSearchForm.isBasicFirmware != null) {
            mapParam.put("metadata.isBasicFirmware", scFileSearchForm.isBasicFirmware.toString());
        }

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dt1.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        if (scFileSearchForm.createdFrom != null && scFileSearchForm.createdTo != null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$lte", dt1.format(scFileSearchForm.createdTo));
            obj.addProperty("$gte", dt1.format(scFileSearchForm.createdFrom));
            mapParam.put("uploadDate", obj);
        }

        if (scFileSearchForm.createdFrom != null && scFileSearchForm.createdTo == null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$gte", dt1.format(scFileSearchForm.createdFrom));
            mapParam.put("uploadDate", obj);
        }

        if (scFileSearchForm.createdFrom == null && scFileSearchForm.createdTo != null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$lte", dt1.format(scFileSearchForm.createdTo));
            mapParam.put("uploadDate", obj);
        }

        if (!mapParam.isEmpty()) {
            String query = new Gson().toJson(mapParam);
            queryParams.put("query", query);
        }
        if (scFileSearchForm.limit != null && scFileSearchForm.page != null) {
            queryParams.put("limit", Integer.toString(scFileSearchForm.limit));
            queryParams.put("skip", Integer.toString((scFileSearchForm.page - 1) * scFileSearchForm.limit));
        }
        queryParams.put("sort", "{\"uploadDate\":-1}");
        String queryString = StringUtils.queryStringFromMap(queryParams);
        if (!ObjectUtils.empty(queryString)) {
            url += "?" + queryString;
        }
        return this.restTemplate.getForEntity(url, String.class, queryParams);
    }

    public SCFile convertToFile(JsonObject jsonObject) throws ParseException {
        SCFile scFile = new SCFile();
        scFile.id = jsonObject.get("_id").getAsString();
        scFile.fileId = jsonObject.get("filename").getAsString();
        scFile.fileName = jsonObject.get("metadata").getAsJsonObject().get("uploadFileName").getAsString();
        scFile.url = fileUrl + "/" + jsonObject.get("filename").getAsString();
        scFile.fileType = jsonObject.get("metadata").getAsJsonObject().get("fileType").getAsString();
        scFile.oui = jsonObject.get("metadata").getAsJsonObject().get("oui").getAsString();
        scFile.productClass = jsonObject.get("metadata").getAsJsonObject().get("productClass").getAsString();
        scFile.modelName = jsonObject.get("metadata").getAsJsonObject().get("modelName").getAsString();
        scFile.manufacturer = jsonObject.get("metadata").getAsJsonObject().get("manufacturer").getAsString();
        scFile.firmwareVersion = jsonObject.get("metadata").getAsJsonObject().get("version").getAsString();
        if (jsonObject.get("metadata").getAsJsonObject().get("fileurl") != null) {
            scFile.fileUrl = jsonObject.get("metadata").getAsJsonObject().get("fileurl").getAsString();
            scFile.url = scFile.fileUrl;
        }
        if (jsonObject.get("metadata").getAsJsonObject().get("username") != null) {
            scFile.username = jsonObject.get("metadata").getAsJsonObject().get("username").getAsString();
        }
        if (jsonObject.get("metadata").getAsJsonObject().get("password") != null) {
            scFile.password = jsonObject.get("metadata").getAsJsonObject().get("password").getAsString();
        }
        if (jsonObject.get("metadata").getAsJsonObject().get("isBasicFirmware") != null) {
            scFile.isBasicFirmware = jsonObject.get("metadata").getAsJsonObject().get("isBasicFirmware").getAsBoolean();
        }
        scFile.createdTime = jsonObject.get("uploadDate") == null ? null : parseDate(jsonObject.get("uploadDate").getAsString());
        if (jsonObject.get("metadata").getAsJsonObject().get("md5") != null && !jsonObject.get("metadata").getAsJsonObject().get("md5").getAsString().isEmpty()) {
            scFile.md5 = jsonObject.get("metadata").getAsJsonObject().get("md5").getAsString();
        }
        else {
            scFile.md5 = jsonObject.get("md5") == null ? "" : jsonObject.get("md5").getAsString();
        }
        if (jsonObject.get("metadata").getAsJsonObject().get("size") != null && !jsonObject.get("metadata").getAsJsonObject().get("size").getAsString().isEmpty()) {
            scFile.length = jsonObject.get("metadata").getAsJsonObject().get("size").getAsInt();
            scFile.size = jsonObject.get("metadata").getAsJsonObject().get("size").getAsString();
        }
        else{
            scFile.length = jsonObject.get("length") == null ? null : jsonObject.get("length").getAsInt();
        }
        return scFile;
    }

    public static Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dt.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        return dt.parse(dateStr);
    }
}
