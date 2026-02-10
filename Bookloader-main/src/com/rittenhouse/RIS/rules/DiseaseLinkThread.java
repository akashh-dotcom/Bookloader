package com.rittenhouse.RIS.rules;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rittenhouse.RIS.MetaData;
import com.rittenhouse.RIS.util.StringUtil;
import com.rittenhouse.RIS.util.XMLUtil;

public class DiseaseLinkThread implements Runnable {

	// logger
	protected Category log = Category.getInstance(DiseaseLinkThread.class.getName());

	private boolean active = false;
	private LinkedHashMap diseaseList = null;
	private Node paraNode = null;
	private Document doc = null;
	private Lock documentLock = new ReentrantLock();

	/**
	 * @param diseaseList
	 * @param doc
	 * @param paraNode
	 */
	public DiseaseLinkThread(LinkedHashMap diseaseList, Document doc, Node paraNode) {
		this.diseaseList = diseaseList;
		this.paraNode = paraNode;
		this.doc = doc;
	}

	public void run() {
		Integer diseaseId = null;
		String diseaseName = null;
		String currPara = null;
		NodeList nodeList = null;
		Node currNode = null;
		String tempPara = null;
		String tempDiseaseName = null;
		boolean linkAdded = true;
		if (diseaseList != null) {
			if (diseaseList.keySet() != null) {
				while (linkAdded) {
					linkAdded = false;
					Iterator diseaseIt = diseaseList.keySet().iterator();
					while (diseaseIt.hasNext()) {
						diseaseId = (Integer) diseaseIt.next();
						diseaseName = (String) MetaData.getDiseaseMetaData().get(diseaseId);
						try {
							nodeList = paraNode.getChildNodes();
							if (nodeList != null) {
								try {
									int len = nodeList.getLength();
									for (int i = 0; i < len; i++) {
										currNode = (Node) nodeList.item(i);
										if (currNode != null) {
											if (XMLUtil.isTextNode(currNode) && (currNode.getParentNode() != null && !currNode.getParentNode().getNodeName().equals("ulink"))) {
												currPara = currNode.getNodeValue();
												if (currPara != null) {
													tempPara = currPara;
													tempDiseaseName = diseaseName;
													int diseaseIndex = StringUtil.findWordIndex(tempPara, tempDiseaseName);
													if (diseaseIndex > -1 && diseaseName != null && currNode != null && doc != null && documentLock != null) {
														log.info("Adding link for disease = " + diseaseName);
														XMLUtil.insertLink(diseaseName, currNode, diseaseIndex, diseaseName, "link.aspx?id=", diseaseId.toString(), "disease", log, doc, documentLock);
														linkAdded = true;
													}
												}
											}
										}
									}
								} catch (NullPointerException npe) {}
							}
						} catch (Exception e) {
							log.error(e.toString());
							log.error("Error adding link to para " + currPara + " and disease name = " + diseaseName);
						}
					}
				}
			}
		}
	}
}