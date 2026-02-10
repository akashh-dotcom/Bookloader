
package com.rittenhouse.RIS.test;

import junit.framework.TestCase;

import com.rittenhouse.RIS.util.StringUtil;

/**
 * @author vbhatia
 *
 */
public class StringUtilTest extends TestCase {

	
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testFindWordIndex() {
	    int index = StringUtil.findWordIndex(".)", "Syndromes, Carbohydrate-Deficient Glycoprotein");
	    assertEquals(-1, index);
	    
		index = StringUtil.findWordIndex("Diagnosing Arthritis", "Diagnosing");
		assertEquals(0, index);
		
		index = StringUtil.findWordIndex("Diagnosing", "Diagnosing");
		assertEquals(0, index);
		
		index = StringUtil.findWordIndex("(Diagnosing)", "Diagnosing");
		assertEquals(1, index);
		
		index = StringUtil.findWordIndex("characteristic", "tic");
		assertEquals(-1, index);
		
		index = StringUtil.findWordIndex("asadsas", "a");
		assertEquals(-1, index);
		
		index = StringUtil.findWordIndex("PURPOSE OF THE IMMUNE SYSTEM Rule 1 Abetalipoproteinemia Diagnosis", "Abetalipoproteinemia");
		if (index==-1) {
			fail("Not found");
		}
		
		index = StringUtil.findWordIndex("PURPOSE OF THE IMMUNE SYSTEM Rule 1) Abetalipoproteinemia Diagnosis", "Diagnosis");
		if (index==-1) {
			fail("Not found");
		}
		
		index = StringUtil.findWordIndex("Diabetes insipidus (central or nephrogenic), diabetes mellitus, hemorrhage, hypotension of any cause, overuse of diuretics, dry mouth of any cause, psychogenic polydipsia.", "diabetes mellitus");
		if (index==-1) {
			fail("Not found");
		}
		//Pathophysiology:
		index = StringUtil.findWordIndex("pathophysiology:", "pathophysiology");
		assertEquals(0, index);
		
		//Larynx Syndromes
		index = StringUtil.findWordIndex("Larynx Syndromes", "x syndromes");
		assertEquals(-1, index);
		
		index = StringUtil.findWordIndex("coma", "coma");
		assertEquals(0, index);
		
		index = StringUtil.findWordIndex("coma ", "coma");
		assertEquals(0, index);
		
		index = StringUtil.findWordIndex(" coma", "coma");
		assertEquals(1, index);
		
		index = StringUtil.findWordIndex("[coma]\\", "coma");
		assertEquals(1, index);
		
		index = StringUtil.findWordIndex("test,coma", "coma");
		if (index==-1) {
			fail("Not found");
		}
	}
	
	
	
	
	
	public void testIsAllUpper() {
	    boolean isUpper = StringUtil.isAllUpperCase("AIDS");
	    assertEquals(isUpper, true);
	    
	    isUpper = StringUtil.isAllUpperCase("aids");
	    assertEquals(isUpper, false);
	    
	    isUpper = StringUtil.isAllUpperCase("Aids");
	    assertEquals(isUpper, false);
	    
	    isUpper = StringUtil.isAllUpperCase("00");
	    assertEquals(isUpper, false);
	    
	    isUpper = StringUtil.isAllUpperCase("");
	    assertEquals(isUpper, false);
	    
	    isUpper = StringUtil.isAllUpperCase(null);
	    assertEquals(isUpper, false);
	}
	
	public void testTrim() {
	    String before = "Cystic Fibrosis";
	    String after = before.trim();
	    assertEquals(before, after);
	}
	
