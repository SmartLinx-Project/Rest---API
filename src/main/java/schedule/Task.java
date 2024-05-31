package schedule;

import database.DB;
import mqtt.MQTT;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

public class Task extends Thread{

    public Task() {}

    @Override
    public void run() {

        try {
            ArrayList<ScheduledDevice> devices = DB.getScheduledDevices();

            LocalTime currentTime = LocalTime.now();
            for(ScheduledDevice device : devices) {
                //controlla se la schedulazione è disabilitata o se oggi è non un giorno periodico
                if(!device.isEnabled() || device.getPeriodicity() != null && !isPeriodicDay(device.getPeriodicity(), getCurrentDay()))
                    continue;

                LocalTime startTime = device.getStartTime().toLocalTime();
                LocalTime endTime = device.getEndTime().toLocalTime();

                if (currentTime.getHour() == startTime.getHour() && currentTime.getMinute() == startTime.getMinute()) {
                    MQTT.setStatus(device.getHubID(), device.getIeeeAddress(), "{\"state\": \"ON\"}");
                }

                if (currentTime.getHour() == endTime.getHour() && currentTime.getMinute() == endTime.getMinute()) {
                    MQTT.setStatus(device.getHubID(), device.getIeeeAddress(), "{\"state\": \"OFF\"}");

                    if(device.getPeriodicity() == null)
                        DB.disableSchedule(device.getDeviceID());
                }

            }
        } catch (SQLException | MqttException e) {
            e.printStackTrace();
        } catch (IllegalStateException ignored) {}


    }

    private String getCurrentDay() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        return dayOfWeek.name().toLowerCase();
    }

    private boolean isPeriodicDay(String[] periocity, String currentDay) {
        for (String day : periocity)
            if (day.equalsIgnoreCase(currentDay))
                return true;
        return false;
    }
}
