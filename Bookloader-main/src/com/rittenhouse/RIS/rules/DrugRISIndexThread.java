package com.rittenhouse.RIS.rules;

import java.util.Iterator;

import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

import com.rittenhouse.RIS.Main;
import com.rittenhouse.RIS.MetaData;
import com.rittenhouse.RIS.db.IndexTopicDB;
import com.rittenhouse.RIS.util.StringUtil;

/**
 * @author vbhatia
 * 
 */
public class DrugRISIndexThread implements Runnable {
	
	//logger
	protected Category log = Category.getInstance(DrugRISIndexThread.class.getName());
	
	private Document doc = null;
	private Node titleNode = null;
	private String ruleId = null;
	private String bookIsbn = null;
	private IndexTopicDB indexTopicDB = new IndexTopicDB();
	
	private Node sect1Node = null;
	private Node parentNode = null;
	private String chapterNumber = "";
	private String sectionId = "";
	private String typeValue = "drug";
	
	public DrugRISIndexThread(Document doc, Node titleNode, String ruleId, String bookIsbn){
		this.doc = doc;
		this.titleNode = titleNode;
		this.ruleId = ruleId;
		this.bookIsbn = bookIsbn.replaceAll("-","");
		this.parentNode = titleNode.getParentNode();
		try {
		    this.sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
			Node chapNode = XPathAPI.selectSingleNode(sect1Node, "ancestor-or-self::chapter/@id");
			if(chapNode!=null){
				this.chapterNumber = chapNode.getFirstChild().getNodeValue();
				
				Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
				if(sectIdNode!=null)
					this.sectionId = sectIdNode.getFirstChild().getNodeValue();
			}
		} catch(Exception de){
			log.error(de.toString());
		}
	}

	
	public void run() {
		Iterator drugIt = Main.foundDrugList.keySet().iterator();
		while (drugIt.hasNext()) {
			Integer drugId = (Integer)drugIt.next();
			String termValue = (String)MetaData.getDrugMetaData().get(drugId);
			Iterator topicIt = MetaData.getTopicMetaData().keySet().iterator();
			String termToSearch = termValue.toLowerCase();
			String termToTag = termValue;
			boolean subTitleFound = false;
			boolean titleFound = false;
			try{
			    if(titleNode!=null && titleNode.getFirstChild()!=null){
			String currTitle = titleNode.getFirstChild().getNodeValue();
			String tempTitle = currTitle.toLowerCase();
			int termIndex = StringUtil.findWordIndex(tempTitle, termToSearch);
			if (termIndex > -1) {//sect1 found drug term
			    titleFound = true;
			//loop over topics
			while (topicIt.hasNext()) {
				String topicValue = (String)MetaData.getTopicMetaData().get(topicIt.next());
				String topicToSearch = topicValue.toLowerCase();
				String topicToTag = topicValue;

					int topicIndex = StringUtil.findWordIndex(tempTitle, topicToSearch);
					try {
							//Rule 1: Term and Topic both appear in any title tag (chapter, sect1..sect10)
							if (topicIndex > -1) {//sect1 found topic
								subTitleFound = true;
								addRISIndexToSect1(titleNode, termToTag, topicToTag, typeValue, termToSearch, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX1);
								addTopicIndexDB(bookIsbn, drugId, sectionId, chapterNumber);
							} else {
							//Rule 2: Term appears in any title tag (chapter, sect1...sect10) AND Topic appears in any child title tag
								NodeIterator subTitleNodeIt = XPathAPI.selectNodeIterator(parentNode, Main.XPATH_RULE2);
								Node subTitleNode = null;
								while ((subTitleNode = subTitleNodeIt.nextNode()) != null) {
									Node tempNode = subTitleNode.getFirstChild();
									if(tempNode!=null){
										String currSubTitle = tempNode.getNodeValue();
										String tempSubTitle = currSubTitle.toLowerCase();
										int subTitleTopicIndex = StringUtil.findWordIndex(tempSubTitle, topicToSearch);
										if(subTitleTopicIndex>-1){//sub-title topic found
											subTitleFound = true;
											addRISIndexToSect1(titleNode, termToTag, topicToTag, typeValue, termToSearch, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX2);
											addTopicIndexDB(bookIsbn, drugId, sectionId, chapterNumber);
										}
									}
								}
							}
					} catch (Exception de) {
						log.error("Unable to apply tagging rule for title = " + currTitle);
						log.error(de.toString());
					}
				}
			
			Iterator topicSynonymIt = MetaData.getTopicSynonymMetaData().keySet().iterator();
			while (topicSynonymIt.hasNext()) {
				Integer topicSynonymId = (Integer) topicSynonymIt.next();
				Integer topicId = (Integer) MetaData.getTopicSynonymXTopicMetaData().get(topicSynonymId);
				String topicSynonymName = (String) MetaData.getTopicSynonymMetaData().get(topicSynonymId);
				String topicName = (String) MetaData.getTopicMetaData().get(topicId);
				
				String topicToSearch = topicSynonymName.toLowerCase();
				String topicToTag = topicSynonymName;
				
					int topicIndex = StringUtil.findWordIndex(tempTitle, topicToSearch);
					try {
							//Rule 1: Term and Topic both appear in any title tag (chapter, sect1..sect10)
							if (topicIndex > -1) {//sect1 found topic
								subTitleFound = true;
								addRISIndexToSect1(titleNode, termToTag, topicToTag, typeValue, termToSearch, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX1);
								addTopicIndexDB(bookIsbn, drugId, sectionId, chapterNumber);
							} else {
								//Rule 2: Term appears in any title tag (chapter, sect1...sect10) AND Topic appears in any child title tag
								NodeIterator subTitleNodeIt = XPathAPI.selectNodeIterator(parentNode, Main.XPATH_RULE2);
								Node subTitleNode = null;
								while ((subTitleNode = subTitleNodeIt.nextNode()) != null) {
									Node tempNode = subTitleNode.getFirstChild();
									if(tempNode!=null){
										String currSubTitle = tempNode.getNodeValue();
										String tempSubTitle = currSubTitle.toLowerCase();
										int subTitleTopicIndex = StringUtil.findWordIndex(tempSubTitle, topicToSearch);
										if(subTitleTopicIndex>-1){//sub-title topic found
											subTitleFound = true;
											addRISIndexToSect1(titleNode, termToTag, topicToTag, typeValue, termToSearch, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX2);
											addTopicIndexDB(bookIsbn, drugId, sectionId, chapterNumber);
										}
									}
								}
							}
					} catch (Exception de) {
						log.error("Unable to apply tagging rule for title = " + currTitle);
						log.error(de.toString());
					}
			}//end topic synonym while
			//Rule 3: Term appears in any title tag AND Topic does not appear in any child - Tag Term/Topic = General
			if(!subTitleFound && titleFound){
				addRISIndexToSect1(titleNode, termToTag, "General", typeValue, termToSearch, "general", Main.XML_TAG_RULE_TYPE_ADDRISINDEX3);
				addTopicIndexDB(bookIsbn, drugId, sectionId, chapterNumber);
			}
			    }
			    }
		} catch (Exception e) {
			log.error("Unable to apply tagging rule for term = " + termToTag + " for node = " + titleNode.getNodeValue());
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
	public void addRISIndexToSect1(Node titleNode, String termToTag, String topicToTag, 
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
			
			
			log.info("Applied Rule Id = " + ruleId + " Drug = \"" + termToSearch + "\", Topic = \"" + topicToSearch + "\", Type = " + typeValue);
		} catch (Exception e) {
			log.error(e.toString());
		}
	}
	
	/**
	 * @param bookIsbn
	 * @param drugId
	 */
	void addTopicIndexDB(String bookIsbn, Integer drugId, String sectionId, String chapterNumber){
		indexTopicDB.insertDrugResource(bookIsbn, drugId, sectionId, chapterNumber);
	}
}