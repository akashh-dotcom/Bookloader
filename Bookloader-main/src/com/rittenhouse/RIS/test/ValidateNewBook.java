package com.rittenhouse.RIS.test;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.xpath.XPathAPI;

/**
 * Validates the processing of the new book (9781962288002)
 */
public class ValidateNewBook {
    
    public static void main(String[] args) {
        System.out.println("=== Validation for Book 9781962288002 ===");
        
        String bookFile = "./test/temp/9781962288002/book.9781962288002.xml";
        
        validateBookXml(bookFile);
    }
    
    public static void validateBookXml(String xmlFile) {
        try {
            File file = new File(xmlFile);
            if (!file.exists()) {
                System.out.println("❌ File not found: " + xmlFile);
                return;
            }
            
            System.out.println("✅ Found XML file: " + xmlFile);
            System.out.println("📄 File size: " + file.length() + " bytes");
            
            // Parse the XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            
            // Check basic structure
            Node bookNode = XPathAPI.selectSingleNode(doc, "/book");
            if (bookNode == null) {
                System.out.println("❌ No <book> root element found");
                return;
            }
            System.out.println("✅ Valid DocBook structure");
            
            // Check title
            Node titleNode = XPathAPI.selectSingleNode(doc, "/book/title");
            if (titleNode != null) {
                String title = titleNode.getTextContent().trim();
                System.out.println("📖 Book Title: " + title);
            }
            
            // Check ISBN
            Node isbnNode = XPathAPI.selectSingleNode(doc, "/book/bookinfo/isbn");
            if (isbnNode != null) {
                String isbn = isbnNode.getTextContent().trim();
                System.out.println("📚 ISBN: " + isbn);
            }
            
            // Check chapters
            NodeList chapters = XPathAPI.selectNodeList(doc, "//chapter");
            int chapterCount = chapters.getLength();
            System.out.println("📑 Chapters found: " + chapterCount);
            
            if (chapterCount > 0) {
                System.out.println("✅ Chapters successfully included:");
                for (int i = 0; i < chapterCount && i < 10; i++) {
                    Node chapter = chapters.item(i);
                    String chapterId = chapter.getAttributes().getNamedItem("id").getNodeValue();
                    
                    Node chapterTitle = XPathAPI.selectSingleNode(chapter, ".//title[text()]");
                    String titleText = "";
                    if (chapterTitle != null) {
                        titleText = chapterTitle.getTextContent().trim();
                        if (titleText.length() > 50) {
                            titleText = titleText.substring(0, 50) + "...";
                        }
                    }
                    
                    System.out.println("  " + chapterId + ": " + (titleText.isEmpty() ? "(no title)" : titleText));
                }
                if (chapterCount > 10) {
                    System.out.println("  ... and " + (chapterCount - 10) + " more chapters");
                }
            }
            
            // Check for author info
            Node authorGroup = XPathAPI.selectSingleNode(doc, "//authorgroup");
            Node collab = XPathAPI.selectSingleNode(doc, "//collab");
            if (authorGroup != null || collab != null) {
                System.out.println("✅ Author information found");
                if (collab != null) {
                    Node collabName = XPathAPI.selectSingleNode(collab, "collabname");
                    if (collabName != null) {
                        System.out.println("👥 Organization: " + collabName.getTextContent().trim());
                    }
                }
            } else {
                System.out.println("⚠️ No author information found");
            }
            
            // Check content length
            String content = doc.getTextContent();
            int contentLength = content.length();
            System.out.println("📝 Total content length: " + contentLength + " characters");
            
            // Overall assessment
            System.out.println("\n=== Processing Summary ===");
            if (chapterCount >= 3 && contentLength > 10000) {
                System.out.println("🎉 SUCCESS: Book processing completed successfully!");
                System.out.println("   ✅ XML structure is valid");
                System.out.println("   ✅ " + chapterCount + " chapters included");
                System.out.println("   ✅ Content appears complete (" + contentLength + " chars)");
            } else {
                System.out.println("⚠️ WARNING: Processing may be incomplete");
                if (chapterCount < 3) {
                    System.out.println("   ❓ Only " + chapterCount + " chapters found (expected more?)");
                }
                if (contentLength < 10000) {
                    System.out.println("   ❓ Content seems short (" + contentLength + " chars)");
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error validating XML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}