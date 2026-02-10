package com.rittenhouse.RIS.test;

import com.rittenhouse.RIS.Main;

public class TestLocalMode {
    public static void main(String[] args) {
        System.out.println("Testing local mode setup...");
        
        // Test if we can access configuration
        try {
            String testMode = Main.getConfigProperty("RIS.TEST_MODE");
            System.out.println("Test mode: " + testMode);
            
            String contentTemp = Main.getConfigProperty("RIS.CONTENT_TEMP");
            System.out.println("Content temp path: " + contentTemp);
            
            String dtdPath = Main.getConfigProperty("RIS.RITTENHOUSE_DTD_PATH");
            System.out.println("DTD path: " + dtdPath);
            
            System.out.println("Configuration test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error testing configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}