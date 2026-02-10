package com.rittenhouse.RIS.test;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.rittenhouse.RIS.util.RISParserException;
import com.rittenhouse.RIS.util.XMLUtil;

/**
 * @author VBhatia
 */
public class EntrezUtilsTest extends TestCase {

    
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    public void testSearch() {
        String title = "Immunol Lett";
        String volumeNum = "84";
        String pageNum = "179";
        String auth1 = null;
        String auth2 = null;
        
        String url = "http://eutils.ncbi.nlm.nih.gov";
	    String term = title + "[ta]+AND+" + volumeNum + "[vi]+AND+" + pageNum + "[pg]";
        
        if (auth1!=null)
            term = term + "+AND+" + auth1 + "[au]";
        
        if (auth2!=null)
            term = term + "+AND+" + auth2 +"[au]";
        
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
        
        
        HttpClient client = new HttpClient();
        client.setConnectionTimeout(5000);
        GetMethod method = null;

        method = new GetMethod(url);
        method.setPath("/entrez/eutils/esearch.fcgi");
        try {
            method.setQueryString(URIUtil.encodeQuery("?db=pubmed&term=Immunol Lett[ta]+AND+84[vi]+AND+179[pg]"));
        } catch (URIException e2) {
            // Auto-generated catch block
            e2.printStackTrace();
        }
        
        
        String responseBody = null;
        try{
            int result = client.executeMethod(method);
            responseBody = method.getResponseBodyAsString();
            method.releaseConnection();
            if (result!=HttpStatus.SC_OK) {
                fail("Invalid response = " + responseBody);
            }
            Document resDoc = XMLUtil.getDocument(responseBody, false);
            Node countNode = XPathAPI.selectSingleNode(resDoc, "eSearchResult/Count");
            String strCount = countNode.getTextContent();
            int count = Integer.parseInt(strCount);
            if (count == 1) {
                Node idNode = XPathAPI.selectSingleNode(resDoc, "eSearchResult/IdList/Id");
                if (idNode!=null) {
                    String id = idNode.getTextContent();
                    System.out.println("Found id = " + id);
                    assertNotNull("Found valid id", id);
                }
            } else {
                fail("No PMID found");
            }
        } catch (HttpException he) {
            System.out.println("Http error connecting to URL = '" + url + "'");
            System.out.println(he.getMessage());
        } catch (IOException ioe) {
            System.out.println("Unable to connect to URL = '" + url + term  + "'");
        } catch (RISParserException e) {
            System.out.println("Unable to parse output response for URL = '" + url + term  + "'");
        }catch (TransformerException e1) {
            System.out.println("Unable to parse output response for URL = '" + url + term  + "'");
        }
    }

}
