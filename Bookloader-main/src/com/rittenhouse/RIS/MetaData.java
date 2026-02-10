package com.rittenhouse.RIS;

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
import org.apache.log4j.Category;
import org.w3c.dom.Element;

import com.rittenhouse.RIS.util.XMLUtil;
import com.rittenhouse.RIS.Main;

/**
 * MetaData - Caches disease and drug meta-data from database
 * @author vbhatia
 */
public class MetaData {
	

	//logger
	protected static Category log = Category.getInstance(MetaData.class.getName());
	
	private DataSource dataSource = null;
	
	//Meta-data cache
	private static LinkedHashMap diseaseMetaData = null;

	private static LinkedHashMap diseaseSynonymMetaData = null;

	private static LinkedHashMap diseaseSynonymXDiseaseMetaData = null;

	private static LinkedHashMap topicMetaData = null;

	private static LinkedHashMap topicSynonymMetaData = null;

	private static LinkedHashMap topicSynonymXTopicMetaData = null;

	private static LinkedHashMap drugMetaData = null;

	private static LinkedHashMap drugSynonymMetaData = null;

	private static LinkedHashMap drugSynonymXDrugMetaData = null;
	
	private static LinkedHashMap medicalTermMetaData = null;
	private static boolean bUseTopics = false;
	private static boolean bUseDrugs = false;

