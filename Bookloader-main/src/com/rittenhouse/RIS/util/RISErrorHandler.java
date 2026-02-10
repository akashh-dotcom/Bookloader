package com.rittenhouse.RIS.util;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class RISErrorHandler implements ErrorHandler
{
    //  logger
	protected Category logger = Category.getInstance(RISErrorHandler.class.getName());

	public void error(SAXParseException e) throws SAXException {
        log(Level.ERROR, "Error", e);
        throw new SAXException(e);
    }

    public void fatalError(SAXParseException e) throws SAXException {
        log(Level.FATAL, "Fatal Error", e);
        throw new SAXException(e);
    }

    public void warning(SAXParseException e) {
        log(Level.WARN, "Warning", e);
    }
    
    private void log(Level level, String message, SAXParseException e) {
        int line = e.getLineNumber();
        int col = e.getColumnNumber();
        String publicId = e.getPublicId();
        String systemId = e.getSystemId();

        message = message + ": " + e.getMessage() + ": line="
            + line + ", col=" + col + ", PUBLIC="
            + publicId + ", SYSTEM=" + systemId;

        logger.log(level, message);
    }
}
