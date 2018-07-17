package vn.ssdc.vnpt.devices.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.swagger.annotations.Api;
import org.apache.xerces.parsers.DOMParser;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.ParameterDetail;
import vn.ssdc.vnpt.devices.services.ParameterDetailService;
import vn.ssdc.vnpt.dto.AcsResponse;
import vn.ssdc.vnpt.logging.model.ElkLoggingDevice;
import vn.ssdc.vnpt.logging.services.LoggingDeviceService;
import vn.ssdc.vnpt.utils.StringUtils;

import javax.ws.rs.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Created by thangnc on 14-Jun-17.
 */
@Component
@Path("rpc")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("RPC")
public class RPCEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RPCEndpoint.class);

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Autowired
    public LoggingDeviceService loggingDeviceService;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private ParameterDetailService parameterDetailService;

    /**
     * Reboot a specific device.
     *
     * @param deviceId The ID of the device
     * @return 202 if the tasks have been queued to be executed at the next inform.
     * 500 Internal server error
     * status code 200 if tasks have been successfully executed
     */
    @POST
    @Path(("/{deviceId}/reboot"))
    public AcsResponse reboot(@PathParam("deviceId") String deviceId,
                              Map<String, Object> request) {
        AcsResponse response = new AcsResponse();
        Boolean now = true;
        String commandKey = null;
        try {
            if (request.get("now") != null) {
                now = Boolean.valueOf((String) request.get("now"));
            }
            if (request.get("commandKey") != null) {
                commandKey = (String) request.get("commandKey");
            }
            ResponseEntity<String> responseEntity = acsClient.createRebootTask(deviceId, commandKey, now, null);
            response.httpResponseCode = responseEntity.getStatusCode().value();
            response.body = responseEntity.getBody();
        } catch (RestClientException e) {
            response.httpResponseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return response;
    }

    /**
     * Gets parameter names for a device
     * Ask ACS to execute a task to get values for given parameters
     *
     * @param deviceId id of device in ACS
     * @param request  a map containing keys "now" and "parameters"
     * @return AcsResponse object
     */
    @POST
    @Path("/{deviceId}/get-parameter-names")
    public AcsResponse getParameterValues(@PathParam("deviceId") String deviceId,
                                          Map<String, Object> request) {
        AcsResponse response = new AcsResponse();
        Boolean now = true;
        Boolean nextLevel = true;
        String parameterPath = null;
        try {
            if (request.get("now") != null) {
                now = (Boolean) request.get("now");
            }
            if (request.get("nextLevel") != null) {
                nextLevel = Boolean.valueOf((String) request.get("nextLevel"));
            }
            if (request.get("parameterPath") != null) {
                parameterPath = (String) request.get("parameterPath");
            }
            ResponseEntity<String> responseEntity = this.acsClient.getParameterNames(deviceId, parameterPath, nextLevel, now);
            response.httpResponseCode = responseEntity.getStatusCodeValue();
            response.body = responseEntity.getBody();
        } catch (RestClientException e) {
            response.httpResponseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return response;
    }

    @POST
    @Path("/{deviceId}/downloadFile")
    public AcsResponse downloadFile(@PathParam("deviceId") String deviceId,
                                    Map<String, Object> request) {
        AcsResponse response = new AcsResponse();
        Boolean now = (Boolean) request.get("now");
        String fileType = (String) request.get("fileType");
        String url = (String) request.get("url");
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String successUrl = (String) request.get("successUrl");
        String failureUrl = (String) request.get("failureUrl");
        String commandKey = (String) request.get("commandKey");
        int fileSize = Integer.parseInt((String) request.get("fileSize"));
        String targetFileName = (String) request.get("targetFileName");
        int delaySeconds = Integer.parseInt((String) request.get("delaySeconds"));
        Boolean status = Boolean.valueOf((String) request.get("status"));
        String startTime = (String) request.get("startTime");
        String completeTime = (String) request.get("completeTime");
        try {
            ResponseEntity<String> entity = this.acsClient.downloadUrlFile(deviceId, fileType, url, username, password,
                    successUrl, failureUrl, commandKey, fileSize, targetFileName, delaySeconds, status, startTime, completeTime, now);
            response.httpResponseCode = entity.getStatusCodeValue();
            response.body = entity.getBody();
        } catch (RestClientException e) {
            response.httpResponseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return response;
    }

    @POST
    @Path("/{deviceId}/uploadFile")
    public AcsResponse uploadFile(@PathParam("deviceId") String deviceId,
                                  Map<String, Object> request) {
        Boolean now = (Boolean) request.get("now");
        String fileType = (String) request.get("fileType");
        String url = (String) request.get("url");
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        int delaySeconds = Integer.parseInt((String) request.get("delaySeconds"));
        String commandKey = (String) request.get("commandKey");
        ResponseEntity<String> entity = this.acsClient.createUploadFileTask(deviceId, fileType, url, username, password, delaySeconds,
                commandKey, now, null);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = entity.getStatusCodeValue();
        response.body = entity.getBody();
        return response;
    }

    Map<String, String> mapFirst = new HashMap<>();
    Map<String, String> mapSecond = new HashMap<>();

    @POST
    @Path("/{deviceId}/set-attribute-draytek/{deviceTypeVersion}")
    public AcsResponse set_attribute_draytek(@PathParam("deviceId") String deviceId,
                                             @PathParam("deviceTypeVersion") String deviceTypeVersion) throws IOException {
        AcsResponse response = new AcsResponse();
        int maxParamSend = 50;

        List<DeviceAttribute> lstDeviceAttribute = new ArrayList<>();

        //Step 1 : GET ALL AVAIL PARAM
        List<ParameterDetail> lstParamAvail = parameterDetailService.findByDraytek(deviceTypeVersion);

        //Step 1.1 : CHECK SIZE
        if (lstParamAvail.size() <= maxParamSend) {
            for (ParameterDetail pd : lstParamAvail) {
                String param = pd.path;
                DeviceAttribute deviceAttribute = new DeviceAttribute(param, true, DeviceAttribute.ACTIVE_NOTIFICATION, false);
                lstDeviceAttribute.add(deviceAttribute);
            }
            acsClient.setParameterAttributes(deviceId, lstDeviceAttribute.toString(), true);
        } else {
            for (int i = 0; i < lstParamAvail.size(); i++) {
                ParameterDetail pd = lstParamAvail.get(i);
                String param = pd.path;
                DeviceAttribute deviceAttribute = new DeviceAttribute(param, true, DeviceAttribute.ACTIVE_NOTIFICATION, false);
                lstDeviceAttribute.add(deviceAttribute);
                if(i%maxParamSend==0){
                    acsClient.setParameterAttributes(deviceId, lstDeviceAttribute.toString(), true);
                    lstDeviceAttribute = new ArrayList<>();
                }
            }
            acsClient.setParameterAttributes(deviceId, lstDeviceAttribute.toString(), true);
        }
        return response;
    }

    @POST
    @Path("/{deviceId}/get-parameter-names_draytek/{option}/{deviceTypeVersionId}")
    public AcsResponse getParameterValues_draytek(@PathParam("deviceId") String deviceId,
                                                  @PathParam("option") String option,
                                                  @PathParam("deviceTypeVersionId") String deviceTypeVersionId,
                                                  Map<String, Object> request) throws IOException {
        //Step 1 : Get Param Name
        AcsResponse response = new AcsResponse();
        Boolean now = true;
        Boolean nextLevel = true;
        String parameterPath = null;

        try {
            if (request.get("now") != null) {
                now = (Boolean) request.get("now");
            }
            if (request.get("nextLevel") != null) {
                nextLevel = Boolean.valueOf((String) request.get("nextLevel"));
            }
            if (request.get("parameterPath") != null) {
                parameterPath = (String) request.get("parameterPath");
            }
            ResponseEntity<String> responseEntity = this.acsClient.getParameterNames(deviceId, parameterPath, nextLevel, now);
            response.httpResponseCode = responseEntity.getStatusCodeValue();
            response.body = responseEntity.getBody();
            JsonObject obj = new JsonParser().parse(response.body).getAsJsonObject();

            String strTask_Id = obj.get("_id").toString();
            String fromDate = getDate();
            //Step 2 : Check response log each 20 sec
            waitForResponse(40);
            //Step 3: Run ElasticSearch
            String strMessageReceive = getFromEL(fromDate, strTask_Id, deviceId);
            int beginXML = strMessageReceive.lastIndexOf("<?xml version=\"1.0\"?>");
            int endXML = strMessageReceive.lastIndexOf("</soap-env:Envelope>") + "</soap-env:Envelope>".length();
            String strHandle = strMessageReceive.substring(beginXML, endXML);
            //Step 4: Get list Param Device
            List<String> lstReturn = handleXML(strHandle);
            String strTask_Id_2 = getValueParam(lstReturn, deviceId);
            fromDate = getDate();
            waitForResponse(130);
            //Step 5: Get value each param
            strMessageReceive = getFromEL(fromDate, strTask_Id_2, deviceId);
            beginXML = strMessageReceive.indexOf("<?xml version=\"1.0\"?>");
            endXML = strMessageReceive.lastIndexOf("</soap-env:Envelope>") + "</soap-env:Envelope>".length();
            strHandle = strMessageReceive.substring(beginXML, endXML);

            Map<String, String> mapReturn = handleBigXML(strHandle);
            //Store
            if (option.equals("1")) {
                mapFirst = mapReturn;
                String strReturn = "Option = 1 : Store Param Done";
                response.body = strReturn;
            } else {
                //Compare
                mapSecond = mapReturn;

                //TODO
                //COMPARE 2 MAP
                StringBuilder strReturn = compare2Map(mapFirst, mapSecond, deviceTypeVersionId);
                response.body = strReturn.toString();
            }
        } catch (RestClientException e) {
            response.httpResponseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return response;
    }

    public StringBuilder compare2Map(Map<String, String> map1, Map<String, String> map2, String deviceTypeVersionId) throws IOException {
        Map<String, String> result_1 = map1.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        Map<String, String> result_2 = map2.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        StringBuilder strReturn = new StringBuilder();

        //Check Write Param
        List<ParameterDetail> lstParamAvail = parameterDetailService.findByDraytek(deviceTypeVersionId);

        for (Map.Entry<String, String> entry : result_2.entrySet()) {
            String key_2 = entry.getKey();
            String value_2 = entry.getValue();

            String value_1 = result_1.get(key_2);
            if (value_1 != null) {
                if (!value_1.trim().equalsIgnoreCase(value_2.trim())) {
                    boolean blCheck = false;
                    //Check Avail Param Allow Editable
                    for (int i = 0; i < lstParamAvail.size(); i++) {
                        String strParam = lstParamAvail.get(i).path + "\n";
                        if (key_2.equalsIgnoreCase(strParam)) {
                            blCheck = true;
                            break;
                        }
                    }

                    if (blCheck) {
                        strReturn.append(System.getProperty("line.separator"));
                        strReturn.append("Key CAN Modify : " + key_2);
                        strReturn.append(System.getProperty("line.separator"));
                        strReturn.append("Value Current : " + value_1);
                        strReturn.append(System.getProperty("line.separator"));
                        strReturn.append("Value Modify : " + value_2);
                        strReturn.append(System.getProperty("line.separator"));
                        strReturn.append("-----------------------------------------");
                    } else {
                        strReturn.append(System.getProperty("line.separator"));
                        strReturn.append("Key CAN NOT Modify : " + key_2);
                        strReturn.append(System.getProperty("line.separator"));
                        strReturn.append("Value Current : " + value_1);
                        strReturn.append(System.getProperty("line.separator"));
                        strReturn.append("Value Modify : " + value_2);
                        strReturn.append(System.getProperty("line.separator"));
                        strReturn.append("-----------------------------------------");
                    }
                }
            }
        }


        //WriteToFile
        Files.write(Paths.get("d:/output.txt"), strReturn.toString().getBytes());
        return strReturn;
    }

    public Map handleBigXML(String strXML) {
        Map<String, String> mapReturn = new HashMap<String, String>();
        String[] spilitHandle = strXML.split("<Name>");
        for (int i = 0; i < spilitHandle.length; i++) {
            String strHandle = spilitHandle[i];
            int endIndex = strHandle.indexOf("</Value>");
            if (endIndex > 0) {
                strHandle = strHandle.substring(0, endIndex);
                strHandle = strHandle.replace("</Name>", "");

                int endIndexParam = strHandle.indexOf("<");
                int beginIndexValue = strHandle.indexOf(">") + ">".length();

                String param = strHandle.substring(0, endIndexParam);
                String value = strHandle.substring(beginIndexValue, strHandle.length());
                mapReturn.put(param, value);
            }
        }
        return mapReturn;
    }

    public String getFromEL(String fromDate, String strTask_Id, String strDeviceID) throws IOException {
        String strMessageReceive = "";
        for (; ; ) {
            JestClient jestClient = elasticSearchClient();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders
                    .boolQuery();

            int i = 1;
            String toDate = getDate();
            try {
                boolQueryBuilder.must(QueryBuilders
                        .rangeQuery("@timestamp")
                        .gte(parseIsoDate(fromDate))
                        .lt(parseIsoDate(toDate))
                        .includeLower(true)
                        .includeUpper(true));
                boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", strTask_Id));

            } catch (ParseException e) {
                e.printStackTrace();
            }

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder).size(9999);
            Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
            builder.addIndex("logging_device");
            builder.addType("logging_device");

            SearchResult result = jestClient.execute(builder.build());
            List<ElkLoggingDevice> elkLoggingDevices = result.getSourceAsObjectList(ElkLoggingDevice.class);

            if (elkLoggingDevices.size() > 0) {
                strMessageReceive = elkLoggingDevices.get(0).message;
                break;
            }
            i++;
            waitForResponse(10);
        }
        return strMessageReceive;
    }


    public String getValueParam(List<String> lstParam, String strDeviceID) {
        Boolean now = true;
        ResponseEntity<String> responseEntity = this.acsClient.getParameterValues(strDeviceID, lstParam, now);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.body = responseEntity.getBody();

        JsonObject obj = new JsonParser().parse(response.body).getAsJsonObject();

        String strTask_Id = obj.get("_id").toString();

        return strTask_Id;
    }

    private static void waitForResponse(int timeWait) {
        while (timeWait > 0) {
            try {
                timeWait--;
                Thread.sleep(1000L);    // 1000L = 1000ms = 1 second
            } catch (InterruptedException e) {
                //I don't think you need to do anything for your particular problem
            }
        }
    }

    public static String parseIsoDate(String date) throws ParseException {
        return StringUtils.convertDateToElk(date, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }

    public static String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    public JestClient elasticSearchClient() {
        JestClientFactory jestClientFactory = new JestClientFactory();
        jestClientFactory.setHttpClientConfig(new HttpClientConfig.Builder(elasticSearchUrl)
                .multiThreaded(true)

                .build());

        return jestClientFactory.getObject();
    }

    public List<String> handleXML(String xml) {
        List<String> lstReturn = new ArrayList<String>();

        DOMParser parser = new DOMParser();
        try {
            parser.parse(new InputSource(new java.io.StringReader(xml)));
            Document doc = parser.getDocument();
            NodeList nl = doc.getElementsByTagName("Name");

            if (nl != null) {
                int length = nl.getLength();
                for (int i = 0; i < length; i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) nl.item(i);
                        String strParam = el.getFirstChild().getNodeValue();
                        if (!strParam.substring(strParam.length() - 1, strParam.length()).equalsIgnoreCase(".")) {
                            lstReturn.add(el.getFirstChild().getNodeValue());
                        }
                    }
                }
            }
        } catch (SAXException e) {
            // handle SAXException
        } catch (IOException e) {
            // handle IOException
        }
        return lstReturn;
    }
}
