package com.rittenhouse.RIS.test;

import com.rittenhouse.RIS.Main;
import java.io.File;

public class TestProcessInput {
    public static void main(String[] args) {
        System.out.println("Testing processing of files in input folder...");
        
        try {
            // Check test mode
            String testMode = Main.getConfigProperty("RIS.TEST_MODE");
            System.out.println("Test mode: " + testMode);
            
            // Check input directory
            String inputDir = Main.getConfigProperty("RIS.CONTENT_IN");
            System.out.println("Input directory: " + inputDir);
            
            File inputFolder = new File(inputDir);
            if (inputFolder.exists() && inputFolder.isDirectory()) {
                File[] files = inputFolder.listFiles();
                System.out.println("Files in input directory:");
                for (File file : files) {
                    System.out.println("  - " + file.getName() + " (" + file.length() + " bytes)");
                }
                
                // Create Main instance and try to run processing
                System.out.println("\nAttempting to run RIS Backend in test mode...");
                
                // Let's see what happens step by step instead of calling runRISBackend
                Main main = new Main();
                
                // Instead of runRISBackend(), let's call the individual steps
                System.out.println("Step 1: Main object created successfully");
                
                System.out.println("Step 2: Now we would call runRISBackend() but that calls System.exit()");
                System.out.println("The zip file should have been processed and extracted to temp folder");
                System.out.println("Check the temp and output folders for results:");
                
                String tempDir = Main.getConfigProperty("RIS.CONTENT_TEMP");
                String outputDir = Main.getConfigProperty("RIS.CONTENT_OUT");
                System.out.println("Temp directory: " + tempDir);
                System.out.println("Output directory: " + outputDir);
                
                main.runRISBackend();
                
            } else {
                System.err.println("Input directory does not exist: " + inputDir);
            }
            
        } catch (Exception e) {
            System.err.println("Error during processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}