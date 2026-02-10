package com.rittenhouse.RIS.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Category;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.rittenhouse.RIS.Main;
import com.rittenhouse.RIS.db.ResourceDB;

public class XMLUtil {

	// logger
	protected static Category log = Category.getInstance(XMLUtil.class.getName());

	private XMLUtil() {
	}

	/**
	 * Non-validating returns an XML DOM object.
	 * 
	 * @param poStringInput
	 *            String XML string input
	 * @return Document XML DOM document
	 */
	public static Document getDocument(String poStringInput, boolean doValidate) throws RISParserException {
		Document retDocument = null;
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(poStringInput.getBytes());
			DocumentBuilderFactory docBuilderfactory = DocumentBuilderFactory.newInstance();
			docBuilderfactory.setValidating(doValidate);
			docBuilderfactory.setIgnoringComments(true);
			docBuilderfactory.setIgnoringElementContentWhitespace(true);
			docBuilderfactory.setExpandEntityReferences(false);

			DocumentBuilder docBuilder = null;
			docBuilder = docBuilderfactory.newDocumentBuilder();
			retDocument = docBuilder.parse(in);
			// to allow XML without DTD
			docBuilder.setErrorHandler(new RISErrorHandler());
		} catch (Exception ex) {
			throw new RISParserException(ex.getMessage());
		}
		return retDocument;
	}

	/**
	 * returns an XML DOM object.
	 * 
	 * @param poInputSource
	 *            InputSource XML string stream input
	 * @return Document XML DOM document
	 */
	public static Document getDocument(InputSource poInputSource) throws RISParserException {
		Document retDocument = null;
		try {
			DocumentBuilderFactory docBuilderfactory = DocumentBuilderFactory.newInstance();
			docBuilderfactory.setValidating(false);
			retDocument = docBuilderfactory.newDocumentBuilder().parse(poInputSource);
		} catch (Exception ex) {
			throw new RISParserException(ex.getMessage());
		}
		return retDocument;
	}

	/**
	 * returns a XML node list.
	 * 
	 * @param pDocument
	 *            Document XML DOM document
	 * @param psTagName
	 *            String XML node name
	 * @return NodeList XML node list
	 */
	public static NodeList getNodeList(Document pDocument, String psTagName) {
		return pDocument.getDocumentElement().getElementsByTagName(psTagName);
	}

	/**
	 * returns a XML element.
	 * 
	 * @param pDocument
	 *            Document XML DOM document
	 * @param psTagName
	 *            String XML node name
	 * @param index
	 *            int XML node position
	 * @return Element XML node element
	 */
	public static Element getElement(Document pDocument, String psTagName, int index) {
		NodeList rows = pDocument.getDocumentElement().getElementsByTagName(psTagName);
		return (Element) rows.item(index);
	}

	/**
	 * returns a XML node size.
	 * 
	 * @param pDocument
	 *            Document XML DOM document
	 * @param psTagName
	 *            String XML node name
	 * @return int XML node size
	 */
	public static int getSize(Document pDocument, String psTagName) {
		NodeList rows = pDocument.getDocumentElement().getElementsByTagName(psTagName);
		return rows.getLength();
	}

	/**
	 * returns a XML node value.
	 * 
	 * @param pDocument
	 *            Document XML DOM document
	 * @param psTagName
	 *            String XML node name
	 * @return String XML node value
	 */
	public static String getValue(Document pDocument, String psTagName) throws RISParserException {
		String s = null;
		try {
			NodeList elements = pDocument.getDocumentElement().getElementsByTagName(psTagName);
			Node node = elements.item(0);
			NodeList nodes = node.getChildNodes();
			// find a value whose value is non-whitespace
			for (int i = 0; i < nodes.getLength(); i++) {
				s = ((Node) nodes.item(i)).getNodeValue().trim();
				if (s.equals("") || s.equals("\r"))
					continue;
			}
		} catch (Exception ex) {
			throw new RISParserException(ex.getMessage());
		}
		return s;
	}

	/**
	 * returns a XML element value.
	 * 
	 * @param pElement
	 *            Document XML element
	 * @return String XML node value
	 */
	public static String getValue(Element pElement) throws RISParserException {
		String s = null;
		try {
			NodeList nodes = pElement.getChildNodes();
			// find a value whose value is non-whitespace
			for (int i = 0; i < nodes.getLength(); i++) {
				s = ((Node) nodes.item(i)).getNodeValue().trim();
				if (s.equals("") || s.equals("\r"))
					continue;
			}
		} catch (Exception ex) {
			throw new RISParserException(ex.getMessage());
		}
		return s;
	}

	/**
	 * returns a XML node value.
	 * 
	 * @param pNode
	 *            Document XML node
	 * @return String XML node value
	 */
	public static String getValue(Node pNode) throws RISParserException {
		String s = null;
		try {
			NodeList nodes = pNode.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				s = ((Node) nodes.item(i)).getNodeValue();
				if (s != null && (s.equals("") || s.equals("\r")))
					continue;
			}
		} catch (Exception ex) {
			throw new RISParserException(ex.getMessage());
		}
		return s;
	}

	/**
	 * Returns an iterator over the children of the given element with the given
	 * tag name.
	 * 
	 * @param element
	 *            The parent element
	 * @param tagName
	 *            The name of the desired child
	 * @return An interator of children or null if element is null.
	 */
	public static Iterator getChildrenByTagName(Element element, String tagName) {
		if (element == null)
			return null;
		// getElementsByTagName gives the corresponding elements in the whole
		// descendance. We want only children

		NodeList children = element.getChildNodes();
		ArrayList goodChildren = new ArrayList();
		for (int i = 0; i < children.getLength(); i++) {
			Node currentChild = children.item(i);
			if (currentChild.getNodeType() == Node.ELEMENT_NODE && ((Element) currentChild).getTagName().equals(tagName)) {
				goodChildren.add((Element) currentChild);
			}
		}
		return goodChildren.iterator();
	}

	/**
	 * 
	 * @param node
	 *            input node to search
	 * @param tagName
	 *            tag name
	 * @param tagValue
	 *            tag value
	 * @return Node found node or null
	 */
	public static Node getChildrenByTagValue(Node node, String tagName, String tagValue) {
		if (node == null)
			return null;
		NodeList children = node.getChildNodes();
		String foundNodeValue = null;
		Node currentChild = null;
		NamedNodeMap nnm = null;
		Node foundNode = null;
		for (int i = 0; i < children.getLength(); i++) {
			foundNodeValue = null;
			currentChild = null;
			nnm = null;
			foundNode = null;
			currentChild = children.item(i);
			nnm = currentChild.getAttributes();
			foundNode = nnm.getNamedItem(tagName);
			if (foundNode != null) {
				foundNodeValue = foundNode.getNodeValue();
			}
			if (foundNodeValue != null && foundNodeValue.equalsIgnoreCase(tagValue)) {
				return (currentChild);
			}
		}
		return null;
	}

	/**
	 * Gets the child of the specified element having the specified unique name.
	 * If there are more than one children elements with the same name and
	 * exception is thrown.
	 * 
	 * @param element
	 *            The parent element
	 * @param tagName
	 *            The name of the desired child
	 * @return The named child.
	 * 
	 * @throws DeploymentException
	 *             Child was not found or was not unique.
	 */
	public static Element getUniqueChild(Element element, String tagName) throws RISParserException {
		Iterator goodChildren = getChildrenByTagName(element, tagName);

		if (goodChildren != null && goodChildren.hasNext()) {
			Element child = (Element) goodChildren.next();
			if (goodChildren.hasNext()) {
				throw new RISParserException("expected only one " + tagName + " tag");
			}
			return child;
		} else {
			throw new RISParserException("expected one " + tagName + " tag");
		}
	}

	/**
	 * Get the content of the given element.
	 * 
	 * @param element
	 *            The element to get the content for.
	 * @return The content of the element or null.
	 */
	public static String getElementContent(final Element element) throws RISParserException {
		return getElementContent(element, null);
	}

	/**
	 * Get the content of the given element.
	 * 
	 * @param element
	 *            The element to get the content for.
	 * @param defaultStr
	 *            The default to return when there is no content.
	 * @return The content of the element or the default.
	 */
	public static String getElementContent(Element element, String defaultStr) throws RISParserException {
		if (element == null)
			return defaultStr;

		NodeList children = element.getChildNodes();

		if (children.getLength() > 0) {
			String result = "";
			for (int i = 0; i < children.getLength(); i++) {
				if (children.item(i).getNodeType() == Node.TEXT_NODE || children.item(i).getNodeType() == Node.CDATA_SECTION_NODE) {
					result += children.item(i).getNodeValue();
				} else {
					NodeList grandChildren = children.item(i).getChildNodes();
					for (int j = 0; j < grandChildren.getLength(); j++) {
						if (grandChildren.item(j).getNodeType() == Node.TEXT_NODE || grandChildren.item(j).getNodeType() == Node.CDATA_SECTION_NODE) {
							result += grandChildren.item(j).getNodeValue();
						}
					}
				}
			}
			return result.replace("\n", " ").replace("\t", " ");
		} else {
			return defaultStr;
		}
	}

	/**
	 * Get the content of the given element.
	 * 
	 * @param element
	 *            The element to get the content for.
	 * @param defaultStr
	 *            The default to return when there is no content.
	 * @return The content of the element or the default.
	 */
	public static String getNodeContent(Node node, String defaultStr) {
		if (node == null)
			return defaultStr;

		NodeList children = node.getChildNodes();

		if (children.getLength() > 0) {
			String result = "";
			for (int i = 0; i < children.getLength(); i++) {
				if (children.item(i).getNodeType() == Node.TEXT_NODE || children.item(i).getNodeType() == Node.CDATA_SECTION_NODE) {
					result += children.item(i).getNodeValue() + " ";
				} else {
					NodeList grandChildren = children.item(i).getChildNodes();
					for (int j = 0; j < grandChildren.getLength(); j++) {
						if (grandChildren.item(j).getNodeType() == Node.TEXT_NODE || grandChildren.item(j).getNodeType() == Node.CDATA_SECTION_NODE) {
							result += grandChildren.item(j).getNodeValue() + " ";
						}
					}
				}
			}
			return result.replace("\n", " ").replace("\t", " ");
		} else {
			return defaultStr;
		}
	}

	/**
	 * creates a new empty DOM document.
	 * 
	 * @return Document
	 */
	public static Document createDocument() {
		Document retDocument = null;
		try {
			DocumentBuilderFactory docBuilderfactory = DocumentBuilderFactory.newInstance();
			retDocument = docBuilderfactory.newDocumentBuilder().newDocument();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return retDocument;
	}

	/**
	 * Insert a node into the document.
	 * 
	 * @param poDocument
	 *            Source Document XML
	 * @param poNode
	 *            Node where the new node is going to be inserted into
	 * @param poNodeToInsert
	 *            Node to be inserted into
	 * @param pbFlag
	 *            Flag to indicate if poNodeToInsert is created by document
	 *            poDocument
	 * 
	 */
	public static void insertNode(Document poDocument, Node poNode, Node poNodeToInsert, boolean pbFlag) {
		Node elementNode = null;
		try {
			if (!pbFlag) {
				elementNode = poDocument.importNode(poNodeToInsert, true);
				poNode.appendChild(elementNode);
			} else {
				poNode.appendChild(poNodeToInsert);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Insert a node into the document.
	 * 
	 * @param poDocument
	 *            Source Document XML
	 * @param poNode
	 *            Node where the new node is going to be inserted into
	 * @param poNodeToInsert
	 *            Node to be inserted into
	 * 
	 */
	public static void insertNode(Document poDocument, Node poNode, Node poNodeToInsert) {
		insertNode(poDocument, poNode, poNodeToInsert, true);
	}

	/**
	 * Convert document to string for display
	 * 
	 * @param doc
	 *            org.w3c.dom.Document
	 * @return String
	 */
	public static String documentToString(Document doc) throws TransformerException {

		System.gc();

		// Create dom source for the document
		DOMSource domSource = new DOMSource(doc);

		// Create a string writer
		StringWriter stringWriter = new StringWriter();

		// Create the result stream for the transform
		StreamResult result = new StreamResult(stringWriter);

		// Create a Transformer to serialize the document
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();

		// Transform the document to the result stream
		transformer.transform(domSource, result);
		return stringWriter.toString();
	}

	/**
	 * Executes xQuery
	 * 
	 * @param queryFileName
	 * @param sourceFileName
	 * @param outputFileName
	 */
	

	/**
	 * Check if the node is text
	 * 
	 * @param Node
	 *            to check
	 * @return true for CDATA and TEXT node
	 */
	public static boolean isTextNode(Node n) {
		if (n == null)
			return false;
		short nodeType = n.getNodeType();
		return nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.TEXT_NODE;
	}

	/**
	 * Apply XSL Tranformation using Xalan
	 * 
	 * @param inFileName
	 * @param xsltFileName
	 * @param outFileName
	 * @throws RISTransformerException
	 */
	public static void transformXalan(String inFileName, String xsltFileName, String outFileName, String params[], String paramVals[]) throws RISTransformerException {
		System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
		try {
			Transformer transformer = StyleSheetCache.newTransformer(xsltFileName);
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					transformer.setParameter(params[i], paramVals[i]);
				}
			}
			transformer.transform(new StreamSource(inFileName), new StreamResult(new FileOutputStream(outFileName)));
		} catch (FileNotFoundException e1) {
			throw new RISTransformerException(e1.toString());
		} catch (TransformerException e1) {
			throw new RISTransformerException(e1.toString());
		}
	}

	public static void transformXSLTC(String inFileName, String xsltFileName, String outFileName, String params[], String paramVals[]) throws RISTransformerException {
		System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
		try {
			Transformer transformer = StyleSheetCache.newTransformer(xsltFileName);
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					transformer.setParameter(params[i], paramVals[i]);
				}
			}
			transformer.transform(new StreamSource(inFileName), new StreamResult(new File(outFileName)));
			System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
		} catch (TransformerException e1) {
			throw new RISTransformerException(e1.toString());
		}
	}

	/**
	 * Apply XSL Transformation using Saxon
	 * 
	 * @param inFileName
	 * @param xsltFileName
	 * @param outFileName
	 */
	public static void transformSaxon(String inFileName, String xsltFileName, String outFileName, String params[], String paramVals[]) throws RISTransformerException {
		log.info("Transforming, inFileName: " + inFileName + ", xsltFilename: " + xsltFileName + ", outFileName: " + outFileName);
		System.setProperty("javax.xml.transform.TransformerFactory", "com.icl.saxon.TransformerFactoryImpl");
		try {
			Transformer transformer = StyleSheetCache.newTransformer(xsltFileName);
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					transformer.setParameter(params[i], paramVals[i]);
				}
			}
			
			// Use SAXSource with EntityResolver to redirect DTD references to local files
			// This prevents attempts to fetch DTDs from http://LOCALHOST/
			try {
				SAXParserFactory saxFactory = SAXParserFactory.newInstance();
				saxFactory.setNamespaceAware(true);
				saxFactory.setValidating(false);
				SAXParser saxParser = saxFactory.newSAXParser();
				XMLReader xmlReader = saxParser.getXMLReader();
				
				// Set up entity resolver to redirect DTD/entity references to local dtd folder
				xmlReader.setEntityResolver(new EntityResolver() {
					public InputSource resolveEntity(String publicID, String systemID) throws SAXException {
						if (systemID != null && (systemID.endsWith(".dtd") || systemID.endsWith(".mod") || systemID.endsWith(".ent") || systemID.endsWith(".dec"))) {
							// Extract DTD filename
							String dtdFileName = systemID;
							if (dtdFileName.contains("/")) {
								dtdFileName = dtdFileName.substring(dtdFileName.lastIndexOf("/") + 1);
							}
							if (dtdFileName.contains("\\")) {
								dtdFileName = dtdFileName.substring(dtdFileName.lastIndexOf("\\") + 1);
							}
							
							// Handle entity files in subdirectories (e.g., ent/iso-amsa.ent)
							String relativePath = dtdFileName;
							if (systemID.contains("/ent/")) {
								relativePath = "ent/" + dtdFileName;
							} else if (systemID.contains("\\ent\\")) {
								relativePath = "ent" + File.separator + dtdFileName;
							}
							
							// Try dtd/v1.1/ first, then dtd/ as fallback
							String dtdBasePath = Main.CURRENT_DIR + File.separator + "dtd" + File.separator + "v1.1" + File.separator;
							File dtdFile = new File(dtdBasePath + relativePath.replace("/", File.separator));
							
							if (!dtdFile.exists()) {
								// Try without v1.1
								dtdBasePath = Main.CURRENT_DIR + File.separator + "dtd" + File.separator;
								dtdFile = new File(dtdBasePath + relativePath.replace("/", File.separator));
							}
							
							if (dtdFile.exists()) {
								try {
									// CRITICAL: Use toURI().toString() and FileInputStream
									// This avoids the "unknown protocol: c" error on Windows
									String fileUri = dtdFile.toURI().toString();
									InputSource source = new InputSource(new java.io.FileInputStream(dtdFile));
									source.setSystemId(fileUri);
									return source;
								} catch (Exception e) {
									log.error("Error resolving entity: " + e.toString());
								}
							} else {
								log.warn("DTD/entity file not found: " + dtdFile.getAbsolutePath());
							}
						}
						return null; // Use default behavior for other entities
					}
				});
				
				InputSource inputSource = new InputSource(new File(inFileName).toURI().toURL().toExternalForm());
				SAXSource saxSource = new SAXSource(xmlReader, inputSource);
				transformer.transform(saxSource, new StreamResult(new File(outFileName)));
				
			} catch (Exception e) {
				// Fallback to StreamSource if SAX approach fails
				log.error("Error setting up SAX parser with EntityResolver, trying fallback: " + e.toString());
				try {
					String inputFileUrl = new java.io.File(inFileName).toURI().toURL().toExternalForm();
					transformer.transform(new StreamSource(inputFileUrl), new StreamResult(new File(outFileName)));
				} catch (java.net.MalformedURLException e2) {
					log.error("Error converting file path to URL: " + e2.toString());
					transformer.transform(new StreamSource(inFileName), new StreamResult(new File(outFileName)));
				}
			}
			System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
		} catch (TransformerException e1) {
			log.error(e1.toString(), e1);
			throw new RISTransformerException(e1.toString());
		}
	}

	public static void insertLink(String keyword, Node currNode, int keywordIndex, String keywordBase, String urlBase, String urlValue, String linkType, Category log, Document doc, Lock documentLock) {
		if (keyword != null && currNode != null && keywordBase != null && urlBase != null && urlValue != null && linkType != null && log != null && doc != null && documentLock != null) {
			String currPara = currNode.getNodeValue();
			Node parentNode = currNode.getParentNode();

			if (currPara != null && parentNode != null) {

				String beforeLinkText = currPara.substring(0, keywordIndex);

				String contentkeyword = currPara.substring(keywordIndex, keywordIndex + keyword.length());
				String afterLinkText = currPara.substring(keywordIndex + keyword.length(), currPara.length());

				// need to look for a trailing space
				String afterLinkTextTrimmed = afterLinkText.replaceAll(" ", "");
				try {

					if ((afterLinkTextTrimmed.equals("") || afterLinkTextTrimmed.equals("\n") || afterLinkTextTrimmed.equals("\r")) && afterLinkText.length() > 0) {
						char nbsp = 160;
						afterLinkText = nbsp + "";
					}

					// also look for leading spaces
					String beforeLinkTextTrimmed = beforeLinkText.replaceAll(" ", "");
					if ((beforeLinkTextTrimmed.equals("") || beforeLinkTextTrimmed.equals("\n") || beforeLinkTextTrimmed.equals("\r")) && beforeLinkText.length() > 0) {
						char nbsp = 160;
						beforeLinkText = nbsp + "";
					}

					Node afterNode = doc.createTextNode(afterLinkText);
					Node beforeNode = null;
					if (keywordIndex > 0) {
						beforeNode = doc.createTextNode(beforeLinkText);
					} else {
						beforeNode = doc.createTextNode("");
					}
					Node nextNode = currNode.getNextSibling();

					try {
						documentLock.lock();
						parentNode.replaceChild(beforeNode, currNode);
					} catch (DOMException dome) {
						log.error(dome.toString());
						log.error("Error adding link to para " + currPara + " and keyword name = " + keyword);
					} finally {
						documentLock.unlock();
					}

					Node ulinkNode = doc.createElement("ulink");
					ulinkNode.appendChild(doc.createTextNode(contentkeyword));
					Attr urlAttr = doc.createAttribute("url");
					urlAttr.setValue(urlBase + urlValue);
					ulinkNode.getAttributes().setNamedItem(urlAttr);

					Attr typeAttr = doc.createAttribute("type");
					typeAttr.setValue(linkType);
					ulinkNode.getAttributes().setNamedItem(typeAttr);

					try {
						documentLock.lock();
						if (nextNode == null) {
							parentNode.appendChild(ulinkNode);
							if (!afterLinkText.equals("")) {
								parentNode.appendChild(afterNode);
							}
						} else {
							parentNode.insertBefore(ulinkNode, nextNode);
							if (!afterLinkText.equals("")) {
								parentNode.insertBefore(afterNode, nextNode);
							}
						}
					} catch (DOMException dome) {
						log.error(dome.toString());
						log.error("Error adding link to para " + currPara + " and keyword name = " + keyword);
					} finally {
						documentLock.unlock();
					}
				} catch (Exception er) {
					log.error(er.toString());
					log.error("Error in add link");
				}
			}
		}
	}
}