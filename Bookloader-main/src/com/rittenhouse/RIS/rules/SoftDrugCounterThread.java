package com.rittenhouse.RIS.rules;

import java.util.LinkedHashMap;
import java.util.Iterator;

import com.rittenhouse.RIS.Main;

/**
 * @author Brian Wright
 */
public class SoftDrugCounterThread implements Runnable {
    
    private LinkedHashMap diseaseMap = null;
    private String content = null;
    private LinkedHashMap softFoundDrugList = null;
    
    public SoftDrugCounterThread(LinkedHashMap diseaseMap, String content, LinkedHashMap softFoundDrugList){
        this.diseaseMap = diseaseMap;
        this.content = content;
        this.softFoundDrugList = softFoundDrugList;
    }

    
    public void run() {
        Iterator diseaseIt = diseaseMap.keySet().iterator();
        Integer diseaseId = null;
        String diseaseName = null;
		
		while (diseaseIt.hasNext()) {
		    diseaseId = (Integer) diseaseIt.next();
			diseaseName = (String) diseaseMap.get(diseaseId);
			int diseaseIndex = content.indexOf(diseaseName.toLowerCase());
			if(diseaseIndex>-1){
			    softFoundDrugList.put(diseaseId, diseaseName);
			}
		}
    }
}