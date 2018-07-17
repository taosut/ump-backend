/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.notification.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;
import java.io.Serializable;

/**
 *
 * @author kiendt
 */
public class NotificationAlarmElk implements Serializable {

    public Long userId;
    public String content;
    public Integer status;
    public String deviceId;
    public Integer type;

    @JestId
    public String _id;

//    @SerializedName("@timestamp")
    public String timestamp;

}
