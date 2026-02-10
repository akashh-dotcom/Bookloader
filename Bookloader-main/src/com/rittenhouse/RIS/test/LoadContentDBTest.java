package com.rittenhouse.RIS.test;

import java.io.File;

import junit.framework.TestCase;

import com.rittenhouse.RIS.Main;
import com.rittenhouse.RIS.db.ResourceDB;

/**
 * @author vbhatia
 */
public class LoadContentDBTest extends TestCase {
    
    private String bookIsbn = "5555555555";

    
    protected void setUp() throws Exception {
        super.setUp();
    }

    
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLoadContentToDB() {
        Main m = new Main();
		m.startLogger();
		
		String sourceDirName = Main.getConfigProperty("RIS.CONTENT_TEMP");
		File sourceDir = new File(sourceDirName);
		ResourceDB resDB = new ResourceDB();
		String collectionName = resDB.getBookCollectionName(bookIsbn);
		int tiGloballyAccessible = 0;
		
		if (collectionName == null) {
		    collectionName = Main.getConfigProperty("XMLDB.DEFAULT_COLLECTION_NAME");
		}
		
		if (collectionName.length()>0) {
		    tiGloballyAccessible = 1;
		}
		
		m.loadContentToDB(sourceDir, tiGloballyAccessible);
		
    }
}