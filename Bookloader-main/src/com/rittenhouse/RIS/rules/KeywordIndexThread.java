package com.rittenhouse.RIS.rules;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

import com.rittenhouse.RIS.Main;
import com.rittenhouse.RIS.db.KeywordDB;
import com.rittenhouse.RIS.util.StringUtil;
import com.rittenhouse.RIS.util.XMLUtil;

/**
 * @author tkeenan
 */
public class KeywordIndexThread implements Runnable {

	//logger
	protected Category log = Category.getInstance(KeywordIndexThread.class
			.getName());

	private Document doc = null;

	private Node chapNode = null;

	private String bookIsbn = null;

	private String chapterNumber = "";

	private LinkedList KeywordList = null;
	private LinkedList KeywordIDList = null;

	private KeywordDB keywordDB = new KeywordDB();

	private Lock documentLock = new ReentrantLock();

	public KeywordIndexThread(Document doc, Node chapNode, String bookIsbn) {
		this.doc = doc;
		this.chapNode = chapNode;
		this.bookIsbn = bookIsbn.replaceAll("-", "");
		Node chapIdNode = chapNode.getAttributes().getNamedItem("id");
		if (chapIdNode != null)
			this.chapterNumber = chapIdNode.getFirstChild().getNodeValue();
	}

