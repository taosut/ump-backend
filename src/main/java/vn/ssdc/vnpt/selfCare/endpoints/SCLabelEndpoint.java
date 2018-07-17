package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.ssdc.vnpt.selfCare.model.SCLabel;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.mapping.model.AccountMapping;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.mapping.model.IpMapping;
import vn.ssdc.vnpt.mapping.services.AccountMappingService;
import vn.ssdc.vnpt.selfCare.model.SCDeviceGroup;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCLabelForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDeviceGroup;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceLabel;
import vn.ssdc.vnpt.umpexception.UmpNbiException;

/**
 * Created by THANHLX on 11/30/2017.
 */
@Component
@Path("/self-care/labels")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Labels")
public class SCLabelEndpoint {

    @Autowired
    private LabelService labelService;

    @Autowired
    private SelfCareServiceLabel selfCareServiceLabel;

    @Autowired
    private SelfCareServiceDeviceGroup selfCareServiceDeviceGroup;

    @Autowired
    private vn.ssdc.vnpt.mapping.services.IpMappingService ipMappingService;

    @Autowired
    private AccountMappingService accountMappingService;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @GET
    @ApiOperation(value = "Read all labels")
    @ApiResponse(code = 200, message = "Success", response = SCLabel.class)
    public List<SCLabel> getAll() {
        List<Label> listLabels = labelService.getAll();
        List<SCLabel> listSCLabels = new ArrayList<>();
        for (Label label : listLabels) {
            SCLabel scLabel = selfCareServiceLabel.convertFromLabelToSCLabel(label);
            Set<IpMapping> ipMappings = new HashSet<>(ipMappingService.getByLabelID(label.id));
            scLabel.ipMapping = ipMappings;

            Set<AccountMapping> accountMappings = new HashSet<>(accountMappingService.getByLabelID(label.id));
            scLabel.accountMapping = accountMappings;
            // chua co devicegroupID
            //---------
            //
            listSCLabels.add(scLabel);
        }
        return listSCLabels;
    }

