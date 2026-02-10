package com.rittenhouse.RIS.mesh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Iterator;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.rittenhouse.RIS.Main;

public class MetaDataXMLCreator {

	private static DataSource dataSource = null;
	private static LinkedHashMap diseaseMetaData = null;

	private static LinkedHashMap diseaseSynonymMetaData = null;

	private static LinkedHashMap diseaseSynonymXDiseaseMetaData = null;

	private static LinkedHashMap topicMetaData = null;

	private static LinkedHashMap topicSynonymMetaData = null;

	private static LinkedHashMap topicSynonymXTopicMetaData = null;

	private static LinkedHashMap drugMetaData = null;

	private static LinkedHashMap drugSynonymMetaData = null;

	private static LinkedHashMap drugSynonymXDrugMetaData = null;

	
	static String encoding = System.getProperty("file.encoding");
    static String fileName = System.getProperty("user.dir") + Main.FILE_SEPARATOR + "..\\meta-data\\diseaseSynonym_topicSynonym.xml";
	
	public static void main(String[] args) {
		
        loadMetaData();
        
        writeDiseaseMetaData();
        
	}
	
	static void writeDiseaseMetaData() {
		StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buffer.append("<MetaData>\n");
        appendToFile(buffer, fileName);
    	
        
        
    	
        Iterator diseaseIt = diseaseSynonymMetaData.keySet().iterator();
		while (diseaseIt.hasNext()) {
			Integer diseaseSynonymId = (Integer) diseaseIt.next();
			Integer diseaseId = (Integer) diseaseSynonymXDiseaseMetaData.get(diseaseSynonymId);
			String diseaseName = (String)diseaseMetaData.get(diseaseId);
			String diseaseSynonymName = (String) diseaseSynonymMetaData.get(diseaseSynonymId);
        	
			
        	
        	buffer = new StringBuffer();        	
        	Iterator topicSynonymIt = topicSynonymMetaData.keySet().iterator();
			while (topicSynonymIt.hasNext()) {
				Integer topicSynonymId = (Integer) topicSynonymIt.next();
				Integer topicId = (Integer) topicSynonymXTopicMetaData.get(topicSynonymId);
				String topicSynonymName = (String) topicSynonymMetaData.get(topicSynonymId);
				String topicName = (String) topicMetaData.get(topicId);
				buffer.append("<MetaDataItem>\n");
	        	buffer.append("<Term>" + diseaseName + "</Term>\n");
	        	buffer.append("<TermSynonym>" + diseaseSynonymName + "</TermSynonym>\n");
				buffer.append("<Topic>" + topicName + "</Topic>\n");
				buffer.append("<TopicSynonym>" + topicSynonymName +"</TopicSynonym>\n");
				buffer.append("</MetaDataItem>\n");
			}
			appendToFile(buffer, fileName);
        	buffer = null;
        }
		
		buffer = new StringBuffer();
		buffer.append("</MetaData>");
		appendToFile(buffer, fileName);
		buffer = null;
	}
		
