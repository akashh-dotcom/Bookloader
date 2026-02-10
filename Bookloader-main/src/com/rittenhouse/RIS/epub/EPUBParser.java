package com.rittenhouse.RIS.epub;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rittenhouse.RIS.Main;

/**
 * EPUB parser that converts EPUB files to XML format compatible with RIS backend
 *
 * @author System
 */
public class EPUBParser {

    protected static Category log = Category.getInstance(EPUBParser.class.getName());

    private File tempDir;
    private String epubPath;
    private Document contentDocument;
    private List<String> chapterOrder;

    public EPUBParser(String epubFilePath) {
        this.epubPath = epubFilePath;
        this.chapterOrder = new ArrayList<String>();
    }

    /**
     * Parse EPUB file and convert to XML format
     *
     * @param outputPath Path where the converted XML should be written
     * @return true if conversion successful, false otherwise
     */
    public boolean parseToXML(String outputPath) {
        try {
            // Extract EPUB (ZIP) file
            if (!extractEPUB()) {
                log.error("Failed to extract EPUB file: " + epubPath);
                return false;
            }

            // Parse the content.opf file to get book structure
            if (!parseContentOPF()) {
                log.error("Failed to parse content.opf file");
                return false;
            }

            // Convert to RIS XML format
            if (!convertToRISXML(outputPath)) {
                log.error("Failed to convert to RIS XML format");
                return false;
            }

            log.info("Successfully converted EPUB to XML: " + outputPath);
            return true;

        } catch (Exception e) {
            log.error("Error parsing EPUB file: " + e.getMessage());
            return false;
        } finally {
            // Clean up temp directory
            if (tempDir != null && tempDir.exists()) {
                deleteDirectory(tempDir);
            }
        }
    }