	public void run() {
		log.debug("Started processing chapter = " + chapterNumber);

		// get the keywords from the chapter and save them to the db to give
		// them common ids
		// if there are none just return
		if ( readKeywordsFromChapter(bookIsbn, chapterNumber) == 0)
			return;

		// for each section get the text
		NodeList sect1Nodes = null;
		try {
			sect1Nodes = XPathAPI.selectNodeList(chapNode, ".//sect1");
		} catch (TransformerException e3) {
			log.error(e3.toString());
			e3.printStackTrace();
		}

		int sect1Len = sect1Nodes.getLength();

		for (int i = 0; i < sect1Len; i++) {
			Node sect1Node = sect1Nodes.item(i);
			NodeList titleList = null;
			NodeList sect1TitleList = null;
			Node sect1TitleNode = null;
			
			try {
				titleList = XPathAPI.selectNodeList(chapNode, ".//title");
			} catch (TransformerException e3) {
				log.error(e3.toString());
				e3.printStackTrace();
			}

			if (titleList.getLength() > 0) {
				sect1TitleNode = titleList.item(0);
			}


			if (sect1Node != null && sect1Node.getFirstChild() != null) {
				// need to get the text value

				String tempTest = sect1Node.getTextContent(); // this needs to
				// read the text
				// from the node

				// Read keywords back from the DB.
				getKeywordList(bookIsbn, chapterNumber, sect1Node
						.getAttributes().getNamedItem("id").getTextContent());

				String keywordValueBase = null;
				Integer keywordIdBase = Integer.valueOf(-1);
				Integer lastMatchedkeywordIdBase = Integer.valueOf(-1);
				for (int j=0, n=KeywordList.size(); j < n; j++)
				 {//loop over topics
					Integer keywordId = (Integer) KeywordIDList.get( j);
					String keywordValue = (String) KeywordList.get( j);
					String keywordSearch = keywordValue.toLowerCase();
					
					if ( keywordIdBase != keywordId) {
						keywordValueBase  = keywordValue;
						keywordIdBase = keywordId;						
					}					

					int found = 0;
					found = -1;
					found = addKeywordLinks(keywordValue, keywordValueBase, sect1Node);
					// if not found look in title list
					if (found  == -1) {
						// title list with emphases etc.
						try {
							sect1TitleList = XPathAPI.selectNodeList(sect1Node, ".//title | .//title/emphasis ");
						} catch (TransformerException e3) {
							log.error(e3.toString());
							e3.printStackTrace();
						}
						for (int tl =0 ;  tl < sect1TitleList.getLength() && (found == -1); tl++){
							found = StringUtil.findWordIndex(sect1TitleList.item(tl).getNodeValue() ,keywordValue);							
						}
						
					}

					try {
						if (found > -1) {							
							if (lastMatchedkeywordIdBase != keywordIdBase){
								String parentTagName = sect1Node.getNodeName();
								Node sectIdNode = sect1Node.getAttributes()
										.getNamedItem("id");
								String sectionId = "";
								lastMatchedkeywordIdBase = keywordIdBase;
								
								if (sectIdNode != null) {
									sectionId = sectIdNode.getNodeValue();
								}
								
								addKeywordIndexToSect1(sect1Node, sect1TitleNode,
										keywordValueBase, "keyword", "keyword",
										keywordSearch, "keyword",
										Main.XML_TAG_RULE_TYPE_LINKKEYWORD,
										sectionId);

								addKeywordIndexDB(bookIsbn, keywordValueBase ,
										sectionId, chapterNumber);

							}
						}

					} catch (Exception e) {

						log.error(e.toString());
						e.printStackTrace();
					}
					keywordSearch = "";
				}

			}//loop over title nodes
		}
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
	public void addKeywordIndexToSect1(Node sect1Node, Node titleNode,
			String termToTag, String topicToTag, String typeValue,
			String termToSearch, String topicToSearch, String ruleId,
			String sectionId) {
		Node oldsect1infoNode = null;
		if (sect1Node != null) {
			try {
				documentLock.lock();
				oldsect1infoNode = XPathAPI.selectSingleNode(sect1Node,
						"sect1info");
				String dupxpath = "KeywordIndex[risterm='" + termToTag
						+ "' and ristopic='" + topicToTag + "']";

				Node sect1infoNode = null;
				if (oldsect1infoNode != null) {
					sect1infoNode = oldsect1infoNode;
					Node dupNode = XPathAPI.selectSingleNode(oldsect1infoNode,
							dupxpath);
					if (dupNode != null && dupNode.getFirstChild() != null
							&& dupNode.getFirstChild().getFirstChild() != null) {
						return;
					}
				} else {
					sect1infoNode = doc.createElement("sect1info");
				}

				// set up the index Node
				Node risIndexNode = doc.createElement("risindex");

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

				// add index node to the sectinfo node
				sect1infoNode.appendChild(risIndexNode);

				// Add sectinfo back into the section
				if (oldsect1infoNode != null) {
					sect1Node.replaceChild(sect1infoNode, oldsect1infoNode);
				} else {
					Node sectFirstNode = sect1Node.getFirstChild();
					if (sectFirstNode != null) {
						sect1Node.insertBefore(sect1infoNode, sectFirstNode);
					} else {
						sect1Node.appendChild(sect1infoNode);
					}
				}
				log.debug("Applied Rule Id = " + ruleId + " disease = \""
						+ termToSearch + "\", Topic = \"" + topicToSearch
						+ "\", Type = " + typeValue);
			} catch (Exception e) {
				log.error(e.toString());
				e.printStackTrace();
			} finally {
				documentLock.unlock();
			}
		}
	}

	int readKeywordsFromChapter(String bookIsbn, String chapterId) {
		NodeList keywordNodes = null;
		KeywordDB keyDB = new KeywordDB();
		int numMatches = 0;

		try {
			keywordNodes = XPathAPI.selectNodeList(chapNode,
					"ancestor-or-self::chapter/chapterinfo/keywordset/keyword");
		} catch (TransformerException e3) {
			log.error(e3.toString());
			e3.printStackTrace();
		}
		// save the words to the DB to set the key values
		int keywordLegth = keywordNodes.getLength();
		for (int i = 0; i < keywordLegth; i++) {
			Node keywordNode = keywordNodes.item(i);
			if (keywordNode != null) {
				keyDB.insertKeywordLocation(bookIsbn, keywordNode
						.getTextContent(), KeywordDB.CHAPTER_LEVEL, chapterId,
						null);
				numMatches++;
			}
		}
		// also look up section items
		try {
			keywordNodes = XPathAPI.selectNodeList(chapNode,
					"descendant-or-self::sect1/sect1info/keywordset/keyword");
		} catch (TransformerException e3) {
			log.error(e3.toString());
			e3.printStackTrace();
		}
		// save the words to the DB to set the key values
		keywordLegth = keywordNodes.getLength();
		String vchSectionName = "";
		for (int i = 0; i < keywordLegth; i++) {
			Node keywordNode = keywordNodes.item(i);
			vchSectionName = keywordNode.getParentNode().getParentNode()
					.getParentNode().getAttributes().getNamedItem("id")
					.getTextContent();
			if (keywordNode != null) {
				keyDB.insertKeywordLocation(bookIsbn, keywordNode
						.getTextContent(), KeywordDB.SECTION_LEVEL, chapterId,
						vchSectionName);
				numMatches++;
			}
		}
		return numMatches;

	}


	void getKeywordList(String bookIsbn, String chapterNumber,
			String vchSectionNumber) {
		KeywordDB keyDB = new KeywordDB();
		ResultSet rs = keyDB.getKeywordLocation(bookIsbn, chapterNumber,
				vchSectionNumber);
		KeywordList = null;
		KeywordIDList = null;

		KeywordList = new LinkedList();
		KeywordIDList = new LinkedList();

		Connection conn = null;
		Statement stmt = null;

		try {
			Integer keywordKey = null;
			String keywordValue = null;
			while (rs.next()) {
				keywordKey = rs.getInt("iKeywordId");
				keywordValue = rs.getString("vchKeywordDesc").trim();
				if (keywordKey != null) {
					// also add plural forms
					// need to also check s,es and ess
					String keywordValue_wS = keywordValue + "s"; 
					String keywordValue_wES = keywordValue + "es"; 
					String keywordValue_wESS = keywordValue + "ess"; 
					
					KeywordIDList.addLast(keywordKey );
					KeywordList.addLast(keywordValue.trim());

					KeywordIDList.addLast(keywordKey );
					KeywordList.addLast(keywordValue_wS.trim());
				
					KeywordIDList.addLast(keywordKey );
					KeywordList.addLast(keywordValue_wES.trim());
					
					KeywordIDList.addLast(keywordKey );
					KeywordList.addLast(keywordValue_wESS.trim());
				}
			}
			stmt= rs.getStatement();
			conn = stmt.getConnection();	

		} catch (SQLException e) {
			log.error("Failed to retrieve keywordLocation list");
			log.error(e.toString());
		}
		 finally {
			try {
				rs.close();
				stmt.close();
				conn.close();
			} catch (SQLException e) {}
		}

	}

	void addKeywordIndexDB(String bookIsbn, String keywordValue,
			String sectionId, String chapterNumber) {
		keywordDB.insertKeywordResource(bookIsbn, keywordValue, chapterNumber,
				sectionId);
	}

	int addKeywordLinks(String keyword, String keywordBase, Node sect1) {
		NodeIterator paraNodeIt = null;
		String currPara = null;
		String tempPara = null;
		String tmpKeyword = null;
		NodeList nodeList = null;
		Node currNode = null;
		Node paraNode = null;
		Node parentNode = null;
		String tempDrugName = null;

		int matches = -1;
		try {
			paraNodeIt = XPathAPI.selectNodeIterator(sect1, "descendant-or-self::para | descendant-or-self::para/emphasis ");
			paraNode = paraNodeIt.nextNode();
			while (paraNode != null) {
				nodeList = paraNode.getChildNodes();
				if (nodeList != null) {
					int sect1Len = nodeList.getLength();
					int x;
					for (x = 0; x < sect1Len;) {
						try {
							currNode = nodeList.item(x);
							x += 1;
							if (XMLUtil.isTextNode(currNode)) {
								currPara = currNode.getNodeValue();
								if (currPara != null
										&& (!currPara.equals("")
												|| !currPara.equals("\r") || currPara
												.trim().length() > 0)) {
									tempPara = currPara;
									tmpKeyword = keyword;

									int keywordIndex = StringUtil.findWordIndex(tempPara, tmpKeyword);
									if (keywordIndex > -1) {
										matches = 0;
										try {
											 XMLUtil.insertLink(keyword, currNode, keywordIndex, keywordBase
											 		, "/search/search_results_index.aspx?searchterm="
													, keyword
													, "keywords"
													, log , doc , documentLock);
											
										} catch (Exception ex) {
											log.error("Inserting link");
											log.error(ex.toString());
											ex.printStackTrace();
										}
									}//end found keyword
								}//tempkeywordName found
								else {

									currPara = currNode.getNodeValue();
									}
							}//paraWords loop
						} catch (Exception e) {
							log.error("node list walking: " + e.toString() + " trace is " + e.getStackTrace().toString());
							
							e.printStackTrace();
						}						
					}
				}
				
				paraNode = paraNodeIt.nextNode();
			}// while

		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return matches;
	}

}