	public static DataSource setupDataSource(String connectURI, String uname, String passwd) {
        ObjectPool connectionPool = new GenericObjectPool(null);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI,uname,passwd);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
        return dataSource;
    }
	
	public static void writeNewFile(StringBuffer buffer, String fileName) {
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(new File(fileName)), System.getProperty("file.encoding"));
			writer.write(buffer.toString());
			writer.flush();
			writer.close();
		} catch (IOException e1) {
					e1.printStackTrace();
		}
	}
	
	public static void appendToFile(StringBuffer buffer, String fileName) {
		try {
			Writer writer = new OutputStreamWriter(new FileOutputStream(fileName, true), System.getProperty("file.encoding"));
			writer.write(buffer.toString());
			writer.flush();
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void loadMetaData() {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;

		topicMetaData = new LinkedHashMap();
		topicSynonymMetaData = new LinkedHashMap();
		topicSynonymXTopicMetaData = new LinkedHashMap();

		drugMetaData = new LinkedHashMap();
		drugSynonymXDrugMetaData = new LinkedHashMap();
		drugSynonymMetaData = new LinkedHashMap();

		diseaseMetaData = new LinkedHashMap();
		diseaseSynonymMetaData = new LinkedHashMap();
		diseaseSynonymXDiseaseMetaData = new LinkedHashMap();
		try {
			Class.forName(Main.getConfigProperty("RISDB.JDBCDriver"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		dataSource = setupDataSource(Main.getConfigProperty("RISDB.URL"),
				Main.getConfigProperty("RISDB.UserID"),
				Main.getConfigProperty("RISDB.Password"));

		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();

			if ( Main.isMiniDrug) {
				rset = stmt.executeQuery("SELECT distinct(vchDiseaseName), iDiseaseNameId FROM tDiseaseName WHERE tiRecordStatus=1 and vchDiseaseName like 'a%'");
				
			} else {
				rset = stmt.executeQuery("SELECT distinct(vchDiseaseName), iDiseaseNameId FROM tDiseaseName WHERE tiRecordStatus=1");
				
			}
			Integer diseaseKey = null;
			String diseaseValue = null;
			while (rset.next()) {
				diseaseKey = rset.getInt("iDiseaseNameId");
				diseaseValue = rset.getString("vchDiseaseName");
				diseaseMetaData.put(diseaseKey, diseaseValue);
			}

			stmt = conn.createStatement();
			if ( Main.isMiniDrug) {
				rset = stmt
				.executeQuery("select distinct(s.vchDiseaseSynonym) as synonymName, s.iDiseaseSynonymId synonymId, s.iDiseaseNameId diseaseId from tDiseaseName d, tDiseaseSynonym s where d.iDiseaseNameId = s.iDiseaseNameId and d.tiRecordStatus = 1 and s.tiRecordStatus = 1 and vchDiseaseSynonym like 'am%' ");
				
			} else {
				rset = stmt
				.executeQuery("select distinct(s.vchDiseaseSynonym) as synonymName, s.iDiseaseSynonymId synonymId, s.iDiseaseNameId diseaseId from tDiseaseName d, tDiseaseSynonym s where d.iDiseaseNameId = s.iDiseaseNameId and d.tiRecordStatus = 1 and s.tiRecordStatus = 1 order by len(vchDiseaseSynonym) desc , vchDiseaseSynonym");
				
			}
			diseaseKey = null;
			String diseaseSynonymName = null;
			Integer diseaseSynonymKey = null;
			while (rset.next()) {
				diseaseKey = rset.getInt("diseaseId");
				diseaseSynonymKey = rset.getInt("synonymId");
				diseaseSynonymName = rset.getString("synonymName");

				diseaseSynonymMetaData.put(diseaseSynonymKey,
						diseaseSynonymName);
				diseaseSynonymXDiseaseMetaData.put(diseaseSynonymKey,
						diseaseKey);
			}

			stmt = conn.createStatement();
			if ( Main.isMiniDrug) {
				//and vchDiseaseSynonym like 'am%'
				rset = stmt.executeQuery("select iTopicId, vchTopicName, iCategoryId from tTopic WHERE tiRecordStatus=1 and vchTopicName like 'am%' order by vchTopicName ");
				
			} else {
				rset = stmt.executeQuery("select iTopicId, vchTopicName, iCategoryId from tTopic WHERE tiRecordStatus=1 order by len(vchTopicName) desc, vchTopicName ");
				
			}
			Integer topicKey = null;
			String topicValue = null;
			while (rset.next()) {
				topicKey = rset.getInt("iTopicId");
				topicValue = rset.getString("vchTopicName");
				topicMetaData.put(topicKey, topicValue);
			}

			stmt = conn.createStatement();
			if ( Main.isMiniDrug) {
				//and vchDiseaseSynonym like 'am%'
				rset = stmt.executeQuery("select s.iSynonymTopicId synonymId, s.iTopicId topicId, s.vchSynonymTopicName synonymName from tTopic t, tsynonymtopics s  where t.iTopicId = s.iTopicId and t.tiRecordStatus = 1 and s.tiRecordStatus = 1 and vchSynonymTopicName like 'am%'");
			} else {
				rset = stmt.executeQuery("select s.iSynonymTopicId synonymId, s.iTopicId topicId, s.vchSynonymTopicName synonymName from tTopic t, tsynonymtopics s  where t.iTopicId = s.iTopicId and t.tiRecordStatus = 1 and s.tiRecordStatus = 1 order by len( vchSynonymTopicName) , vchSynonymTopicName");
				
			}
			
			topicKey = null;
			String topicSynonymName = null;
			Integer topicSynonymKey = null;
			while (rset.next()) {
				topicKey = rset.getInt("topicId");
				topicSynonymKey = rset.getInt("synonymId");
				topicSynonymName = rset.getString("synonymName");

				topicSynonymMetaData.put(topicSynonymKey, topicSynonymName);
				topicSynonymXTopicMetaData.put(topicSynonymKey, topicKey);
			}
			stmt = conn.createStatement();
			if ( Main.isMiniDrug) {
				rset = stmt.executeQuery("select iDrugListId, vchDrugName from tDrugsList WHERE tiRecordStatus=1 and  ( vchdrugname like 'Amoxicil%' or vchdrugname like 'ampicillin' ) order by len(vchdrugname ) desc , vchdrugname ");
				
			} else {
				rset = stmt.executeQuery("select  iDrugListId, vchDrugName from tDrugsList WHERE tiRecordStatus=1 order by len(vchdrugname ) desc , vchdrugname ");
			}

			Integer drugKey = null;
			String drugName = null;
			while (rset.next()) {
				drugKey = rset.getInt("iDrugListId");
				drugName = rset.getString("vchDrugName");
				drugMetaData.put(drugKey, drugName);
			}

			stmt = conn.createStatement();
			if ( Main.isMiniDrug) {
				rset = stmt.executeQuery("select s.iDrugSynonymId synonymId, s.iDrugListId drugId, s.vchDrugSynonymName synonymName from tDrugsList d, tDrugSynonym s where d.iDrugListId = s.iDrugListId and d.tiRecordStatus = 1 and s.tiRecordStatus = 1  and ( vchdrugsynonymname like 'Amoxicil%' or vchdrugsynonymname like 'ampicillin' ) order by len(vchdrugsynonymname ) desc , vchdrugsynonymname ");
			} else {
				rset = stmt.executeQuery("select s.iDrugSynonymId synonymId, s.iDrugListId drugId, s.vchDrugSynonymName synonymName from tDrugsList d, tDrugSynonym s where d.iDrugListId = s.iDrugListId and d.tiRecordStatus = 1 and s.tiRecordStatus = 1 order by len(vchdrugsynonymname ) desc , vchdrugsynonymname ");
			}

			drugKey = null;
			String drugSynonymName = null;
			Integer drugSynonymKey = null;
			while (rset.next()) {
				drugKey = rset.getInt("drugId");
				drugSynonymKey = rset.getInt("synonymId");
				drugSynonymName = rset.getString("synonymName");

				drugSynonymMetaData.put(drugSynonymKey, drugSynonymName);
				drugSynonymXDrugMetaData.put(drugSynonymKey, drugKey);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rset.close();
			} catch (SQLException e) {}
			try {
				stmt.close();
			} catch (SQLException e) {}
			try {
				conn.close();
			} catch (SQLException e) {}
		}
	}

}