    @POST
    @ApiOperation(value = "Create label")
    @ApiResponse(code = 200, message = "Success", response = SCLabel.class)
    public SCLabel create(@RequestBody SCLabelForm scLabelForm) {
        // create label
        Label label = selfCareServiceLabel.convertFromSCLabelToLabel(scLabelForm);
        label = labelService.create(label);
        SCLabel scLabelCreated = selfCareServiceLabel.convertFromLabelToSCLabel(label);
        // check duplicate mapping
        String labelCommon = selfCareServiceLabel.createAllLabel(label);
        try {
            selfCareServiceLabel.checkDulicateMapping(label.id, scLabelForm);
        } catch (Exception e) {
            labelService.delete(scLabelCreated.id);
            throw e;
        }

        if (scLabelForm.ipMapping != null) {
            selfCareServiceLabel.createIpMappingForLabel(scLabelForm.ipMapping, scLabelCreated.id, labelCommon);
        }
        // create account mapping 
        if (scLabelForm.accountMapping != null) {
            selfCareServiceLabel.createAccountMappingForLabel(scLabelForm.accountMapping, scLabelCreated.id, labelCommon);
        }
        // create device group for this label
        SCDeviceGroup scDeviceGroup = new SCDeviceGroup();
        scDeviceGroup.manufacturer = "All";
        scDeviceGroup.modelName = "All";
        scDeviceGroup.firmwareVersion = "All";
        scDeviceGroup.name = scLabelCreated.name;
        Set<String> labels = new HashSet<>();
        labels.add(labelCommon);
        scDeviceGroup.labels = labels;
        Set<Long> labelIds = new HashSet<>();
        labelIds.add(scLabelCreated.id);
        scDeviceGroup.labelIds = labelIds;
        scLabelCreated.deviceGroupId = selfCareServiceDeviceGroup.create(scDeviceGroup).id;
        label = selfCareServiceLabel.convertFromSCLabelToLabel(scLabelCreated);
        labelService.update(label.id, label);

        return scLabelCreated;
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Delete label")
    @ApiResponse(code = 200, message = "Success", response = SCLabel.class)
    public void delete(@PathParam("id") Long id) throws UmpNbiException {
        labelService.delete(id);
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Read label")
    @ApiResponse(code = 200, message = "Success", response = SCLabel.class)
    public SCLabel read(@PathParam("id") Long id) {
        SCLabel scLabel = selfCareServiceLabel.convertFromLabelToSCLabel(labelService.get(id));
        Set<IpMapping> ipMappings = new HashSet<>(ipMappingService.getByLabelID(id));
        scLabel.ipMapping = ipMappings;
        // account mapping
        Set<AccountMapping> accountMappings = new HashSet<>(accountMappingService.getByLabelID(id));
        scLabel.accountMapping = accountMappings;
        // chua co devicegroup id
        // ---

        return scLabel;
    }

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "Edit label")
    @ApiResponse(code = 200, message = "Success", response = SCLabel.class)
    public SCLabel update(@PathParam("id") Long id, @RequestBody SCLabelForm scLabelForm) {

//        Label label = selfCareServiceLabel.convertFromSCLabelToLabel(scLabel);
        SCLabel scLabelCreated = selfCareServiceLabel.convertFromLabelToSCLabel(labelService.get(id));
        Label label = selfCareServiceLabel.convertFromSCLabelToLabel(selfCareServiceLabel.mappingLabel(scLabelForm, scLabelCreated));
        try {
            selfCareServiceLabel.checkDulicateMapping(id, scLabelForm);
        } catch (Exception e) {
            throw e;
        }
        labelService.update(id, label);
        String labelCommon = selfCareServiceLabel.createAllLabel(label);
        if (scLabelForm.ipMapping != null) {
            // delete all ipmapping of label
            List<IpMapping> ipMappings = ipMappingService.getByLabelID(id);
            for (IpMapping tmp : ipMappings) {
                ipMappingService.delete(tmp.id);
            }
            // try create again
            selfCareServiceLabel.createIpMappingForLabel(scLabelForm.ipMapping, scLabelCreated.id, labelCommon);
        }
        // create account mapping 
        // delte all accountmapping of label
        if (scLabelForm.accountMapping != null) {
            List<AccountMapping> accountMappings = accountMappingService.getByLabelID(id);
            for (AccountMapping tmp : accountMappings) {
                accountMappingService.delete(tmp.id);
            }
            selfCareServiceLabel.createAccountMappingForLabel(scLabelForm.accountMapping, scLabelCreated.id, labelCommon);
        }

        // update device group for this label
        SCDeviceGroup scDeviceGroup = selfCareServiceDeviceGroup.convertDeviceGroupToSCDeviceGroup(deviceGroupService.get(label.deviceGroupId));
        scDeviceGroup.manufacturer = "All";
        scDeviceGroup.modelName = "All";
        scDeviceGroup.firmwareVersion = "All";
        scDeviceGroup.name = scLabelCreated.name;
        Set<String> labels = new HashSet<>();
        labels.add(labelCommon);
        scDeviceGroup.labels = labels;
        Set<Long> labelIds = new HashSet<>();
        labelIds.add(scLabelCreated.id);
        scDeviceGroup.labelIds = labelIds;
        scLabelCreated.deviceGroupId = selfCareServiceDeviceGroup.update(label.deviceGroupId, scDeviceGroup).id;
        return scLabelCreated;

    }

    @POST
    @Path("/check-in-use/{id}")
    @ApiOperation(value = "Check in use")
    @ApiResponse(code = 200, message = "Success", response = Boolean.class)
    public boolean checkInUse(@PathParam("id") Long id) throws UmpNbiException, Exception {
        if (labelService.isParent(id)) {
            return true;
        }
        return selfCareServiceLabel.checkInUse(id);
    }

}
