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

public class DrugSynonymLinkThread implements Runnable {

	// logger
	protected Category log = Category.getInstance(DrugSynonymLinkThread.class.getName());

	private boolean active = false;
	private LinkedHashMap drugSynList = null;
	private Node paraNode = null;
	private Document doc = null;
	private Lock documentLock = new ReentrantLock();

	/**
	 * @param drugList
	 * @param doc
	 * @param paraNode
	 */
	public DrugSynonymLinkThread(LinkedHashMap drugSynList, Document doc, Node paraNode) {
		this.drugSynList = drugSynList;
		this.paraNode = paraNode;
		this.doc = doc;
	}

	/**
	 * Runnable implemenation method
	 */
	public void run() {
		Integer drugId = null;
		Integer drugSynonymId = null;
		String drugSynonymName = null;
		String currPara = null;
		NodeList nodeList = null;
		Node currNode = null;
		String tempPara = null;
		String tempDrugName = null;
		boolean linkAdded = true;
		if (drugSynList != null) {
			if (drugSynList.keySet() != null) {
				while (linkAdded) {
					linkAdded = false;
					Iterator drugIt = drugSynList.keySet().iterator();
					while (drugIt.hasNext()) {
						linkAdded = false;
						drugSynonymId = (Integer) drugIt.next();
						drugId = (Integer) MetaData.getDrugSynonymXDrugMetaData().get(drugSynonymId);
						drugSynonymName = (String) MetaData.getDrugSynonymMetaData().get(drugSynonymId);
						try {
							nodeList = paraNode.getChildNodes();
							if (nodeList != null) {
								try {
									int len = nodeList.getLength();
									for (int i = 0; i < len; i++) {
										currNode = (Node) nodeList.item(i);
										if (currNode != null) {
											if (XMLUtil.isTextNode(currNode) && !currNode.hasChildNodes() && !currNode.getNodeName().equalsIgnoreCase("ulink")) {
												currPara = currNode.getNodeValue();
												if (currPara != null && (!currPara.equals("") || !currPara.equals("\r") || currPara.trim().length() > 0)) {
													tempPara = currPara;
													tempDrugName = drugSynonymName;

													int drugIndex = StringUtil.findWordIndex(tempPara, tempDrugName);
													if (drugIndex > -1 && drugSynonymName != null && currNode != null && doc != null && documentLock != null) {
														log.info("Linking drug synonym = " + drugSynonymName);
														XMLUtil.insertLink(drugSynonymName, currNode, drugIndex, drugSynonymName, "/search/search_results_index.aspx?searchterm=", drugSynonymName,
																"drugsynonym", log, doc, documentLock);
														linkAdded = true;
													}
												}
											}
										}
									}
								} catch (NullPointerException npe) {}
							}
						} catch (Exception e) {
							log.debug("Error adding link at para = " + currPara + " with drug synonym = " + drugSynonymName);
							log.error(e.toString());
						}
					}
				}
			}
		}
	}
}