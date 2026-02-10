package com.rittenhouse.RIS.mesh;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.rittenhouse.RIS.Main;

public class MeshTreeLoader {
	
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
            
            String records = readFile(System.getProperty("user.dir") + Main.FILE_SEPARATOR + "\\meta-data\\disease.txt");
            String[] fileArr = records.split("\r\n");

			//loop over file
            for(int k=0;k<fileArr.length;k++) {
				String record = fileArr[k];
				String[] recArr = record.split(";");
				
				//loop over line
				for(int j=0;j<recArr.length;j=j+2) {
					String diseaseName = recArr[j];
					String relation = recArr[j+1];
					
					String[] relArr = relation.split("\\.");
					
					if (relArr.length==1) {
						insertDisease(diseaseName, relation, null);
					} else {
						//loop over relation
						int parentId = 0;
						String parentRelName = relation;
						if (relation.lastIndexOf(".")>0)
							parentRelName = relation.substring(0, relation.lastIndexOf("."));
						parentId = getTermId(parentRelName);
						insertDisease(diseaseName, relation, parentId);
					}
				}
			}
            
            rset = stmt.executeQuery("SELECT * FROM tTerms");
            int numcols = rset.getMetaData().getColumnCount();
            while(rset.next()) {
                for(int i=1;i<=numcols;i++) {
                    System.out.print("\t" + rset.getString(i));
                }
                System.out.println("");
            }
        }catch(IOException ioe) {
        	ioe.printStackTrace();
		}catch(SQLException e) {
            e.printStackTrace();
        } finally {
            try { rset.close(); } catch(Exception e) { }
            try { stmt.close(); } catch(Exception e) { }
            try { conn.close(); } catch(Exception e) { }
        }
	}
	
	public static DataSource setupDataSource(String connectURI, String uname, String passwd) {
        ObjectPool connectionPool = new GenericObjectPool(null);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI,uname,passwd);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
        return dataSource;
    }
	
	private static String readFile(String fileName) throws IOException {

	    String encoding = System.getProperty("file.encoding");

	    StringBuffer sb = new StringBuffer();
	    Reader reader = null;

	    try {
	      if (encoding == null)
	        reader = new FileReader(fileName);
	      else
	        reader = new InputStreamReader(new FileInputStream(fileName), encoding);

	      int c;
	      while ((c=reader.read()) != -1)
	        sb.append((char)c);
	    }
	    catch (IOException ioe) {
	      throw ioe;
	    }
	    finally {
	      if (reader != null)
	        reader.close();
	    }

	    return sb.toString().trim();
	  }
	
	private static void insertDisease(String diseaseName, String relation, Integer parentId) {
		Connection conn = null;
        Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            String insertSQL = "INSERT INTO tTerms(vchTermName, vchRelationName, iParentTermId, vchCreatorId, dtCreationDate, tiRecordStatus) VALUES('" + diseaseName + "','" + relation + "',";
            if (parentId!=null && parentId.intValue() == 0) {
            	insertSQL = insertSQL + " NULL "; 
            } else {
            	insertSQL = insertSQL + parentId;
            }
            insertSQL = insertSQL + ", 'vbhatia',  getDate(), 1)";
            System.out.println(insertSQL);
            stmt.executeUpdate(insertSQL);
        }catch(SQLException e) {
            e.printStackTrace();
        } finally {
            try { stmt.close(); } catch(Exception e) { e.printStackTrace(); }
            try { conn.close(); } catch(Exception e) { e.printStackTrace(); }
        }
	}

	private static int getTermId(String relation) {
		Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        int termId = 0;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            String selectSQL = "SELECT iTermsId FROM tTerms WHERE vchRelationName='" + relation + "'";
            System.out.println(selectSQL);
            rset = stmt.executeQuery(selectSQL);
            if (rset.next())
            	termId = rset.getInt(1);
            
        }catch(SQLException e) {
            e.printStackTrace();
        } finally {
            try { rset.close(); } catch(Exception e) { e.printStackTrace(); }
            try { stmt.close(); } catch(Exception e) { e.printStackTrace(); }
            try { conn.close(); } catch(Exception e) { e.printStackTrace(); }
        }
        return termId;
	}
	
}