	/**
	 * Load meta-data
	 * 
	 * @return true on success and false on failure
	 */	
	public boolean loadMetaData() {
	    
	    log.info("Loading meta-data from database...");
	    
		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		String ruleAction = Main.XML_TAG_RULE_ACTION_APPLY;
		String ruleId = null;
		bUseTopics = false;
		bUseDrugs = false;

		Element rulesElem = Main.rulesDoc.getDocumentElement();
		Iterator ruleIt = XMLUtil.getChildrenByTagName(rulesElem, Main.XML_TAG_RULE);

		//Iterate on all rules to see which maps we will use
		while (ruleIt.hasNext()){
			Element ruleElem = ((Element) ruleIt.next());
			if (ruleElem.getNodeName().equals(Main.XML_TAG_RULE)){
				ruleId = ruleElem.getAttributes().getNamedItem(Main.XML_TAG_RULE_TYPE).getNodeValue();
				ruleAction = ruleElem.getAttributes().getNamedItem(Main.XML_TAG_RULE_ACTION).getNodeValue();
				if (ruleAction.equals(Main.XML_TAG_RULE_ACTION_APPLY)){
					if (ruleId.equalsIgnoreCase(Main.XML_TAG_RULE_TYPE_LINKDRUG) ){
						bUseDrugs = true;
					}
					if (ruleId.equalsIgnoreCase(Main.XML_TAG_RULE_TYPE_LINKDISEASE) ){
						bUseTopics = true;
					}
					if (ruleId.equalsIgnoreCase(Main.XML_TAG_RULE_TYPE_ADDRISINDEX1) ){
						bUseTopics = true;
						bUseDrugs = true;
					}
				}
			}
			
		}
		try {
			Class.forName(Main.getConfigProperty("RISDB.JDBCDriver"));
		} catch (ClassNotFoundException e) {
			log.error("Unable to retreive Content meta-data. Please check your database configuration.");
			log.error(e.toString());
			return false;
		} finally{
			ruleIt = null;
			
		}
		

		setDataSource(Main.getConfigProperty("RISDB.URL"), Main.getConfigProperty("RISDB.UserID"), Main.getConfigProperty("RISDB.Password"));
		String subsetTerm = "am%";
		if (!Main.useTermsSubset){
			try{
				if (Main.getConfigProperty("RIS.LOOKUP_SUBSET").equals("true") ){
					Main.useTermsSubset = true;
					subsetTerm = Main.getConfigProperty("RIS.LOOKUP_TERM") ;
					if (subsetTerm.equals("")){
						subsetTerm = "am%";
					}
				}
			} catch (Exception e){
				
			}
		}

		if (bUseTopics){
		topicMetaData = new LinkedHashMap();
		topicSynonymMetaData = new LinkedHashMap();
		topicSynonymXTopicMetaData = new LinkedHashMap();
		diseaseMetaData = new LinkedHashMap();
		diseaseSynonymMetaData = new LinkedHashMap();
		diseaseSynonymXDiseaseMetaData = new LinkedHashMap();
		
		medicalTermMetaData = new LinkedHashMap();
		}
		if (bUseDrugs){
		drugMetaData = new LinkedHashMap();
		drugSynonymXDrugMetaData = new LinkedHashMap();
		drugSynonymMetaData = new LinkedHashMap();
		}
		

		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			if (bUseTopics){
				if ( Main.useTermsSubset){
					rset = stmt.executeQuery("SELECT distinct vchDiseaseName , iDiseaseNameId FROM tDiseaseName WHERE tiRecordStatus=1 and vchDiseaseName like '" + subsetTerm + "' order by vchDiseaseName ");
					
				} else {
					rset = stmt.executeQuery("SELECT distinct vchDiseaseName, iDiseaseNameId , len(vchDiseaseName) FROM tDiseaseName WHERE tiRecordStatus=1  order by len(vchDiseaseName) desc, vchDiseaseName ");
					
				}


			Integer diseaseKey = null;
			String diseaseValue = null;
			while (rset.next()) {
				diseaseKey = rset.getInt("iDiseaseNameId");
				diseaseValue = rset.getString("vchDiseaseName");
                diseaseMetaData.put(diseaseKey, diseaseValue.trim());
			}

			stmt = conn.createStatement();
			if ( Main.useTermsSubset){
				rset = stmt
				.executeQuery("select distinct s.vchDiseaseSynonym as vchDiseaseSynonym, s.iDiseaseSynonymId synonymId, s.iDiseaseNameId diseaseId , len(vchDiseaseSynonym) from tDiseaseName d, tDiseaseSynonym s where d.iDiseaseNameId = s.iDiseaseNameId and d.tiRecordStatus = 1 and s.tiRecordStatus = 1 and vchDiseaseSynonym like '" + subsetTerm + "'  order by len(vchDiseaseSynonym) desc , vchDiseaseSynonym ");
				
			} else {
				rset = stmt
				.executeQuery("select distinct s.vchDiseaseSynonym , len(vchDiseaseSynonym), s.iDiseaseSynonymId synonymId, s.iDiseaseNameId diseaseId from tDiseaseName d, tDiseaseSynonym s where d.iDiseaseNameId = s.iDiseaseNameId and d.tiRecordStatus = 1 and s.tiRecordStatus = 1 order by len(vchDiseaseSynonym) desc , vchDiseaseSynonym");
				
			}

			diseaseKey = null;
			String diseaseSynonymName = null;
			Integer diseaseSynonymKey = null;
			while (rset.next()) {
				diseaseKey = rset.getInt("diseaseId");
				diseaseSynonymKey = rset.getInt("synonymId");
				diseaseSynonymName = rset.getString("vchDiseaseSynonym");

				if (!diseaseSynonymMetaData.containsValue(diseaseSynonymName)){
				    diseaseSynonymMetaData.put(diseaseSynonymKey, diseaseSynonymName.trim());
				    diseaseSynonymXDiseaseMetaData.put(diseaseSynonymKey, diseaseKey);
				}
			}
			log.info("Loaded disease = " + diseaseMetaData.size() + " and disease synonyms = " + diseaseSynonymMetaData.size());

			stmt = conn.createStatement();
			if ( Main.useTermsSubset){
				//and vchDiseaseSynonym like 'am%'
				rset = stmt.executeQuery("select iTopicId, vchTopicName, iCategoryId from tTopic WHERE tiRecordStatus=1 and vchTopicName like '" + subsetTerm + "' order by vchTopicName ");
				
			} else {
				rset = stmt.executeQuery("select iTopicId, vchTopicName, iCategoryId , len(vchTopicName) from tTopic WHERE tiRecordStatus=1 order by len(vchTopicName) desc, vchTopicName ");
				
			}
			Integer topicKey = null;
			String topicValue = null;
			while (rset.next()) {
				topicKey = rset.getInt("iTopicId");
				topicValue = rset.getString("vchTopicName").toLowerCase().trim();
                topicMetaData.put(topicKey, topicValue);
			}

			stmt = conn.createStatement();
			if ( Main.useTermsSubset){
				//and vchDiseaseSynonym like 'am%'
				rset = stmt.executeQuery("select s.iSynonymTopicId synonymId, s.iTopicId topicId, s.vchSynonymTopicName synonymName from tTopic t, tsynonymtopics s  where t.iTopicId = s.iTopicId and t.tiRecordStatus = 1 and s.tiRecordStatus = 1 and vchSynonymTopicName like '" + subsetTerm + "'");
			} else {
				rset = stmt.executeQuery("select s.iSynonymTopicId synonymId, s.iTopicId topicId, s.vchSynonymTopicName synonymName , len( vchSynonymTopicName) from tTopic t, tsynonymtopics s  where t.iTopicId = s.iTopicId and t.tiRecordStatus = 1 and s.tiRecordStatus = 1 order by len( vchSynonymTopicName) , vchSynonymTopicName");
				
			}
			topicKey = null;
			String topicSynonymName = null;
			Integer topicSynonymKey = null;
			while (rset.next()) {
				topicKey = rset.getInt("topicId");
				topicSynonymKey = rset.getInt("synonymId");
				topicSynonymName = rset.getString("synonymName").toLowerCase().trim();
				if (!topicSynonymMetaData.containsValue(topicSynonymName) && !topicMetaData.containsValue(topicSynonymName)){
					topicSynonymMetaData.put(topicSynonymKey, topicSynonymName);
					topicSynonymXTopicMetaData.put(topicSynonymKey, topicKey);
				}
			}
			log.info("Loaded topics = " + topicMetaData.size() + " and topic synonyms = " + topicSynonymMetaData.size());
			}
			if ( bUseDrugs){
			stmt = conn.createStatement();
			if( Main.useTermsSubset){
				rset = stmt.executeQuery("select iDrugListId, vchDrugName , len(vchdrugname ) from tDrugsList WHERE tiRecordStatus=1 and  ( vchdrugname like 'Betamethasone %' or vchdrugname like '" + subsetTerm + "' ) order by len(vchdrugname ) desc , vchdrugname ");
				
			} else {
				rset = stmt.executeQuery("select iDrugListId, vchDrugName , len(vchdrugname ) from tDrugsList WHERE tiRecordStatus=1 order by len(vchdrugname ) desc , vchdrugname ");
			}
			Integer drugKey = null;
			String drugName = null;
			while (rset.next()) {
				drugKey = rset.getInt("iDrugListId");
				drugName = rset.getString("vchDrugName");
				
				drugMetaData.put(drugKey, drugName.trim());
				
			}

			stmt = conn.createStatement();
			if( Main.useTermsSubset){
				rset = stmt.executeQuery("select s.iDrugSynonymId synonymId, s.iDrugListId drugId, s.vchDrugSynonymName synonymName , len(vchdrugsynonymname ) from tDrugsList d, tDrugSynonym s where d.iDrugListId = s.iDrugListId and d.tiRecordStatus = 1 and s.tiRecordStatus = 1  and ( vchdrugsynonymname like 'Betamethasone %' or vchdrugsynonymname like '" + subsetTerm + "' ) order by len(vchdrugsynonymname ) desc , vchdrugsynonymname ");
			} else {
				rset = stmt.executeQuery("select s.iDrugSynonymId synonymId, s.iDrugListId drugId, s.vchDrugSynonymName synonymName , len(vchdrugsynonymname ) from tDrugsList d, tDrugSynonym s where d.iDrugListId = s.iDrugListId and d.tiRecordStatus = 1 and s.tiRecordStatus = 1 order by len(vchdrugsynonymname ) desc , vchdrugsynonymname ");
			}

			drugKey = null;
			String drugSynonymName = null;
			Integer drugSynonymKey = null;
			while (rset.next()) {
				drugKey = rset.getInt("drugId");
				drugSynonymKey = rset.getInt("synonymId");
				drugSynonymName = rset.getString("synonymName");

				drugSynonymMetaData.put(drugSynonymKey, drugSynonymName.trim());
				drugSynonymXDrugMetaData.put(drugSynonymKey, drugKey);
			}
			log.info("Loaded drug = " + drugMetaData.size() + " and drug synonyms = " + drugSynonymMetaData.size());
			}

		} catch (SQLException e) {
			log.error("Unable to retreive Content meta-data. Please check your database configuration.");
			if (stmt != null) {
				log.error("Statement: " + stmt.toString());
			}
			log.error("SQLException: " + e.toString(), e);
			return false;
		} finally {
			if (rset != null)
			try {
				rset.close();
			} catch (SQLException e) {}
			if (stmt != null)
			try {
				stmt.close();
			} catch (SQLException e) {}
			if (conn != null)
			try {
				conn.close();
			} catch (SQLException e) {}
		}
		return true;
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
	 * @return Returns the dataSource.
	 */
	public DataSource getDataSource() {
		return dataSource;
	}
	
