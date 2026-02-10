package com.rittenhouse.RIS.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Category;

import com.rittenhouse.RIS.Main;
import com.rittenhouse.RIS.util.DateUtility;
import com.rittenhouse.RIS.util.IsbnHelper;
import com.rittenhouse.RIS.util.StringUtil;

/**
 * Adds resource information to database
 * 
 * @author vbhatia
 */
public class ResourceDB {

	private DataSource dataSource = null;

	// logger
	protected static Category log = Category.getInstance(ResourceDB.class
			.getName());

	private Connection dbConn = null;

	/**
	 * default constructor
	 */
	public ResourceDB() {
		try {
			Class.forName(Main.getConfigProperty("RISDB.JDBCDriver"));
		} catch (ClassNotFoundException e) {
			log.error(e.toString());
		}
		setDataSource(Main.getConfigProperty("RISDB.URL"),
				Main.getConfigProperty("RISDB.UserID"),
				Main.getConfigProperty("RISDB.Password"));
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
		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
				connectURI, uname, passwd);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
				connectionFactory, connectionPool, null, null, false, true);
		PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
		this.dataSource = dataSource;
	}

	/**
	 * Get resource Id for book ISBN
	 * 
	 * @param bookIsbn
	 * @param activeOnly
	 * @return resource id
	 */
	private static double resPrice = 1.0;

	public Integer getResourceInfo(String bookIsbn, boolean activeOnly) {
		bookIsbn = bookIsbn.replaceAll("-", "");
		Connection conn = null;
		Statement stmt = null;
		int rowId = 0;
		resPrice = 1.0;
		try {
			dbConn = dataSource.getConnection();
			stmt = dbConn.createStatement();
			String selSQL = "SELECT iResourceId ,tiRecordStatus , isnull(tiDrugMonograph, 0) as tiDrugMonograph, decResourcePrice   FROM tResource WHERE REPLACE(vchresourceisbn,'-','') = '"
					+ bookIsbn + "'";
			stmt = dbConn.createStatement();
			ResultSet rs = stmt.executeQuery(selSQL);
			// check info about resource
			if (rs.next()) {
				if (activeOnly) {
					if (1 == rs.getInt("tiRecordStatus")) {
						rowId = rs.getInt("iResourceId");
					}
				} else
					rowId = rs.getInt("iResourceId");
				if (1 == rs.getInt("tiDrugMonograph")) {
					Main.isDrugMongraph = true;
				}
				resPrice = rs.getDouble("decResourcePrice");
			}
			rs.close();
		} catch (SQLException e) {
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
			try {
				dbConn.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
		if (rowId == 0) {
			return null;
		} else {
			return rowId;
		}
	}

	/**
	 * Get XML store collection for existing book (using ISBN)
	 * 
	 * @param bookIsbn
	 * @return XML store collection name
	 */
	public String getBookCollectionName(String bookIsbn) {
		String collectionName = null;
		bookIsbn = bookIsbn.replaceAll("-", "");
		Connection conn = null;
		Statement stmt = null;
		try {
			dbConn = dataSource.getConnection();
			stmt = dbConn.createStatement();
			String selSQL = "SELECT distinct vchLibraryName "
					+ " FROM tResource r, tResourceDiscipline rd, tLibrary l, tLibrarydiscipline ld "
					+ " WHERE vchresourceisbn = '"
					+ bookIsbn
					+ "' and "
					+ " r.iResourceId = rd.iResourceId and l.iLibraryId = ld.iLibraryId"
					+ " and rd.iLibraryDisciplineId = ld.iLibraryDisciplineId";
			stmt = dbConn.createStatement();
			ResultSet rs = stmt.executeQuery(selSQL);
			if (rs.next()) {
				collectionName = rs.getString("vchLibraryName");
			} else {
				return "Medicine";
			}
			rs.close();
		} catch (SQLException e) {
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
			try {
				dbConn.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
		return collectionName;
	}

	/**
	 * @param pubName
	 * @return Publisher Id or null
	 */
	public Integer getPublisherId(String pubName) {
		Connection conn = null;
		Statement stmt = null;
		int rowId = 0;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			String selSQL = "SELECT case when tiRecordStatus = 0 then iConsolidatedPublisherId else  iPublisherId end as iPublisherId FROM tPublisher WHERE UPPER(vchPublisherName) = '"
					+ pubName.toUpperCase() + "'";
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(selSQL);
			if (rs.next()) {
				rowId = rs.getInt("iPublisherId");
			}
			rs.close();
		} catch (SQLException e) {
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
			try {
				conn.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
		if (rowId == 0) {
			return null;
		} else {
			return rowId;
		}
	}

	/**
	 * @param lkpGroup
	 * @param lkpCode
	 * @return
	 */
	public Integer getLookupValueId(String lkpGroup, String lkpCode) {
		Connection conn = null;
		Statement stmt = null;
		int rowId = 0;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			String selSQL = "SELECT iLookupValueId, vchLookupCode FROM tLookupGroup l, tLookupValues lv WHERE l.iLookupGroupId = lv.iLookupGroupId and l.vchLookupGroupCode = '"
					+ lkpGroup.toUpperCase()
					+ "' and vchLookupCode = '"
					+ lkpCode + "'";
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(selSQL);
			if (rs.next()) {
				rowId = rs.getInt("iLookupValueId");
			}
			rs.close();
		} catch (SQLException e) {
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
			try {
				conn.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
		if (rowId == 0) {
			return null;
		} else {
			return rowId;
		}
	}

	/**
	 * Adds a new resource
	 * 
	 * @return
	 */
	public int addNewResource(String vchResourceISBN, String vchResourceDesc,
			String vchResourceTitle, String vchResourceAuthors,
			String pubIdStr, int tiBrandonHillStatus, String decResourcePrice,
			int tiResourceReady, int tiAllowSubscriptions,
			int iResourceStatusId, Date dtResourcePublicationDate,
			String edition, String vchCopyRight, int tiGloballyAccessible) {
		Statement stmt = null;
		PreparedStatement ps = null;
		int currId = 0;
		boolean committed = false;
		boolean failed = false;
		try {
			dbConn = dataSource.getConnection();
			dbConn.setAutoCommit(false);
			stmt = dbConn.createStatement();
			// Get resource status
			String resStatusStr = "NULL";
			
			resStatusStr = "8";
			if (vchCopyRight == null) {
				vchCopyRight = "NULL";
			} else {
				vchCopyRight = "'"
						+ StringUtil
								.replace(vchCopyRight, "'", "''", -1, false)
						+ "'";
			}

			// BJW - 2/9/2011 added iResourceStatusId & decResourcePrice
			// defaults
			if (edition == null)
				edition = "";

			if (decResourcePrice == null) {
				decResourcePrice = "1.0";
			} else {
				double price = Double.parseDouble(decResourcePrice);
				if (price < 1.0) {
					decResourcePrice = "1.0";
				}
			}

			String insSql = "INSERT INTO tResource(vchResourceISBN, vchResourceDesc, vchResourceTitle, vchResourceAuthors, vchResourcePublisher, tiBrandonHillStatus, decResourcePrice, tiResourceReady, tiAllowSubscriptions, iResourceStatusId, iPublisherId, dtResourcePublicationDate, vchResourceEdition,vchCopyRight,tiGloballyAccessible, vchCreatorId, dtCreationDate, tiRecordStatus, vchAuthorXML) VALUES('"
					+ vchResourceISBN
					+ "', '"
					+ StringUtil.replace(vchResourceDesc, "'", "''", -1, false)
					+ "', '"
					+ StringUtil
							.replace(vchResourceTitle, "'", "''", -1, false)
					+ "', '"
					+ StringUtil.replace(vchResourceAuthors, "'", "''", -1,
							false)
					+ "', NULL,"
					+ tiBrandonHillStatus
					+ ","
					+ decResourcePrice
					+ ","
					+ tiResourceReady
					+ ","
					+ tiAllowSubscriptions
					+ ","
					+ resStatusStr
					+ ","
					+ pubIdStr
					+ ","
					+ DateUtility.getDBFormatSQL(dtResourcePublicationDate)
					+ ",'"
					+ (edition.trim().length() > 0 ? edition.trim() : "1st")
					+ "',"
					+ vchCopyRight
					+ ", "
					+ tiGloballyAccessible
					+ ", 'risbackend',  getDate(), 1, ?)";
			log.info("Insert Resource = " + insSql);

			ps = dbConn.prepareStatement(insSql);
			ps.setString(1, Main.authorListXML);
			int count = ps.executeUpdate();

			if (count == 0) {
				dbConn.rollback();
				log.error("Error performing:" + insSql);
				return -1;
			}

			

			stmt = dbConn.createStatement();
			String selSql = "SELECT MAX(iResourceId) currId FROM tResource";
			ResultSet rs = stmt.executeQuery(selSql);
			if (rs.next())
				currId = rs.getInt("currId");
			rs.close();

			// New updtes for tResourceISBN Table updates - 3/3/2009 - Brian
			// Wright

			// First, insert the current ISBN that will be associated with
			// TextML(bIsTextMLIsbn=1)
			insSql = "INSERT INTO tResourceISBN(iResourceId, iResourceIsbnTypeId, vchIsbn, bIsTextMLIsbn) values("
					+ currId
					+ ","
					+ (vchResourceISBN.length() == 10 ? 1 : 2)
					+ ",'" + vchResourceISBN + "',1)";
			log.info(insSql);
			count = stmt.executeUpdate(insSql);
			if (count == 0) {
				dbConn.rollback();
				log.error("Error performing:" + insSql);
				return -1;
			}

			// Second, insert the opposite representation of that isbn (either
			// from 10 to 13, or from 13 to 10) as bIsTextMLIsbn=0
			if (vchResourceISBN.length() == 10) {
				// Convert the isbn
				String isbn13 = IsbnHelper.ISBN10to13(vchResourceISBN);

				// If there was an error converting, ignore
				if (!"ERROR".equalsIgnoreCase(isbn13)) {
					insSql = "INSERT INTO tResourceISBN(iResourceId, iResourceIsbnTypeId, vchIsbn, bIsTextMLIsbn) values("
							+ currId + ",2,'" + isbn13 + "',0)";
					log.info(insSql);
					count = stmt.executeUpdate(insSql);
					if (count == 0) {
						dbConn.rollback();
						log.error("Error performing:" + insSql);
						return -1;
					} else {
						dbConn.commit();
						committed = true;
					}
				} else {
					log.warn("ISBN10->ISBN13 conversion failed; transaction not committed. ISBN: "
							+ vchResourceISBN + ", ResourceId: " + currId);
				}
			} else if (vchResourceISBN.length() == 13) {
				String isbn10 = IsbnHelper.ISBN13to10(vchResourceISBN);
				if (!"ERROR".equalsIgnoreCase(isbn10)) {
					insSql = "INSERT INTO tResourceISBN(iResourceId, iResourceIsbnTypeId, vchIsbn, bIsTextMLIsbn) values("
							+ currId + ",1,'" + isbn10 + "',0)";
					log.info(insSql);
					count = stmt.executeUpdate(insSql);
					if (count == 0) {
						dbConn.rollback();
						log.error("Error performing:" + insSql);
						return -1;
					} else {
						dbConn.commit();
						committed = true;
					}
				} else {
					log.warn("ISBN13->ISBN10 conversion failed; transaction not committed. ISBN: "
							+ vchResourceISBN + ", ResourceId: " + currId);
				}
			} else {
				log.error("vchResourceISBN MUST BE A LENGTH OF EITHER 10 or 13!");
			}
		} catch (Exception e) {
			log.error(e.toString());
			failed = true;
		} finally {
			if (!committed && currId > 0) {
				log.warn("Resource insert not committed (autoCommit=false). ISBN: "
						+ vchResourceISBN + ", ResourceId: " + currId);
			}
			try {
				stmt.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
			try {
				ps.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
		if (failed || !committed) {
			return -1;
		}
		return currId;
	}

	/**
	 * 
	 * @param resourceId
	 * @param vchResourceDesc
	 * @param vchResourceTitle
	 * @param vchResourceSubTitle
	 * @param vchResourceAuthors
	 * @param vchResourcePublisher
	 * @param dtRISReleaseDate
	 * @param pubDate
	 * @param tiBrandonHillStatus
	 * @param vchResourceNLMCall
	 * @param vchResourceISBN
	 * @param vchResourceEdition
	 * @param decResourcePrice
	 * @param decPayPerView
	 * @param decSubScriptionPrice
	 * @param vchResourceImageName
	 * @param tiResourceReady
	 * @param tiAllowSubscriptions
	 */
	public boolean updateResource(Integer resourceId, String vchResourceDesc,
			String vchResourceTitle, String vchResourceSubTitle,
			String vchResourceAuthors, String vchResourcePublisher,
			String dtRISReleaseDate, Date pubDate, int tiBrandonHillStatus,
			String vchResourceNLMCall, String vchResourceISBN,
			String vchResourceEdition, double decResourcePrice,
			double decPayPerView, double decSubScriptionPrice,
			String vchResourceImageName, int tiResourceReady,
			int tiAllowSubscriptions, String copyRightStr) {
		// Get publisher id
		Integer pubId = getPublisherId(vchResourcePublisher);
		String pubIdStr = "NULL";
		if (pubId != null) {
			pubIdStr = "" + pubId.intValue();
		}
		// Get resource status
		String resStatusStr = "NULL";
		Integer resStatusId = getLookupValueId("RESSTATUS", "RESFORTH");
		if (resStatusId != null) {
			resStatusStr = "" + resStatusId.intValue();
		}
		if (dtRISReleaseDate == null) {
			dtRISReleaseDate = "NULL";
		} else {
			dtRISReleaseDate = "'" + dtRISReleaseDate + "'";
		}
		if (vchResourceEdition == null)
			vchResourceEdition = "";

		if (resStatusStr.equals("NULL"))
			resStatusStr = "8";

		String updSql = "UPDATE tResource SET " + " vchResourceTitle = '"
				+ StringUtil.replace(vchResourceTitle, "'", "''", -1, false)
				+ "',"
				+ " vchResourceAuthors = '"
				+ vchResourceAuthors
				+ "',"
				+ " vchResourcePublisher = '"
				+ vchResourcePublisher
				+ "',"
				+ " dtResourcePublicationDate = "
				+ DateUtility.getDBFormatSQL(pubDate)
				+ ","
				+ " vchResourceEdition = '"
				+ (vchResourceEdition.trim().length() > 0 ? vchResourceEdition
						: "1st") + "'," + " tiResourceReady = "
				+ tiResourceReady + "," + " iPublisherId = " + pubIdStr + ","
				+ " iResourceStatusId = " + resStatusStr + ","
				+ " vchUpdaterId = 'risbackend',"
				+ " dtLastUpdate = getDate()," + " tiRecordStatus = 1,"
				+ " vchCopyRight = '" + copyRightStr + "'"
				+ ", vchAuthorXML=?,decResourcePrice=?  WHERE iResourceId = "
				+ resourceId.intValue();
		Statement stmt = null;
		PreparedStatement ps = null;
		boolean success = true;
		int currId = 0;
		try {
			dbConn = dataSource.getConnection();
			dbConn.setAutoCommit(false);
			log.info("Update Resource = " + updSql);
			ps = dbConn.prepareStatement(updSql);
			ps.setString(1, Main.authorListXML);
			ps.setDouble(2, (resPrice > 0 ? resPrice : 1.0));
			int count = ps.executeUpdate();
			if (count == 0) {
				dbConn.rollback();
				log.error("Error performing:" + updSql);
				success = false;
			} else {
				dbConn.commit();
			}
		} catch (SQLException e) {
			log.error(e.toString());
			success = false;
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
		// don't forget to update the disciplines or life gets complex
		updSql = "UPDATE tResourceDiscipline SET "
				+ " vchUpdaterId = 'risbackend',"
				+ " dtLastUpdate = getDate()," + " tiRecordStatus = 1"
				+ " WHERE iResourceId = " + resourceId.intValue();
		stmt = null;
		try {
			dbConn = dataSource.getConnection();
			dbConn.setAutoCommit(true);
			stmt = dbConn.createStatement();
			log.debug("Update Resourcediscipline = " + updSql);
			int count = stmt.executeUpdate(updSql);

		} catch (SQLException e) {
			log.error(e.toString());
			success = false;
		} finally {
			try {
				stmt.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
		return success;

	}

	/**
	 * Adds a new publisher. Also checks for duplicates.
	 * <p>
	 * </p>
	 * 
	 * @param vchPublisherName
	 * @param vchPublisherAddr1
	 * @param vchPublisherAddr2
	 * @param vchPublisherCity
	 * @param vchPublisherState
	 * @return publisher Id
	 */
	public int addNewPublisher(String vchPublisherName,
			String vchPublisherAddr1, String vchPublisherAddr2,
			String vchPublisherCity, String vchPublisherState) {
		Statement stmt = null;
		int currId = 0;
		try {
			dbConn = dataSource.getConnection();
			dbConn.setAutoCommit(false);
			stmt = dbConn.createStatement();
			// Get publisher id
			Integer pubId = getPublisherId(vchPublisherName);
			String pubIdStr = "NULL";
			if (pubId != null) {
				pubIdStr = pubId.toString();
				log.debug("Publisher = '" + vchPublisherName
						+ "' already exists.");
				return pubId.intValue();
			} else {
				String insSql = "INSERT INTO tPublisher(vchPublisherName, vchPublisherAddr1,"
						+ " vchPublisherAddr2, vchPublisherCity,"
						+ " vchPublisherState, vchCreatorId, dtCreationDate, tiRecordStatus) VALUES('"
						+ StringUtil.replace(vchPublisherName, "'", "''", -1,
								false)
						+ "', '"
						+ StringUtil.replace(vchPublisherAddr1, "'", "''", -1,
								false)
						+ "', '"
						+ StringUtil.replace(vchPublisherAddr2, "'", "''", -1,
								false)
						+ "', '"
						+ StringUtil.replace(vchPublisherCity, "'", "''", -1,
								false)
						+ "', '"
						+ StringUtil.replace(vchPublisherState, "'", "''", -1,
								false) + "'," + " 'risbackend',  getDate(), 1)";
				log.debug("Insert Publisher = " + insSql);
				int count = stmt.executeUpdate(insSql);
				if (count == 0) {
					dbConn.rollback();
				} else {
					dbConn.commit();
				}

				stmt = dbConn.createStatement();
				String selSql = "SELECT MAX(iPublisherId) currId FROM tPublisher";
				ResultSet rs = stmt.executeQuery(selSql);
				if (rs.next())
					currId = rs.getInt("currId");
				rs.close();
			}
		} catch (Exception e) {
			log.error(e.toString());
		} finally {
			try {
				stmt.close();
			} catch (Exception e) {
				log.error(e.toString());
			}
		}
		return currId;
	}

	/**
	 * @return Returns the dbConn.
	 */
	public Connection getDbConn() {
		return dbConn;
	}

	/**
	 * @param dbConn
	 *            The dbConn to set.
	 */
	public void setDbConn(Connection dbConn) {
		this.dbConn = dbConn;
	}
}
