/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.nbi.stb.endpoints;

import com.google.common.base.Strings;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.ssdc.vnpt.nbi.stb.model.STBUpgradeFirmwareRequest;
import vn.ssdc.vnpt.nbi.stb.model.STBUpgradeFirmwareResponse;
import vn.ssdc.vnpt.nbi.stb.services.NBISTBServices;
import vn.ssdc.vnpt.rabbitmq.endpoints.CwmpEndPoint;

import java.util.Base64;

/**
 *
 * @author kiendt
 */
@Controller
public class NBISTBEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(CwmpEndPoint.class);
    @Autowired
    NBISTBServices nbiSTBService;

    @RequestMapping(value = "/update-firmware", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public STBUpgradeFirmwareResponse upgradeFirmware(@RequestBody STBUpgradeFirmwareRequest request, @RequestHeader("Authorization") String authorization) {
        Gson g = new Gson();
        String base64Credentials = authorization.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));
        STBUpgradeFirmwareResponse response = new STBUpgradeFirmwareResponse();
        try {
            if (!Strings.isNullOrEmpty(request.request) && "update_firmware".equals(request.request)) {
                if (nbiSTBService.authenticateUpgradeRequest(credentials)) {
                    response = nbiSTBService.upgradeFirmware(request);
                } else {
                    response.code = STBUpgradeFirmwareResponse.STB_RESPONSE.REPONSE_01.errorCode();
                    response.message = STBUpgradeFirmwareResponse.STB_RESPONSE.REPONSE_01.message();
                }
            } else {
                response.code = STBUpgradeFirmwareResponse.STB_RESPONSE.REPONSE_07.errorCode();
                response.message = STBUpgradeFirmwareResponse.STB_RESPONSE.REPONSE_07.message();
            }
        } catch (Exception e) {
            response.code = STBUpgradeFirmwareResponse.STB_RESPONSE.REPONSE_10.errorCode();
            response.message = STBUpgradeFirmwareResponse.STB_RESPONSE.REPONSE_10.message();
        }
        logger.info("API OTA:" + g.toJson(response));
        return response;
    }
}
