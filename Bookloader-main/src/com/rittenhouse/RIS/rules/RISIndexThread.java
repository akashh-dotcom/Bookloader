package com.rittenhouse.RIS.rules;

import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.transform.TransformerException;

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
 */
public class RISIndexThread implements Runnable  {
    
    //logger
	protected Category log = Category.getInstance(RISIndexThread.class.getName());
	
	private Document doc = null;
	private Node chapNode = null;
	private String bookIsbn = null;
	private String chapterNumber = "";
	private IndexTopicDB indexTopicDB = new IndexTopicDB();
	
	private Lock documentLock = new ReentrantLock();
	
	public RISIndexThread(Document doc, Node chapNode, String bookIsbn){
	    this.doc = doc;
	    this.chapNode = chapNode;
		this.bookIsbn = bookIsbn.replaceAll("-","");
		Node chapIdNode = chapNode.getAttributes().getNamedItem("id");
		if (chapIdNode!=null)
			this.chapterNumber = chapIdNode.getFirstChild().getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
	}
	
	public void run() {
	    log.debug("Started processing chapter = " + chapterNumber);
	    
	        NodeList titleNodes = null;
            try {
                titleNodes = XPathAPI.selectNodeList(chapNode, Main.XPATH_TITLE);
            } catch (TransformerException e3) {
                log.error(e3.toString());
                e3.printStackTrace();
            }
            int titleLen = titleNodes.getLength();
            if ( Main.foundDiseaseList == null)
            {
            	return;
            }
            for(int i=0;i<titleLen;i++){
                Node titleNode = titleNodes.item(i);
                if (titleNode!=null && titleNode.getFirstChild()!=null){
    			    String currTitle = titleNode.getFirstChild().getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
    			    String tempTitle = currTitle.toLowerCase();
    			    if(tempTitle!=null && tempTitle.trim().length()>0){
    				    String termToSearch = null;
    				    if (Main.isDrugMongraph) {
    				    	log.info("Searching for monograph index drugs");
    						Iterator DrugIt = MetaData.getDrugMetaData().keySet().iterator();
    						searchDrugs(DrugIt,currTitle,titleNode,false);
    						
    						DrugIt = MetaData.getDrugSynonymMetaData().keySet().iterator();
    						searchDrugs(DrugIt,currTitle,titleNode,true);
    				    	
    				    }

												
    				    Iterator diseaseIt = Main.foundDiseaseList.keySet().iterator();
    					while (diseaseIt.hasNext()) {//loop over diseases
    					    Integer diseaseId = (Integer)diseaseIt.next();
    					    String termValue = (String)MetaData.getDiseaseMetaData().get(diseaseId);
    					    int termIndex = -1;
    					    if(!StringUtil.isAllUpperCase(termValue)){
    					        termToSearch = termValue.toLowerCase();
    					        termIndex = StringUtil.findWordIndex(tempTitle, termToSearch);
    					    } else {
    					        termToSearch = termValue;
    					        termIndex = StringUtil.findWordIndex(currTitle, termToSearch);
    					    }
    						if (termIndex > -1) {//found disease
    						    boolean subTitleFound = false;
    						    Iterator topicIt = MetaData.getTopicMetaData().keySet().iterator();
    							while (topicIt.hasNext()) {//loop over topics
    							    try{
    							        Integer topicId = (Integer)topicIt.next();
	    							    String topicValue = (String)MetaData.getTopicMetaData().get(topicId);
	    							    String topicToSearch = topicValue.toLowerCase();
	    								int topicIndex = StringUtil.findWordIndex(tempTitle, topicToSearch);
	    								if (topicIndex > -1) {
	    								    //Rule 1
	    								    Node sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
	    							        Node realTitleNode = XPathAPI.selectSingleNode(titleNode,"ancestor-or-self::title");
	    							        Node parentNode = null;
	    							        if(realTitleNode == null){
	    							            parentNode = titleNode.getParentNode();
	    							        } else {
	    							            parentNode = realTitleNode.getParentNode();
	    							        }
	    							        Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
	    							        String sectionId = "";
	    							        if(sect1Node!=null && sectIdNode!=null){
	    							            sectionId = sectIdNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
		    							        log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
		    									log.debug("Found term = \"" + termValue + "\" in title = \"" + currTitle + "\"");
		    									log.debug("Found topic = \"" + topicValue + "\" in title = \"" + currTitle + "\"");
		    									addRISIndexToSect1(sect1Node, titleNode, termValue, topicValue, "disease", termValue, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX1, sectionId);
		    									addTopicIndexDB(bookIsbn, diseaseId, sectionId, chapterNumber);
	    							        }
	    								}
    							    } catch (Exception e) {
    						            log.error(e.toString());
    						            e.printStackTrace();
    						        }
    							}
    							
    							Iterator topicSynIt = MetaData.getTopicSynonymMetaData().keySet().iterator();
    							while (topicSynIt.hasNext()) {//loop over topic synonyms
    							    try{
	    							    Integer topicSynId = (Integer)topicSynIt.next();
	    							    String topicSynValue = (String)MetaData.getTopicSynonymMetaData().get(topicSynId);
	    							    String topicSynToSearch = topicSynValue.toLowerCase();
	    								int topicIndex = StringUtil.findWordIndex(tempTitle, topicSynToSearch);
	    								if (topicIndex > -1) {
	    								    //Rule 1
	    								    subTitleFound = true;
	    								    Node sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
	    							        Node realTitleNode = XPathAPI.selectSingleNode(titleNode,"ancestor-or-self::title");
	    							        Node parentNode = null; 
	    							        if(realTitleNode == null){
	    							            parentNode = titleNode.getParentNode();
	    							        } else {
	    							            parentNode = realTitleNode.getParentNode();
	    							        }
	    							        Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
	    							        String sectionId = "";
	    							        if(sect1Node != null && sectIdNode!=null){
	    							            sectionId = sectIdNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
		    							        log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
		    									log.debug("Found term = \"" + termValue + "\" in title = \"" + currTitle + "\"");
		    									log.debug("Found topic synonym = \"" + topicSynValue + "\" in title = \"" + currTitle + "\"");
		    									addRISIndexToSect1(sect1Node, titleNode, termValue, topicSynValue, "disease", termValue, topicSynToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX1, sectionId);
		    									addTopicIndexDB(bookIsbn, diseaseId, sectionId, chapterNumber);
	    							        }
	    								}
    							    } catch (Exception e) {
    						            log.error(e.toString());
    						            e.printStackTrace();
    						        }
    							}
    							
//    							Rule 2
    					        Node realTitleNode = null;
                                try {
                                    realTitleNode = XPathAPI.selectSingleNode(titleNode,"ancestor-or-self::title");
	                                Node parentNode = null;
	                                if(realTitleNode == null){
	    					            parentNode = titleNode.getParentNode();
	    					        } else {
	    					            parentNode = realTitleNode.getParentNode();
	    					        }
	    					        NodeList subNodeList = null;
                                    subNodeList = XPathAPI.selectNodeList(parentNode, Main.XPATH_RULE2);
                                    int len = subNodeList.getLength();
    							for (int j = 0; j < len; j++){//loop over subtitle nodes
    							    try{
    							        Node subNode = subNodeList.item(j);
    							    if(subNode!=null){
    							        Node tempNode = subNode.getFirstChild();
    							        String currSubTitle = null;
    							        if(tempNode!=null)
    							            currSubTitle = tempNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
    									if(currSubTitle!=null && currSubTitle.trim().length()>0){
    									    
    									    String tempSubTitle = currSubTitle.toLowerCase();
    									    topicIt = MetaData.getTopicMetaData().keySet().iterator();
    										while (topicIt.hasNext()) {//loop over topics
    										    Integer topicId = (Integer)topicIt.next();
    										    String topicValue = (String)MetaData.getTopicMetaData().get(topicId);
    										    String topicToSearch = topicValue.toLowerCase();
    											int topicIndex = StringUtil.findWordIndex(tempSubTitle, topicToSearch);
    											if (topicIndex > -1) {
    											    subTitleFound = true;
    											    realTitleNode = XPathAPI.selectSingleNode(titleNode,"ancestor-or-self::title");
    		    							        parentNode = null;
    		    							        if(realTitleNode == null){
    		    							            parentNode = titleNode.getParentNode();
    		    							        } else {
    		    							            parentNode = realTitleNode.getParentNode();
    		    							        }
    		    							        String parentTagName = parentNode.getNodeName();
    											    Node sect1Node = null;
    		    							        if(parentTagName!=null && parentTagName.equalsIgnoreCase("chapter")){
    		    							            sect1Node = XPathAPI.selectSingleNode(subNode, "ancestor-or-self::sect1");
    		    							        } else {
    		    							            sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
    		    							        }
    		    							        Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
    		    							        String sectionId = "";
    		    							        if(sectIdNode!=null){
    		    							            sectionId = sectIdNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
    		    							        }
    											    subTitleFound = true;
        											log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
        											log.debug("Found term = \"" + termValue + "\" in title = \"" + currTitle + "\"");
        											log.debug("Found topic = \"" + topicValue + "\" in title = \"" + currSubTitle + "\"");
        											addRISIndexToSect1(sect1Node, titleNode, termValue, topicValue, "disease", termValue, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX2, sectionId);
        											addTopicIndexDB(bookIsbn, diseaseId, sectionId, chapterNumber);
        											}
    											}
    										
    										topicSynIt = MetaData.getTopicSynonymMetaData().keySet().iterator();
    										while (topicSynIt.hasNext()) {//loop over topic synonyms
    										    Integer topicSynId = (Integer)topicSynIt.next();
    										    String topicSynValue = (String)MetaData.getTopicSynonymMetaData().get(topicSynId);
    										    String topicSynToSearch = topicSynValue.toLowerCase();
    											int topicIndex = StringUtil.findWordIndex(tempSubTitle, topicSynToSearch);
    											if (topicIndex > -1) {
    											    subTitleFound = true;
    											    realTitleNode = XPathAPI.selectSingleNode(titleNode,"ancestor-or-self::title");
    		    							        parentNode = null;
    		    							        if(realTitleNode == null){
    		    							            parentNode = titleNode.getParentNode();
    		    							        } else {
    		    							            parentNode = realTitleNode.getParentNode();
    		    							        }
    		    							        String parentTagName = parentNode.getNodeName();
    											    Node sect1Node = null;
    		    							        if(parentTagName!=null && parentTagName.equalsIgnoreCase("chapter")){
    		    							            sect1Node = XPathAPI.selectSingleNode(subNode, "ancestor-or-self::sect1");
    		    							        } else {
    		    							            sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
    		    							        }
    		    							        
    		    							        Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
    		    							        String sectionId = "";
    		    							        if(sectIdNode!=null){
    		    							            sectionId = sectIdNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
    		    							        }
    											    subTitleFound = true;
        											log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
        											log.debug("Found term = \"" + termValue + "\" in title = \"" + currTitle + "\"");
        											log.debug("Found topic synonym = \"" + topicSynValue + "\" in title = \"" + currSubTitle + "\"");
        											addRISIndexToSect1(sect1Node, titleNode, termValue, topicSynValue, "disease", termValue, topicSynToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX2, sectionId);
        											addTopicIndexDB(bookIsbn, diseaseId, sectionId, chapterNumber);
    											}
    										}
    									}
    							    }
    							} catch (Exception e) {
    					            log.error(e.toString());
    					            e.printStackTrace();
    					        }
    							}//subtitle loop
                                } catch (TransformerException e1) {
                                    log.error(e1.toString());
                                    e1.printStackTrace();
                                }
    						}
    					}//end loop over diseases
    					
    					Iterator diseaseSynIt = Main.foundDiseaseSynList.keySet().iterator();
    					while (diseaseSynIt.hasNext()) {//loop over disease synonyms
    					    Integer diseaseSynId = (Integer)diseaseSynIt.next();
    					    String termSynValue = (String)MetaData.getDiseaseSynonymMetaData().get(diseaseSynId);
    					    String termSynToSearch = termSynValue;
    					    int termSynIndex = StringUtil.findWordIndex(currTitle, termSynToSearch);
    					    if(!StringUtil.isAllUpperCase(termSynValue)){
    					        termSynToSearch = termSynValue.toLowerCase();
    					        termSynIndex = StringUtil.findWordIndex(tempTitle, termSynToSearch);
    					    }
    						if (termSynIndex > -1) {
    						    boolean subTitleFound = false;
    						    Iterator topicIt = MetaData.getTopicMetaData().keySet().iterator();
    							while (topicIt.hasNext()) {//loop over topics
    							    Integer topicId = (Integer)topicIt.next();
    							    String topicValue = (String)MetaData.getTopicMetaData().get(topicId);
    							    String topicToSearch = topicValue.toLowerCase();
    								int topicIndex = StringUtil.findWordIndex(tempTitle, topicToSearch);
    								if (topicIndex > -1) {
    								    //Rule 1
    								    subTitleFound = true;
    								    try {
                                            Node sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
    							            Node realTitleNode = XPathAPI.selectSingleNode(titleNode,"ancestor-or-self::title");
    							            
    							            Node parentNode = realTitleNode.getParentNode();
    							            
	                                        if(realTitleNode == null){
	    							            parentNode = titleNode.getParentNode();
	    							        }
	                                        
	    							        Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
	    							        String sectionId = "";
	    							        if(sectIdNode!=null && sect1Node != null){
	    							            sectionId = sectIdNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
		    							        log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
		    									log.debug("Found term synonym = \"" + termSynValue + "\" in title = \"" + currTitle + "\"");
		    									log.debug("Found topic = \"" + topicValue + "\" in title = \"" + currTitle + "\"");
		    									addRISIndexToSect1(sect1Node, titleNode, termSynValue, topicValue, "disease", termSynToSearch, topicValue, Main.XML_TAG_RULE_TYPE_ADDRISINDEX1, sectionId);
		    									addTopicSynonymIndexDB(bookIsbn, diseaseSynId, sectionId, chapterNumber);
	    							        }
    								    } catch (TransformerException e1) {
                                            log.error(e1.toString());
                                            e1.printStackTrace();
                                        }
    								}
    							}
    							
    							Iterator topicSynIt = MetaData.getTopicSynonymMetaData().keySet().iterator();
    							while (topicSynIt.hasNext()) {//loop over topic synonyms
    							    try{
	    							    Integer topicSynId = (Integer)topicSynIt.next();
	    							    String topicSynValue = (String)MetaData.getTopicSynonymMetaData().get(topicSynId);
	    							    String topicSynToSearch = topicSynValue.toLowerCase();
	    								int topicIndex = StringUtil.findWordIndex(tempTitle, topicSynToSearch);
	    								if (topicIndex > -1) {
	    								    //Rule 1
	    								    subTitleFound = true;
	    								    Node sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
	    							        Node realTitleNode = XPathAPI.selectSingleNode(titleNode,"ancestor-or-self::title");
	    							        Node parentNode = realTitleNode.getParentNode();
	    							        if(realTitleNode == null){
	    							            parentNode = titleNode.getParentNode();
	    							        }
	    							        
	    							        Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
	    							        String sectionId = "";
	    							        if(sectIdNode!=null && sect1Node!=null){
	    							            sectionId = sectIdNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
		    							        log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
		    									log.debug("Found term synonym = \"" + termSynValue + "\" in title = \"" + currTitle + "\"");
		    									log.debug("Found topic synonym = \"" + topicSynValue + "\" in title = \"" + currTitle + "\"");
		    									addRISIndexToSect1(sect1Node, titleNode, termSynValue, topicSynValue, "disease", termSynToSearch, topicSynValue, Main.XML_TAG_RULE_TYPE_ADDRISINDEX1, sectionId);
		    									addTopicSynonymIndexDB(bookIsbn, diseaseSynId, sectionId, chapterNumber);
	    							        }
	    								}
    							    } catch (Exception e) {
    						            log.error(e.toString());
    						            e.printStackTrace();
    						        }
    							}
    						
    						
//							Rule 2
                            try {
                                Node realTitleNode = XPathAPI.selectSingleNode(titleNode,"ancestor-or-self::title");
	                            Node parentNode = realTitleNode.getParentNode();
	                            if(realTitleNode == null){
						            parentNode = titleNode.getParentNode();
						        }
						        NodeList subNodeList = null;
                                subNodeList = XPathAPI.selectNodeList(parentNode, Main.XPATH_RULE2);
                            int len = subNodeList.getLength();
							for (int j = 0; j < len; j++){//loop over sub titles
							    Node subNode = subNodeList.item(j);
							    if(subNode!=null){
							        Node tempNode = subNode.getFirstChild();
							        String currSubTitle = null;
							        if(tempNode!=null)
							            currSubTitle = tempNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
									if(currSubTitle!=null && currSubTitle.trim().length()>0){
									    String tempSubTitle = currSubTitle.toLowerCase();
									    topicIt = MetaData.getTopicMetaData().keySet().iterator();
									    
										while (topicIt.hasNext()) {//loop over topics
										    Integer topicId = (Integer)topicIt.next();
										    String topicValue = (String)MetaData.getTopicMetaData().get(topicId);
										    String topicToSearch = topicValue.toLowerCase();
											int topicIndex = StringUtil.findWordIndex(tempSubTitle, topicToSearch);
											if (topicIndex > -1) {
											    subTitleFound = true;
											    if(realTitleNode == null){
		    							            parentNode = titleNode.getParentNode();
		    							        }
											    String parentTagName = parentNode.getNodeName();
											    Node sect1Node = null;
		    							        if(parentTagName!=null && parentTagName.equalsIgnoreCase("chapter")){
		    							            sect1Node = XPathAPI.selectSingleNode(subNode, "ancestor-or-self::sect1");
		    							        } else {
		    							            sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
		    							        }
		    							        Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
		    							        String sectionId = "";
		    							        if(sectIdNode!=null){
		    							            sectionId = sectIdNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
		    							        }
    											log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
    											log.debug("Found term synonym = \"" + termSynValue + "\" in title = \"" + currTitle + "\"");
    											log.debug("Found topic = \"" + topicValue + "\" in title = \"" + currSubTitle + "\"");
    											addRISIndexToSect1(sect1Node, titleNode, termSynValue, topicValue, "disease", termSynToSearch, topicToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX2, sectionId);
    											addTopicSynonymIndexDB(bookIsbn, diseaseSynId, sectionId, chapterNumber);
    											}
											}
										
										
										topicSynIt = MetaData.getTopicSynonymMetaData().keySet().iterator();
										
										while (topicSynIt.hasNext()) {//loop over topic synonyms
										    Integer topicSynId = (Integer)topicSynIt.next();
										    String topicSynValue = (String)MetaData.getTopicSynonymMetaData().get(topicSynId);
										    String topicSynToSearch = topicSynValue.toLowerCase();
											int topicIndex = StringUtil.findWordIndex(tempSubTitle, topicSynToSearch);
											if (topicIndex > -1) {
											    subTitleFound = true;
											    String parentTagName = parentNode.getNodeName();
											    Node sect1Node = null;
		    							        if(parentTagName!=null && parentTagName.equalsIgnoreCase("chapter")){
		    							            sect1Node = XPathAPI.selectSingleNode(subNode, "ancestor-or-self::sect1");
		    							        } else {
		    							            sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");
		    							        }
		    							        if(realTitleNode == null){
		    							            parentNode = titleNode.getParentNode();
		    							        }
		    							        
		    							        Node sectIdNode = parentNode.getAttributes().getNamedItem("id");
		    							        String sectionId = "";
		    							        if(sectIdNode!=null){
		    							            sectionId = sectIdNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
		    							        }
    											log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
    											log.debug("Found term synonym = \"" + termSynValue + "\" in title = \"" + currTitle + "\"");
    											log.debug("Found topic synonym = \"" + topicSynValue + "\" in title = \"" + currSubTitle + "\"");
    											addRISIndexToSect1(sect1Node, titleNode, termSynValue, topicSynValue, "disease", termSynToSearch, topicSynToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX2, sectionId);
    											addTopicSynonymIndexDB(bookIsbn, diseaseSynId, sectionId, chapterNumber);
											}
											
											
										}//while
									}// if
							    }//while
							}//subtitle loop
                            } catch (TransformerException e1) {
                                log.error(e1.toString());
                                e1.printStackTrace();
                            }
    					}
    					}//loop over disease synonyms
    			    }
                }
            }//loop over title nodes
            
	}
	

