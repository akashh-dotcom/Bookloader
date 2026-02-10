package com.rittenhouse.RIS.util;

import java.sql.*;

/**
 * Utility for cleaning up failed RIS Backend operations
 * This tool removes incomplete or failed resource entries from the database
 */
public class FailedResourceCleanup {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java FailedResourceCleanup <ISBN> [dryRun]");
            System.out.println("  ISBN: The ISBN of the resource to clean up (with or without dashes)");
            System.out.println("  dryRun: Optional - if 'true', only shows what would be deleted");
            System.exit(1);
        }
        
        String isbn = args[0].replaceAll("-", ""); // Remove any dashes
        boolean dryRun = args.length > 1 && "true".equalsIgnoreCase(args[1]);
        
        try {
            cleanupFailedResource(isbn, dryRun);
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Clean up a failed resource by ISBN
     */
    public static void cleanupFailedResource(String isbn, boolean dryRun) throws SQLException, ClassNotFoundException {
        System.out.println("=== RIS Backend Resource Cleanup ===");
        System.out.println("ISBN: " + isbn);
        System.out.println("Mode: " + (dryRun ? "DRY RUN (no changes)" : "EXECUTE CLEANUP"));
        System.out.println();
        
        Connection conn = null;
        try {
            // Get database connection using same config as ResourceDB
            String driver = com.rittenhouse.RIS.Main.getConfigProperty("RISDB.JDBCDriver");
            String url = com.rittenhouse.RIS.Main.getConfigProperty("RISDB.URL");
            String username = com.rittenhouse.RIS.Main.getConfigProperty("RISDB.UserID");
            String password = com.rittenhouse.RIS.Main.getConfigProperty("RISDB.Password");
            
            Class.forName(driver);
            conn = java.sql.DriverManager.getConnection(url, username, password);
            conn.setAutoCommit(false); // Start transaction
            
            // Step 1: Find the resource ID
            int resourceId = findResourceId(conn, isbn);
            if (resourceId == -1) {
                System.out.println("No resource found with ISBN: " + isbn);
                return;
            }
            
            System.out.println("Found Resource ID: " + resourceId);
            
            // Step 2: Show what will be deleted
            showRelatedRecords(conn, resourceId);
            
            if (!dryRun) {
                // Step 3: Perform cleanup
                performCleanup(conn, isbn, resourceId);
                
                // Step 4: Verify cleanup
                verifyCleanup(conn, resourceId);
                
                // Step 5: Commit transaction
                conn.commit();
                System.out.println("\n✅ Cleanup completed successfully!");
            } else {
                System.out.println("\n🔍 DRY RUN - No changes made. Use without 'dryRun' to execute cleanup.");
                conn.rollback();
            }
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("\n❌ Error occurred - transaction rolled back");
                } catch (SQLException rollbackEx) {
                    System.err.println("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
    
    private static int findResourceId(Connection conn, String isbn) throws SQLException {
        String sql = "SELECT iResourceId, vchTitle FROM tResource WHERE REPLACE(vchResourceISBN,'-','') = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, isbn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int resourceId = rs.getInt("iResourceId");
                    String title = rs.getString("vchTitle");
                    System.out.println("Resource Title: " + title);
                    return resourceId;
                }
            }
        }
        return -1;
    }
    
    private static void showRelatedRecords(Connection conn, int resourceId) throws SQLException {
        System.out.println("\nRelated records to be deleted:");
        
        String[] tables = {
            "tNewResourceQue",
            "tResourceDiscipline", 
            "tKeywordResource",
            "tChapterResource"
        };
        
        for (String table : tables) {
            String sql = "SELECT COUNT(*) as cnt FROM " + table + " WHERE iResourceId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, resourceId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt("cnt");
                        System.out.println("  " + table + ": " + count + " records");
                    }
                }
            }
        }
    }
    
    private static void performCleanup(Connection conn, String isbn, int resourceId) throws SQLException {
        System.out.println("\nPerforming cleanup...");
        
        // Remove keywords using stored procedure
        try {
            String sql = "EXEC sp_removeKeywordResources ?, 'risbackend'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, isbn);
                stmt.execute();
                System.out.println("✅ Removed keyword associations");
            }
        } catch (SQLException e) {
            System.out.println("⚠️  Keyword removal failed (stored procedure may not exist): " + e.getMessage());
            // Continue with manual cleanup
            deleteFromTable(conn, "tKeywordResource", resourceId);
        }
        
        // Remove from other tables
        deleteFromTable(conn, "tNewResourceQue", resourceId);
        deleteFromTable(conn, "tResourceDiscipline", resourceId);
        deleteFromTable(conn, "tChapterResource", resourceId);
        
        // Finally remove main resource record
        deleteFromTable(conn, "tResource", resourceId);
    }
    
    private static void deleteFromTable(Connection conn, String tableName, int resourceId) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE iResourceId = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, resourceId);
            int deleted = stmt.executeUpdate();
            System.out.println("✅ Deleted " + deleted + " records from " + tableName);
        }
    }
    
    private static void verifyCleanup(Connection conn, int resourceId) throws SQLException {
        System.out.println("\nVerifying cleanup:");
        
        String[] tables = {
            "tNewResourceQue",
            "tResourceDiscipline", 
            "tKeywordResource",
            "tChapterResource",
            "tResource"
        };
        
        boolean allClean = true;
        for (String table : tables) {
            String sql = "SELECT COUNT(*) as cnt FROM " + table + " WHERE iResourceId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, resourceId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt("cnt");
                        if (count > 0) {
                            System.out.println("❌ " + table + ": " + count + " records remain");
                            allClean = false;
                        } else {
                            System.out.println("✅ " + table + ": cleaned");
                        }
                    }
                }
            }
        }
        
        if (!allClean) {
            throw new SQLException("Cleanup verification failed - some records remain");
        }
    }
}