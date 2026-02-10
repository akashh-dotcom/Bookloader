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

public class DrugLinkThread implements Runnable {

	// logger
	protected Category log = Category.getInstance(DrugLinkThread.class.getName());

	private boolean active = false;
	private LinkedHashMap drugList = null;
	private Node paraNode = null;
	private Document doc = null;
	private Lock documentLock = new ReentrantLock();

	/**
	 * @param drugList
	 * @param doc
	 * @param paraNode
	 */
	public DrugLinkThread(LinkedHashMap drugList, Document doc, Node paraNode) {
		this.drugList = drugList;
		this.paraNode = paraNode;
		this.doc = doc;
	}

	public void run() {

		Integer drugId = null;
		String drugName = null;
		String currPara = null;
		NodeList nodeList = null;
		Node currNode = null;
		String tempPara = null;
		String tempDrugName = null;
		boolean linkAdded = true;
		if (drugList != null) {
			if (drugList.keySet() != null) {
				while (linkAdded) {
					linkAdded = false;
					Iterator drugIt = drugList.keySet().iterator();
					while (drugIt.hasNext()) {
						drugId = (Integer) drugIt.next();
						drugName = (String) MetaData.getDrugMetaData().get(drugId);
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
													tempDrugName = drugName;

													int drugIndex = StringUtil.findWordIndex(tempPara, tempDrugName);
													if (drugIndex > -1 && drugName != null && currNode != null && drugId != null && doc != null && documentLock != null) {
														log.info("Adding link for drug = " + drugName);
														XMLUtil.insertLink(drugName, currNode, drugIndex, drugName, "link.aspx?id=", drugId.toString(), "drug", log, doc, documentLock);
														linkAdded = true;
													}// end found drug
												}// tempDrugName found
											}// paraWords loop
										}
									}
								} catch (NullPointerException npe) {}
							}// end check for Text node
						} catch (Exception e) {
							log.error(e.toString());
							log.debug("Error adding link at para = " + currPara + " with drug = " + drugName);
						}
					}
				}
			}
		}
	}
}