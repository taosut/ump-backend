package vn.ssdc.vnpt.devices.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.InputStream;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.Tr069Parameter;
import vn.ssdc.vnpt.devices.model.Tr069Profile;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

/**
 * Created by kiendt on 2/7/2017.
 */
@Service
public class Tr069ProfileService extends SsdcCrudService<Long, Tr069Profile> {

    private static final Logger logger = LoggerFactory.getLogger(Tr069ProfileService.class);

    @Autowired
    public Tr069ProfileService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Tr069Profile.class);
    }

    @Autowired
    public Tr069ParameterService tr069ParameterService;

    public String parseParameter(Node profileNode, Integer type) {
        JsonArray arrayParameter = new JsonArray();
        List<Node> listObjectNode = profileNode.selectNodes("object");
        for (Node tmp : listObjectNode) {
            String baseObjectName = tmp.valueOf("@ref");
            List<Node> listParameterNodes = tmp.selectNodes("parameter");
            for (Node tmpParameter : listParameterNodes) {
                String pathParameter = baseObjectName + tmpParameter.valueOf("@ref");
                if (type == 2) {
                    pathParameter = "InternetGatewayDevice.Services." + pathParameter;
                } else if (type == 3) {
                    pathParameter = "Device.Services." + pathParameter;
                }
                Tr069Parameter tr069Parameter = tr069ParameterService.searchByPath(pathParameter);
                if (tr069Parameter != null) {
                    JsonObject objectParameter = new Gson().fromJson(new Gson().toJson(tr069Parameter), JsonObject.class);
                    arrayParameter.add(objectParameter);
                }
            }
        }
        return arrayParameter.toString();
    }

    public List<Tr069Profile> getProfileIsDiagnostics() {
        String whereExp = "diagnostics=1 ";
        List<Tr069Profile> list = this.repository.search(whereExp);
        if (!ObjectUtils.empty(list)) {
            return list;
        }
        return null;
    }

    public void importProfile(String nameFile, Document document, int type) throws DocumentException {

        List<Node> objects = document.selectNodes("//dm:document/model/object");
        List<Node> dataTypeNodes = document.selectNodes("//dm:document/dataType");
        List<Node> profiles = document.selectNodes("//dm:document/model/profile");
        logger.info("{}", "Start insert to paramters table");
        for (Node object : objects) {
            List<Node> listParameterNode = object.selectNodes("parameter");
            for (Node childNode : listParameterNode) {
                if (type == 1) {
                    tr069ParameterService.create(tr069ParameterService.processChildObjectNode(dataTypeNodes, object, childNode, profiles, nameFile, 1));
                } else {
                    tr069ParameterService.create(tr069ParameterService.processChildObjectNode(dataTypeNodes, object, childNode, profiles, nameFile, 2));
                    tr069ParameterService.create(tr069ParameterService.processChildObjectNode(dataTypeNodes, object, childNode, profiles, nameFile, 3));
                }
            }
        }
        logger.info("{}", "Finish insert to parametes table");
        logger.info("{}", "Start insert to profiles");
        for (Node object : profiles) {
            Tr069Profile tr069Profile = new Tr069Profile();
            String uniqueProfileName = object.valueOf("@name");
            tr069Profile.version = object.valueOf("@dmr:version");
            if (type == 1) {
                tr069Profile.name = uniqueProfileName + ":" + nameFile;
                tr069Profile.parameters = parseParameter(object, 1);
                if (tr069Profile.name.contains("Diagnostics") || tr069Profile.parameters.contains("Diagnostics")) {
                    tr069Profile.diagnostics = true;
                } else {
                    tr069Profile.diagnostics = false;
                }
                create(tr069Profile);
            } else if (type == 2) {
                //Add services for InternetGatewayDevice Root
                tr069Profile.name = uniqueProfileName + ":" + nameFile + ":InternetGatewayDevice";
                tr069Profile.parameters = parseParameter(object, 2);
                if (tr069Profile.name.contains("Diagnostics") || tr069Profile.parameters.contains("Diagnostics")) {
                    tr069Profile.diagnostics = true;
                } else {
                    tr069Profile.diagnostics = false;
                }
                create(tr069Profile);
                //Add services for Device Root
                tr069Profile.id = null;
                tr069Profile.name = uniqueProfileName + ":" + nameFile + ":Device";
                tr069Profile.parameters = parseParameter(object, 3);
                if (tr069Profile.name.contains("Diagnostics") || tr069Profile.parameters.contains("Diagnostics")) {
                    tr069Profile.diagnostics = true;
                } else {
                    tr069Profile.diagnostics = false;
                }
                create(tr069Profile);
            }
        }
        logger.info("{}", "Finish insert to profiles");
    }

}
