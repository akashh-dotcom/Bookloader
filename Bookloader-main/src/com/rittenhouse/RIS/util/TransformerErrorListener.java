package com.rittenhouse.RIS.util;

import java.util.*;
import javax.xml.transform.*;


/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class TransformerErrorListener implements javax.xml.transform.ErrorListener{

  List exceptionList;
  String xml;
  public TransformerErrorListener(String xml) {
       exceptionList = new ArrayList();
       this.xml = xml;
  }

   public void error(TransformerException te) throws RISTransformerException{
    exceptionList.add(te);
    throw new RISTransformerException(te, xml);
  }
  public void fatalError(TransformerException te) throws TransformerException{
    exceptionList.add(te);
    throw new RISTransformerException(te, xml);
  }
  public void warning(TransformerException te) throws TransformerException{
    String warningString = "XSLT Warning Line=" + te.getLocationAsString() + ":" + te.getMessage();
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    String newLine = System.getProperty("line.separator");
    String tab = "\t";
    buf.append("[XML]" + newLine );
    buf.append(xml + newLine );
    ListIterator iterator = getErrors();
    while(iterator.hasNext()) {
        TransformerException te = (TransformerException)iterator.next();
        buf.append(tab + "[LINE_NUMBER=");
        buf.append(te.getLocationAsString());
        buf.append("]");
        buf.append(newLine);
        buf.append(tab + "[ERROR]"+te.getMessage());
        buf.append(newLine);
    }
    return buf.toString();
  }

  protected boolean errorsEncountered() {
    return !exceptionList.isEmpty();
  }
  protected ListIterator getErrors() {
    return exceptionList.listIterator();
  }
}