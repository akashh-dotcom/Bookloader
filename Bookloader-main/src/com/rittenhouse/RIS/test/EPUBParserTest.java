package com.rittenhouse.RIS.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.rittenhouse.RIS.epub.EPUBParser;

/**
 * Test class for EPUB parser functionality
 *
 * @author System
 */
public class EPUBParserTest {

    public static void main(String[] args) {
        EPUBParserTest test = new EPUBParserTest();

        try {
            // Create a sample EPUB file for testing
            String testEpubPath = test.createSampleEPUB();

            // Test the EPUB parser
            test.testEPUBParser(testEpubPath);

            // Clean up
            new File(testEpubPath).delete();

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a minimal sample EPUB file for testing
     */
    private String createSampleEPUB() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String epubPath = tempDir + File.separator + "test_sample.epub";

        FileOutputStream fos = new FileOutputStream(epubPath);
        ZipOutputStream zos = new ZipOutputStream(fos);

        // Add mimetype file
        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
        zos.putNextEntry(mimetypeEntry);
        zos.write("application/epub+zip".getBytes());
        zos.closeEntry();

        // Add META-INF/container.xml
        ZipEntry containerEntry = new ZipEntry("META-INF/container.xml");
        zos.putNextEntry(containerEntry);
        String containerXml = "<?xml version=\"1.0\"?>\n" +
            "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
            "  <rootfiles>\n" +
            "    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
            "  </rootfiles>\n" +
            "</container>";
        zos.write(containerXml.getBytes());
        zos.closeEntry();

        // Add OEBPS/content.opf
        ZipEntry contentOpfEntry = new ZipEntry("OEBPS/content.opf");
        zos.putNextEntry(contentOpfEntry);
        String contentOpf = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"BookId\" version=\"2.0\">\n" +
            "  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n" +
            "    <dc:title>Test Medical Book</dc:title>\n" +
            "    <dc:creator>Dr. Test Author</dc:creator>\n" +
            "    <dc:identifier id=\"BookId\" opf:scheme=\"ISBN\">978-0-123456-78-9</dc:identifier>\n" +
            "    <dc:language>en</dc:language>\n" +
            "  </metadata>\n" +
            "  <manifest>\n" +
            "    <item id=\"chapter1\" href=\"chapter1.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "    <item id=\"chapter2\" href=\"chapter2.xhtml\" media-type=\"application/xhtml+xml\"/>\n" +
            "  </manifest>\n" +
            "  <spine>\n" +
            "    <itemref idref=\"chapter1\"/>\n" +
            "    <itemref idref=\"chapter2\"/>\n" +
            "  </spine>\n" +
            "</package>";
        zos.write(contentOpf.getBytes());
        zos.closeEntry();

        // Add OEBPS/chapter1.xhtml
        ZipEntry chapter1Entry = new ZipEntry("OEBPS/chapter1.xhtml");
        zos.putNextEntry(chapter1Entry);
        String chapter1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "<head><title>Introduction to Cardiology</title></head>\n" +
            "<body>\n" +
            "  <h1>Introduction to Cardiology</h1>\n" +
            "  <p>Cardiology is the study of the heart and blood vessels. " +
            "This chapter covers basic cardiac anatomy, including the four chambers " +
            "of the heart: left atrium, right atrium, left ventricle, and right ventricle. " +
            "Common cardiac diseases include myocardial infarction, atrial fibrillation, " +
            "and congestive heart failure.</p>\n" +
            "  <p>Risk factors for cardiovascular disease include hypertension, diabetes, " +
            "smoking, and hyperlipidemia. Treatment often involves medications such as " +
            "ACE inhibitors, beta blockers, and statins.</p>\n" +
            "</body>\n" +
            "</html>";
        zos.write(chapter1.getBytes());
        zos.closeEntry();

        // Add OEBPS/chapter2.xhtml
        ZipEntry chapter2Entry = new ZipEntry("OEBPS/chapter2.xhtml");
        zos.putNextEntry(chapter2Entry);
        String chapter2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "<head><title>Pharmacology Basics</title></head>\n" +
            "<body>\n" +
            "  <h1>Pharmacology Basics</h1>\n" +
            "  <p>This chapter discusses fundamental concepts in pharmacology. " +
            "Key drug classes include antibiotics like penicillin and amoxicillin, " +
            "which are used to treat bacterial infections such as pneumonia and " +
            "urinary tract infections.</p>\n" +
            "  <p>Other important medications include analgesics such as acetaminophen " +
            "and ibuprofen for pain management, and antihypertensives like lisinopril " +
            "for managing high blood pressure. Drug interactions and contraindications " +
            "must always be considered.</p>\n" +
            "</body>\n" +
            "</html>";
        zos.write(chapter2.getBytes());
        zos.closeEntry();

        zos.close();
        fos.close();

        System.out.println("Created sample EPUB: " + epubPath);
        return epubPath;
    }

    /**
     * Test the EPUB parser functionality
     */
    private void testEPUBParser(String epubPath) {
        System.out.println("Testing EPUB parser...");

        String outputPath = System.getProperty("java.io.tmpdir") + File.separator + "test_output.xml";

        EPUBParser parser = new EPUBParser(epubPath);
        boolean success = parser.parseToXML(outputPath);

        if (success) {
            System.out.println("✓ EPUB parsing successful!");
            System.out.println("✓ Output XML created at: " + outputPath);

            // Verify the output file exists and has content
            File outputFile = new File(outputPath);
            if (outputFile.exists() && outputFile.length() > 0) {
                System.out.println("✓ Output file verification passed");
                System.out.println("File size: " + outputFile.length() + " bytes");
            } else {
                System.err.println("✗ Output file verification failed");
            }

            // Clean up
            outputFile.delete();

        } else {
            System.err.println("✗ EPUB parsing failed!");
        }
    }
}