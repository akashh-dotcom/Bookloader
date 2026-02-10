package com.rittenhouse.RIS.rules;

import java.util.Iterator;

import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rittenhouse.RIS.Main;
import com.rittenhouse.RIS.MetaData;
import com.rittenhouse.RIS.db.IndexTopicDB;
import com.rittenhouse.RIS.util.StringUtil;

/**
 * @author vbhatia
 * 
 */
public class DiseaseRISIndexThread implements Runnable {
	
	//logger
	protected Category log = Category.getInstance(DiseaseRISIndexThread.class.getName());
	
	private Document doc = null;
	private Node titleNode = null;
	private String ruleId = null;
	private String bookIsbn = null;
	private IndexTopicDB indexTopicDB = new IndexTopicDB();
	
	private Node sect1Node = null;
	private Node parentNode = null;
	private String chapterNumber = "";
	private String sectionId = "";
	private String typeValue = "disease";
	
	public DiseaseRISIndexThread(Document doc, Node titleNode, String ruleId, String bookIsbn){
		this.doc = doc;
		this.titleNode = titleNode;
		this.ruleId = ruleId;
		this.bookIsbn = bookIsbn.replaceAll("-","");
		this.parentNode = titleNode.getParentNode();
		try {
		    this.sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
			Node chapNode = XPathAPI.selectSingleNode(sect1Node, "ancestor-or-self::chapter/@id");
			if (chapNode!=null){
				this.chapterNumber = chapNode.getFirstChild().getNodeValue();
				
				Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
				if (sectIdNode!=null)
					this.sectionId = sectIdNode.getFirstChild().getNodeValue();
			}
		} catch(Exception de){
			log.error(de.toString());
		}
	}

	
	public void run() {
	    Iterator diseaseIt = Main.foundDiseaseList.keySet().iterator();
		while (diseaseIt.hasNext()) {
			Integer diseaseId = (Integer)diseaseIt.next();
			String termValue = (String)MetaData.getDiseaseMetaData().get(diseaseId);
			Iterator topicIt = MetaData.getTopicMetaData().keySet().iterator();
			String termToSearch = termValue.toLowerCase();
			String termToTag = termValue;
			boolean subTitleFound = false;
			boolean titleFound = false;
			try{
			    if (titleNode!=null && titleNode.getFirstChild()!=null){
			    String currTitle = titleNode.getFirstChild().getNodeValue();
			
			String tempTitle = currTitle.toLowerCase();
			int termIndex = StringUtil.findWordIndex(tempTitle, termToSearch);
			if (termIndex > -1) {//sect1 found disease term
				titleFound = true;
			//loop over topics
			while (topicIt.hasNext()) {
				String topicValue = (String)MetaData.getTopicMetaData().get(topicIt.next());
				String topicToSearch = topicValue.toLowerCase();
				String topicToTag = topicValue;
				try {
					int topicIndex = StringUtil.findWordIndex(tempTitle, topicToSearch);
					
							//Rule 1: Term and Topic both appear in any title tag (chapter, sect1..sect10)
							if (topicIndex > -1) {//sect1 found topic
								subTitleFound = true;
								log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
								log.debug("Found term = \"" + termToSearch + "\" in title = \"" + tempTitle + "\"");
								log.debug("Found topic = \"" + topicToSearch + "\" in title = \"" + tempTitle + "\"");
								addRISIndexToSect1(termToTag, topicToTag, typeValue, termToSearch, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX1);
								addTopicIndexDB(bookIsbn, diseaseId, sectionId, chapterNumber);
							}
							//Rule 2: Term appears in any title tag (chapter, sect1...sect10) AND Topic appears in any child title tag
								NodeList subNodeList = XPathAPI.selectNodeList(parentNode, Main.XPATH_RULE2);
								Node subNode = null;
								Node tempNode = null;
								int len = subNodeList.getLength();
								for (int j = 0; j < len; j++){
								    subNode = subNodeList.item(j);
								    if(subNode!=null){
								        tempNode = subNode.getFirstChild();
										String currSubTitle = tempNode.getNodeValue();
										if(currSubTitle!=null){
										    String tempSubTitle = currSubTitle.toLowerCase();
										    int subTitleTopicIndex = StringUtil.findWordIndex(tempSubTitle, topicToSearch);
											if(subTitleTopicIndex>-1){//sub-title topic found
												subTitleFound = true;
												log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
												log.debug("Found term = \"" + termToSearch + "\" in title = \"" + tempTitle + "\"");
												log.debug("Found topic = \"" + topicToSearch + "\" in title = \"" + tempSubTitle + "\"");
												addRISIndexToSect1(termToTag, topicToTag, typeValue, termToSearch, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX2);
												addTopicIndexDB(bookIsbn, diseaseId, sectionId, chapterNumber);
											}
										//Rule4 - term found and term also in sub-level tags
										
									}
								    }
								}
				} catch (Exception e) {
					log.error("Unable to apply tagging rule for title = " + currTitle);
					log.error(e.toString());
				}
			}//end topic while
			
			Iterator topicSynonymIt = MetaData.getTopicSynonymMetaData().keySet().iterator();
			while (topicSynonymIt.hasNext()) {
			    Integer topicSynonymId = (Integer) topicSynonymIt.next();
				Integer topicId = (Integer) MetaData.getTopicSynonymXTopicMetaData().get(topicSynonymId);
				String topicSynonymName = (String) MetaData.getTopicSynonymMetaData().get(topicSynonymId);
				String topicName = (String) MetaData.getTopicMetaData().get(topicId);
				
				String topicToSearch = topicSynonymName.toLowerCase();
				String topicToTag = topicSynonymName;
				try {
					int topicIndex = StringUtil.findWordIndex(tempTitle, topicToSearch);
							//Rule 1: Term and Topic both appear in any title tag (chapter, sect1..sect10)
							if (topicIndex > -1) {//sect1 found topic
								subTitleFound = true;
								log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
								log.debug("Found term = \"" + termToSearch + "\" in title = \"" + tempTitle + "\"");
								log.debug("Found topic = \"" + topicToSearch + "\" in title = \"" + tempTitle + "\"");
								addRISIndexToSect1(termToTag, topicToTag, typeValue, termToSearch, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX1);
								addTopicIndexDB(bookIsbn, diseaseId, sectionId, chapterNumber);
							}
								//Rule 2: Term appears in any title tag (sect1...sect10) AND Topic appears in any child title tag
								NodeList subTitleNodeList = XPathAPI.selectNodeList(this.parentNode, Main.XPATH_RULE2);
								Node subTitleNode = null;
								int len = subTitleNodeList.getLength();
								for (int j = 0; j < len; j++){
								    subTitleNode = subTitleNodeList.item(j);
								    if(subTitleNode!=null){
								    Node tempNode = subTitleNode.getFirstChild();
										String currSubTitle = tempNode.getNodeValue();
										if(currSubTitle!=null){
										String tempSubTitle = currSubTitle.toLowerCase();
										int subTitleTopicIndex = StringUtil.findWordIndex(tempSubTitle, topicToSearch);
										if(subTitleTopicIndex>-1){//sub-title topic found
											subTitleFound = true;
											log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
											log.debug("Found term = \"" + termToSearch + "\" in title = \"" + tempTitle + "\"");
											log.debug("Found topic = \"" + topicToSearch + "\" in title = \"" + tempSubTitle + "\"");
											addRISIndexToSect1(termToTag, topicToTag, typeValue, termToSearch, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX2);
											addTopicIndexDB(bookIsbn, diseaseId, sectionId, chapterNumber);
										}
									}
									}
								}
					} catch (Exception e) {
						log.error("Unable to apply tagging rule for title = " + currTitle);
						log.error(e.toString());
					}
				//}//end sect1 found disease term
			}//end topic synonym while
			//Rule 3: Term appears in any title tag AND Topic does not appear in any child - Tag Term/Topic = General
			}
			    }
		} catch (Exception e) {
			log.error("Unable to apply tagging rule for term = " + termValue + " for node = " + titleNode.getNodeValue());
			log.error(e.toString());
		}
		}
	}