	/**
	 * 
	 * @param sect1Node
	 * @param titleNode
	 * @param termToTag
	 * @param topicToTag
	 * @param typeValue
	 * @param termToSearch
	 * @param topicToSearch
	 * @param ruleId
	 * @param sectionId
	 */
	public void addRISIndexToSect1(Node sect1Node, Node titleNode, String termToTag, String topicToTag, 
									String typeValue, String termToSearch, String topicToSearch, 
									String ruleId, String sectionId){
		Node oldsect1infoNode = null;
		if(sect1Node!=null){
			try {
			    documentLock.lock();
				oldsect1infoNode = XPathAPI.selectSingleNode(sect1Node, "sect1info");
				String dupxpath = "risindex[risterm='" + termToTag + "' and ristopic='" + topicToTag + "']";
				
				
					Node sect1infoNode = null;
					if (oldsect1infoNode != null) {
						sect1infoNode = oldsect1infoNode;
						Node dupNode = XPathAPI.selectSingleNode(oldsect1infoNode, dupxpath);
						if(dupNode!=null && dupNode.getFirstChild()!=null && dupNode.getFirstChild().getFirstChild()!=null){
						    return;
						}
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
					
					Node risposIdNode = doc.createElement("risposid");
					risposIdNode.appendChild(doc.createTextNode(sectionId));
					risIndexNode.appendChild(risposIdNode);
		
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
				log.debug("Applied Rule Id = " + ruleId + " disease = \"" + termToSearch + "\", Topic = \"" + topicToSearch + "\", Type = " + typeValue);
			} catch (Exception e) {
				log.error(e.toString());
				e.printStackTrace();
			}finally{
			    documentLock.unlock();
			}
		}
	}
	void searchDrugs(Iterator DrugIt, String currTitle, Node titleNode , boolean drugSyn){
		String DrugValue ;
		String DrugToSearch ;
	    String tempTitle ;

		while (DrugIt.hasNext()) {//loop over topic drugs
		    Integer DrugId = (Integer)DrugIt.next();
		    if(!drugSyn)
		    	DrugValue = (String)MetaData.getDrugMetaData().get(DrugId);
		    else
		    	DrugValue = (String)MetaData.getDrugSynonymMetaData().get(DrugId);
		    
		    DrugToSearch = DrugValue.toLowerCase();
		    tempTitle = currTitle.toLowerCase();
		    
			boolean drugFound = false;
		    int drugIndex = -1;
		    if(!StringUtil.isAllUpperCase(DrugValue)){
		    	DrugToSearch = DrugValue.toLowerCase();
		    	drugIndex = StringUtil.findWordIndex(tempTitle, DrugToSearch);
		    } else {
		    	DrugToSearch = DrugValue;
		    	drugIndex = StringUtil.findWordIndex(currTitle, DrugToSearch);
		    }
			if (drugIndex > -1) {
				drugFound  = true;
			    String parentTagName = titleNode.getNodeName();
			    Node sect1Node = null;
			    int iReturn = 0;
			    try{							    
			    	sect1Node = XPathAPI.selectSingleNode(titleNode, "ancestor-or-self::sect1");							        
			    } catch (Exception e) {
		            log.error(e.toString());
		            e.printStackTrace();
		        }
			        
		        
		        Node sectIdNode = sect1Node.getAttributes().getNamedItem("id");
		        String sectionId = "";
		        if(sectIdNode!=null){
		            sectionId = sectIdNode.getNodeValue().replaceAll("    "," ").replaceAll("  "," ").replaceAll("  "," ");
		        }
				log.debug("Working on chapter = " + chapterNumber + ", section = " + sectionId);
				log.debug("Found drug = \"" + DrugValue + "\" in title = \"" + tempTitle + "\"");
				// Add the drug value first the return should be a 1
				if (!drugSyn)
					iReturn = addDrugIndexDB(bookIsbn, DrugValue, DrugId, sectionId, chapterNumber, Main.bookTitle + " : " + titleNode );
				else
					iReturn = addSynonymDrugIndexDB(bookIsbn, DrugValue, DrugId, sectionId, chapterNumber, Main.bookTitle + " : " + titleNode );
				if (iReturn == 1)
					addRISIndexToSect1(sect1Node, titleNode,  DrugValue,"Drug Monograph", "Drug", DrugToSearch, DrugToSearch, Main.XML_TAG_RULE_TYPE_ADDRISINDEX2, sectionId);
			}
		}//while

	}
	
	
	void addTopicIndexDB(String bookIsbn, Integer diseaseId, String sectionId, String chapterNumber){
		indexTopicDB.insertDiseaseResource(bookIsbn, diseaseId, sectionId, chapterNumber);
	}
	
	void addTopicSynonymIndexDB(String bookIsbn, Integer diseaseSynonymId, String sectionId, String chapterNumber){
		indexTopicDB.insertDiseaseSynonymResource(bookIsbn, diseaseSynonymId, sectionId, chapterNumber);
	}
	int addDrugIndexDB(String bookIsbn, String DrugName, Integer DrugId, String sectionId, String chapterNumber, String displayTitle){
		return indexTopicDB.insertDrugResource(bookIsbn, DrugName ,DrugId, sectionId, chapterNumber, displayTitle);
	}
	int  addSynonymDrugIndexDB(String bookIsbn, String DrugSynonymName, Integer DrugId, String sectionId, String chapterNumber, String displayTitle){
		return indexTopicDB.insertDrugSynonymResource(bookIsbn, DrugSynonymName, DrugId, sectionId, chapterNumber, displayTitle);
	}
}
