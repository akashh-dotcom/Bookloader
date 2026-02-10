package com.rittenhouse.RIS.rules;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rittenhouse.RIS.MetaData;
import com.rittenhouse.RIS.util.StringUtil;
import com.rittenhouse.RIS.util.XMLUtil;

public class DiseaseSynonymLinkThread implements Runnable {

	// logger
	protected Category log = Category.getInstance(DiseaseSynonymLinkThread.class.getName());

	private boolean active = false;
	private LinkedHashMap diseaseSynList = null;
	private Node paraNode = null;
	private Document doc = null;
	private Lock documentLock = new ReentrantLock();

	/**
	 * @param diseaseSynList
	 * @param doc
	 * @param paraNode
	 */
	public DiseaseSynonymLinkThread(LinkedHashMap diseaseSynList, Document doc, Node paraNode) {
		this.diseaseSynList = diseaseSynList;
		this.paraNode = paraNode;
		this.doc = doc;
	}

	/**
	 * Runnable implemenation method
	 */
	public void run() {
		Integer diseaseId = null;
		Integer diseaseSynonymId = null;
		String diseaseSynonymName = null;
		String currPara = null;
		NodeList nodeList = null;
		Node currNode = null;
		String tempPara = null;
		String tempDiseaseName = null;
		boolean linkAdded = true;
		if (diseaseSynList != null) {
			if (diseaseSynList.keySet() != null) {
				while (linkAdded) {
					linkAdded = false;
					Iterator diseaseIt = diseaseSynList.keySet().iterator();
					while (diseaseIt.hasNext()) {
						diseaseSynonymId = (Integer) diseaseIt.next();
						diseaseId = (Integer) MetaData.getDiseaseSynonymXDiseaseMetaData().get(diseaseSynonymId);
						diseaseSynonymName = (String) MetaData.getDiseaseSynonymMetaData().get(diseaseSynonymId);
						try {
							nodeList = paraNode.getChildNodes();
							if (nodeList != null) {
								try {
									int len = nodeList.getLength();
									for (int i = 0; i < len; i++) {
										if (nodeList.item(i) != null) {
											currNode = (Node) nodeList.item(i);
											if (currNode != null) {
												if (XMLUtil.isTextNode(currNode)) {
													currPara = currNode.getNodeValue();
													if (currPara != null) {
														tempPara = currPara;
														tempDiseaseName = diseaseSynonymName;
														int diseaseIndex = StringUtil.findWordIndex(tempPara, tempDiseaseName);
														if (diseaseIndex > -1 && diseaseSynonymName != null && currNode != null && doc != null && documentLock != null) {
															log.info("Adding link for disease synonym = " + diseaseSynonymName);
															XMLUtil.insertLink(diseaseSynonymName, currNode, diseaseIndex, diseaseSynonymName, "link.aspx?id=", diseaseId.toString(), "disease", log,
																	doc, documentLock);
															linkAdded = true;
														}
													}
												}
											}
										}
									}
								} catch (NullPointerException npe) {}
							}
						} catch (Exception e) {
							log.error(e.toString());
							log.error("Error adding link at para = " + currPara + " with disease synonym = " + diseaseSynonymName);
						}
					}
				}
			}
		}
	}
}