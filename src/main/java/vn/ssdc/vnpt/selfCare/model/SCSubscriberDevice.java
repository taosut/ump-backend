/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import vn.ssdc.vnpt.subscriber.model.SubscriberDevice;

/**
 *
 * @author Admin
 */
public class SCSubscriberDevice {

    public Long id;
    public String subscriberId;
    public String deviceId;
    public String manufacturer;
    public String oui;
    public String productClass;
    public String serialNumber;

    public void generateDeviceId() {

        // Create device id from oui, productClass, serialNumber.
        this.deviceId = oui + "-" + productClass + "-" + serialNumber;
//        this.deviceId = oui + productClass + "-" + serialNumber;
    }

    public static SCSubscriberDevice convertToScSubcriberDevice(SubscriberDevice device) {
        SCSubscriberDevice scSubscriberDevice = new SCSubscriberDevice();
        scSubscriberDevice.deviceId = device.deviceId;
        scSubscriberDevice.id = device.id;
        scSubscriberDevice.manufacturer = device.manufacturer;
        scSubscriberDevice.oui = device.oui;
        scSubscriberDevice.productClass = device.productClass;
        scSubscriberDevice.serialNumber = device.serialNumber;
        scSubscriberDevice.subscriberId = device.subscriberId;
        return scSubscriberDevice;
    }

}
