/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.nbi.stb.services;

import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.nbi.stb.model.STBUpgradeFirmwareRequest;
import vn.ssdc.vnpt.nbi.stb.model.STBUpgradeFirmwareResponse;
import vn.ssdc.vnpt.selfCare.model.SCFile;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCFileSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceFile;

/**
 *
 * @author kiendt
 */
@Service
public class NBISTBServices {

    public static final String MAC_ADDRESS_KEY = "Device.DeviceInfo.MAC";
    public static final String MODEL_NAME_KEY = "Device.DeviceInfo.ModelName";

    @Value("${stb.pre_autho}")
    private String preAuthenString;

    @Value("${stb.suff_autho}")
    private String suffAuthenString;

    @Autowired
    private SelfCareServiceDevice selfCareServiceDevice;

    @Autowired
    private SelfCareServiceFile selfCareServiceFile;

    public boolean authenticateUpgradeRequest(String credentials) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (Strings.isNullOrEmpty(credentials)) {
            return false;
        }
        String macAddress = credentials.split(":")[0];
        String password = credentials.split(":")[1];
        String passwordBySHA = generatePassword(macAddress);
        if (!Strings.isNullOrEmpty(password) && password.equals(passwordBySHA)) {
            return true;
        } else {
            return false;
        }
    }

    public String generatePassword(String authentication) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String mUsername = authentication;
        String mPassword;
        if ((preAuthenString != null) && (suffAuthenString == null)) {
            mPassword = preAuthenString + mUsername;
        } else if ((preAuthenString == null) && (suffAuthenString != null)) {
            mPassword = mUsername + suffAuthenString;
        } else if ((preAuthenString != null) && (suffAuthenString != null)) {
            mPassword = preAuthenString + mUsername + suffAuthenString;
        } else {
            mPassword = mUsername;
        }
        mPassword = getSHA1(mPassword).substring(0, 16);
        return mPassword;
    }

    public String getSHA1(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = null;
        byte[] input = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            input = digest.digest(str.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
            throw e1;
        }
        return convertToHex(input);
    }

    public String convertToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return sb.toString();
    }

    public STBUpgradeFirmwareResponse upgradeFirmware(STBUpgradeFirmwareRequest stbRequest) throws ParseException {
        STBUpgradeFirmwareResponse response = new STBUpgradeFirmwareResponse();
        String modelName = stbRequest.device;
        SCFileSearchForm fileSearchForm = new SCFileSearchForm();
        fileSearchForm.modelName = modelName;
        fileSearchForm.isBasicFirmware = false;
        fileSearchForm.limit = 1;
        List<SCFile> files = selfCareServiceFile.search(fileSearchForm);
        if (!files.isEmpty()) {
            SCFile file = files.get(0);
            if (file != null) {
                response.delta_url = file.url;
                response.delta_md5 = file.md5;
                response.delta_fw_size = String.valueOf(file.length);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyy");
                response.releaseDate = simpleDateFormat.format(file.createdTime);
                response.releaseNote = "";
                response.device = modelName;
                response.version = file.firmwareVersion;
                response.code = STBUpgradeFirmwareResponse.STB_RESPONSE.REPONSE_00.errorCode();
                response.message = STBUpgradeFirmwareResponse.STB_RESPONSE.REPONSE_00.message();
            }
        }

        SCFileSearchForm basicFileSearchForm = new SCFileSearchForm();
        basicFileSearchForm.modelName = modelName;
        basicFileSearchForm.isBasicFirmware = true;
        basicFileSearchForm.limit = 1;
        List<SCFile> basicFiles = selfCareServiceFile.search(basicFileSearchForm);
        if (!basicFiles.isEmpty()) {
            SCFile basicFile = basicFiles.get(0);
            if (basicFile != null) {
                response.url = basicFile.url;
                response.md5 = basicFile.md5;
                response.size = String.valueOf(basicFile.length);
            }
        }
        return response;
    }
}
