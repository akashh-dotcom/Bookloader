package com.rittenhouse.bookloader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBSmokeTest {
    public static void main(String[] args) throws Exception {
        // You can later read these from the same properties file BookLoader uses
        String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        String url = "jdbc:sqlserver://127.0.0.1:11433;databaseName=STG_RIT001";
        String user = "RittAdmin";      // or whatever you’re using
        String password = "49jR6xQybSCDeA5ObTp0";

        System.out.println("Loading driver: " + driver);
        Class.forName(driver);

        System.out.println("Connecting to: " + url);
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT TOP 1 iResourceId FROM dbo.tResource")) {

            System.out.println("Connected successfully.");
            if (rs.next()) {
                System.out.println("tResource first iResourceId = " + rs.getInt(1));
            } else {
                System.out.println("Query returned no rows.");
            }
        }
    }
}
