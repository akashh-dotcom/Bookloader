package com.rittenhouse.RIS.util;

import java.sql.Connection;
import java.sql.SQLException;

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
 * @author vbhatia
 */
public class DBUtil {
	
	private static DataSource dataSource = null;
	//logger
	protected static Category log = Category.getInstance(DBUtil.class.getName());
	
	private DBUtil() {}
	
	/**
	 * @param connectURI
	 * @param uname
	 * @param passwd
	 */
	public static void setDataSource(String connectURI, String uname, String passwd) {
		if (dataSource!=null) {
			ObjectPool connectionPool = new GenericObjectPool(null);
			ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI, uname, passwd);
			PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
			PoolingDataSource dSource = new PoolingDataSource(connectionPool);
			dataSource = dSource;
		}
	}
	
	/**
	 * @return DB Connection
	 */
	public static Connection getDBConnection() {
		try {
			Class.forName(Main.getConfigProperty("RISDB.JDBCDriver"));
		} catch (ClassNotFoundException e) {
			log.error(e.toString());
		}
		DBUtil.setDataSource(Main.getConfigProperty("RISDB.URL"), Main.getConfigProperty("RISDB.UserID"), Main.getConfigProperty("RISDB.Password"));
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			log.error(e.toString());
		}
		return null;
	}
}
