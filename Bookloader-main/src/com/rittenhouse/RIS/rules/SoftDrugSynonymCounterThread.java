package com.rittenhouse.RIS.rules;

import java.util.LinkedHashMap;
import java.util.Iterator;

import com.rittenhouse.RIS.Main;

/**
 * @author Brian Wright
 */
public class SoftDrugSynonymCounterThread implements Runnable {
    
    private LinkedHashMap diseaseMap = null;
    private LinkedHashMap softFoundDrugSynList = null;
    private String content = null;
    
    public SoftDrugSynonymCounterThread(LinkedHashMap diseaseMap, String content, LinkedHashMap softFoundDrugSynList){
        this.diseaseMap = diseaseMap;
        this.content = content;
        this.softFoundDrugSynList = softFoundDrugSynList;
    }

    
    public void run() {
        Iterator diseaseIt = diseaseMap.keySet().iterator();
        Integer diseaseId = null;
        String diseaseName = null;
		
		while (diseaseIt.hasNext()) {
		    diseaseId = (Integer) diseaseIt.next();
			diseaseName = (String) diseaseMap.get(diseaseId);
			int diseaseIndex = content.indexOf(diseaseName.toLowerCase());
			if (diseaseIndex>-1){
				softFoundDrugSynList.put(diseaseId, diseaseName);
			}
		}
    }
}