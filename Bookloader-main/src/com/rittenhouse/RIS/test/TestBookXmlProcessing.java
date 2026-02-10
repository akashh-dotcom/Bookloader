package com.rittenhouse.RIS.test;

import com.rittenhouse.RIS.Main;
import java.io.File;

public class TestBookXmlProcessing {
    public static void main(String[] args) {
        System.out.println("Testing book.xml processing...");
        
        try {
            // Check if book.xml exists
            String tempDir = Main.getConfigProperty("RIS.CONTENT_TEMP");
            System.out.println("Temp directory: " + tempDir);
            
            File bookXmlFile = new File(tempDir + "/9781635930733/book.xml");
            if (bookXmlFile.exists()) {
                System.out.println("Found book.xml: " + bookXmlFile.getAbsolutePath());
                System.out.println("File size: " + bookXmlFile.length() + " bytes");
                
                // Test if we can call prepareBookXml directly
                Main main = new Main();
                System.out.println("Calling prepareBookXml on: " + tempDir);
                
                // This will tell us if prepareBookXml succeeds or fails
                boolean result = main.prepareBookXml(tempDir);
                System.out.println("prepareBookXml result: " + result);
                
                if (!result) {
                    System.out.println("prepareBookXml returned false - this is why the system exits early");
                } else {
                    System.out.println("prepareBookXml succeeded!");
                }
                
            } else {
                System.out.println("book.xml not found at: " + bookXmlFile.getAbsolutePath());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}