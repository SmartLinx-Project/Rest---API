package schedule;

import java.sql.Time;

public class ScheduledDevice {

    private int deviceID;
    private int hubID;
    private String ieeeAddress;
    private boolean enabled;
    private Time startTime;
    private Time endTime;
    private String[] periodicity;

    public ScheduledDevice() {}

    public int getDeviceID() {
        return deviceID;
    }
    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    public int getHubID() {
        return hubID;
    }

    public void setHubID(int hubID) {
        this.hubID = hubID;
    }

    public String getIeeeAddress() {
        return ieeeAddress;
    }

    public void setIeeeAddress(String ieeeAddress) {
        this.ieeeAddress = ieeeAddress;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    public String[] getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(String[] periodicity) {
        this.periodicity = periodicity;
    }
}
