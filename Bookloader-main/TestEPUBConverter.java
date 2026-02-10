import com.rittenhouse.RIS.epub.EPUBParser;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class TestEPUBConverter {
    public static void main(String[] args) {
        try {
            String epubPath = "./test_medical_book.epub";
            String outputPath = "./test_output.xml";

            System.out.println("=== EPUB Converter Test ===");
            System.out.println("Input EPUB: " + epubPath);
            System.out.println("Output XML: " + outputPath);

            // Check if EPUB exists
            File epubFile = new File(epubPath);
            if (!epubFile.exists()) {
                System.out.println("❌ EPUB file not found: " + epubPath);
                return;
            }
            System.out.println("✓ EPUB file found (" + epubFile.length() + " bytes)");

            // Test the parser
            EPUBParser parser = new EPUBParser(epubPath);
            boolean result = parser.parseToXML(outputPath);

            System.out.println("Parse result: " + (result ? "SUCCESS" : "FAILED"));

            // Check output
            File outputFile = new File(outputPath);
            if (outputFile.exists()) {
                System.out.println("✓ XML output created (" + outputFile.length() + " bytes)");

                // Show first few lines of output
                String content = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
                System.out.println("\n=== Generated XML Content ===");
                String[] lines = content.split("\n");
                for (int i = 0; i < Math.min(20, lines.length); i++) {
                    System.out.println(lines[i]);
                }
                if (lines.length > 20) {
                    System.out.println("... (" + (lines.length - 20) + " more lines)");
                }
            } else {
                System.out.println("❌ XML output file not created");
            }

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}