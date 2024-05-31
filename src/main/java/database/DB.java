package database;

import firebase.Firebase;
import info.DeviceInfo;
import info.HomeInfo;
import info.Info;
import info.RoomInfo;
import mqtt.MQTT;
import org.eclipse.paho.client.mqttv3.MqttException;
import schedule.ScheduledDevice;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DB {
    static final String ADDRESS = "example.com";
    static final int MYSQL_PORT = 3306;
    static final String NAME = "example-db";
    static final String USER = "example-password";
    static final String PASSWORD = "example-password";

    public static Connection startConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return DriverManager.getConnection("jdbc:mysql://" + ADDRESS + ":" + MYSQL_PORT + "/" + NAME, USER, PASSWORD);
    }
    private static void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }
    public static void addUser(User newUser) throws SQLException {
        final String query = "INSERT INTO users (firstName, lastName, mail) VALUES (?, ?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, newUser.getFirstName());
        statement.setString(2, newUser.getLastName());
        statement.setString(3, newUser.getMail());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static User getUserByMail(String mail) throws SQLException {
        final String query = "SELECT * FROM users WHERE mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);

        ResultSet resultSet = statement.executeQuery();


        resultSet.next();

        User user = new User();
        user.setMail(resultSet.getString("mail"));
        user.setFirstName(resultSet.getString("firstName"));
        user.setLastName(resultSet.getString("lastName"));
        user.setProfilePicture(Firebase.getProfilePicture(mail));

        closeConnection(connection);

        return user;
    }
    public static void setUser(User user) throws SQLException {
        final String query =    "UPDATE users " +
                                "SET firstName = ?, lastName = ? " +
                                "WHERE mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, user.getFirstName());
        statement.setString(2, user.getLastName());
        statement.setString(3, user.getMail());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void delUser(String mail) throws SQLException, MqttException {
        final String delUser = "DELETE FROM users WHERE mail = ?";
        final String delHomesByMail =   "DELETE homes " +
                                        "FROM homes " +
                                        "JOIN user_home USING(homeID) " +
                                        "WHERE user_home.mail = ? AND user_home.owner = true";
        final String selectQuery =  "SELECT hubID, ieeeAddress " +
                                    "FROM devices " +
                                    "JOIN rooms USING(roomID) " +
                                    "JOIN homes USING(homeID) " +
                                    "JOIN user_home USING(homeID) " +
                                    "WHERE mail=? AND owner=true";

        Connection connection = startConnection();

        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setString(1, mail);
        ResultSet aboutDevice = selectStatement.executeQuery();

        while(aboutDevice.next()){
            MQTT.leaveDevice(aboutDevice.getInt("hubID"), aboutDevice.getString("ieeeAddress"));
        }

        PreparedStatement homesStatement = connection.prepareStatement(delHomesByMail);
        homesStatement.setString(1, mail);
        homesStatement.executeUpdate();

        PreparedStatement userStatement = connection.prepareStatement(delUser);
        userStatement.setString(1, mail);
        userStatement.executeUpdate();

        closeConnection(connection);
    }
    public static void addHome(Home newHome, String mail) throws SQLException {
        final String homeInsertQuery = "INSERT INTO homes (name, address) VALUES (?, ?)";
        final String userHomeInsertQuery = "INSERT INTO user_home (mail, homeID, owner) VALUES (?, ?, true)";

        Connection connection = startConnection();

        //aggiunge record alla tabella home
        PreparedStatement homeStatement = connection.prepareStatement(homeInsertQuery, Statement.RETURN_GENERATED_KEYS);
        homeStatement.setString(1, newHome.getName());
        homeStatement.setString(2, newHome.getAddress());
        homeStatement.executeUpdate();

        //aggiunge record alla tabella utenti_home
        ResultSet generatedKeys = homeStatement.getGeneratedKeys();
        if (generatedKeys.next()) {
            int homeId = generatedKeys.getInt(1);
            PreparedStatement userHomeStatement = connection.prepareStatement(userHomeInsertQuery);
            userHomeStatement.setString(1, mail);
            userHomeStatement.setInt(2, homeId);
            userHomeStatement.executeUpdate();
        } else {
            throw new SQLException("Failed to insert home, no generated key obtained.");
        }

        closeConnection(connection);
    }
    public static ArrayList<HomeOwner> getHomes(String mail) throws SQLException {
        final String query =    "SELECT homes.homeID, homes.hubID, homes.name, homes.address, user_home.owner " +
                                "FROM homes " +
                                "INNER JOIN user_home USING(homeID) " +
                                "INNER JOIN users USING(mail) " +
                                "WHERE users.mail = ?";
        ArrayList<HomeOwner> homeList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            HomeOwner home = new HomeOwner();
            home.setHomeID(resultSet.getInt("homeID"));
            home.setHubID(resultSet.getInt("hubID"));
            home.setName(resultSet.getString("name"));
            home.setAddress(resultSet.getString("address"));
            home.setOwner(resultSet.getBoolean("owner"));

            homeList.add(home);
        }

        closeConnection(connection);

        return homeList;
    }
    public static ArrayList<HomeInfo> getHomesInfo(String mail) throws SQLException {
        final String query =    "SELECT homes.homeID, homes.hubID, homes.name, homes.address, user_home.owner " +
                                "FROM homes " +
                                "INNER JOIN user_home USING(homeID) " +
                                "INNER JOIN users USING(mail) " +
                                "WHERE users.mail = ?";
        ArrayList<HomeInfo> homeList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            HomeInfo home = new HomeInfo();
            home.setHomeID(resultSet.getInt("homeID"));
            home.setHubID(resultSet.getInt("hubID"));
            home.setName(resultSet.getString("name"));
            home.setAddress(resultSet.getString("address"));
            home.setOwner(resultSet.getBoolean("owner"));

            homeList.add(home);
        }

        closeConnection(connection);

        return homeList;
    }
    public static void setHome(Home home) throws SQLException {
        final String query =    "UPDATE homes " +
                                "SET name = ?, address = ? " +
                                "WHERE homeID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, home.getName());
        statement.setString(2, home.getAddress());
        statement.setInt(3, home.getHomeID());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void delHome(int homeId) throws SQLException, MqttException {
        final String query = "DELETE FROM homes WHERE homeID = ?";
        final String selectQuery =  "SELECT hubID, ieeeAddress " +
                                    "FROM devices " +
                                    "JOIN rooms USING(roomID) " +
                                    "JOIN homes USING(homeID) " +
                                    "WHERE homes.homeID = ?";

        Connection connection = startConnection();

        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setInt(1, homeId);
        ResultSet aboutDevice = selectStatement.executeQuery();

        while(aboutDevice.next()){
            MQTT.leaveDevice(aboutDevice.getInt("hubID"), aboutDevice.getString("ieeeAddress"));
        }

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void setHub(int homeId, int hubId) throws SQLException {
        final String query =    "UPDATE homes " +
                                "SET hubID = ? " +
                                "WHERE homeID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, hubId);
        statement.setInt(2, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void addFamilyMember(String mail, int homeId) throws SQLException {
        final String query = "INSERT INTO user_home(mail, homeID) VALUES (?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        statement.setInt(2, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static ArrayList<UserOwner> getFamilyMembers(int homeId) throws SQLException {
        final String query =    "SELECT users.mail, users.firstName, users.lastName, user_home.owner " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "WHERE homes.homeID = ?";
        ArrayList<UserOwner> userList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            UserOwner user = new UserOwner();
            user.setMail(resultSet.getString("mail"));
            user.setFirstName(resultSet.getString("firstName"));
            user.setLastName(resultSet.getString("lastName"));
            user.setOwner(resultSet.getBoolean("owner"));
            user.setProfilePicture(Firebase.getProfilePicture(user.getMail()));

            userList.add(user);
        }

        closeConnection(connection);

        return userList;
    }
    public static void delFamilyMember(String mail, int homeId) throws SQLException {
        final String query = "DELETE FROM user_home WHERE mail = ? AND homeID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        statement.setInt(2, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void addRoom(Room newRoom, int homeId) throws SQLException {
        final String query = "INSERT INTO rooms(name, homeID) VALUES (?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, newRoom.getName());
        statement.setInt(2, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static ArrayList<Room> getRooms(int homeId) throws SQLException {
        final String query =    "SELECT rooms.roomID, rooms.name " +
                                "FROM rooms " +
                                "WHERE rooms.homeID = ?";
        ArrayList<Room> roomList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            Room room = new Room();
            room.setRoomID(resultSet.getInt("roomID"));
            room.setName(resultSet.getString("name"));

            roomList.add(room);
        }

        closeConnection(connection);

        return roomList;
    }
    public static ArrayList<RoomInfo> getRoomsInfo(int homeId) throws SQLException {
        final String query =    "SELECT rooms.roomID, rooms.name " +
                                "FROM rooms " +
                                "WHERE rooms.homeID = ?";
        ArrayList<RoomInfo> roomList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            RoomInfo room = new RoomInfo();
            room.setRoomID(resultSet.getInt("roomID"));
            room.setName(resultSet.getString("name"));

            roomList.add(room);
        }

        closeConnection(connection);

        return roomList;
    }
    public static void setRoom(Room room) throws SQLException {
        final String query =    "UPDATE rooms " +
                                "SET name = ? " +
                                "WHERE roomID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, room.getName());
        statement.setInt(2, room.getRoomID());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void delRoom(int roomId) throws SQLException, MqttException {
        final String query = "DELETE FROM rooms WHERE roomID = ?";
        final String selectQuery =  "SELECT hubID, ieeeAddress " +
                                    "FROM devices " +
                                    "JOIN rooms USING(roomID) " +
                                    "JOIN homes USING(homeID) " +
                                    "WHERE rooms.roomID = ?";

        Connection connection = startConnection();

        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setInt(1, roomId);
        ResultSet aboutDevice = selectStatement.executeQuery();

        while(aboutDevice.next()){
            MQTT.leaveDevice(aboutDevice.getInt("hubID"), aboutDevice.getString("ieeeAddress"));
        }

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void addDevice(Device newDevice) throws SQLException {
        final String query =    "INSERT INTO devices(name, ieeeAddress, type, model, roomID, enabled, startTime, endTime, periodicity) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, newDevice.getName());
        statement.setString(2, newDevice.getIeeeAddress());
        statement.setString(3, newDevice.getType());
        statement.setString(4, newDevice.getModel());
        statement.setInt(5, newDevice.getRoomID());
        statement.setBoolean(6, newDevice.isEnabled());
        statement.setString(7, newDevice.getStartTime().toString());
        statement.setString(8, newDevice.getEndTime().toString());
        statement.setString(9, String.join(",", newDevice.getPeriodicity()));
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static ArrayList<Device> getDevices(int roomId) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM devices " +
                                "WHERE devices.roomID = ?";
        ArrayList<Device> deviceList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            Device device = new Device();

            device.setDeviceID(resultSet.getInt("deviceID"));
            device.setName(resultSet.getString("name"));
            device.setIeeeAddress(resultSet.getString("ieeeAddress"));
            device.setType(resultSet.getString("type"));
            device.setModel(resultSet.getString("model"));
            device.setRoomID(resultSet.getInt("roomID"));
            device.setEnabled(resultSet.getBoolean("enabled"));
            device.setStartTime(resultSet.getTime("startTime"));
            device.setEndTime(resultSet.getTime("endTime"));
            device.setPeriodicity(resultSet.getString("periodicity").split(","));

            deviceList.add(device);
        }

        closeConnection(connection);

        return deviceList;
    }
    public static ArrayList<DeviceInfo> getDevicesInfo(int roomId) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM devices " +
                                "WHERE devices.roomID = ?";
        ArrayList<DeviceInfo> deviceList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            DeviceInfo device = new DeviceInfo();

            device.setDeviceID(resultSet.getInt("deviceID"));
            device.setName(resultSet.getString("name"));
            device.setIeeeAddress(resultSet.getString("ieeeAddress"));
            device.setType(resultSet.getString("type"));
            device.setModel(resultSet.getString("model"));
            device.setRoomID(resultSet.getInt("roomID"));
            device.setEnabled(resultSet.getBoolean("enabled"));
            device.setStartTime(resultSet.getTime("startTime"));
            device.setEndTime(resultSet.getTime("endTime"));
            device.setPeriodicity(resultSet.getString("periodicity").split(","));

            deviceList.add(device);
        }

        closeConnection(connection);

        return deviceList;
    }
    public static void setDevice (Device device) throws SQLException {
        final String query =    "UPDATE devices " +
                                "SET name = ?, enabled = ?, startTime = ?, endTime = ?, periodicity = ? " +
                                "WHERE deviceID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, device.getName());
        statement.setBoolean(2, device.isEnabled());
        statement.setTime(3, device.getStartTime());
        statement.setTime(4, device.getEndTime());
        statement.setString(5, String.join(",", device.getPeriodicity()));
        statement.setInt(6, device.getDeviceID());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void delDevice(int deviceId) throws SQLException, MqttException {
        final String deleteQuery = "DELETE FROM devices WHERE deviceID = ?";
        final String selectQuery =  "SELECT hubID, ieeeAddress FROM devices " +
                                    "JOIN rooms USING(roomID) " +
                                    "JOIN homes USING(homeID) " +
                                    "WHERE deviceID = ?";

        Connection connection = startConnection();

        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setInt(1, deviceId);
        ResultSet aboutDevice = selectStatement.executeQuery();

        while(aboutDevice.next()){
            MQTT.leaveDevice(aboutDevice.getInt("hubID"), aboutDevice.getString("ieeeAddress"));
        }

        PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
        deleteStatement.setInt(1, deviceId);
        deleteStatement.executeUpdate();

        closeConnection(connection);
    }
    public static void addFavourite(String mail, int deviceId) throws SQLException {
        final String query = "INSERT INTO favourites(mail, deviceID) VALUES (?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        statement.setInt(2, deviceId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static ArrayList<Integer> getFavourites(String mail) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM devices " +
                                "INNER JOIN favourites USING(deviceID) " +
                                "WHERE mail = ?";
        ArrayList<Integer> deviceList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            deviceList.add(resultSet.getInt("deviceID"));
        }

        closeConnection(connection);

        return deviceList;
    }
    public static void delFavourite(String mail, int deviceId) throws SQLException {
        final String query = "DELETE FROM favourites WHERE mail = ? AND deviceID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        statement.setInt(2, deviceId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static boolean hasOwnerRightsOnHome(String clientMail, int homeId) throws SQLException {
        final String query =    "SELECT users.mail, user_home.owner " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "WHERE homes.homeID = ? AND users.mail = ? AND user_home.owner = TRUE";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        statement.setString(2, clientMail);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRights = resultSet.next();

        closeConnection(connection);
        return hasRights;
    }
    public static boolean hasMemberRightsOnHome(String clientMail, int homeId) throws SQLException {
        final String query =    "SELECT users.mail " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "WHERE homes.homeID = ? AND users.mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        statement.setString(2, clientMail);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRights = resultSet.next();

        closeConnection(connection);
        return hasRights;
    }
    public static boolean hasMemberRightsOnRoom(String clientMail, int roomId) throws SQLException {
        final String query =    "SELECT users.mail " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "INNER JOIN rooms USING(homeID) " +
                                "WHERE rooms.roomID = ? AND users.mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        statement.setString(2, clientMail);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRights = resultSet.next();

        closeConnection(connection);
        return hasRights;
    }
    public static boolean hasMemberRightsOnDevice(String clientMail, int deviceId) throws SQLException {
        final String query =    "SELECT users.mail " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "INNER JOIN rooms USING(homeID) " +
                                "INNER JOIN devices USING(roomID) " +
                                "WHERE devices.deviceID = ? AND users.mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, deviceId);
        statement.setString(2, clientMail);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRights = resultSet.next();

        closeConnection(connection);
        return hasRights;
    }
    public static int getHomeIDFromHubID(int hubID) throws SQLException {
        final String query =    "SELECT homes.homeID " +
                                "FROM homes " +
                                "WHERE homes.hubID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, hubID);
        ResultSet resultSet = statement.executeQuery();

        int homeID = 0;
        if(resultSet.next())
            homeID = resultSet.getInt("homeID");

        closeConnection(connection);
        return homeID;
    }
    public static boolean ieeeAddressAlreadyInUse(String ieeeAddress) throws SQLException {
        final String query =    "SELECT COUNT(*) " +
                                "FROM devices " +
                                "WHERE ieeeAddress = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, ieeeAddress);
        ResultSet resultSet = statement.executeQuery();

        resultSet.next();
        boolean alreadyInUse = resultSet.getInt(1) > 0;

        closeConnection(connection);
        return alreadyInUse;

    }
    public static Info getInfo(String mail) throws SQLException {
        Info info = new Info();


        info.setHomes(getHomesInfo(mail));
        info.setFavourites(getFavourites(mail));

        for(HomeInfo home : info.getHomes()) {
            try {
                home.setOnline(MQTT.isHubOnline(home.getHubID()));
            } catch (MqttException e) {
                //e.printStackTrace();
                home.setOnline(false);
            }

            home.setFamilyMembers(getFamilyMembers(home.getHomeID()));
            home.setRooms(getRoomsInfo(home.getHomeID()));

            for(RoomInfo room : home.getRooms()) {
                room.setDevices(getDevicesInfo(room.getRoomID()));

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (DeviceInfo device : room.getDevices()) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            device.setStatus(MQTT.getStatus(home.getHubID(), device.getIeeeAddress(), device.getType()));
                        } catch (MqttException | IllegalStateException e) {
                            //e.printStackTrace();
                            device.setStatus(null);
                        }
                    });
                    futures.add(future);
                }

                // Attendere il completamento di tutti i CompletableFuture
                CompletableFuture<Void> allOfFutures = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                );

                try {
                    allOfFutures.get(); // Attendere il completamento di tutti i CompletableFuture
                } catch (InterruptedException | ExecutionException e) {
                    // Gestire eventuali eccezioni
                    e.printStackTrace();
                }
            }

            }

        return info;
    }
    public static ArrayList<ScheduledDevice> getScheduledDevices() throws SQLException{
        final String query =    "SELECT * " +
                                "FROM devices " +
                                "JOIN rooms USING(roomID) " +
                                "JOIN homes USING(homeID) " +
                                "WHERE type='light' OR  type='switch'";
        ArrayList<ScheduledDevice> deviceList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            ScheduledDevice device = new ScheduledDevice();

            device.setDeviceID(resultSet.getInt("deviceID"));
            device.setHubID(resultSet.getInt("hubID"));
            device.setIeeeAddress(resultSet.getString("ieeeAddress"));
            device.setEnabled(resultSet.getBoolean("enabled"));
            device.setStartTime(resultSet.getTime("startTime"));
            device.setEndTime(resultSet.getTime("endTime"));
            if(resultSet.getString("periodicity").isEmpty())
                device.setPeriodicity(null);
            else
                device.setPeriodicity(resultSet.getString("periodicity").split(","));


            deviceList.add(device);
        }

        closeConnection(connection);

        return deviceList;
    }
    public static void disableSchedule(int deviceID) throws SQLException {
        final String query =    "UPDATE devices " +
                                "SET enabled = false " +
                                "WHERE deviceID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, deviceID);
        statement.executeUpdate();

        closeConnection(connection);
    }

}