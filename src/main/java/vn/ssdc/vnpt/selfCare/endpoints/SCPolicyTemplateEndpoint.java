/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.policy.model.PolicyTemplate;
import vn.ssdc.vnpt.policy.services.PolicyTemplateService;
import vn.ssdc.vnpt.selfCare.model.SCPolicyTemplate;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCPolicySearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCPolicyTemplateSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServicePolicyTemplate;
import vn.ssdc.vnpt.umpexception.QosException;

/**
 *
 * @author kiendt
 */
@Component
@Path("/self-care/policy-template")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Policy Template")
public class SCPolicyTemplateEndpoint {

    @Autowired
    private PolicyTemplateService policyTemplateService;

    @Autowired
    private SelfCareServicePolicyTemplate selfCareServicePolicyTemplate;

    @POST
    @Path("/search")
    @ApiOperation(value = "do search")
    public List<SCPolicyTemplate> search(@RequestBody SCPolicyTemplateSearchForm searchForm) {
        return selfCareServicePolicyTemplate.search(searchForm);
    }

    @POST
    @Path("/count")
    @ApiOperation(value = "do count")
    public int count(@RequestBody SCPolicyTemplateSearchForm searchForm) {
        return selfCareServicePolicyTemplate.count(searchForm);
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get infor policy template")
    public SCPolicyTemplate getPolicyTemplate(@PathParam("id") Long id) {
        return new SCPolicyTemplate(policyTemplateService.get(id));
    }

    @GET
    @Path("/getAll")
    @ApiOperation(value = "Get infor policy template")
    public List<SCPolicyTemplate> getAll() {
        List<SCPolicyTemplate> sCPolicyTemplates = new ArrayList<>();
        List<PolicyTemplate> policyTemplates = policyTemplateService.getAll();
        for (PolicyTemplate policyTemplate : policyTemplates) {
            sCPolicyTemplates.add(new SCPolicyTemplate(policyTemplate));
        }
        return sCPolicyTemplates;
    }

    @POST
    @ApiOperation(value = "create policy template")
    public SCPolicyTemplate addPolicyTemplate(@RequestBody SCPolicyTemplate sCPolicyTemplate) throws QosException {
        PolicyTemplate policyTemplate = sCPolicyTemplate.convertToPolicyTemplate();
        policyTemplate.standardObject();
        policyTemplateService.validate(policyTemplate);
        return new SCPolicyTemplate(policyTemplateService.create(policyTemplate));
    }

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "update policy template")
    public SCPolicyTemplate updatePolicyTemplate(@PathParam("id") Long id, @RequestBody SCPolicyTemplate sCPolicyTemplate) throws QosException {
        PolicyTemplate policyTemplate = sCPolicyTemplate.convertToPolicyTemplate();
        policyTemplate.standardObject();
        policyTemplateService.validate(policyTemplate);
        policyTemplate.id = id;
        return new SCPolicyTemplate(policyTemplateService.update(id, policyTemplate));
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "delete policy template")
    public void deletePolicyTemplate(@PathParam("id") Long id) {
        policyTemplateService.delete(id);
    }

}
