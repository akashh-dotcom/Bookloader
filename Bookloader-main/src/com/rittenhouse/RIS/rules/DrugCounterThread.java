package com.rittenhouse.RIS.rules;

import java.util.LinkedHashMap;
import java.util.Iterator;

import com.rittenhouse.RIS.Main;

/**
 * @author VBhatia
 */
public class DrugCounterThread implements Runnable {
    
    private LinkedHashMap diseaseMap = null;
    private String content = null;
    
    public DrugCounterThread(LinkedHashMap diseaseMap, String content) {
        this.diseaseMap = diseaseMap;
        this.content = content;
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
			    Main.foundDrugList.put(diseaseId, diseaseName);
			}
		}
    }
}