	/**
	 * @return Returns the diseaseMetaData.
	 */
	public static LinkedHashMap getDiseaseMetaData() {
		return diseaseMetaData;
	}
	/**
	 * @param diseaseMetaData The diseaseMetaData to set.
	 */
	public static void setDiseaseMetaData(LinkedHashMap diseaseMetaData) {
		MetaData.diseaseMetaData = diseaseMetaData;
	}
	/**
	 * @return Returns the diseaseSynonymMetaData.
	 */
	public static LinkedHashMap getDiseaseSynonymMetaData() {
		return diseaseSynonymMetaData;
	}
	/**
	 * @param diseaseSynonymMetaData The diseaseSynonymMetaData to set.
	 */
	public static void setDiseaseSynonymMetaData(LinkedHashMap diseaseSynonymMetaData) {
		MetaData.diseaseSynonymMetaData = diseaseSynonymMetaData;
	}
	/**
	 * @return Returns the diseaseSynonymXDiseaseMetaData.
	 */
	public static LinkedHashMap getDiseaseSynonymXDiseaseMetaData() {
		return diseaseSynonymXDiseaseMetaData;
	}
	/**
	 * @param diseaseSynonymXDiseaseMetaData The diseaseSynonymXDiseaseMetaData to set.
	 */
	public static void setDiseaseSynonymXDiseaseMetaData(
			LinkedHashMap diseaseSynonymXDiseaseMetaData) {
		MetaData.diseaseSynonymXDiseaseMetaData = diseaseSynonymXDiseaseMetaData;
	}
	/**
	 * @return Returns the drugMetaData.
	 */
	public static LinkedHashMap getDrugMetaData() {
		return drugMetaData;
	}
	/**
	 * @param drugMetaData The drugMetaData to set.
	 */
	public static void setDrugMetaData(LinkedHashMap drugMetaData) {
		MetaData.drugMetaData = drugMetaData;
	}
	/**
	 * @return Returns the drugSynonymMetaData.
	 */
	public static LinkedHashMap getDrugSynonymMetaData() {
		return drugSynonymMetaData;
	}
	/**
	 * @param drugSynonymMetaData The drugSynonymMetaData to set.
	 */
	public static void setDrugSynonymMetaData(LinkedHashMap drugSynonymMetaData) {
		MetaData.drugSynonymMetaData = drugSynonymMetaData;
	}
	/**
	 * @return Returns the drugSynonymXDrugMetaData.
	 */
	public static LinkedHashMap getDrugSynonymXDrugMetaData() {
		return drugSynonymXDrugMetaData;
	}
	/**
	 * @param drugSynonymXDrugMetaData The drugSynonymXDrugMetaData to set.
	 */
	public static void setDrugSynonymXDrugMetaData(
			LinkedHashMap drugSynonymXDrugMetaData) {
		MetaData.drugSynonymXDrugMetaData = drugSynonymXDrugMetaData;
	}
	/**
	 * @return Returns the topicMetaData.
	 */
	public static LinkedHashMap getTopicMetaData() {
		return topicMetaData;
	}
	/**
	 * @param topicMetaData The topicMetaData to set.
	 */
	public static void setTopicMetaData(LinkedHashMap topicMetaData) {
		MetaData.topicMetaData = topicMetaData;
	}
	/**
	 * @return Returns the topicSynonymMetaData.
	 */
	public static LinkedHashMap getTopicSynonymMetaData() {
		return topicSynonymMetaData;
	}
	/**
	 * @param topicSynonymMetaData The topicSynonymMetaData to set.
	 */
	public static void setTopicSynonymMetaData(LinkedHashMap topicSynonymMetaData) {
		MetaData.topicSynonymMetaData = topicSynonymMetaData;
	}
	/**
	 * @return Returns the topicSynonymXTopicMetaData.
	 */
	public static LinkedHashMap getTopicSynonymXTopicMetaData() {
		return topicSynonymXTopicMetaData;
	}
	/**
	 * @param topicSynonymXTopicMetaData The topicSynonymXTopicMetaData to set.
	 */
	public static void setTopicSynonymXTopicMetaData(
			LinkedHashMap topicSynonymXTopicMetaData) {
		MetaData.topicSynonymXTopicMetaData = topicSynonymXTopicMetaData;
	}
    /**
     * @return Returns the medicalTermMetaData.
     */
    public static LinkedHashMap getMedicalTermMetaData() {
        return medicalTermMetaData;
    }
    /**
     * @param medicalTermMetaData The medicalTermMetaData to set.
     */
    public static void setMedicalTermMetaData(LinkedHashMap medicalTermMetaData) {
        MetaData.medicalTermMetaData = medicalTermMetaData;
    }
    /**
     * @return Returns the medicalTermSynonymMetaData.
     */
    
    /**
     * @param medicalTermSynonymMetaData The medicalTermSynonymMetaData to set.
     */
    
    /**
     * @return Returns the medicalTermSynonymXmedicalTermMetaData.
     */
    
    /**
     * @param medicalTermSynonymXmedicalTermMetaData The medicalTermSynonymXmedicalTermMetaData to set.
     */
    
}
