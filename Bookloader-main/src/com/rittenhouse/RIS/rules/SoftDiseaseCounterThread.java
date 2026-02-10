package com.rittenhouse.RIS.rules;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class SoftDiseaseCounterThread implements Runnable {

	private LinkedHashMap diseaseMap = null;
	private String content = null;
	private LinkedHashMap softFoundDiseaseList = null;

	public SoftDiseaseCounterThread(LinkedHashMap diseaseMap, String content, LinkedHashMap softFoundDiseaseList) {
		this.diseaseMap = diseaseMap;
		this.content = content;
		this.softFoundDiseaseList = softFoundDiseaseList;
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
				softFoundDiseaseList.put(diseaseId, diseaseName);
			}
		}
	}
}