	/**
	 * @param titleNode
	 * @param termToTag
	 * @param topicToTag
	 * @param typeValue
	 * @param termToSearch
	 * @param topicToSearch
	 * @param ruleId
	 */
	public void addRISIndexToSect1(String termToTag, String topicToTag, 
									String typeValue, String termToSearch, String topicToSearch, 
									String ruleId){
		Node oldsect1infoNode = null;
		try {
			oldsect1infoNode = XPathAPI.selectSingleNode(sect1Node, "sect1info");
		
			Node sect1infoNode = null;
			if (oldsect1infoNode != null) {
				sect1infoNode = oldsect1infoNode;
			} else {
				sect1infoNode = doc.createElement("sect1info");
			}
			Node risIndexNode = doc.createElement("risindex");
			sect1infoNode.appendChild(risIndexNode);
			
			Node risTermNode = doc.createElement("risterm");
			risTermNode.appendChild(doc.createTextNode(termToTag));
			risIndexNode.appendChild(risTermNode);
			
			Node risTopicNode = doc.createElement("ristopic");
			risTopicNode.appendChild(doc.createTextNode(topicToTag));
			risIndexNode.appendChild(risTopicNode);
			
			Node risTypeNode = doc.createElement("ristype");
			risTypeNode.appendChild(doc.createTextNode(typeValue));
			risIndexNode.appendChild(risTypeNode);
			
			Node risRuleNode = doc.createElement("risrule");
			risRuleNode.appendChild(doc.createTextNode(ruleId));
			risIndexNode.appendChild(risRuleNode);

			if (oldsect1infoNode != null) {
				sect1Node.replaceChild(sect1infoNode, oldsect1infoNode);
			} else {
				if (titleNode != null){
					sect1Node.insertBefore(sect1infoNode, titleNode);
				}
				else {
					sect1Node.appendChild( sect1infoNode);
				}
			}
			log.info("Applied Rule Id = " + ruleId + " disease = \"" + termToSearch + "\", Topic = \"" + topicToSearch + "\", Type = " + typeValue);
		} catch (Exception e) {
			log.error(e.toString());
		}
	}
	
	/**
	 * @param bookIsbn
	 * @param diseaseId
	 */
	void addTopicIndexDB(String bookIsbn, Integer diseaseId, String sectionId, String chapterNumber){
		indexTopicDB.insertDiseaseResource(bookIsbn, diseaseId, sectionId, chapterNumber);
	}
}