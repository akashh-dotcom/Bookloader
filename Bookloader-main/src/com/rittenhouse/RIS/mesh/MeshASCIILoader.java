package com.rittenhouse.RIS.mesh;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.rittenhouse.RIS.Main;

public class MeshASCIILoader {

	private static DataSource dataSource = null;
	
	public static void main(String[] args) {
		
		Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;

        try {
            Class.forName(Main.getConfigProperty("RISDB.JDBCDriver"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        dataSource = setupDataSource(Main.getConfigProperty("RISDB.URL"),Main.getConfigProperty("RISDB.UserID"),Main.getConfigProperty("RISDB.Password"));

        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            
            String encoding = System.getProperty("file.encoding");
            String fileName = System.getProperty("user.dir") + Main.FILE_SEPARATOR + "..\\meta-data\\d2005.bin";
    	    StringBuffer sb = new StringBuffer();
    	    BufferedReader reader = null;

    	    try {
    	    	reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), encoding));
    	    	String currentLine= null;
    	    	int count = 0;
    	    	boolean foundNew = false;
    	    	String diseaseName = null;
    	    	int index = -1;
    	    	int syncount = 0;
    	    	int mncount = 0;
    	    	String diseaseDesc = null;
    	    	ArrayList synonyms = new ArrayList();
    	    	ArrayList MNEntries = new ArrayList();
    	    	String synName = null;
    	    	while((currentLine = reader.readLine())!=null) {
    	    		
    	    		if (currentLine.equals("*NEWRECORD")) {
    	    			foundNew = true;
    	    			count++;
    	    		} else {
    	    			foundNew = false;
    	    		}
    	    		
    	    		if (foundNew && count>1) {
    	    			    if (mncount>0) {
		    	                Iterator mnIt = MNEntries.iterator();
		    	                while(mnIt.hasNext()) {
		    	                    String relation = (String)mnIt.next();
		    	                    String parentRelName = relation;
		    	                    int parentId = -1;
		    						if (relation.lastIndexOf(".")>0)
		    							parentRelName = relation.substring(0, relation.lastIndexOf("."));
		    						parentId = getTermId(parentRelName);
		    	                    
		    						if (relation.startsWith("C")) {
		    						    
		    						    int diseaseId = getDiseaseIdByName(diseaseName);
		    						    
		    						    
				    	                    String insertSQL = "INSERT INTO tDiseaseName(vchDiseaseName, vchRelationName, vchDiseaseDesc, iParentDiseaseNameId, vchCreatorId, dtCreationDate, tiRecordStatus) VALUES('" + replace(diseaseName, "'", "''") + "','" + replace(relation, "'", "''") + "','" + replace(diseaseDesc, "'", "''") + "',";
					    	    			if (parentId == 0) {
					    	                	insertSQL = insertSQL + " NULL "; 
					    	                } else {
					    	                	insertSQL = insertSQL + parentId;
					    	                }
					    	                insertSQL = insertSQL + ", 'vbhatia',  getDate(), 1);";
				    	                
					    	                System.out.println(insertSQL);
				    	                
					    	                int termId = insertRow(insertSQL, "iDiseaseNameId");
		    						    
				    	                
					    	                if (syncount>0) {
					    	                	Iterator synIt = synonyms.iterator();
					    	                	while(synIt.hasNext()) {
						    	                	String syn = (String)synIt.next();
						    	                	insertSQL = "INSERT INTO tDiseaseSynonym(vchDiseaseSynonym, iDiseaseNameId, vchCreatorId, dtCreationDate, tiRecordStatus) VALUES('" + replace(syn, "'", "''") + "'," + termId;
						    	                	insertSQL = insertSQL + ", 'vbhatia',  getDate(), 1);";
						    	                	System.out.println(insertSQL);
						    	                	insertRow(insertSQL, null);
					    	                	}
					    	                }
		    						}
		    	                }
		    	                
		    	                synonyms = new ArrayList();
		    	                MNEntries = new ArrayList();
		    	    			syncount = 0;
		    	    			mncount = 0;
		    	                
	    	                }
    	    		}
    	    		
    	    		index = currentLine.lastIndexOf("=");
    	    		
    	    		//parse each line
    	    		if (currentLine.startsWith("MH = ")) {
    	    			diseaseName = currentLine.substring(index+1).trim();
    	    		} else if (currentLine.startsWith("MS =")) {
    	    			diseaseDesc = currentLine.substring(index+1).trim();
    	    		} else if (currentLine.startsWith("ENTRY = ") || currentLine.startsWith("PRINT ENTRY = ")) {
    	    			
    	    			if (currentLine.indexOf("|")>0) {
    	    				synName = currentLine.substring(index+1,currentLine.indexOf("|")).trim();
    	    			} else {
    	    				synName = currentLine.substring(index+1).trim();
    	    			}
    	    			
    	    			synonyms.add(syncount, synName);
    	    			syncount++;
    	    		} else if (currentLine.startsWith("MN = ")) {
    	    			String relation = currentLine.substring(index+1).trim();
    	    			MNEntries.add(mncount, relation);
    	    			mncount++;
    	    		} else {
    	    			continue;
    	    		}
    	    	}
    	    }catch (Exception ioe) {
    	      ioe.printStackTrace();
    	    }finally {
    	      if (reader != null)
				try {
					reader.close();
				} catch (IOException e1) {}
    	    }
        }catch(Exception e) {
            e.printStackTrace();
        } finally {
            try { rset.close(); } catch(Exception e) { }
            try { stmt.close(); } catch(Exception e) { }
            try { conn.close(); } catch(Exception e) { }
        }
	}
	
	private static int insertRow(String insertSQL, String idColName) {
		Connection conn = null;
        Statement stmt = null;
        int rowID = 0;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate(insertSQL);
            if (idColName != null) {
            	String selSQL = "SELECT MAX(" + idColName + ") currID FROM tDiseaseName";
            	stmt = conn.createStatement();
            	ResultSet rs = stmt.executeQuery(selSQL);
            	if (rs.next())
            		rowID = rs.getInt("currID");
            	rs.close();
            }
        }catch(SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            try { stmt.close(); } catch(Exception e) { e.printStackTrace(); }
            try { conn.close(); } catch(Exception e) { e.printStackTrace(); }
        }
        return rowID;
	}
	
	static int getDiseaseIdByName(String diseaseName) {
	    Connection conn = null;
        Statement stmt = null;
        int rowId = -1;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
        	String selSQL = "SELECT iDiseaseNameId FROM tDiseaseName WHERE vchDiseaseName = '" + replace(diseaseName, "'", "''") + "'";
        	stmt = conn.createStatement();
        	System.out.println(selSQL);
        	ResultSet rs = stmt.executeQuery(selSQL);
        	if (rs.next())
        		rowId = rs.getInt("iDiseaseNameId");
        	rs.close();
        }catch(SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            try { stmt.close(); } catch(Exception e) { e.printStackTrace(); }
            try { conn.close(); } catch(Exception e) { e.printStackTrace(); }
        }
        return rowId;
	}
	
	public static DataSource setupDataSource(String connectURI, String uname, String passwd) {
        ObjectPool connectionPool = new GenericObjectPool(null);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI,uname,passwd);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
        return dataSource;
    }
	
	private static int getTermId(String relation) {
		Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        int termId = 0;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            String selectSQL = "SELECT iDiseaseNameId FROM tDiseaseName WHERE vchRelationName='" + relation + "'";
            System.out.println(selectSQL);
            rset = stmt.executeQuery(selectSQL);
            if (rset.next())
            	termId = rset.getInt(1);
            
        }catch(SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            try { rset.close(); } catch(Exception e) { e.printStackTrace(); }
            try { stmt.close(); } catch(Exception e) { e.printStackTrace(); }
            try { conn.close(); } catch(Exception e) { e.printStackTrace(); }
        }
        return termId;
	}
	
	/**
     * Replaces all occurences of find String
     * with replacement String in source String
     */
    private static String replace(String source, String find, String replacement) {
        int sourceLength, findLength, currentPosition, lastPosition;
        String result = new String();

        //Retrieve string lengths
        sourceLength = source.length();
        findLength = find.length();

        if ((findLength == 0) | (sourceLength == 0)) {
            //nothing to find, nothing to replace, or source string empty
            return result;
        }

        //build result String by reading through source string for
        //occurrences of find string and replacing them along the way
        lastPosition = 0;
        while(lastPosition < sourceLength) {
            //locate find string in source string starting at last position
            currentPosition = source.indexOf(find, lastPosition);
            if (currentPosition == -1) {
                //find string not found, add the remainder of string to result string
                result = result + source.substring(lastPosition, sourceLength);
                break;
            } else {
                //find string found, replace it with replace string and add to result
                result = result
                         + source.substring(lastPosition, currentPosition)
                         + replacement;

                //skip past the occurence of find string
                lastPosition = currentPosition + findLength;
            }
        }

        return result;
    }
}