    /**
     * Extract EPUB file to temporary directory
     */
    private boolean extractEPUB() {
        try {
            // Create temporary directory
            tempDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "epub_" + System.currentTimeMillis());
            tempDir.mkdirs();

            // Extract ZIP file
            FileInputStream fis = new FileInputStream(epubPath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                File entryFile = new File(tempDir, entry.getName());

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // Create parent directories if needed
                    entryFile.getParentFile().mkdirs();

                    // Write file content
                    FileOutputStream fos = new FileOutputStream(entryFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    fos.close();
                }
                zis.closeEntry();
            }

            zis.close();
            fis.close();

            log.debug("EPUB extracted to: " + tempDir.getAbsolutePath());
            return true;

        } catch (IOException e) {
            log.error("Error extracting EPUB: " + e.getMessage());
            return false;
        }
    }

    /**
     * Parse the content.opf file to understand book structure
     */
    private boolean parseContentOPF() {
        try {
            // Find container.xml first
            File containerFile = new File(tempDir, "META-INF/container.xml");
            if (!containerFile.exists()) {
                log.error("container.xml not found in EPUB");
                return false;
            }

            // Parse container.xml to find content.opf location
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document containerDoc = builder.parse(containerFile);

            NodeList rootfiles = containerDoc.getElementsByTagName("rootfile");
            if (rootfiles.getLength() == 0) {
                log.error("No rootfile found in container.xml");
                return false;
            }

            Element rootfile = (Element) rootfiles.item(0);
            String contentOPFPath = rootfile.getAttribute("full-path");

            // Parse content.opf
            File contentOPFFile = new File(tempDir, contentOPFPath);
            if (!contentOPFFile.exists()) {
                log.error("content.opf not found at: " + contentOPFPath);
                return false;
            }

            contentDocument = builder.parse(contentOPFFile);

            // Extract spine order for chapters
            NodeList spineItems = contentDocument.getElementsByTagName("itemref");
            for (int i = 0; i < spineItems.getLength(); i++) {
                Element item = (Element) spineItems.item(i);
                String idref = item.getAttribute("idref");
                chapterOrder.add(idref);
            }

            log.debug("Found " + chapterOrder.size() + " chapters in EPUB");
            return true;

        } catch (Exception e) {
            log.error("Error parsing content.opf: " + e.getMessage());
            return false;
        }
    }

    /**
     * Convert EPUB content to RIS XML format
     */
    private boolean convertToRISXML(String outputPath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document risDoc = builder.newDocument();

            // Create root book element
            Element bookElement = risDoc.createElement("book");
            risDoc.appendChild(bookElement);

            // Add DOCTYPE attributes
            bookElement.setAttribute("xmlns", "http://www.rittenhouse.com/dtd/v1.1");

            // Extract metadata
            addMetadata(risDoc, bookElement);

            // Add chapters
            addChapters(risDoc, bookElement);

            // Write to output file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // Add DOCTYPE if DTD path is configured
            String dtdPath = null;
            try {
                dtdPath = Main.getConfigProperty("RIS.RITTENHOUSE_DTD_PATH");
            } catch (Exception e) {
                // DTD path not configured, skip DOCTYPE
                log.debug("DTD path not configured, skipping DOCTYPE declaration");
            }

            if (dtdPath != null && !dtdPath.trim().isEmpty()) {
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, dtdPath + "RittDocBook.dtd");
            }

            DOMSource source = new DOMSource(risDoc);
            StreamResult result = new StreamResult(new File(outputPath));
            transformer.transform(source, result);

            return true;

        } catch (Exception e) {
            log.error("Error converting to RIS XML: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add metadata from EPUB to RIS XML
     */
    private void addMetadata(Document risDoc, Element bookElement) {
        try {
            NodeList metadata = contentDocument.getElementsByTagName("metadata");
            if (metadata.getLength() > 0) {
                Element metadataElement = (Element) metadata.item(0);

                // Extract title
                NodeList titles = metadataElement.getElementsByTagName("dc:title");
                if (titles.getLength() > 0) {
                    Element titleElement = risDoc.createElement("title");
                    titleElement.setTextContent(titles.item(0).getTextContent());
                    bookElement.appendChild(titleElement);
                }

                // Extract authors
                NodeList creators = metadataElement.getElementsByTagName("dc:creator");
                if (creators.getLength() > 0) {
                    Element authorsElement = risDoc.createElement("authors");
                    for (int i = 0; i < creators.getLength(); i++) {
                        Element authorElement = risDoc.createElement("author");
                        authorElement.setTextContent(creators.item(i).getTextContent());
                        authorsElement.appendChild(authorElement);
                    }
                    bookElement.appendChild(authorsElement);
                }

                // Extract ISBN
                NodeList identifiers = metadataElement.getElementsByTagName("dc:identifier");
                for (int i = 0; i < identifiers.getLength(); i++) {
                    Element identifier = (Element) identifiers.item(i);
                    String scheme = identifier.getAttribute("opf:scheme");
                    if ("ISBN".equalsIgnoreCase(scheme)) {
                        Element isbnElement = risDoc.createElement("isbn");
                        isbnElement.setTextContent(identifier.getTextContent());
                        bookElement.appendChild(isbnElement);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error adding metadata: " + e.getMessage());
        }
    }

    /**
     * Add chapters from EPUB to RIS XML
     */
    private void addChapters(Document risDoc, Element bookElement) {
        try {
            Element chaptersElement = risDoc.createElement("chapters");
            bookElement.appendChild(chaptersElement);

            // Get manifest items
            NodeList manifestItems = contentDocument.getElementsByTagName("item");
            System.out.println("DEBUG: Found " + manifestItems.getLength() + " manifest items");
            System.out.println("DEBUG: Chapter order size: " + chapterOrder.size());

            for (String chapterId : chapterOrder) {
                System.out.println("DEBUG: Processing chapter ID: " + chapterId);
                // Find the manifest item for this chapter
                for (int i = 0; i < manifestItems.getLength(); i++) {
                    Element item = (Element) manifestItems.item(i);
                    if (chapterId.equals(item.getAttribute("id"))) {
                        String href = item.getAttribute("href");
                        String mediaType = item.getAttribute("media-type");
                        System.out.println("DEBUG: Found item - href: " + href + ", mediaType: " + mediaType);

                        // Only process XHTML content
                        if ("application/xhtml+xml".equals(mediaType) || "text/html".equals(mediaType)) {
                            addChapterContent(risDoc, chaptersElement, href, chapterId);
                        }
                        break;
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error adding chapters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Add individual chapter content
     */
    private void addChapterContent(Document risDoc, Element chaptersElement, String href, String chapterId) {
        try {
            System.out.println("DEBUG: Adding chapter content for " + chapterId + " from " + href);

            // The href is relative to the content.opf location (usually OEBPS/)
            File oebpsDir = new File(tempDir, "OEBPS");
            File chapterFile = new File(oebpsDir, href);
            System.out.println("DEBUG: Looking for chapter file at: " + chapterFile.getAbsolutePath());

            // Fallback: search for the file in temp directory structure
            if (!chapterFile.exists()) {
                chapterFile = new File(tempDir, href);
                System.out.println("DEBUG: Fallback to: " + chapterFile.getAbsolutePath());
            }

            if (!chapterFile.exists()) {
                System.out.println("DEBUG: Chapter file not found: " + href + " (searched in OEBPS and temp root)");
                return;
            }

            System.out.println("DEBUG: Found chapter file: " + chapterFile.getAbsolutePath() + " (exists: " + chapterFile.exists() + ")");

            // Parse XHTML content (disable validation for compatibility)
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document chapterDoc = builder.parse(chapterFile);

            Element chapterElement = risDoc.createElement("chapter");
            chapterElement.setAttribute("id", chapterId);

            // Extract title from h1, h2, or title tags
            String chapterTitle = extractChapterTitle(chapterDoc);
            if (chapterTitle != null && !chapterTitle.trim().isEmpty()) {
                Element titleElement = risDoc.createElement("title");
                titleElement.setTextContent(chapterTitle);
                chapterElement.appendChild(titleElement);
            }

            // Extract and clean content
            String content = extractTextContent(chapterDoc);
            if (content != null && !content.trim().isEmpty()) {
                Element contentElement = risDoc.createElement("content");
                contentElement.setTextContent(content);
                chapterElement.appendChild(contentElement);
            }

            chaptersElement.appendChild(chapterElement);

        } catch (Exception e) {
            log.warn("Error adding chapter content for " + href + ": " + e.getMessage());
        }
    }

    /**
     * Extract chapter title from XHTML document
     */
    private String extractChapterTitle(Document doc) {
        // Try h1 first
        NodeList h1s = doc.getElementsByTagName("h1");
        if (h1s.getLength() > 0) {
            return h1s.item(0).getTextContent().trim();
        }

        // Try h2
        NodeList h2s = doc.getElementsByTagName("h2");
        if (h2s.getLength() > 0) {
            return h2s.item(0).getTextContent().trim();
        }

        // Try title
        NodeList titles = doc.getElementsByTagName("title");
        if (titles.getLength() > 0) {
            return titles.item(0).getTextContent().trim();
        }

        return null;
    }

    /**
     * Extract text content from XHTML document, excluding scripts and styles
     */
    private String extractTextContent(Document doc) {
        try {
            // Get body content
            NodeList bodies = doc.getElementsByTagName("body");
            if (bodies.getLength() > 0) {
                Element body = (Element) bodies.item(0);
                return extractTextFromNode(body);
            }

            // Fallback to entire document
            return extractTextFromNode(doc.getDocumentElement());

        } catch (Exception e) {
            log.warn("Error extracting text content: " + e.getMessage());
            return "";
        }
    }

    /**
     * Recursively extract text from a node, excluding script and style elements
     */
    private String extractTextFromNode(Node node) {
        StringBuilder sb = new StringBuilder();

        if (node.getNodeType() == Node.TEXT_NODE) {
            sb.append(node.getTextContent());
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String tagName = element.getTagName().toLowerCase();

            // Skip script and style elements
            if ("script".equals(tagName) || "style".equals(tagName)) {
                return "";
            }

            // Add space before block elements
            if (isBlockElement(tagName)) {
                sb.append(" ");
            }

            // Process child nodes
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                sb.append(extractTextFromNode(children.item(i)));
            }

            // Add space after block elements
            if (isBlockElement(tagName)) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    /**
     * Check if element is a block-level element
     */
    private boolean isBlockElement(String tagName) {
        return "div".equals(tagName) || "p".equals(tagName) || "h1".equals(tagName) ||
               "h2".equals(tagName) || "h3".equals(tagName) || "h4".equals(tagName) ||
               "h5".equals(tagName) || "h6".equals(tagName) || "section".equals(tagName) ||
               "article".equals(tagName) || "blockquote".equals(tagName);
    }

    /**
     * Recursively delete directory and all contents
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}