package com.rittenhouse.RIS.util;

import javax.xml.transform.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */
public class RISTransformerException  extends TransformerException{

    private String excptionString;
    private String xml;
    private TransformerConfigurationException transformerConfigException;
    private TransformerException transformerException;
    public RISTransformerException(String excptionString) {
        super(excptionString);
        this.excptionString = excptionString;
    }

    public RISTransformerException(TransformerException transformerException, String xml) {
        super("");
        this.transformerException = transformerException;
        this.xml = xml;
    }

    public RISTransformerException(TransformerConfigurationException transformerConfigException, String xml) {
        super("");
        this.transformerConfigException = transformerConfigException;
        this.xml = xml;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        String newLine = System.getProperty("line.separator");
        String tab = "\t";
        buf.append("XSLT ERRORS:" + newLine );
        if (excptionString != null) {
            buf.append(excptionString  + newLine );
        }
        if (xml != null) {
            buf.append("[XML]" + newLine );
            buf.append(xml + newLine );
        }
        if (transformerConfigException != null) {
            buf.append(tab + "[LINE_NUMBER=");
            buf.append(transformerConfigException.getLocationAsString());
            buf.append("]");
            buf.append(newLine);
            buf.append(tab + "[ERROR]"+transformerConfigException.getMessage());
            buf.append(newLine);
        }
        if (transformerException != null) {
            buf.append(tab + "[LINE_NUMBER=");
            buf.append(transformerException.getLocationAsString());
            buf.append("]");
            buf.append(newLine);
            buf.append(tab + "[ERROR]"+transformerConfigException.getMessage());
            buf.append(newLine);
        }
        return buf.toString();
  }

}