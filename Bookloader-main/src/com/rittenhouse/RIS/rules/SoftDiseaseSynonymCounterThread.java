package com.rittenhouse.RIS.rules;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class SoftDiseaseSynonymCounterThread implements Runnable {

	private LinkedHashMap diseaseMap = null;
	private String content = null;
	private LinkedHashMap softFoundDiseaseSynList = null;

	public SoftDiseaseSynonymCounterThread(LinkedHashMap diseaseMap, String content, LinkedHashMap softFoundDiseaseSynList) {
		this.diseaseMap = diseaseMap;
		this.content = content;
		this.softFoundDiseaseSynList = softFoundDiseaseSynList;
	}

	public void run() {
		Iterator diseaseIt = diseaseMap.keySet().iterator();
		Integer diseaseId = null;
		String diseaseName = null;

		while (diseaseIt.hasNext()) {
			diseaseId = (Integer) diseaseIt.next();
			diseaseName = (String) diseaseMap.get(diseaseId);
			int diseaseIndex = content.indexOf(diseaseName.toLowerCase());
			if (diseaseIndex > -1) {
				softFoundDiseaseSynList.put(diseaseId, diseaseName);
			}
		}
	}
}