	public void testLink() {
	    String currPara = "as in a Cystic Fibrosis Center.";
	    String diseaseName = "Cystic Fibrosis";
	    int diseaseIndex = -1;
	    diseaseIndex = StringUtil.findWordIndex(currPara.toLowerCase(), diseaseName.toLowerCase());
	    String beforeLinkText = null;
		String contentDisease = null;
		String afterLinkText = null;
		
		beforeLinkText = currPara.substring(0, diseaseIndex);
		contentDisease = currPara.substring(diseaseIndex, diseaseIndex + diseaseName.length());
		afterLinkText = currPara.substring(diseaseIndex + diseaseName.length(), currPara.length());
		
		assertEquals(beforeLinkText, "as in a ");
		assertEquals(contentDisease, "Cystic Fibrosis");
		assertEquals(afterLinkText, " Center.");
		
		currPara = "Acetaminophen is an anti-inflammatory and analgesic that can be used alone for mild pain or adjunctively with opiates for moderate to severe pain. Acetaminophen (15 mg/kg orally [PO] or rectally [PR] every 6 hours) dosing is not age dependent. Acetaminophen may be hepatotoxic above 140 mg/kg per d.";
		
		diseaseName = "Acetaminophen";
	    diseaseIndex = -1;
	    diseaseIndex = StringUtil.findWordIndex(currPara.toLowerCase(), diseaseName.toLowerCase());
		if (diseaseIndex>-1) {
		    beforeLinkText = currPara.substring(0, diseaseIndex);
		    contentDisease = currPara.substring(diseaseIndex, diseaseIndex + diseaseName.length());
		    afterLinkText = currPara.substring(diseaseIndex + diseaseName.length(), currPara.length());
		}
		
		currPara = "Large families are preferable perhaps because the presence of many children increases the risk of infection";
		diseaseName = "Infection";
	    diseaseIndex = -1;
	    diseaseIndex = StringUtil.findWordIndex(currPara.toLowerCase(), diseaseName.toLowerCase());
		if (diseaseIndex>-1){
		    beforeLinkText = currPara.substring(0, diseaseIndex);
		    contentDisease = currPara.substring(diseaseIndex, diseaseIndex + diseaseName.length());
		    afterLinkText = currPara.substring(diseaseIndex + diseaseName.length(), currPara.length());
		}

		currPara = "chains that closely resemble the heavy and light chains of the immunoglobulin molecules and belong to a family of molecules that are similar to immunoglobulins. It is divided into HLA-DP, HLA-DQ, and HLA-DR regions. These surface structures are important in the interaction of immune active cells with each other. Class II molecules present exogenous antigens, such as bacterial products, to CD4+ cells stimulating an immune response.";
		diseaseName = "Dysphonia";
		diseaseIndex = StringUtil.findWordIndex(currPara.toLowerCase(), diseaseName.toLowerCase());
		if (diseaseIndex>-1){
		    beforeLinkText = currPara.substring(0, diseaseIndex);
		    contentDisease = currPara.substring(diseaseIndex, diseaseIndex + diseaseName.length());
		    afterLinkText = currPara.substring(diseaseIndex + diseaseName.length(), currPara.length());
		}
		
		currPara = "The volume status and then the measured and calculated osmolarities are evaluated. True hyponatremia presents with a reduced osmolarity. Factitious hyponatremia" + 
		  "presents with a normal to high osmolarity. The most common cause is dilutional. It may be brought on by trauma, sepsis, cardiac failure, cirrhosis, or renal failure. Hyponatremia also may be factitious (false measurement of the serum sodium) due to hyperglycemia,elevated protein, or hy-perlipidemia. Extracellular fluid or volume status and urine sodium level can classify true hyponatremia (low osmolarity). The syndrome of inappropriate antidiuretic hormone is a diagnosis made by exclusion. Causes of hypona-tremia are listed in <link linkend=\"ch0004ta0001\">Table 4-1</link>."; 
		diseaseName = "hyponatremia";
		diseaseIndex = StringUtil.findWordIndex(currPara.toLowerCase(), diseaseName.toLowerCase());
		if (diseaseIndex>-1){
		    beforeLinkText = currPara.substring(0, diseaseIndex);
		    contentDisease = currPara.substring(diseaseIndex, diseaseIndex + diseaseName.length());
		    afterLinkText = currPara.substring(diseaseIndex + diseaseName.length(), currPara.length());
		}
		
		currPara = "Relative contraindications to percutaneous bladder catheterization include patients who have a history of pelvic cancer or pelvic radiation therapy, ascites, urinary tract infections, or who are uncooperative.";
		diseaseName = "infections";
		diseaseIndex = StringUtil.findWordIndex(currPara.toLowerCase(), diseaseName.toLowerCase());
		if (diseaseIndex>-1){
		    beforeLinkText = currPara.substring(0, diseaseIndex);
		    contentDisease = currPara.substring(diseaseIndex, diseaseIndex + diseaseName.length());
		    afterLinkText = currPara.substring(diseaseIndex + diseaseName.length(), currPara.length());
		}
		
		currPara = "infection";
		diseaseName = "infection";
		diseaseIndex = StringUtil.findWordIndex(currPara.toLowerCase(), diseaseName.toLowerCase());
		if (diseaseIndex>-1){
		    beforeLinkText = currPara.substring(0, diseaseIndex);
		    contentDisease = currPara.substring(diseaseIndex, diseaseIndex + diseaseName.length());
		    afterLinkText = currPara.substring(diseaseIndex + diseaseName.length(), currPara.length());
		}
		
		currPara = " Aggressive management is often necessary for secondary derangements, including respiratory insufficiency, acute renal failure, lactic acidosis, disseminated intravascular coagulation, hypoglycemia, and hypocalcemia.";
		diseaseName = "disseminated intravascular coagulation";
		diseaseIndex = StringUtil.findWordIndex(currPara.toLowerCase(), diseaseName.toLowerCase());
		if (diseaseIndex>-1){
		    beforeLinkText = currPara.substring(0, diseaseIndex);
		    contentDisease = currPara.substring(diseaseIndex, diseaseIndex + diseaseName.length());
		    afterLinkText = currPara.substring(diseaseIndex + diseaseName.length(), currPara.length());
		}
		
		currPara = "Treatment objectives for painful crises consist of thorough evaluation for etiology of the crisis and treatment of discomfort. Pain management must be individualized, with previous effective regimens as a guide.";
		diseaseName = "pain";
		diseaseIndex = StringUtil.findWordIndex(currPara.toLowerCase(), diseaseName.toLowerCase());
		if (diseaseIndex>-1){
		    beforeLinkText = currPara.substring(0, diseaseIndex);
		    contentDisease = currPara.substring(diseaseIndex, diseaseIndex + diseaseName.length());
		    afterLinkText = currPara.substring(diseaseIndex + diseaseName.length(), currPara.length());
		}
	}

}
