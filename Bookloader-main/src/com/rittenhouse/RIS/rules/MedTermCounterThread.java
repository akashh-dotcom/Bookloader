package com.rittenhouse.RIS.rules;

import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 * @author vbhatia
 */
public class MedTermCounterThread implements Runnable {
    private LinkedHashMap medTermMap = null;
    private String content = null;
    
    public MedTermCounterThread(LinkedHashMap medTermMap, String content){
        this.medTermMap = medTermMap;
        this.content = content;
    }

    
    public void run() {
        Iterator diseaseIt = medTermMap.keySet().iterator();
        Integer diseaseId = null;
        String diseaseName = null;
		
		while (diseaseIt.hasNext()) {
		    diseaseId = (Integer) diseaseIt.next();
			diseaseName = (String) medTermMap.get(diseaseId);
			int diseaseIndex = content.indexOf(diseaseName.toLowerCase());
		}
    }
}
