package com.rittenhouse.RIS;

import java.util.LinkedHashMap;

import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import com.rittenhouse.RIS.rules.DiseaseLinkThread;
import com.rittenhouse.RIS.rules.DiseaseSynonymLinkThread;

public class SoftLinkDiseaseThread implements Runnable {
	private Document doc = null;
	private LinkedHashMap softFoundDiseaseList = null;
	private LinkedHashMap softFoundDiseaseSynList = null;
	private MetaData metaData = null;
	protected Category log = Category.getInstance(SoftLinkDiseaseThread.class.getName());
	public static final String XPATH_SOFT_LINKING = "//para | //para/emphasis";
	private int linkingThreadCountMax = 2;

	public SoftLinkDiseaseThread(Document doc, LinkedHashMap softFoundDiseaseList, LinkedHashMap softFoundDiseaseSynList, MetaData metaData, int linkingThreadCountMax) {
		this.doc = doc;
		this.softFoundDiseaseList = softFoundDiseaseList;
		this.softFoundDiseaseSynList = softFoundDiseaseSynList;
		this.metaData = metaData;
		this.linkingThreadCountMax = linkingThreadCountMax;
	}

	public void run() {
		NodeIterator paraNodeIt = null;
		LinkedHashMap diseaseMetaData = metaData.getDiseaseMetaData();
		LinkedHashMap diseaseSynXMetaData = metaData.getDiseaseSynonymXDiseaseMetaData();
		LinkedHashMap diseaseSynMetaData = metaData.getDiseaseSynonymMetaData();

		try {
			paraNodeIt = XPathAPI.selectNodeIterator(doc, XPATH_SOFT_LINKING);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), linkingThreadCountMax);
		threadPool.setKeepAliveTime(100);
		threadPool.createThreads(linkingThreadCountMax);
		int count = 0;
		if (paraNodeIt != null) {
			Node paraNode = null;
			while ((paraNode = paraNodeIt.nextNode()) != null) {
				count++;
				try {
					DiseaseLinkThread aThread = new DiseaseLinkThread(softFoundDiseaseList, doc, paraNode);
					threadPool.execute(aThread);

				} catch (InterruptedException e1) {
				}
			}
		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(1000);
				}
			} catch (Exception e1) {
			}
		}
		threadPool = null;
		paraNodeIt = null;

		try {
			paraNodeIt = XPathAPI.selectNodeIterator(doc, XPATH_SOFT_LINKING);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		threadPool = new PooledExecutor(new LinkedQueue(), linkingThreadCountMax);
		threadPool.setKeepAliveTime(100);
		threadPool.createThreads(linkingThreadCountMax);
		count = 0;
		if (paraNodeIt != null) {
			Node paraNode = null;
			while ((paraNode = paraNodeIt.nextNode()) != null) {
				count++;
				try {

					DiseaseSynonymLinkThread bThread = new DiseaseSynonymLinkThread(softFoundDiseaseSynList, doc, paraNode);
					threadPool.execute(bThread);

				} catch (InterruptedException e1) {
				}
			}
		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(100);
				}
			} catch (Exception e1) {
			}
		}
	}
}
