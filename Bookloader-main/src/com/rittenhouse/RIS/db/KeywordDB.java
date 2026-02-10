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
public class KeywordDB {

	private DataSource dataSource = null;
	public static final int CHAPTER_LEVEL = 1;
	public static final int SECTION_LEVEL = 2;
	
	
	//logger
	protected static Category log = Category.getInstance(KeywordDB.class.getName());
	
	/**
	 * default constructor
	 */
	public KeywordDB() {
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
	 * @param keyword
	 * @param chapterNumber
	 * @param sectionId
	 */
	public void removeKeywordResources(String bookIsbn) {
		Connection conn = null;
		Statement stmt = null;
		String removeStr = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			
			removeStr = "exec sp_removeKeywordResources '" + bookIsbn + "'   ,'risbackend'" ;
			stmt.execute(removeStr);
       		conn.commit();
        	
		} catch (SQLException e) {
			log.error("Failed to insert keyword resource entry.");
			log.error(removeStr);
			log.error(e.toString());			
			
		} //
		finally {
		try {
		   stmt.close();
			conn.close();
		} catch (SQLException e) {}
		}
		
	}
	
	/**
	 * @param bookIsbn
	 * @param keyword
	 * @param chapterNumber
	 * @param sectionId
	 */
	public void insertKeywordResource(String bookIsbn, String keyword, String chapterNumber, String sectionId) {
		Connection conn = null;
		Statement stmt = null;
		String insertStr = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			
			insertStr = "exec sp_insertKeywordResource '" + keyword + "'  ,'" + bookIsbn + "'  , '" + chapterNumber + "' , '" + sectionId + "' ,'risbackend'" ;
			log.debug(insertStr);
	    	stmt.execute(insertStr );
       		conn.commit();
        	
		} catch (SQLException e) {
			log.error("Failed to insert keyword resource entry.");
			log.error(insertStr);
			log.error(e.toString());			
			
		} //
		finally {
		try {
		   stmt.close();
			conn.close();
		} catch (SQLException e) {}
		}
	}
	
	
	/**
	 * @param bookIsbn
	 * @param keyword
	 * @param locationLevel
	 * @param chapterNumber
	 * @param sectionId
	 */
	public void insertKeywordLocation(String bookIsbn, String keyword, int locationLevel, String chapterNumber, String sectionId) {
		Connection conn = null;
		Statement stmt = null;
		String insertStr = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			if (locationLevel == CHAPTER_LEVEL)
			{
				insertStr = "exec sp_insertKeywordLocation '" + keyword + "'  ,'" + bookIsbn + "'  , " + CHAPTER_LEVEL + ", '" + chapterNumber + "' , null ,'risbackend'" ;		    		
			}	
			else // must be SECTION_LEVEL 
			{
				insertStr = "exec sp_insertKeywordLocation '" + keyword + "'  ,'" + bookIsbn + "'  , " + SECTION_LEVEL + ", '" + chapterNumber + "' , '" + sectionId + "' ,'risbackend'" ;		    		
			}
				
			stmt.execute(insertStr );
       		conn.commit();
        	
		} catch (SQLException e) {
			log.error("Failed to update insert keyword location entry.");
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
	 */
	public void purgeKeywordResource(String bookIsbn) {
		Connection conn = null;
		Statement stmt = null;
		String updateSql = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			
        	updateSql = "UPDATE tKeywordResource set vchUpdaterId = 'risbackend', dtLastUpdate = getdate(), tiRecordStatus = 0 where vchResourceISBN = '" + bookIsbn +"'";
        	int count = stmt.executeUpdate(updateSql);
        	conn.commit();
        	
		} catch (SQLException e) {
			log.error("Failed to update keyword resource status flag");
			log.error(updateSql);
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
	 */
	public void purgeKeywordLocation(String bookIsbn) {
		Connection conn = null;
		Statement stmt = null;
		String updateSql = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			
        	updateSql = "UPDATE tKeywordLocation set vchUpdaterId = 'risbackend', dtLastUpdate = getdate(), tiRecordStatus = 0 where vchResourceISBN = '" + bookIsbn +"'";
        	int count = stmt.executeUpdate(updateSql);
        	conn.commit();
        	
		} catch (SQLException e) {
			log.error("Failed to update keyword location status flag");
			log.error(updateSql);
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
				conn.close();
			} catch (SQLException e) {}
		}
	}
	
	public ResultSet getKeywordLocation(String bookIsbn, String chapter, String section) {
		String getSql = "exec sp_getKeywordLocation 		@vchResourceISBN = '" + bookIsbn +"',	@vchChapterId = '" + chapter + "', @vchSectionId = " + section;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			log.debug("Executing " + getSql);
			rs = stmt.executeQuery(getSql);
			
			
		} catch (SQLException e) {
			log.error("Failed to retrieve keywordLocation list");
			log.error(getSql );
			log.error(e.toString());
			
		}
		
		return rs;
	}
	
}