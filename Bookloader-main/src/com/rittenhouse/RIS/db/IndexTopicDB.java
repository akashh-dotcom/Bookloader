package com.rittenhouse.RIS.db;

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
import org.apache.log4j.Category;

import com.rittenhouse.RIS.Main;

/**
 * Adds index topic entries to database
 * @author vbhatia
 */
public class IndexTopicDB {

	private DataSource dataSource = null;
	
	//logger
	protected static Category log = Category.getInstance(IndexTopicDB.class.getName());
	
	/**
	 * default constructor
	 */
	public IndexTopicDB() {
		try {
			Class.forName(Main.getConfigProperty("RISDB.JDBCDriver"));
		} catch (ClassNotFoundException e) {
			log.error(e.toString());
		}
		setDataSource(Main.getConfigProperty("RISDB.URL"), Main.getConfigProperty("RISDB.UserID"), Main.getConfigProperty("RISDB.Password"));
	}
	
	/**
	 * Setup a new Datasource
	 * 
	 * @param connectURI
	 * @param uname
	 * @param passwd
	 * @return DataSource
	 */
	public void setDataSource(String connectURI, String uname, String passwd) {
		ObjectPool connectionPool = new GenericObjectPool(null);
		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI, uname, passwd);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
		PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
		this.dataSource = dataSource;
	}
	
	/**
	 * @param bookIsbn
	 * @param diseaseId
	 * @param sectionId
	 * @param chapterNumber
	 */
	public void insertDiseaseResource(String bookIsbn, Integer diseaseId, String sectionId, String chapterNumber) {
		Connection conn = null;
		Statement stmt = null;
		String insertStr = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			
			String selStr = "SELECT iDiseaseResourceId FROM tDiseaseResource WHERE iDiseaseNameId = " + diseaseId + " AND vchResourceISBN = '" + bookIsbn + "' AND vchChapterId='" + chapterNumber + "' AND vchSectionId='" + sectionId + "'";
			stmt = conn.createStatement();
        	ResultSet rs = stmt.executeQuery(selStr);
        	int diseaseResId = -1;
        	if (rs.next())
        		diseaseResId = rs.getInt("iDiseaseResourceId");
        	rs.close();
        	
        	if (diseaseResId == -1) {
        		insertStr = "INSERT INTO tDiseaseResource(iDiseaseNameId,vchResourceISBN,vchChapterId,vchSectionId,vchCreatorId,dtCreationDate,tiRecordStatus) VALUES(" + diseaseId + ",'" + bookIsbn + "','" + chapterNumber + "','" + sectionId + "','risbackend',  getDate(), 1);";
        		int count = stmt.executeUpdate(insertStr);
        		conn.commit();
        	} else {
        	    String updateSql = "UPDATE tDiseaseResource set vchUpdaterId = 'risbackend', dtLastUpdate = getdate(), tiRecordStatus = 1 where iDiseaseResourceId=" + diseaseResId;
        	    int count = stmt.executeUpdate(updateSql);
        		conn.commit();
        	}
		} catch (SQLException e) {
			log.error("Failed to update disease topic index entry.");
			log.error(insertStr);
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
				conn.close();
			} catch (SQLException e) {}
		}
	}
	
	
	/**
	 * @param bookIsbn
	 * @param drugId
	 * @param sectionId
	 * @param chapterNumber
	 */
	public int insertDrugResource(String bookIsbn, Integer drugId, String sectionId, String chapterNumber) {
		Connection conn = null;
		Statement stmt = null;
		String insertStr = null;
		int count = 0;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			
			String selStr = "SELECT iDrugResourceId FROM tDrugResource WHERE iDrugListId = " + drugId + " AND vchResourceISBN = '" + bookIsbn + "' AND vchChapterId='" + chapterNumber + "' AND vchSectionId='" + sectionId + "'";
			stmt = conn.createStatement();
        	ResultSet rs = stmt.executeQuery(selStr);
        	int drugResId = -1;
        	if (rs.next())
        		drugResId = rs.getInt("iDrugResourceId");
        	rs.close();
        	
        	if (drugResId == -1) {
        		insertStr = "INSERT INTO tDrugResource(iDrugListId,vchResourceISBN,vchChapterId,vchSectionId,vchCreatorId,dtCreationDate,tiRecordStatus) VALUES(" + drugId + ",'" + bookIsbn + "','" + chapterNumber + "','" + sectionId + "','risbackend',  getDate(), 1);";
        		count = stmt.executeUpdate(insertStr);
        		conn.commit();
        	} else {
        	    String updateSql = "UPDATE tDrugResource set vchUpdaterId = 'risbackend', dtLastUpdate = getdate(), tiRecordStatus = 1 where iDrugResourceId=" + drugResId;
        	    count = stmt.executeUpdate(updateSql);
        	    conn.commit();
        	}
		} catch (SQLException e) {
			log.error("Failed to update drug topic index flag");
			log.error(insertStr);
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
				conn.close();
			} catch (SQLException e) {}
		}
		return count;
	}
	
	/** This version block names that are a subset of previously added names
	 * @param bookIsbn
	 * @param vchDrugName
	 * @param drugId
	 * @param sectionId
	 * @param chapterNumber
	 * @param displayTitle
	 */
	public int insertDrugResource(String bookIsbn , String vchDrugName , Integer drugId, String sectionId, String chapterNumber, String displayTitle) {
		Connection conn = null;
		Statement stmt = null;
		String insertStr = null;
		int count = -1;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
		// Check for matches of the drug or an ancestor	
			String selStr = "SELECT iDrugResourceId , vchDrugName FROM tDrugResource dr inner join tdrugslist dl on dr.idruglistid = dl.idruglistid and dl.tirecordstatus = 1 and vchDrugName like '%" + vchDrugName.replaceAll("'","''") + "%' WHERE vchResourceISBN = '" + bookIsbn + "' AND vchChapterId='" + chapterNumber + "' AND vchSectionId='" + sectionId + "'";
			stmt = conn.createStatement();
        	ResultSet rs = stmt.executeQuery(selStr);
        	int drugResId = -1;
        	
        	while(rs.next()) {
        		drugResId = rs.getInt("iDrugResourceId");
        		if (rs.getString("vchDrugName").contains(vchDrugName) &&  !(rs.getString("vchDrugName").equals(vchDrugName)))
        			count = 0;
        	}
        	rs.close();

    		// Check for matches of the drug or an ancestor
        	if (count == -1) {
    	       	if (drugResId == -1) {
    	       		insertStr = "INSERT INTO tDrugResource(iDrugListId,vchResourceISBN,vchChapterId,vchSectionId,vchTitle, vchCreatorId,dtCreationDate,tiRecordStatus ) VALUES(" + drugId + ",'" + bookIsbn + "','" + chapterNumber + "','" + sectionId + "','" + displayTitle.replaceAll("'","''") +"' ,'risbackend',  getDate(), 1);";
    	       		count = stmt.executeUpdate(insertStr);
    	       		conn.commit();
    	       	} else {
    	       		String updateSql = "UPDATE tDrugResource set vchUpdaterId = 'risbackend', dtLastUpdate = getdate(), tiRecordStatus = 1 where iDrugResourceId=" + drugResId;
    	       		count = stmt.executeUpdate(updateSql);
    	       		conn.commit();
    	       	}
        	}      	        	
        	
		} catch (SQLException e) {
			log.error("Failed to update drug topic index flag");
			log.error(insertStr);
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
				conn.close();
			} catch (SQLException e) {}
		}
		return count;
	}
	
	

	/**
	 * @param bookIsbn
	 * @param drugSynonymName
	 * @param drugSynonymId
	 * @param sectionId
	 * @param chapterNumber
	 * @param displayTitle
	 */
	public int insertDrugSynonymResource(String bookIsbn, String drugSynonymName, Integer drugSynonymId, String sectionId, String chapterNumber, String displayTitle) {
		Connection conn = null;
		Statement stmt = null;
		String insertStr = null;
		String selStr = null;
		String updateStr = null;
		int count = -1;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			
			// Check for matches of the drugsysnonym or an ancestor
			// ancestors include drugs
			// set up a conditional string which is the same for both tdrugresoruces and tdrugsynonymresoruces
			// because of how similar the tables are.
			String conditionalStr = " like '%" + drugSynonymName.replaceAll("'","''") + "%'  WHERE vchResourceISBN = '" + bookIsbn + "' AND vchChapterId='" + chapterNumber + "' AND vchSectionId='" + sectionId + "'"; 
			selStr = "SELECT iDrugSynonymResourceId , vchDrugSynonymName FROM tDrugSynonymResource dr inner join tdrugsynonym ds on dr.idrugsynonymid = ds.idrugsynonymid and ds.tirecordstatus = 1 and vchdrugsynonymName " ;
			selStr +=  conditionalStr;
			// add in union for drugs which should also be considered
			selStr +=  " union SELECT (-10 * iDrugResourceId) as iDrugSynonymResourceId, vchDrugName  as vchDrugSynonymName from tDrugResource dr inner join tdrugslist ds on dr.idruglistid = ds.idruglistid and ds.tirecordstatus = 1  and vchdrugName ";
			selStr +=  conditionalStr;
			
			stmt = conn.createStatement();
        	ResultSet rs = stmt.executeQuery(selStr);
        	int drugResId = -1;

        	while(rs.next()) {
        		drugResId = rs.getInt("iDrugSynonymResourceId");
        		if (rs.getString("vchDrugSynonymName").contains(drugSynonymName ) &&  !(rs.getString("vchDrugSynonymName").equals(drugSynonymName ))) {
        			count = 0;
        			break;
        		}
        		// check for a drug parent
        		if (drugResId < -10) {
        			count = 0;
        			break;
        		}
        	}

        	rs.close();
        	
//        	 Check for matches of the drug or an ancestor
        	if (count == -1) {
        		if (drugResId == -1) {
	        		insertStr = "INSERT INTO tDrugSynonymResource(iDrugSynonymId,vchResourceISBN,vchChapterId,vchSectionId,vchTitle, vchCreatorId,dtCreationDate,tiRecordStatus) VALUES(" + drugSynonymId + ",'" + bookIsbn + "','" + chapterNumber + "','" + sectionId + "','" + displayTitle.replaceAll("'","''") +"','risbackend',  getDate(), 1);";
	        		count = stmt.executeUpdate(insertStr);
	        		conn.commit();
	        	} else {
	        	    updateStr = "UPDATE tDrugSynonymResource set vchUpdaterId = 'risbackend', dtLastUpdate = getdate(), tiRecordStatus = 1 where iDrugSynonymResourceId=" + drugResId;
	        	    count = stmt.executeUpdate(updateStr);
	        	    conn.commit();
	        	}
        	}
    	    
		} catch (SQLException e) {
			log.error("Failed to update drug synonym index flag");
			if (insertStr != null) {
				log.error(insertStr);
			} else if (updateStr !=null) {
				log.error(updateStr);
			} else {
				log.error(selStr);
			}
				
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
				conn.close();
			} catch (SQLException e) {}
		}
		return count;
	}

	/**
	 * @param bookIsbn
	 * @param diseaseSynonymId
	 * @param sectionId
	 * @param chapterNumber
	 */
	public void insertDiseaseSynonymResource(String bookIsbn, Integer diseaseSynonymId, String sectionId, String chapterNumber) {
		Connection conn = null;
		Statement stmt = null;
		String insertStr = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			
			String selStr = "SELECT iDiseaseSynonymResourceId FROM tDiseaseSynonymResource WHERE iDiseaseSynonymId = " + diseaseSynonymId + " AND vchResourceISBN = '" + bookIsbn + "' AND vchChapterId='" + chapterNumber + "' AND vchSectionId='" + sectionId + "'";
			stmt = conn.createStatement();
        	ResultSet rs = stmt.executeQuery(selStr);
        	int diseaseResId = -1;
        	if(rs.next())
        		diseaseResId = rs.getInt("iDiseaseSynonymResourceId");
        	rs.close();
        	
        	if(diseaseResId == -1) {
        		insertStr = "INSERT INTO tDiseaseSynonymResource(iDiseaseSynonymId,vchResourceISBN,vchChapterId,vchSectionId,vchCreatorId,dtCreationDate,tiRecordStatus) VALUES(" + diseaseSynonymId + ",'" + bookIsbn + "','" + chapterNumber + "','" + sectionId + "','risbackend',  getDate(), 1);";
        		int count = stmt.executeUpdate(insertStr);
        		conn.commit();
        	} else {
        	    String updateSql = "UPDATE tDiseaseSynonymResource set vchUpdaterId = 'risbackend', dtLastUpdate = getdate(), tiRecordStatus = 1 where iDiseaseSynonymResourceId=" + diseaseResId;
        	    int count = stmt.executeUpdate(updateSql);
        		conn.commit();
        	}
		} catch (SQLException e) {
			log.error("Failed to update disease synonym topic index flag");
			log.error(insertStr);
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
				conn.close();
			} catch (SQLException e) {}
		}
	}
	
	
}