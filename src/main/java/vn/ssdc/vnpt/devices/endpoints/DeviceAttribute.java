package vn.ssdc.vnpt.devices.endpoints;

/**
 * Created by Admin on 10/18/2017.
 */
public class DeviceAttribute {

    private String name;
    private boolean notifycationChange;
    private String notification;
    private boolean accessListChange;
    private String[] accessList;

    public static final String ACTIVE_NOTIFICATION = "2";
    public static final String PASSIVE_NOTIFICATION = "1";
    public static final String OFF_NOTIFICATION = "0";

    public DeviceAttribute(String name, boolean notifycationChange, String notification, boolean accessListChange) {
        this.name = name;
        this.notifycationChange = notifycationChange;
        this.notification = notification;
        this.accessListChange = accessListChange;
        accessList = new String[]{};
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isNotifycationChange() {
        return notifycationChange;
    }

    public void setNotifycationChange(boolean notifycationChange) {
        this.notifycationChange = notifycationChange;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public boolean isAccessListChange() {
        return accessListChange;
    }

    public void setAccessListChange(boolean accessListChange) {
        this.accessListChange = accessListChange;
    }

    public String[] getAccessList() {
        return accessList;
    }

    public void setAccessList(String[] accessList) {
        this.accessList = accessList;
    }

    @Override
    public String toString() {
        String accessListStr = "";
        if (accessList.length != 0) {
            accessListStr = "[";
            for (int i = 0; i < accessList.length; i++) {
                accessListStr += "\"" + accessList[i] + "\",";
            }
            accessListStr = accessListStr.substring(0, accessListStr.length() - 1);
            accessListStr += "]";
            return "[" + "\"" + name + "\",\"" + notifycationChange + "\",\"" + notification + "\",\"" + accessListChange + "\"," + accessListStr + "]";
        }
        return "[" + "\"" + name + "\",\"" + notifycationChange + "\",\"" + notification + "\",\"" + accessListChange + "\"" + "]";

    }
}
