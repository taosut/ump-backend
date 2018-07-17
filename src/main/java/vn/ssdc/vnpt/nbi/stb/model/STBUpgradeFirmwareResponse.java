/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.nbi.stb.model;

/**
 *
 * @author kiendt
 */
public class STBUpgradeFirmwareResponse extends Response {

    public enum STB_RESPONSE {
        REPONSE_00("OK_SUCCESSFULLY", 0),
        REPONSE_01("NOK_UNAUTHORIZED", 1),
        REPONSE_02("NOK_INVALID_USERNAME", 2),
        REPONSE_03("NOK_INVALID_PARAMETER", 3),
        REPONSE_04("NOK_ACCOUNT_EXIST", 4),
        REPONSE_05("NOK_VERSION_NOT_FOUND", 5),
        REPONSE_06("PASSWORD_NOT_MATCH", 6),
        REPONSE_07("NOK_MALFORMED_REQUEST", 7),
        REPONSE_10("NOK_UNKNOWN_ERROR", 10),
        REPONSE_11("FIRMWARE_DOWNLOADED", 11),
        REPONSE_12("NOK_INVALID_SERIAL_NUMBER", 12),
        REPONSE_13("NOK_SERIAL_NUMBER_NOT_EXIST", 13),
        REPONSE_14("NOK_DATA_NOT_FOUND", 14);

        private String message;
        private Integer errorCode;

        STB_RESPONSE(String message, Integer errorCode) {
            this.message = message;
            this.errorCode = errorCode;
        }

        public String message() {
            return message;
        }

        public Integer errorCode() {
            return errorCode;
        }
    }

    public String releaseDate;
    public String device;
    public String delta_md5;
    public String md5;
    public String releaseNote;
    public String delta_url;
    public String url;
    public String delta_fw_size;
    public String size;
    public String version;

}
