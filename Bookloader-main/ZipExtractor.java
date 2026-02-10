package com.rittenhouse.RIS.util;

import java.io.*;
import java.util.zip.*;

/**
 * Minimal ZIP extraction utility for production use
 */
public class ZipExtractor {
    
    /**
     * Extract ZIP file to destination directory
     * @param zipFile Source ZIP file
     * @param destDir Destination directory
     * @throws IOException if extraction fails
     */
    public static void extractZip(File zipFile, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        // Create subdirectory based on ZIP filename (without extension)
        String zipName = zipFile.getName();
        if (zipName.toLowerCase().endsWith(".zip")) {
            zipName = zipName.substring(0, zipName.length() - 4);
        }
        File extractDir = new File(destDir, zipName);
        if (!extractDir.exists()) {
            extractDir.mkdirs();
        }
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    new File(extractDir, entry.getName()).mkdirs();
                } else {
                    File outputFile = new File(extractDir, entry.getName());
                    outputFile.getParentFile().mkdirs();
                    
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}