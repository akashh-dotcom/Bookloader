package com.rittenhouse.RIS;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.socialchange.doctype.Doctype;
import net.socialchange.doctype.DoctypeChangerStream;
import net.socialchange.doctype.DoctypeGenerator;
import net.socialchange.doctype.DoctypeImpl;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Category;
import org.apache.log4j.Layout;
import org.apache.log4j.WriterAppender;
import org.apache.xpath.XPathAPI;
import org.jdom.input.SAXBuilder;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import com.rittenhouse.RIS.db.KeywordDB;
import com.rittenhouse.RIS.db.ResourceDB;
import com.rittenhouse.RIS.rules.DiseaseCounterThread;
import com.rittenhouse.RIS.rules.DiseaseLinkThread;
import com.rittenhouse.RIS.rules.DiseaseRISIndexThread;
import com.rittenhouse.RIS.rules.DiseaseSynonymCounterThread;
import com.rittenhouse.RIS.rules.DiseaseSynonymLinkThread;
import com.rittenhouse.RIS.rules.DiseaseSynonymRISIndexThread;
import com.rittenhouse.RIS.rules.DrugCounterThread;
import com.rittenhouse.RIS.rules.DrugLinkThread;
import com.rittenhouse.RIS.rules.DrugRISIndexThread;
import com.rittenhouse.RIS.rules.DrugSynonymCounterThread;
import com.rittenhouse.RIS.rules.DrugSynonymLinkThread;
import com.rittenhouse.RIS.rules.DrugSynonymRISIndexThread;
import com.rittenhouse.RIS.rules.KeywordIndexThread;
import com.rittenhouse.RIS.rules.RISIndexThread;
import com.rittenhouse.RIS.rules.SoftDiseaseCounterThread;
import com.rittenhouse.RIS.rules.SoftDiseaseSynonymCounterThread;
import com.rittenhouse.RIS.rules.SoftDrugCounterThread;
import com.rittenhouse.RIS.rules.SoftDrugSynonymCounterThread;
import com.rittenhouse.RIS.util.DateUtility;
import com.rittenhouse.RIS.util.FileComparator;
import com.rittenhouse.RIS.util.FileUtil;
import com.rittenhouse.RIS.util.MultiWordComparator;
import com.rittenhouse.RIS.util.RISErrorHandler;
import com.rittenhouse.RIS.util.RISParserException;
import com.rittenhouse.RIS.util.RISTransformerException;
import com.rittenhouse.RIS.util.StringUtil;
import com.rittenhouse.RIS.util.XMLUtil;
import com.rittenhouse.RIS.util.PhaseLogger;
import com.rittenhouse.RIS.epub.EPUBParser;

/**
 * RIS Backend - Content tagging and linking for content preparation
 * 
 * @author vbhatia - thoroughly modded by brianwright@technotects.com
 */
public class Main {

	private static final boolean isInDevelopmentMode = false;

	// Constants
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");

	public static final String CURRENT_DIR = System.getProperty("user.dir");

	// File paths - defaults for staging/production (running from subdirectory)
	public static String CONFIG_FILE_NAME = CURRENT_DIR + FILE_SEPARATOR + "RISBackend.cfg";
	public static String RULES_CONFIG = CURRENT_DIR + FILE_SEPARATOR + ".." + FILE_SEPARATOR + "rules" + FILE_SEPARATOR + "ris_rules.xml";
	public static String XSLT_RISINFO = CURRENT_DIR + FILE_SEPARATOR + ".." + FILE_SEPARATOR + "rules" + FILE_SEPARATOR + "AddRISInfo.xsl";
	public static String XSLT_CHUNKER = CURRENT_DIR + FILE_SEPARATOR + ".." + FILE_SEPARATOR + "rules" + FILE_SEPARATOR + "RISChunker.xsl";
	public static String XSLT_TOC_GENERATOR = CURRENT_DIR + FILE_SEPARATOR + ".." + FILE_SEPARATOR + "rules" + FILE_SEPARATOR + "toctransform.xsl";

	public static final String XML_TAG_RULE = "rule";
	public static final String XML_TAG_RULE_TYPE = "type";
	public static final String XML_TAG_RULE_ACTION = "action";

	public static final String XML_TAG_RULE_TYPE_ADDRISINFO = "addRISInfo";
	public static final String XML_TAG_RULE_TYPE_ADDPMID = "addPMID";
	public static final String XML_TAG_RULE_TYPE_ADDRISINDEX1 = "addRISIndex1";
	public static final String XML_TAG_RULE_TYPE_ADDRISINDEX2 = "addRISIndex2";
	public static final String XML_TAG_RULE_TYPE_ADDRISINDEX3 = "addRISIndex3";
	public static final String XML_TAG_RULE_TYPE_ADDRISINDEX4 = "addRISIndex4";
	public static final String XML_TAG_RULE_TYPE_LINKDISEASE = "linkDisease";
	public static final String XML_TAG_RULE_TYPE_LINKDRUG = "linkDrug";
	public static final String XML_TAG_RULE_TYPE_SPLITXML = "splitXML";
	public static final String XML_TAG_RULE_TYPE_LOADCONTENT = "loadContent";
	public static final String XML_TAG_RULE_TYPE_LINKKEYWORD = "linkKeyword";

	public static final String XML_TAG_RULE_ACTION_APPLY = "apply";
	public static final String XML_TAG_RULE_ACTION_DISABLE = "disable";
	public static final String XML_TAG_RULE_ACTION_REMOVE = "remove";

	private static int THREAD_COUNT_MAXIMUM = 8;
	private static int LINKING_THREAD_COUNT_MAXIMUM = 4;
	private static int THREAD_KEEP_ALIVE_TIME = 1000 * 10 * 1;

	public static final String XPATH_TITLE = ".//sect1/title | .//sect1/title/emphasis | .//sect1/title/emphasis/emphasis | " + ".//sect2/title | .//sect2/title/emphasis | .//sect2/title/emphasis/emphasis | " + ".//sect3/title | .//sect3/title/emphasis | .//sect3/title/emphasis/emphasis | " + ".//sect4/title | .//sect4/title/emphasis | .//sect4/title/emphasis/emphasis | " + ".//sect5/title | .//sect5/title/emphasis | .//sect5/title/emphasis/emphasis | " + ".//sect6/title | .//sect6/title/emphasis | .//sect6/title/emphasis/emphasis | " + "./title | ./title/emphasis | ./title/emphasis/emphasis";
	public static final String XPATH_LINKING = "//sect1//para | //sect1//para/emphasis";
	public static final String XPATH_SOFT_LINKING = "//para | //para/emphasis";
	public static final String XPATH_RULE2 = "./title | ./title/emphasis | ./title/emphasis/emphasis | " + ".//sect1/title | .//sect1/title/emphasis | .//sect1/title/emphasis/emphasis | " + ".//sect2/title | .//sect2/title/emphasis | .//sect2/title/emphasis/emphasis | " + ".//sect3/title | .//sect3/title/emphasis | .//sect3/title/emphasis/emphasis | " + ".//sect4/title | .//sect4/title/emphasis | .//sect4/title/emphasis/emphasis | " + ".//sect5/title | .//sect5/title/emphasis | .//sect5/title/emphasis/emphasis | " + ".//sect6/title | .//sect6/title/emphasis | .//sect6/title/emphasis/emphasis | " + ".//emphasis | .//emphasis/emphasis";

	// logger
	protected static Category log = null;
	protected static WriterAppender logOrig = null;
	protected static Writer defaultlogFile = null;
	protected static PhaseLogger phaseLogger = null;

	// Configuration
	private static Properties configProperties;

	public static Document rulesDoc;

	// Flag to indicate if server should keep on running
	private static boolean keepRunning = true;

	private static MetaData metaData = null;

	public static LinkedHashMap foundDiseaseList = null;
	public static LinkedHashMap foundDiseaseSynList = null;
	public static LinkedHashMap foundDrugList = null;
	public static LinkedHashMap foundDrugSynList = null;

	public static boolean isDebug = false;

	public static boolean isMiniDrug = false;
	
	// DTD resolution tracking to reduce log spam
	private static ThreadLocal<Integer> dtdResolutionCount = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return 0;
		}
	};
	private static ThreadLocal<Boolean> dtdResolutionStarted = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		}
	};
	private static ThreadLocal<java.util.List<String>> dtdResolutionFailures = new ThreadLocal<java.util.List<String>>() {
		@Override
		protected java.util.List<String> initialValue() {
			return new java.util.ArrayList<String>();
		}
	};

	public static boolean isDrugMongraph = false; // var to look at if the
	// book is a drug monograph

	public static boolean useTermsSubset = false; // ELK a config parameter
	// used in debugging to
	// limit the number of drugs
	// you look at to AM so that
	// it doesn't
	// Take forever to load datasets while debugging.

	public static boolean isMetaDataFiltered = false; // ELK starts as false
	// for non-testing and
	// is set to true after
	// filtering
	// if you want to test only keyword then set to true and make sure the rules
	// don't specify the other rules.
	public static String bookTitle = "";

	private boolean LINK_DRUGS = false;
	private boolean LINK_DISEASES = false;

	// Local testing flags - allow control of update mode, database save, and exit code tracking
	public static boolean allowResourceUpdate = false;
	public static boolean skipDatabaseSave = false;
	public static boolean skipDrugDiseaseLinks = false;  // Skip drug/disease linking
	public static boolean skipAllDatabaseOperations = false;  // Skip ALL database operations
	public static boolean skipPMID = false;  // Skip PMID lookup during main processing
	public static boolean savePMIDProgress = false;  // Save PMID progress for resume capability
	public static int lastExitCode = 0;
	public static String lastErrorMessage = null;
	public static Throwable lastException = null;
	
	// NCBI API rate limiting (3 req/sec without key, 10 req/sec with key)
	private static Semaphore ncbiRateLimiter = null;
	private static long ncbiLastRequestTime = 0;

	public static String bookISBN = null;
	public static String authors = null;
	public static String pubName = null;
	public static Date pubDate = null;
	public static String edition = null;
	public static String copyRightStr = null;
	public static String authorListXML = null;

	/**
	 * default constructor
	 */
	public Main() {
		// Smart path resolution: check current directory first (for local dev), 
		// then parent directory (for staging/production deployments)
		if (isInDevelopmentMode || new File(CURRENT_DIR + FILE_SEPARATOR + "rules" + FILE_SEPARATOR + "ris_rules.xml").exists()) {
			// Running from project root (local dev) - rules are in ./rules/
			CONFIG_FILE_NAME = CURRENT_DIR + FILE_SEPARATOR + "RISBackend.cfg";
			RULES_CONFIG = CURRENT_DIR + FILE_SEPARATOR + "rules" + FILE_SEPARATOR + "ris_rules.xml";
			XSLT_RISINFO = CURRENT_DIR + FILE_SEPARATOR + "rules" + FILE_SEPARATOR + "AddRISInfo.xsl";
			XSLT_CHUNKER = CURRENT_DIR + FILE_SEPARATOR + "rules" + FILE_SEPARATOR + "RISChunker.xsl";
			XSLT_TOC_GENERATOR = CURRENT_DIR + FILE_SEPARATOR + "rules" + FILE_SEPARATOR + "toctransform.xsl";
		}
		// else: use default paths (CURRENT_DIR/../rules/) for staging/production
		System.setProperty("file.encoding", "UTF-8");
		startLogger();
		
		// Skip metadata loading if database operations are disabled
		if (!skipAllDatabaseOperations) {
			metaData = new MetaData();
		}
		
		try {
			THREAD_COUNT_MAXIMUM = Integer.parseInt(Main.getConfigProperty("RIS.THREAD_COUNT_MAXIMUM"));
		} catch (NumberFormatException nfe) {
		}
		try {
			LINKING_THREAD_COUNT_MAXIMUM = Integer.parseInt(Main.getConfigProperty("RIS.LINKING_THREAD_COUNT_MAXIMUM"));
		} catch (NumberFormatException nfe) {
		}
	}

	/**
	 * RIS backend entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// CRITICAL: Parse flags that affect initialization BEFORE creating Main instance
		// These flags must be set before Main constructor runs (it may load metadata)
		for (String arg : args) {
			if ("--skipLinks".equalsIgnoreCase(arg) || "-skipLinks".equalsIgnoreCase(arg) || 
			    "--skiplinks".equalsIgnoreCase(arg) || "-skiplinks".equalsIgnoreCase(arg)) {
				skipDrugDiseaseLinks = true;
			}
			if ("--noDB".equalsIgnoreCase(arg) || "-noDB".equalsIgnoreCase(arg) ||
			    "--nodb".equalsIgnoreCase(arg) || "-nodb".equalsIgnoreCase(arg) ||
			    "--no-db".equalsIgnoreCase(arg) || "-no-db".equalsIgnoreCase(arg)) {
				skipAllDatabaseOperations = true;
			}
			if ("-update".equalsIgnoreCase(arg) || "--update".equalsIgnoreCase(arg)) {
				allowResourceUpdate = true;
			}
			if ("--skipPMID".equalsIgnoreCase(arg) || "-skipPMID".equalsIgnoreCase(arg) ||
			    "--skippmid".equalsIgnoreCase(arg) || "-skippmid".equalsIgnoreCase(arg)) {
				skipPMID = true;
			}
			if ("--savePMIDProgress".equalsIgnoreCase(arg) || "-savePMIDProgress".equalsIgnoreCase(arg) ||
			    "--savepmidprogress".equalsIgnoreCase(arg) || "-savepmidprogress".equalsIgnoreCase(arg)) {
				savePMIDProgress = true;
			}
		}
		
		Main m = new Main();
		m.startLogger();
		
		// Log the flags that were set
		if (skipDrugDiseaseLinks) {
			log.info("SKIP LINKS MODE: Drug and disease linking will be skipped");
		}
		if (skipAllDatabaseOperations) {
			log.info("NO-DB MODE: All database operations will be skipped");
		}
		if (allowResourceUpdate) {
			log.info("UPDATE MODE ENABLED: Will allow updating existing resources");
		}
		if (skipPMID) {
			log.info("SKIP PMID MODE: PMID lookup will be skipped during processing");
		}
		if (savePMIDProgress) {
			log.info("SAVE PMID PROGRESS MODE: Progress will be checkpointed for resume capability");
		}
		
		// Check for special modes first
		boolean isJflMode = false;
		boolean isPmidOnlyMode = false;
		for (String arg : args) {
			if ("-jfl".equals(arg)) {
				isJflMode = true;
				break;
			}
			if ("--pmidOnly".equalsIgnoreCase(arg) || "-pmidOnly".equalsIgnoreCase(arg) ||
			    "--pmidonly".equalsIgnoreCase(arg) || "-pmidonly".equalsIgnoreCase(arg)) {
				isPmidOnlyMode = true;
				break;
			}
		}
		
		// Parse other command-line arguments
		for (String arg : args) {
			// Accept --normal, --fast, -normal, -fast but these are currently ignored
			// The modes are handled via rules file selection in config
			if ("--normal".equalsIgnoreCase(arg) || "-normal".equalsIgnoreCase(arg)) {
				log.info("NORMAL MODE: Using standard processing rules");
			}
			if ("--fast".equalsIgnoreCase(arg) || "-fast".equalsIgnoreCase(arg)) {
				log.info("FAST MODE: Using fast processing rules");
			}
		}
		
		if (isPmidOnlyMode) {
			// PMID-only post-processing mode
			log.info("==== PMID POST-PROCESS MODE ====");
			m.runPMIDPostProcess();
		} else if (isJflMode && args.length >= 2) {
			// Extract ISBN from args (should be after -jfl flag)
			for (int i = 0; i < args.length - 1; i++) {
				if ("-jfl".equals(args[i])) {
					bookISBN = args[i + 1];
					break;
				}
			}
			if (!m.prepareBookXml(getConfigProperty("RIS.CONTENT_TEMP"))) {
				m.failure(-1);
			}
			System.out.println("Re-copy all of the temp contents into the temp folder and press enter to continue.");
			Scanner in = new Scanner(System.in);
			in.nextLine();
			m.finishLoadingFiles();
		} else {
			m.runRISBackend();
		}
	}

	/**
	 * Runs the RIS backend
	 */
	public void runRISBackend() {
		// Initialize phase logger
		if (phaseLogger == null) {
			phaseLogger = new PhaseLogger(log);
		}
		
		System.out.println("==== RISBackend started =====");
		log.info("==== RISBackend started =====");

		// Phase 1: Load Rules Configuration
		phaseLogger.startPhase("Load Rules Configuration");
		if (!loadRulesConfig()){
			phaseLogger.phaseError("Failed to load rules configuration file");
			failure(-10);
			return;
		}
		phaseLogger.completePhase();
			
		// Phase 2: Load Metadata (if needed)
		// Load metadata unless ALL database operations are disabled
		// Note: Drug/disease linking requires metadata even if we're not saving to DB
		boolean shouldLoadMetadata = !skipAllDatabaseOperations;
		if (shouldLoadMetadata) {
			phaseLogger.startPhase("Load Metadata from Database");
			if (!metaData.loadMetaData()) {
				phaseLogger.phaseError("Failed to load metadata from database");
				failure(-11);
				return;
			}
			phaseLogger.completePhase();
		} else {
			phaseLogger.progress("Skipping metadata loading (all database operations disabled)");
		}
		
		{
			// Phase 3: Clean Directories
			phaseLogger.startPhase("Clean Working Directories");
			FileUtil.cleanDir(new File(getConfigProperty("RIS.CONTENT_TEMP")), true);
			FileUtil.cleanDir(new File(getConfigProperty("RIS.CONTENT_OUT")), true);
			phaseLogger.completePhase();

			// Phase 4: Prepare Content Files
			phaseLogger.startPhase("Prepare Content Files");
			prepareContentFiles(null, true);
			if (!prepareBookXml(getConfigProperty("RIS.CONTENT_TEMP"))) {
				phaseLogger.phaseError("Failed to prepare book XML");
                failure(-2);
                return;
			}
			phaseLogger.completePhase();

			// Phase 5: Process Rules (main processing)
			phaseLogger.startPhase("Process Rules and Transform Content");
		try {
			if (!processRules()) {
				phaseLogger.phaseError("Failed processing the rules");
				failure(-3);
				return;
			}
		} catch (Exception e) {
			log.error("==========================================");
			log.error("UNCAUGHT EXCEPTION in processRules:");
			log.error("Exception type: " + e.getClass().getName());
			log.error("Exception message: " + e.getMessage());
			log.error("Stack trace:", e);
			log.error("==========================================");
			phaseLogger.phaseError("Exception during rule processing: " + e.getClass().getName() + ": " + e.getMessage());
			}
			phaseLogger.completePhase();
			
			endBookLogger();
			
			phaseLogger.completeIsbn(true);
			log.info("Shutting down.");

			success();
		}
	}

	/**
	 * Ensure DTD directory is available in book subdirectory for Saxon DTD resolution
	 * Creates a link to the shared DTD directory at test/temp/dtd/
	 * @param bookDir The book directory (e.g., test/temp/9781234567890/)
	 */
	private void ensureDTDAvailableForBook(File bookDir) {
		try {
			// Safety check: don't process directories named 'dtd'
			if ("dtd".equalsIgnoreCase(bookDir.getName())) {
				return;
			}
			
			File bookDtdDir = new File(bookDir, "dtd");
			
			// If DTD directory already exists in book directory, skip
			if (bookDtdDir.exists()) {
				log.debug("DTD directory already exists for book: " + bookDir.getName());
				return;
			}
			
			// Source DTD directory in temp root
			String tempDir = getConfigProperty("RIS.CONTENT_TEMP");
			File sourceDtdDir = new File(CURRENT_DIR, tempDir + FILE_SEPARATOR + "dtd");
			
			if (!sourceDtdDir.exists()) {
				log.warn("Shared DTD directory not found at: " + sourceDtdDir.getAbsolutePath());
				return;
			}
			
			// Copy DTD directory to book subdirectory
			// This allows Saxon to resolve relative DTD paths from XML files
			FileUtil.copyDirectory(sourceDtdDir, bookDtdDir);
			log.debug("DTD directory made available for book: " + bookDir.getName());
		} catch (Exception e) {
			log.error("Failed to ensure DTD availability for book: " + bookDir.getName(), e);
		}
	}

	/**
	 * Prepare content files for loading and linking
	 */
	void prepareContentFiles(File sourceDir, boolean recurse) {
		String sourceDirName = getConfigProperty("RIS.CONTENT_IN");
		String baseDestDirName = getConfigProperty("RIS.CONTENT_TEMP");
		File baseDestDir = new File(baseDestDirName);
		String destDirName = baseDestDirName;
		if (sourceDir == null) {
			sourceDir = new File(sourceDirName);
			
			// Check for zip files in the input directory and extract them first
			if (sourceDir.isDirectory()) {
				File[] inputFiles = sourceDir.listFiles();
				if (inputFiles != null) {
					for (File file : inputFiles) {
						if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
							log.info("Found zip file: " + file.getName() + ", extracting to input directory...");
							try {
								// Extract ISBN from filename (handle names like 9781394155972_all_fixes.zip)
								String zipBaseName = file.getName().substring(0, file.getName().length() - 4);
								String isbn = zipBaseName.split("_")[0];  // Take first part before underscore
								log.info("Extracted ISBN from filename: " + isbn);
								
								File extractDir = new File(sourceDir, isbn);
								
								// Only extract if the directory doesn't already exist
								if (!extractDir.exists()) {
									extractZipFile(file, extractDir);
									log.info("Successfully extracted " + file.getName() + " to " + extractDir.getAbsolutePath());
								} else {
									log.info("Skipping extraction - directory already exists: " + extractDir.getAbsolutePath());
								}
							} catch (IOException e) {
								log.error("Failed to extract zip file: " + file.getName(), e);
							}
						}
					}
				}
			}
		} else {
			destDirName = CURRENT_DIR + FILE_SEPARATOR + destDirName + FILE_SEPARATOR + sourceDir.getName();
			File destDir = new File(destDirName);
			if (!destDir.exists())
				destDir.mkdirs();
		}

		if (sourceDir.isDirectory()) {
			File[] files = sourceDir.listFiles();
			try {
				if (files == null || files.length == 0) {
					log.info("No content files found in " + sourceDir.getCanonicalPath());
				}
				for (int i = 0; i < files.length; i++) {
					File inFile = files[i];
					String inFileName = inFile.getCanonicalPath();

					// Skip zip files - they've already been extracted
					if (inFile.getName().toLowerCase().endsWith(".zip")) {
						log.debug("Skipping zip file in processing: " + inFile.getName());
						continue;
					}

					if (inFile.isDirectory() && recurse) {
						prepareContentFiles(inFile, true);
					} else if (inFile.getName().toUpperCase().endsWith("XML")) {
						FileUtil.copyFile(inFile.getCanonicalPath(), destDirName + FILE_SEPARATOR + inFile.getName());
					} else if (inFile.getName().toUpperCase().endsWith("EPUB")) {
						// Convert EPUB to XML format
						String epubFileName = inFile.getName();
						String xmlFileName = epubFileName.substring(0, epubFileName.lastIndexOf('.')) + ".xml";
						String outputPath = destDirName + FILE_SEPARATOR + "BOOK_" + xmlFileName;

						EPUBParser epubParser = new EPUBParser(inFile.getCanonicalPath());
						if (!epubParser.parseToXML(outputPath)) {
							log.error("Failed to convert EPUB to XML: " + inFile.getName());
						} else {
							log.info("Successfully converted EPUB to XML: " + epubFileName + " -> " + outputPath);
						}
					} else {
						String tmpDirname = baseDestDir.getAbsolutePath() + FILE_SEPARATOR + inFile.getParentFile().getParentFile().getName() + FILE_SEPARATOR + inFile.getParentFile().getName() + FILE_SEPARATOR + inFile.getName();
						FileUtil.copyFile(inFile.getCanonicalPath(), tmpDirname);
					}
				}
			} catch (UnsupportedEncodingException e) {
				log.error(e.toString());
			} catch (FileNotFoundException e) {
				log.error(e.toString());
			} catch (IOException e1) {
				log.error(e1.toString());
			}
		}
	}

	/**
	 * Extract a zip file to the specified destination directory
	 * @param zipFile The zip file to extract
	 * @param destDir The destination directory
	 * @throws IOException if extraction fails
	 */
	private void extractZipFile(File zipFile, File destDir) throws IOException {
		if (!destDir.exists()) {
			destDir.mkdirs();
		}

		byte[] buffer = new byte[1024];
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				File newFile = new File(destDir, zipEntry.getName());
				
				if (zipEntry.isDirectory()) {
					newFile.mkdirs();
				} else {
					// Create parent directories if they don't exist
					File parent = newFile.getParentFile();
					if (!parent.exists()) {
						parent.mkdirs();
					}
					
					// Extract file
					try (FileOutputStream fos = new FileOutputStream(newFile)) {
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
		}
	}

	/**
	 * @param baseDestDirName
	 * @return
	 */
	boolean prepareBookXml(String baseDestDirName) {
		File baseDestDir = new File(baseDestDirName);

		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		dfactory.setNamespaceAware(true);
		dfactory.setValidating(true);

		Properties xmlOutProps = new Properties();
		xmlOutProps.setProperty(OutputKeys.METHOD, "xml");
		xmlOutProps.setProperty(OutputKeys.INDENT, "yes");
		xmlOutProps.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		xmlOutProps.setProperty(OutputKeys.DOCTYPE_SYSTEM, getConfigProperty("RIS.RITTENHOUSE_DTD_PATH") + "RittDocBook.dtd");
		xmlOutProps.setProperty(OutputKeys.ENCODING, System.getProperty("file.encoding"));
		Transformer transformer = null;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperties(xmlOutProps);

			File[] files = baseDestDir.listFiles();
			for (int i = 0; i < files.length; i++) {
				File inFile = files[i];
				String inFileName = inFile.getCanonicalPath();
				if (inFile.isDirectory()) {
					// Skip DTD directories to prevent infinite recursion
					if ("dtd".equalsIgnoreCase(inFile.getName())) {
						continue;
					}
					
					// Ensure DTD directory is available in each book subdirectory for Saxon resolution
					ensureDTDAvailableForBook(inFile);
					
					if (!prepareBookXml(inFileName)) {
						return false;
					}
					// Continue processing other files/directories instead of returning
					continue;
				} else if (inFile.getName().toUpperCase().startsWith("BOOK") && inFile.getName().toUpperCase().endsWith(".XML")) {
					InputSource in = new InputSource(changeDoctype(inFile.toURL()));

					org.jdom.output.DOMOutputter outputter = new org.jdom.output.DOMOutputter();
					SAXBuilder testBuilder = new SAXBuilder();
					testBuilder.setIgnoringElementContentWhitespace(true);
					testBuilder.setValidation(false);
					testBuilder.setEntityResolver(new ChunkResolver(inFile.getParentFile().getCanonicalPath()));

					Document doc = null;
					try {
						org.jdom.Document jdomDoc = testBuilder.build(in);
						doc = outputter.output(jdomDoc);
						// Log DTD resolution summary after parsing
						logDTDResolutionSummary();
					} catch (IOException ioe) {
						log.error(ioe.toString());
						// Log DTD summary even on failure
						logDTDResolutionSummary();
						return false;
					}
					Node isbnNode = XPathAPI.selectSingleNode(doc, "/book/bookinfo/isbn");
					String bookIsbn = getCleanIsbn(isbnNode);
					
					if (bookIsbn == null || bookIsbn.isEmpty()) {
						log.error("==================================================================");
						log.error("CRITICAL: Failed to extract ISBN from Book.XML");
						log.error("XPath: /book/bookinfo/isbn");
						log.error("isbnNode found: " + (isbnNode != null));
						log.error("==================================================================");
						if (phaseLogger != null) {
							phaseLogger.phaseError("Failed to extract ISBN from Book.XML");
						}
						return false;
					}
					
					bookISBN = bookIsbn;
					log.info("Successfully extracted ISBN: " + bookISBN);

					Node bookTitleNode = XPathAPI.selectSingleNode(doc, "/book/title");
					if (bookTitleNode == null)
						bookTitleNode = XPathAPI.selectSingleNode(doc, "/book/bookinfo/title");

					bookTitle = bookTitleNode.getFirstChild().getTextContent().replace("\n", "");
					
					// Log ISBN being processed
					if (phaseLogger != null) {
						phaseLogger.startIsbn(bookIsbn, bookTitle);
					}

					Node bookInfoNode = XPathAPI.selectSingleNode(doc, "/book/bookinfo");

					Node authorsCompleteNode = XPathAPI.selectSingleNode(bookInfoNode, "//authorgroup");

					if (authorsCompleteNode != null) {
						authorListXML = xmlToString(authorsCompleteNode);
					}

					// Debug: log.info(authorListXML);

					NodeIterator authorsNodeIt = XPathAPI.selectNodeIterator(bookInfoNode, "authorgroup/author/personname ");
					Node authorNode;
					StringBuffer authBuf = new StringBuffer();
					int count;
					count = 1;
					Node firstNameNode, surNameNode, degreeNode;
					String firstNameStr, surNameStr;

					// Check that there is at least 1 node
					authorNode = authorsNodeIt.nextNode();

					if (authorNode == null) {
						// use editors
						log.debug("Checking alternate authors");
						authorsNodeIt = XPathAPI.selectNodeIterator(bookInfoNode, "author/personname ");
						authorNode = authorsNodeIt.nextNode();
					}

					if (authorNode == null) {
						// use editors
						log.debug("Checking alternate authors");
						authorsNodeIt = XPathAPI.selectNodeIterator(bookInfoNode, "author");
						authorNode = authorsNodeIt.nextNode();
					}
					if (authorNode == null) {
						// use editors
						log.debug("No authors trying to use editors");
						authorsNodeIt = XPathAPI.selectNodeIterator(bookInfoNode, "authorgroup/editor/personname ");
						authorNode = authorsNodeIt.nextNode();
					}

					if (authorNode == null) {
						// use editors
						log.debug("No authors trying to use editors");
						authorsNodeIt = XPathAPI.selectNodeIterator(bookInfoNode, "editor/personname ");
						authorNode = authorsNodeIt.nextNode();
					}

					if (authorNode == null) {
						// use editors
						log.debug("No authors trying to use editors");
						authorsNodeIt = XPathAPI.selectNodeIterator(bookInfoNode, " editor ");
						authorNode = authorsNodeIt.nextNode();
					}

					// else we have a major problem shutdown now.
					if (authorNode == null) {
						// bomb out
						log.error("No Author string could be found");
						failure(-4);
					}

					String endChar = "";
					while (authorNode != null) {
						firstNameNode = XPathAPI.selectSingleNode(authorNode, "firstname");
						surNameNode = XPathAPI.selectSingleNode(authorNode, "surname ");
						degreeNode = XPathAPI.selectSingleNode(authorNode, "degree");
						try {
							firstNameStr = firstNameNode.getFirstChild().getTextContent().replace(",", "").replace("\n", "");
						} catch (NullPointerException e) {
							firstNameStr = "";
						}
						try {
							surNameStr = surNameNode.getFirstChild().getTextContent().replace(",", "").replace("\n", "");
						} catch (NullPointerException e) {
							surNameStr = "";
						}
						if (firstNameStr.replace(" ", "").length() == 0) {
							try {
								firstNameStr = XMLUtil.getNodeContent(firstNameNode, "").replace(",", "");
								log.error("First Name contains additional tags author = " + firstNameStr);
							} catch (Exception ex) {
							}
						}
						if (surNameStr.replace(" ", "").length() == 0) {
							try {
								surNameStr = XMLUtil.getNodeContent(surNameNode, "").replace(",", "");
								log.error("SurName contains additional tags author = " + surNameStr);
							} catch (Exception ex) {
							}
						}
						authBuf.append(endChar);
						endChar = " ";
						authBuf.append(firstNameStr + " ");

						authBuf.append(surNameStr + ",");
						if (degreeNode != null) {
							String degreeList[];
							String degreeText = degreeNode.getFirstChild().getTextContent();
							degreeList = degreeText.split(",");
							for (int j = 0; j < degreeList.length; j++) {
								if (degreeList[j].length() > 0) {
									authBuf.append(degreeList[j] + ",");
								}
							}
						}
						count++;
						authorNode = authorsNodeIt.nextNode();
					}
					authors = authBuf.toString();
					if (authors.length() > 0) {
						authors = authors.substring(0, authors.length() - 1);
					}

					Node pubNode = XPathAPI.selectSingleNode(bookInfoNode, "publisher/publishername");
					pubName = "";
					if (pubNode != null) {
						pubName = XMLUtil.getNodeContent(pubNode, "");
					}

					Node pubDateNode = XPathAPI.selectSingleNode(bookInfoNode, "pubdate");
					String pubDateStr = "NULL";
					pubDate = null;
					if (pubDateNode != null) {
						pubDateStr = pubDateNode.getFirstChild().getNodeValue();
						pubDate = DateUtility.convertToDate(pubDateStr, "MMMMM yyyy");
						if (pubDate == null) {
							pubDate = DateUtility.convertToDate(pubDateStr, "yyyy");
						}

					} else {
						// if empty try to take it out of the copyright date
						Node pubDateNodeFirst = XPathAPI.selectSingleNode(bookInfoNode, "copyright/year[1]");
						Node pubDateNodeLast = XPathAPI.selectSingleNode(bookInfoNode, "copyright/year[last()]");
						try {
							pubDateStr = pubDateNodeFirst.getFirstChild().getNodeValue().replaceAll(",", "");
							Date pubDateFirst = DateUtility.convertToDate(pubDateStr, "yyyy");
							pubDateStr = pubDateNodeLast.getFirstChild().getNodeValue().replaceAll(",", "");
							Date pubDateLast = DateUtility.convertToDate(pubDateStr, "yyyy");
							if (pubDateLast.before(pubDateFirst)) {
								pubDate = pubDateFirst;
							} else {
								pubDate = pubDateLast;
							}

						} catch (NullPointerException e2) {
							log.error("No Pubdate or copyright year");
							return false;
						}
					}

					Node editionNode = XPathAPI.selectSingleNode(bookInfoNode, "edition");
					edition = "";
					if (editionNode != null) {
						edition = XMLUtil.getNodeContent(editionNode, "");
					}

					Node copyRightNode = XPathAPI.selectSingleNode(bookInfoNode, "copyright");
					copyRightStr = "";
					if (copyRightNode != null) {
						// holder isn't required if no use publisher name
						try {
							copyRightStr = XPathAPI.selectSingleNode(copyRightNode, "holder").getTextContent();
						} catch (Exception e2) {
							// expected use publisher info.
							if (pubName != null) {
								copyRightStr = pubName;
							}
						}
						copyRightStr = copyRightStr + " " + XPathAPI.selectSingleNode(copyRightNode, "year").getTextContent();
					}

					if (copyRightStr.length() == 0) {
						copyRightStr = XPathAPI.selectSingleNode(bookInfoNode, "legalnotice/para/text()").getTextContent();
					} else {
						copyRightStr = copyRightStr.trim();
					}

					startBookLogger(bookIsbn);
				
				// Only check if resource exists when we're actually saving to database
				if (!skipAllDatabaseOperations && !skipDatabaseSave) {
					ResourceDB resDB = new ResourceDB();
					Integer resId = resDB.getResourceInfo(bookIsbn, true);

					if (resId != null) {
						if (allowResourceUpdate) {
							log.info("Resource with ISBN # " + bookIsbn + " exists with resource id = " + resId + " - UPDATE MODE enabled, will proceed with update");
						} else {
							log.error("Resource with ISBN # " + bookIsbn + " exists with resource id = " + resId + " on " + getConfigProperty("RISDB.URL") + " database.");
							log.error("Please delete the resource and re-run RIS backend, OR use -update flag to update existing resource.");
							// processLog.
							return false;
						}
					}
				} else if (skipAllDatabaseOperations) {
					log.info("[NO-DB MODE] Skipping resource existence check for ISBN: " + bookIsbn);
					log.info("[NO-DB MODE] In production, this would verify resource doesn't already exist");
				}

				String outFileName = baseDestDirName + FILE_SEPARATOR + "book." + bookIsbn + ".xml";

				Writer writer = new OutputStreamWriter(new FileOutputStream(outFileName), System.getProperty("file.encoding"));
					transformer.transform(new DOMSource(doc), new StreamResult(writer));
					log.info("Preparing content file = " + files[i].getCanonicalPath() + " as " + outFileName);

					// For this book, copy all images and multi-media files to
					// web server
					String inDirName = inFile.getParentFile().getName();
					String outDirName = getConfigProperty("RIS.CONTENT_MEDIA") + FILE_SEPARATOR + bookIsbn + FILE_SEPARATOR;
					FileUtil.moveNonXMLFiles(new File(inFile.getParentFile().getCanonicalPath() + FILE_SEPARATOR), new File(outDirName), true);

					FileUtil.cleanDir(inFile.getParentFile(), false);
					writer.close();
					return true;
				}
			}
		} catch (Exception e) {
			log.error(e.toString(), e);
			return false;
		}
		return true;
	}

    /**
     * shutdown RIS Backend with a success code (0)
     */
    public void success() {
        keepRunning = false;
        logMessage("==== Stopping RISBackend with Success ====");
        lastExitCode = 0;
        if (!skipDatabaseSave) {
            Runtime.getRuntime().exit(0);
        }
    }

	/**
	 * Save PMID processing checkpoint
	 */
	private void savePMIDCheckpoint(String outputDir, int filesProcessed, int totalFiles, String lastFile) {
		if (!savePMIDProgress) return;
		
		try {
			File checkpointFile = new File(outputDir, ".pmid_checkpoint.txt");
			BufferedWriter writer = new BufferedWriter(new FileWriter(checkpointFile));
			writer.write("filesProcessed=" + filesProcessed + "\n");
			writer.write("totalFiles=" + totalFiles + "\n");
			writer.write("lastFile=" + lastFile + "\n");
			writer.write("timestamp=" + System.currentTimeMillis() + "\n");
			writer.close();
			log.info("Checkpoint saved: " + filesProcessed + "/" + totalFiles + " files processed");
		} catch (IOException e) {
			log.warn("Failed to save checkpoint: " + e.getMessage());
		}
	}
	
	/**
	 * Load PMID processing checkpoint
	 * @return files processed count, or 0 if no checkpoint exists
	 */
	private int loadPMIDCheckpoint(String outputDir) {
		if (!savePMIDProgress) return 0;
		
		try {
			File checkpointFile = new File(outputDir, ".pmid_checkpoint.txt");
			if (!checkpointFile.exists()) {
				return 0;
			}
			
			BufferedReader reader = new BufferedReader(new FileReader(checkpointFile));
			int filesProcessed = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("filesProcessed=")) {
					filesProcessed = Integer.parseInt(line.substring("filesProcessed=".length()));
				}
			}
			reader.close();
			
			if (filesProcessed > 0) {
				log.info("Checkpoint found: Resuming from file " + (filesProcessed + 1));
			}
			return filesProcessed;
		} catch (Exception e) {
			log.warn("Failed to load checkpoint: " + e.getMessage());
			return 0;
		}
	}
	
	/**
	 * Delete PMID checkpoint file
	 */
	private void deletePMIDCheckpoint(String outputDir) {
		try {
			File checkpointFile = new File(outputDir, ".pmid_checkpoint.txt");
			if (checkpointFile.exists()) {
				checkpointFile.delete();
				log.info("Checkpoint file deleted (processing complete)");
			}
		} catch (Exception e) {
			log.warn("Failed to delete checkpoint: " + e.getMessage());
		}
	}
	
	/**
	 * PMID Post-Process Mode - Runs ONLY PMID lookup on already-processed content files
	 * This allows running the parallelized PMID process separately after main bookloader completes
	 */
	public void runPMIDPostProcess() {
		// Initialize phase logger
		if (phaseLogger == null) {
			phaseLogger = new PhaseLogger(log);
		}
		
		log.info("==== PMID POST-PROCESS MODE STARTED ====");
		log.info("This mode will add PMIDs to already-processed content files");
		
		try {
			// Get output directory where processed files are located
			String outDir = getConfigProperty("RIS.CONTENT_OUT");
			File outputDirectory = new File(outDir);
			
			if (!outputDirectory.exists() || !outputDirectory.isDirectory()) {
				log.error("Output directory does not exist: " + outDir);
				log.error("Please run the main bookloader first, then use --pmidOnly mode");
				failure(-1);
				return;
			}
			
			// Find all XML files in output directory
			File[] xmlFiles = outputDirectory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".xml");
				}
			});
			
			if (xmlFiles == null || xmlFiles.length == 0) {
				log.info("No XML files found in output directory: " + outDir);
				success();
				return;
			}
			
			log.info("Found " + xmlFiles.length + " XML file(s) to process for PMID");
			
			// Load checkpoint if progress saving is enabled
			int startFromIndex = 0;
			if (savePMIDProgress) {
				startFromIndex = loadPMIDCheckpoint(outDir);
				if (startFromIndex > 0) {
					log.info("Resuming from checkpoint - starting at file " + (startFromIndex + 1) + "/" + xmlFiles.length);
				}
			}
			
			// Process each file
			int filesProcessed = startFromIndex;
			int filesWithPMID = 0;
			long startTime = System.currentTimeMillis();
			
			for (int i = startFromIndex; i < xmlFiles.length; i++) {
				File xmlFile = xmlFiles[i];
				String fileName = xmlFile.getName();
				
				// Progress indicator
				int percentComplete = (int) ((i * 100.0) / xmlFiles.length);
				log.info(String.format("Processing file %d/%d (%d%%): %s", 
					i + 1, xmlFiles.length, percentComplete, fileName));
				
				try {
					// Read the XML file
					Document doc = XMLUtil.getDocument(xmlFile.getAbsolutePath(), false);
					
					// Check if file has bibliography entries
					NodeList biblioNodes = XPathAPI.selectNodeList(doc, "//bibliomixed");
					int biblioCount = (biblioNodes != null) ? biblioNodes.getLength() : 0;
					
					if (biblioCount > 0) {
						log.info("  Found " + biblioCount + " bibliography entries in " + fileName);
						
						// Run PMID lookup (parallelized)
						addPMID(doc);
						
						// Save the updated document back to file
						Transformer transformer = TransformerFactory.newInstance().newTransformer();
						transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
						transformer.setOutputProperty(OutputKeys.INDENT, "yes");
						
						Writer writer = new OutputStreamWriter(
							new FileOutputStream(xmlFile), 
							System.getProperty("file.encoding")
						);
						transformer.transform(new DOMSource(doc), new StreamResult(writer));
						writer.close();
						
						filesWithPMID++;
						log.info("  Successfully updated " + fileName + " with PMID data");
					} else {
						log.info("  No bibliography entries found in " + fileName + " - skipping");
					}
					
					filesProcessed++;
					
					// Save checkpoint if progress saving is enabled
					if (savePMIDProgress) {
						savePMIDCheckpoint(outDir, filesProcessed, xmlFiles.length, fileName);
					}
					
				} catch (Exception e) {
					log.error("Error processing file " + fileName + ": " + e.getMessage(), e);
					// Continue with next file
				}
			}
			
			long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
			
			log.info("==== PMID POST-PROCESS COMPLETE ====");
			log.info(String.format("Summary: Processed %d files (%d with bibliographies) in %d seconds", 
				filesProcessed, filesWithPMID, elapsedTime));
			
			// Delete checkpoint file on successful completion
			if (savePMIDProgress) {
				deletePMIDCheckpoint(outDir);
			}
			
			success();
		} catch (Exception e) {
			log.error("Fatal error in PMID post-process mode: " + e.getMessage(), e);
			failure(-1);
		}
	}

	/**
	 * shutdown RIS Backend with a failure code
	 */
	public void failure(int code) {
		keepRunning = false;
        logMessage(String.format(
                "==== Stopping RISBackend with Failure - Code: %d ====", code
        ));
		lastExitCode = code;
		if (!skipDatabaseSave) {
			Runtime.getRuntime().exit(code);
		}
	}


    public void logMessage(String msg) {
        if (log != null) {
            log.info(msg);
        }
        System.out.println(msg);
    }

    /**
	 * Reads RIS backend configuration
	 * 
	 * @return Properties
	 */
	private static Properties getConfigProperties() {
		if (configProperties == null) {
			configProperties = new Properties();
			FileInputStream in = null;
			try {
				in = new FileInputStream(CONFIG_FILE_NAME);
			} catch (FileNotFoundException e) {
				log.error(e.getMessage());
			}
			try {
				configProperties.load(in);
				in.close();
			} catch (IOException e1) {
				log.error(e1.getMessage());
			}
			// make sure surrounding whitespace is trimmed from property values
			for (Enumeration enum1 = configProperties.propertyNames(); enum1.hasMoreElements();) {
				String propertyName = (String) enum1.nextElement();
				String value = configProperties.getProperty(propertyName);
				configProperties.put(propertyName, value.trim());
			}
		}
		return configProperties;
	}

	/**
	 * Gets config entry
	 * 
	 * @param property
	 *            Name
	 * @return property Value
	 */
	static public String getConfigProperty(String propertyName) {
		String propertyValue = getConfigProperties().getProperty(propertyName, "");
		if (propertyValue == null) {
			return "";
		}
		return propertyValue;
	}

	/**
	 * Load rules config
	 * 
	 * @return true on success and false on failure
	 */
	public boolean loadRulesConfig() {
		try {
			log.info("Loading rules config file - " + RULES_CONFIG);
			
			File rulesFile = new File(RULES_CONFIG);
			if (!rulesFile.exists()) {
				log.error("Couldn't find rule config at: " + RULES_CONFIG);
				return false;
			}
			
			InputSource inConfigSrc = new InputSource(new BufferedInputStream(new FileInputStream(rulesFile)));
			rulesDoc = XMLUtil.getDocument(inConfigSrc);
		} catch (RISParserException spe) {
			log.error("The following exception occurred " + spe.getMessage());
			logMessage("Rules were not properly loaded from config. RISParserException in Main.loadRulesConfig()");
			return false;
		} catch (IOException ioe) {
			log.error("The following exception occurred " + ioe.getMessage());
			logMessage("Rules were not properly loaded from config. IOException in Main.loadRulesConfig()");
			return false;
		}
		return true;
	}

	/**
	 * Apply all content tagging and linking rules
	 * 
	 * @return success or failure
	 */
	public boolean processRules() {
		String ruleFileName = null;
		String ruleAction = XML_TAG_RULE_ACTION_APPLY;
		String term = null;
		String topic = null;
		String type = null;
		String ruleId = null;
		Element rulesElem = rulesDoc.getDocumentElement();
		Iterator ruleIt = XMLUtil.getChildrenByTagName(rulesElem, XML_TAG_RULE);

		Document doc = null;
		boolean loadAfter = false;

		ArrayList files = FileUtil.getXMLFiles(getConfigProperty("RIS.CONTENT_TEMP"), true);
		if (files == null || files.size() == 0) {
			return true;
		}
		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		dfactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = dfactory.newDocumentBuilder();
			// Set EntityResolver to redirect DTD references to local files
			docBuilder.setEntityResolver(new ChunkResolver(getConfigProperty("RIS.CONTENT_TEMP")));
		} catch (ParserConfigurationException e4) {
			log.error(e4.toString());
			return false;
		}
		RISErrorHandler risErr = new RISErrorHandler();
		docBuilder.setErrorHandler(risErr);
		// interate on all docs then all rules

		while (ruleIt.hasNext()) {
			Element ruleElem = ((Element) ruleIt.next());
			if (ruleElem.getNodeName().equals(XML_TAG_RULE)) {
				ruleId = ruleElem.getAttributes().getNamedItem(XML_TAG_RULE_TYPE).getNodeValue();
				ruleAction = ruleElem.getAttributes().getNamedItem(XML_TAG_RULE_ACTION).getNodeValue();

				ruleId = ruleElem.getAttributes().getNamedItem(XML_TAG_RULE_TYPE).getNodeValue();
				ruleAction = ruleElem.getAttributes().getNamedItem(XML_TAG_RULE_ACTION).getNodeValue();
				if (ruleAction.equals(XML_TAG_RULE_ACTION_APPLY)) {
					if (ruleId.equals(XML_TAG_RULE_TYPE_ADDRISINFO)) {

						if (!addRISInfoTags()) {
							log.error("Error adding risinfo tags.");
						}
					}
				}
			}
		}

		for (int i = 0; i < files.size(); i++) {
			Reader reader = null;
			InputSource in = null;
			String contentFileName = null;
			isMetaDataFiltered = isDebug; // make sure to set to false as each
			// book set has differnt values
			ruleIt = XMLUtil.getChildrenByTagName(rulesElem, XML_TAG_RULE);

			// Log progress
			if (phaseLogger != null) {
				phaseLogger.progress(i + 1, files.size(), "file");
			}

			try {
				contentFileName = ((File) files.get(i)).getCanonicalPath();
				reader = new InputStreamReader(new FileInputStream((File) files.get(i)), System.getProperty("file.encoding"));
				in = new InputSource(reader);
				doc = docBuilder.parse(in);
			} catch (Exception exp) {
				if (phaseLogger != null) {
					phaseLogger.phaseError("Error parsing content file: " + contentFileName, exp);
				} else {
					log.error("Error parsing content file = " + contentFileName);
					log.error(exp.toString());
				}
				return false;
			}
			// always filter first (unless database operations are disabled)
			if (!skipAllDatabaseOperations) {
				try {
					log.info("Filtering " + contentFileName);
					filterMetaData(XMLUtil.documentToString(doc));
				} catch (Exception exp) {
					if (phaseLogger != null) {
						phaseLogger.phaseError("Error filtering content file: " + contentFileName, exp);
					} else {
						log.error("Error filtering content file = " + contentFileName);
						log.error(exp.toString());
					}
					exp.printStackTrace();
				}
			} else {
				log.info("[NO-DB MODE] Skipping metadata filtering for " + contentFileName);
				log.info("[NO-DB MODE] This would normally filter drug/disease metadata from the database");
			}
			// Iterate on all rules
			while (ruleIt.hasNext()) {
				Element ruleElem = ((Element) ruleIt.next());
				if (ruleElem.getNodeName().equals(XML_TAG_RULE)) {
					ruleId = ruleElem.getAttributes().getNamedItem(XML_TAG_RULE_TYPE).getNodeValue();
					ruleAction = ruleElem.getAttributes().getNamedItem(XML_TAG_RULE_ACTION).getNodeValue();

					if (ruleAction.equals(XML_TAG_RULE_ACTION_APPLY)) {
						if (ruleId.equals(XML_TAG_RULE_TYPE_LINKDISEASE)) {
							// Process current resource
							try {
								log.info("Running disease link to " + contentFileName);
								LINK_DISEASES = true;
							} catch (Exception exp) {
								log.error("Error parsing content file = " + contentFileName);
								log.error(exp.toString());
								exp.printStackTrace();
							}
						} else if (ruleId.equals(XML_TAG_RULE_TYPE_LINKDRUG)) {
							// Process each resource
							try {
								log.info("Running drug link to " + contentFileName);
								LINK_DRUGS = true;
							} catch (Exception exp) {
								log.error("Invalid content file while linking drugs= " + contentFileName);
								log.error(exp.toString());
								exp.printStackTrace();
							}
							} else if (ruleId.equals(XML_TAG_RULE_TYPE_ADDPMID)) {
							if (skipPMID) {
								log.info("Skipping PMID lookup for " + contentFileName + " (--skipPMID flag set)");
							} else {
								try {
									log.info("Adding PMID to " + contentFileName);

									addPMID(doc);

								} catch (Exception exp) {
									log.error("Error reading content file = " + contentFileName);
									log.error(exp.toString());
									exp.printStackTrace();
								}
							}

						} else if (ruleId.equals(XML_TAG_RULE_TYPE_LOADCONTENT)) {
							loadAfter = true;
						} else if (ruleId.equals(XML_TAG_RULE_TYPE_ADDRISINDEX1) || ruleId.equals(XML_TAG_RULE_TYPE_ADDRISINDEX2) || ruleId.equals(XML_TAG_RULE_TYPE_ADDRISINDEX3)) {
							// Apply RIS Index rules
							try {
								log.info("Running rule " + ruleId + " to " + contentFileName);
								executeTaggingRules(doc);
							} catch (Exception exp) {
								log.error("Error reading content file = " + contentFileName);
								log.error(exp.toString());
							}

						} else if (ruleId.equals(XML_TAG_RULE_TYPE_LINKKEYWORD)) {
							// Process each resource
							try {
								log.info("Running rule " + ruleId + " to " + contentFileName);
								executeKeywordTaggingRules(doc);
							} catch (Exception exp) {
								log.error("problem running executeKeywordTaggingRules= " + contentFileName);
								log.error(exp.toString());
							}
						}// end files loop

					} else if (ruleAction.equals(XML_TAG_RULE_ACTION_REMOVE)) {
						try {
							String inFileName = contentFileName;
							log.info("Removing tags from content file = " + inFileName);
							String removexpath = null;
							if (ruleId.equals(XML_TAG_RULE_TYPE_LINKDISEASE)) {
								removexpath = "//para/ulink[@type='disease']";
								removeTags(removexpath, doc);
							} else if (ruleId.equals(XML_TAG_RULE_TYPE_LINKDRUG)) {
								removexpath = "//para/ulink[@type='drug']";
								removeTags(removexpath, doc);
							} else if (ruleId.equals(Main.XML_TAG_RULE_TYPE_ADDRISINDEX1)) {
								removexpath = "//risindex[risrule='" + XML_TAG_RULE_TYPE_ADDRISINDEX1 + "']";
								removeTags(removexpath, doc);

								removexpath = "//risindex[risrule='" + XML_TAG_RULE_TYPE_ADDRISINDEX2 + "']";
								removeTags(removexpath, doc);

								removexpath = "//risindex[risrule='" + XML_TAG_RULE_TYPE_ADDRISINDEX3 + "']";
								removeTags(removexpath, doc);
							} else if (ruleId.equals(Main.XML_TAG_RULE_TYPE_ADDPMID)) {
								removexpath = "//biblioid[otherclass='PubMedID']";
								removeTags(removexpath, doc);
							}

						} catch (Exception exp) {
							log.error("Unable to remove rule");
							log.error(exp.toString());
						}
					}// finish remove
					try {
						reader.close();
						writeProcessedContent(doc, contentFileName);
						doc = null;
										in = new InputSource(new FileInputStream(contentFileName));
						doc = docBuilder.parse(in);
						in = null;
									} catch (Exception exp) {
						log.error("Close the files after the rule");
						log.error(exp.toString());
					}

				}// if ruleAction
			}// rule iterator
			try {
				reader.close();
				writeProcessedContent(doc, contentFileName);

			} catch (Exception exp) {
				log.error("Exception during rule processing: " + exp.getClass().getName() + ": " + exp.getMessage());
				log.error("Stack trace:", exp);
			}

			if (!loadFiles()) {
				log.error("==========================================");
				log.error("CRITICAL ERROR: Unable to load files");
				log.error("This typically indicates:");
				log.error("  - XML parsing/validation errors");
				log.error("  - Content splitting/chunking failures");
				log.error("  - Cross-reference validation issues");
				log.error("Check errors above for specific details");
				log.error("==========================================");
				return false; // Exit processing for this file
			} else {
				// Skip drug/disease linking if flag is set
				if (!skipDrugDiseaseLinks) {
					performDrugAndDiseaseLinking();
				} else {
					log.info("Skipping drug and disease linking (skipDrugDiseaseLinks=true)");
				}
				
				try {
					if (!finishLoadingFiles()) {
						log.error("==========================================");
						log.error("CRITICAL ERROR: Failed to finish loading files");
						log.error("This typically indicates:");
						log.error("  - TextML/database content loading failures");
						log.error("  - File system or network issues");
						log.error("Check errors above for specific details");
						log.error("==========================================");
						return false;
					}
				} catch (Exception e) {
					log.error("==========================================");
					log.error("EXCEPTION in finishLoadingFiles:");
					log.error("Exception type: " + e.getClass().getName());
					log.error("Exception message: " + e.getMessage());
					log.error("Stack trace:", e);
					log.error("==========================================");
					return false;
				}
				
				System.out.println("Process complete - ISBN: " + bookISBN);
			}

		}// end files loop

		FileUtil.copyXMLFiles(new File(getConfigProperty("RIS.CONTENT_TEMP")), new File(getConfigProperty("RIS.CONTENT_OUT")), true);
		return true;
	}

	/**
	 * Fiter meta data
	 * 
	 * @param content
	 */
	public void filterMetaData(String content) {
		if (isMetaDataFiltered) {
			return;
		}
		
		if (phaseLogger != null) {
			phaseLogger.progress("Filtering metadata: diseases, disease synonyms, drugs, and drug synonyms");
		} else {
			log.info("Trying to find the count of disease and drug names in content...");
		}
		
		content = content.toLowerCase();
		ArrayList chunkedDiseaseList = StringUtil.splitCollection(MetaData.getDiseaseMetaData(), 500);

		foundDiseaseList = new LinkedHashMap();
		foundDiseaseSynList = new LinkedHashMap();
		foundDrugList = new LinkedHashMap();
		foundDrugSynList = new LinkedHashMap();

		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);
		threadPool.createThreads(THREAD_COUNT_MAXIMUM);

		for (int i = 0; i < chunkedDiseaseList.size(); i++) {
			LinkedHashMap mapToUse = (LinkedHashMap) chunkedDiseaseList.get(i);
			DiseaseCounterThread aThread = new DiseaseCounterThread(mapToUse, content);
			try {
				threadPool.execute(aThread);
			} catch (InterruptedException e) {
			}
		}

		// BW Added this to keep memory usage low by only performing one set of
		// threads at a time
		threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
		try {
			threadPool.awaitTerminationAfterShutdown();
			while (!threadPool.isTerminatedAfterShutdown()) {
				Thread.sleep(1000);
			}
		} catch (Exception e1) {
		}

		threadPool = null;
		chunkedDiseaseList = null;
		
		if (phaseLogger != null) {
			phaseLogger.progress("Found disease occurrences (filtering 1/4 complete)");
		} else {
			log.info("Finished finding all disease occurrences.");
		}

		threadPool = new PooledExecutor(new LinkedQueue(), THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);
		threadPool.createThreads(THREAD_COUNT_MAXIMUM);
		// End of coded added by BW

		ArrayList chunkedDiseaseSynList = StringUtil.splitCollection(MetaData.getDiseaseSynonymMetaData(), 5000);

		for (int i = 0; i < chunkedDiseaseSynList.size(); i++) {
			LinkedHashMap mapToUse = (LinkedHashMap) chunkedDiseaseSynList.get(i);
			DiseaseSynonymCounterThread aThread = new DiseaseSynonymCounterThread(mapToUse, content);
			try {
				threadPool.execute(aThread);
			} catch (InterruptedException e) {
			}
		}

		// BW Added this to keep memory usage low by only performing one set of
		// threads at a time
		threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
		try {
			threadPool.awaitTerminationAfterShutdown();
			while (!threadPool.isTerminatedAfterShutdown()) {
				Thread.sleep(1000);
			}
		} catch (Exception e1) {
		}

		threadPool = null;
		chunkedDiseaseSynList = null;
		
		if (phaseLogger != null) {
			phaseLogger.progress("Found disease synonym occurrences (filtering 2/4 complete)");
		} else {
			log.info("Finished finding all disease synonym occurrences.");
		}

		threadPool = new PooledExecutor(new LinkedQueue(), THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);
		threadPool.createThreads(THREAD_COUNT_MAXIMUM);
		// End of coded added by BW

		ArrayList chunkedDrugList = StringUtil.splitCollection(MetaData.getDrugMetaData(), 5000);

		for (int i = 0; i < chunkedDrugList.size(); i++) {
			LinkedHashMap mapToUse = (LinkedHashMap) chunkedDrugList.get(i);
			DrugCounterThread aThread = new DrugCounterThread(mapToUse, content);
			try {
				threadPool.execute(aThread);
			} catch (InterruptedException e) {
			}
		}
		// BW Added this to keep memory usage low by only performing one set of
		// threads at a time
		threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
		try {
			threadPool.awaitTerminationAfterShutdown();
			while (!threadPool.isTerminatedAfterShutdown()) {
				Thread.sleep(1000);
			}
		} catch (Exception e1) {
		}

		threadPool = null;
		chunkedDrugList = null;
		
		if (phaseLogger != null) {
			phaseLogger.progress("Found drug occurrences (filtering 3/4 complete)");
		} else {
			log.info("Finished finding all drug occurrences.");
		}

		threadPool = new PooledExecutor(new LinkedQueue(), THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);
		threadPool.createThreads(THREAD_COUNT_MAXIMUM);
		// End of coded added by BW

		ArrayList chunkedDrugSynList = StringUtil.splitCollection(MetaData.getDrugSynonymMetaData(), 5000);

		for (int i = 0; i < chunkedDrugSynList.size(); i++) {
			LinkedHashMap mapToUse = (LinkedHashMap) chunkedDrugSynList.get(i);
			DrugSynonymCounterThread aThread = new DrugSynonymCounterThread(mapToUse, content);
			try {
				threadPool.execute(aThread);
			} catch (InterruptedException e) {
			}
		}
		// BW Added this to keep memory usage low by only performing one set of
		// threads at a time
		threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
		try {
			threadPool.awaitTerminationAfterShutdown();
			while (!threadPool.isTerminatedAfterShutdown()) {
				Thread.sleep(1000);
			}
		} catch (Exception e1) {
		}

		threadPool = null;
		chunkedDrugSynList = null;
		
		if (phaseLogger != null) {
			phaseLogger.progress("Found drug synonym occurrences (filtering 4/4 complete)");
		} else {
			log.info("Finished finding all drug synonym occurrences.");
		}

		threadPool = new PooledExecutor(new LinkedQueue(), THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);
		threadPool.createThreads(THREAD_COUNT_MAXIMUM);
		// End of coded added by BW

		// reorderMapUsingOriginalMap
		try {
			foundDiseaseList = reorderMapUsingOriginalMap(MetaData.getDiseaseMetaData(), foundDiseaseList);
			foundDiseaseSynList = reorderMapUsingOriginalMap(MetaData.getDiseaseSynonymMetaData(), foundDiseaseSynList);
			foundDrugList = reorderMapUsingOriginalMap(MetaData.getDrugMetaData(), foundDrugList);
			foundDrugSynList = reorderMapUsingOriginalMap(MetaData.getDrugSynonymMetaData(), foundDrugSynList);
		} catch (Exception e1) {
			log.info("reordering problem");
			log.error(e1.toString());
		}

		log.info("Found " + Main.foundDiseaseList.size() + " unique disease names in content.");

		Iterator diseaseIt = Main.foundDiseaseList.keySet().iterator();
		StringBuffer sb = new StringBuffer();
		while (diseaseIt.hasNext()) {// loop over diseases
			Integer diseaseId = (Integer) diseaseIt.next();
			String termValue = (String) MetaData.getDiseaseMetaData().get(diseaseId);
			sb.append(termValue + ",");
		}
		log.debug(sb.toString());

		log.info("Found " + Main.foundDiseaseSynList.size() + " unique disease synonyms in content.");
		Iterator diseaseSynIt = Main.foundDiseaseSynList.keySet().iterator();
		sb = new StringBuffer();
		while (diseaseSynIt.hasNext()) {// loop over diseases
			Integer diseaseSynId = (Integer) diseaseSynIt.next();
			String termValue = (String) MetaData.getDiseaseSynonymMetaData().get(diseaseSynId);
			sb.append(termValue + ",");
		}
		log.debug(sb.toString());

		log.info("Found " + Main.foundDrugList.size() + " unique drug names in content.");
		Iterator drugIt = Main.foundDrugList.keySet().iterator();
		sb = new StringBuffer();
		while (drugIt.hasNext()) {// loop over diseases
			Integer drugId = (Integer) drugIt.next();
			String termValue = (String) MetaData.getDrugMetaData().get(drugId);
			sb.append(termValue + ", ");
		}
		log.debug(sb.toString());
		log.info("Found " + Main.foundDrugSynList.size() + " unique drug synonyms in content.");
		drugIt = Main.foundDrugSynList.keySet().iterator();
		sb = new StringBuffer();
		while (drugIt.hasNext()) {// loop over diseases
			Integer drugId = (Integer) drugIt.next();
			String termValue = (String) MetaData.getDrugSynonymMetaData().get(drugId);
			sb.append(termValue + ", ");
		}
		isMetaDataFiltered = true;
	}

	/**
	 * Add PMID to document
	 * 
	 * @param doc
	 */
	/**
	 * Add PMID to document (parallelized with progress logging and timeouts)
	 * 
	 * @param doc
	 */
	public void addPMID(Document doc) {
		// Initialize NCBI rate limiter based on API key availability
		if (ncbiRateLimiter == null) {
			String apiKey = getConfigProperty("NCBI.API_KEY");
			boolean hasApiKey = (apiKey != null && !apiKey.trim().isEmpty());
			int requestsPerSecond = hasApiKey ? 10 : 3;
			ncbiRateLimiter = new Semaphore(requestsPerSecond);
			log.info("NCBI rate limiter initialized: " + requestsPerSecond + " requests/second" + 
				(hasApiKey ? " (API key detected)" : " (no API key - slower)"));
		}
		
		NodeList biblioNodes = null;
		try {
			biblioNodes = XPathAPI.selectNodeList(doc, "//bibliomixed");
		} catch (TransformerException e) {
			log.error("Error selecting bibliomixed nodes", e);
			return;
		}
		int biblioCount = (biblioNodes == null) ? 0 : biblioNodes.getLength();
		if (biblioCount == 0) {
			return;
		}

		// Prepare tasks: capture search params but do not mutate DOM in threads
		final ArrayList<Integer> taskIndexes = new ArrayList<Integer>();
		final ArrayList<String[]> taskParams = new ArrayList<String[]>();

		for (int j = 0; j < biblioCount; j++) {
			Node biblioNode = biblioNodes.item(j);

			// Skip if existing PubMedID present and non-empty
			Node checkBiblioIdNode = null;
			try {
				checkBiblioIdNode = XPathAPI.selectSingleNode(biblioNode, "./biblioid");
			} catch (TransformerException ignored) {
			}
			if (checkBiblioIdNode != null) {
				Node otherclassNode = checkBiblioIdNode.getAttributes().getNamedItem("otherclass");
				if (otherclassNode != null && "PUBMEDID".equalsIgnoreCase(otherclassNode.getNodeValue())) {
					if (checkBiblioIdNode.getTextContent() != null && checkBiblioIdNode.getTextContent().trim().length() > 0) {
						continue;
					}
				}
			}

			Node titleNode = null;
			Node volNode = null;
			Node pageNode = null;
			Node authorSurNameNode1 = null;
			Node authorSurNameNode2 = null;
			try {
				titleNode = XPathAPI.selectSingleNode(biblioNode, "./title | ./title/emphasis");
				volNode = XPathAPI.selectSingleNode(biblioNode, "./volumenum | ./volumenum/emphasis");
				pageNode = XPathAPI.selectSingleNode(biblioNode, "./artpagenums | ./artpagenums/emphasis");
				authorSurNameNode1 = XPathAPI.selectSingleNode(biblioNode, "./authorgroup/author//surname[0] | ./author//surname[0]");
				authorSurNameNode2 = XPathAPI.selectSingleNode(biblioNode, "./authorgroup/author//surname[1] | ./author//surname[1]");
			} catch (TransformerException te) {
				log.debug("XPath error reading bibliographic fields for index " + j, te);
			}

			if (titleNode != null && volNode != null && pageNode != null) {
				String title = titleNode.getTextContent();
				title = title.replaceAll("\\W", " ");
				title = title.replaceAll("\\Z", " ");
				title = title.trim();
				String volumeNum = volNode.getTextContent().trim();
				String pageNum = pageNode.getTextContent().trim();
				String auth1 = (authorSurNameNode1 != null) ? authorSurNameNode1.getTextContent() : null;
				String auth2 = (authorSurNameNode2 != null) ? authorSurNameNode2.getTextContent().replaceFirst("and ", "") : null;

				// normalize volume/page
				int end = volumeNum.indexOf(":");
				if (end >= 0) volumeNum = volumeNum.substring(0, end);
				end = volumeNum.indexOf("(");
				if (end >= 0) volumeNum = volumeNum.substring(0, end);
				end = volumeNum.indexOf("-");
				if (end >= 0) volumeNum = volumeNum.substring(0, end);
				end = volumeNum.indexOf(",");
				if (end >= 0) volumeNum = volumeNum.substring(0, end);

				end = pageNum.indexOf("&");
				if (end >= 0) pageNum = pageNum.substring(0, end);
				end = pageNum.indexOf("�");
				if (end >= 0) pageNum = pageNum.substring(0, end);
				end = pageNum.indexOf(".");
				if (end >= 0) pageNum = pageNum.substring(0, end);

				taskIndexes.add(j);
				taskParams.add(new String[] { title, volumeNum, pageNum, auth1, auth2 });
			}
		}

		int tasksTotal = taskParams.size();
		if (tasksTotal == 0) {
			log.info("No bibliography entries eligible for PMID lookup");
			return;
		}

		log.info("Starting parallel PMID lookup for " + tasksTotal + " bibliography entries");
		// Limit threads to avoid overwhelming NCBI rate limits (3-4 concurrent requests is safer)
		int threadCount = Math.min(4, Math.max(1, tasksTotal));
		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		List<Future<String>> futures = new ArrayList<Future<String>>(tasksTotal);

		for (int t = 0; t < tasksTotal; t++) {
			final String[] p = taskParams.get(t);
			Callable<String> c = new Callable<String>() {
				@Override
				public String call() {
					try {
						return searchPMID(p[0], p[1], p[2], p[3], p[4]);
					} catch (Throwable ex) {
						// return null on error
						return null;
					}
				}
			};
			futures.add(pool.submit(c));
		}

		pool.shutdown();

		// Progress logging loop - poll periodically
		try {
			int lastLoggedPercent = -1;
			long startTime = System.currentTimeMillis();
			while (!pool.isTerminated()) {
				int completed = 0;
				for (Future<String> f : futures) {
					if (f.isDone()) completed++;
				}
				int percent = (int) ((completed * 100.0) / tasksTotal);
				long elapsed = (System.currentTimeMillis() - startTime) / 1000;
				if (percent != lastLoggedPercent && (percent % 10 == 0 || percent == 100)) {
					log.info(String.format("PMID progress: %d/%d (%d%%) - %ds elapsed", 
						completed, tasksTotal, percent, elapsed));
					lastLoggedPercent = percent;
				}
				// wait briefly
				pool.awaitTermination(1, TimeUnit.SECONDS);
			}
		} catch (InterruptedException ie) {
			log.warn("PMID lookup progress thread interrupted", ie);
		}

		// Apply results back into DOM (single-threaded)
		int successCount = 0;
		for (int t = 0; t < tasksTotal; t++) {
			String pmid = null;
			try {
				pmid = futures.get(t).get();
			} catch (Exception e) {
				log.debug("Error getting PMID future result for task " + t, e);
			}
			if (pmid != null && pmid.trim().length() > 0 && !"Exception".equals(pmid)) {
				int nodeIndex = taskIndexes.get(t);
				Node biblioNode = biblioNodes.item(nodeIndex);

				org.w3c.dom.Node preNode = doc.createTextNode("[PMID: ");
				Element biblioidNode = doc.createElement("biblioid");
				biblioidNode.setAttribute("class", "other");
				biblioidNode.setAttribute("otherclass", "PubMedID");
				biblioidNode.setTextContent(pmid);
				org.w3c.dom.Node postNode = doc.createTextNode("]");

				try {
					biblioNode.appendChild(preNode);
					biblioNode.appendChild(biblioidNode);
					biblioNode.appendChild(postNode);
					successCount++;
				} catch (DOMException dome) {
					try {
						if (biblioNode.hasChildNodes()) {
							// attempt safe append
							biblioNode.appendChild(biblioidNode);
							successCount++;
						}
					} catch (Exception ignore) {
					}
				}
			}
		}

		log.info(String.format("PMID search complete: %d/%d PMIDs found and added", successCount, tasksTotal));
	}

	/**
	 * Search for PMID using Entrez Search utilities
	 * 
	 * @param title
	 * @param volumeNum
	 * @param pageNum
	 * @param auth1
	 * @param auth2
	 * @return PMID as String
	 */
	String searchPMID(String title, String volumeNum, String pageNum, String auth1, String auth2) {
		String pmid = new_SearchPMID(title, volumeNum, pageNum, auth1, auth2);
		if ("Exception".equals(pmid)) {
			log.info("Repeating search for PMID...");
			try {
				Thread.sleep(1000);
			} catch (Exception ignored) {
			}
			pmid = new_SearchPMID(title, volumeNum, pageNum, auth1, auth2);
			if ("Exception".equals(pmid)) {
				log.info("PMID search failed.");
			}
		}
		return pmid;
	}

	public String new_SearchPMID(String title, String volumeNum, String pageNum, String auth1, String auth2) {
		// Enforce NCBI rate limiting
		try {
			ncbiRateLimiter.acquire();
			// Release permit after 1 second (rate limit window)
			new Thread(() -> {
				try {
					Thread.sleep(1000);
					ncbiRateLimiter.release();
				} catch (InterruptedException ignored) {
				}
			}).start();
		} catch (InterruptedException e) {
			log.warn("Rate limiter interrupted", e);
		}
		
		String apiKey = getConfigProperty("NCBI.API_KEY");
		String url = null;
		String pmid = null;
		try {
			String term = title + "+AND+" + volumeNum + "[vi]+AND+" + pageNum + "[pg]";
			if (auth1 != null)
				term = term + "+AND+" + auth1 + "[au]";

			if (auth2 != null)
				term = term + "+AND+" + auth2 + "[au]";
			url = getConfigProperty("NCBI.EUTILITIES_URL") + URIUtil.encodeQuery("?db=pubmed&term=" + term);
			
			if (apiKey != null && !apiKey.trim().isEmpty())
				url += "&api_key=" + apiKey;
				
			URL u = new URL(url);

			HttpURLConnection huc = (HttpURLConnection) u.openConnection();
			huc.setDoInput(true);
			huc.setRequestMethod("GET");
			// Prevent indefinite hangs when NCBI is slow/unresponsive
			huc.setConnectTimeout(5000); // 5s connect timeout
			huc.setReadTimeout(10000);   // 10s read timeout
			huc.connect();
			InputStream is = huc.getInputStream();
			int code = huc.getResponseCode();

			if (code != HttpStatus.SC_OK) {
				log.error("Invalid response for URL = '" + url + "'");
				return null;
			}
			String responseBody = StringUtil.getStringFromInputStream(is);
			Document resDoc = XMLUtil.getDocument(responseBody, false);
			Node countNode = XPathAPI.selectSingleNode(resDoc, "eSearchResult/Count");
			if (countNode != null) {
				String strCount = countNode.getTextContent();
				int count = Integer.parseInt(strCount);
				if (count == 1) {
					Node idNode = XPathAPI.selectSingleNode(resDoc, "eSearchResult/IdList/Id");
					if (idNode != null) {
						String id = idNode.getTextContent();
						log.info("Found PMID: " + id);
						return id;
					}
				}
			} else {
				log.error("Invalid XML response from pubmed: (expected: eSearchResult/IdList/Id)\n" + responseBody);
			}
			huc.disconnect();
		} catch (IOException e) {
			log.error("Unable to connect to URL = '" + url + "'", e);
			log.equals(e.getMessage());
			pmid = "Exception";
		} catch (RISParserException e) {
			log.error("Unable to parse output response for URL = '" + url + "'");
			log.equals(e.getMessage());
			pmid = "Exception";
		} catch (TransformerException e) {
			log.error("Unable to parse output response for URL = '" + url + "'");
			log.equals(e.getMessage());
			pmid = "Exception";
		}
		return pmid;
	}

	SimpleDateFormat log_sdf = new SimpleDateFormat("MM-dd-yyyy [hh:mm:ss a]");

	public void printLine(String note) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("c:\\logs\\pubmed-search.log", true));
			out.write(log_sdf.format(new Date()) + " INFO: " + note + "\n");
			out.close();
		} catch (Exception ignored) {
		}
	}

	/**
	 * @param doc
	 * @param contentFileName
	 */
	public static void softWriteProcessedContent(Document doc, String contentFileName) {
		try {

			Properties xmlOutProps = new Properties();
			xmlOutProps.setProperty(OutputKeys.METHOD, "xml");
			xmlOutProps.setProperty(OutputKeys.INDENT, "yes");
			xmlOutProps.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			xmlOutProps.setProperty(OutputKeys.ENCODING, System.getProperty("file.encoding"));

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperties(xmlOutProps);
			File srcFile = new File(contentFileName);
			Writer writer = new OutputStreamWriter(new FileOutputStream(contentFileName), System.getProperty("file.encoding"));

			try {
				transformer.transform(new DOMSource(doc), new StreamResult(writer));
			} catch (NullPointerException npe) {
				System.setProperty("javax.xml.transform.TransformerFactory", "com.icl.saxon.TransformerFactoryImpl");
				transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperties(xmlOutProps);
				transformer.transform(new DOMSource(doc), new StreamResult(writer));
			}
			log.debug("Processed content = " + contentFileName);
		} catch (Exception exp) {
			log.error("Unable to write content file");
			log.error(exp.toString());
		}
	}

	/**
	 * @param doc
	 * @param contentFileName
	 */
	public static void writeProcessedContent(Document doc, String contentFileName) {
		try {
			Properties xmlOutProps = new Properties();
			xmlOutProps.setProperty(OutputKeys.METHOD, "xml");
			xmlOutProps.setProperty(OutputKeys.INDENT, "yes");
			xmlOutProps.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			xmlOutProps.setProperty(OutputKeys.DOCTYPE_SYSTEM, getConfigProperty("RIS.RITTENHOUSE_DTD_PATH") + "RittDocBook.dtd");
			xmlOutProps.setProperty(OutputKeys.ENCODING, System.getProperty("file.encoding"));

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperties(xmlOutProps);
			File srcFile = new File(contentFileName);
			Writer writer = new OutputStreamWriter(new FileOutputStream(contentFileName), System.getProperty("file.encoding"));

			try {
				transformer.transform(new DOMSource(doc), new StreamResult(writer));
			} catch (NullPointerException npe) {
				System.setProperty("javax.xml.transform.TransformerFactory", "com.icl.saxon.TransformerFactoryImpl");
				transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperties(xmlOutProps);
				transformer.transform(new DOMSource(doc), new StreamResult(writer));
			}
			log.debug("Processed content = " + contentFileName);
		} catch (Exception exp) {
			log.error("Unable to write content file");
			log.error(exp.toString());
		}
	}

	/**
	 * Remove the RIS Index tags for a particular rule
	 * 
	 * @param xpath
	 * @param doc
	 */
	public void removeTags(String xpath, Document doc) {
		try {
			NodeList risIndexNodes = XPathAPI.selectNodeList(doc, xpath);
			Node risIndexNode = null;
			Node sect1InfoNode = null;
			for (int j = 0; j < risIndexNodes.getLength(); j++) {
				risIndexNode = risIndexNodes.item(j);
				sect1InfoNode = risIndexNode.getParentNode();
				sect1InfoNode.removeChild(risIndexNode);
			}
		} catch (Exception exp) {
			log.error("Error removing tags for xpath = " + xpath);
			log.error(exp.getMessage());
			exp.printStackTrace();
		}
	}

	/**
	 * Executes content tagging rule 1
	 * 
	 * @param doc
	 * @param ruleId
	 * @deprecated
	 * @see com.rittenhouse.RIS.Main.executeTaggingRules(Document)
	 */
	private void executeRule1(Document doc, String ruleId) {
		int count = 0;
		NodeIterator titleNodeIt = null;
		Node titleNode = null;
		String bookIsbn = null;
		try {
			Node isbnNode = XPathAPI.selectSingleNode(doc, "/book/bookinfo/isbn");
			bookIsbn = isbnNode.getFirstChild().getNodeValue();

			titleNodeIt = XPathAPI.selectNodeIterator(doc, Main.XPATH_TITLE);
		} catch (TransformerException e) {
			log.error(e.toString());
		} catch (DOMException de) {
			log.error(de.toString());
		}
		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);
		threadPool.createThreads(THREAD_COUNT_MAXIMUM);

		while ((titleNode = titleNodeIt.nextNode()) != null) {
			count++;
			try {
				DiseaseRISIndexThread aThread = new DiseaseRISIndexThread(doc, titleNode, ruleId, bookIsbn);
				threadPool.execute(aThread);

				DiseaseSynonymRISIndexThread bThread = new DiseaseSynonymRISIndexThread(doc, titleNode, ruleId, bookIsbn);
				threadPool.execute(bThread);

				DrugRISIndexThread cThread = new DrugRISIndexThread(doc, titleNode, ruleId, bookIsbn);
				threadPool.execute(cThread);

				DrugSynonymRISIndexThread dThread = new DrugSynonymRISIndexThread(doc, titleNode, ruleId, bookIsbn);
				threadPool.execute(dThread);
			} catch (InterruptedException e1) {
			}

		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(1000);
				}
			} catch (Exception e1) {
			}
		}
	}

	/**
	 * Executes content tagging rules
	 * 
	 * @param doc
	 * @param ruleId
	 */
	public void executeTaggingRules(Document doc) {
		long start = System.currentTimeMillis();
		int count = 0;
		NodeList chapterNodes = null;
		Node titleNode = null;
		try {
			chapterNodes = XPathAPI.selectNodeList(doc, "//chapter");
		} catch (TransformerException e) {
			log.error(e.toString());
		} catch (DOMException de) {
			log.error(de.toString());
		}

		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);// threads timeout
		// after inactivity
		threadPool.createThreads(THREAD_COUNT_MAXIMUM);
		int chapCount = chapterNodes.getLength();
		// Loop 1 for rules 1-3

		for (int j = 0; j < chapCount; j++) {
			Node chapNode = chapterNodes.item(j);
			try {
				RISIndexThread indexThread = new RISIndexThread(doc, chapNode, bookISBN);
				threadPool.execute(indexThread);
			} catch (InterruptedException e1) {
				log.error(e1.toString());
			}
		}

		threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
		try {
			int x = 0;
			threadPool.awaitTerminationAfterShutdown();
			while (!threadPool.isTerminatedAfterShutdown()) {
				Thread.sleep(100);
				x += 1;
				log.debug("After shut loop attempt" + x);
			}
		} catch (Exception e1) {
			log.error(e1.toString());
		}

		// end rule 1-3
	}

	/**
	 * Executes content tagging rules
	 * 
	 * @param doc
	 * @param ruleId
	 */
	public void executeKeywordTaggingRules(Document doc) {
		int count = 0;
		NodeList chapterNodes = null;
		Node titleNode = null;
		try {
			chapterNodes = XPathAPI.selectNodeList(doc, "//chapter");
		} catch (TransformerException e) {
			log.error(e.toString());
		} catch (DOMException de) {
			log.error(de.toString());
		}
		
		// Skip keyword database operations if database is disabled
		if (!skipAllDatabaseOperations) {
			KeywordDB keyDb = new KeywordDB();
			keyDb.removeKeywordResources(bookISBN);
		}
		
		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);// threads timeout
		// after inactivity
		threadPool.createThreads(THREAD_COUNT_MAXIMUM);
		int chapCount = chapterNodes.getLength();
		// Loop 1 for rules 1-3

		for (int j = 0; j < chapCount; j++) {
			Node chapNode = chapterNodes.item(j);
			try {
				KeywordIndexThread keywordThread = new KeywordIndexThread(doc, chapNode, bookISBN);
				threadPool.execute(keywordThread);
			} catch (InterruptedException e1) {
				log.error(e1.toString());
			}
		}

		threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
		try {
			threadPool.awaitTerminationAfterShutdown();
			while (!threadPool.isTerminatedAfterShutdown()) {
				Thread.sleep(1000);
			}
		} catch (Exception e1) {
			log.error(e1.toString());
		}
	}

	/**
	 * Add Links for disease names and synonyms
	 * 
	 * @param doc
	 *            DOM document
	 */
	public void linkDisease(Document doc) {
		NodeIterator paraNodeIt = null;
		try {
			paraNodeIt = XPathAPI.selectNodeIterator(doc, XPATH_LINKING);
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), LINKING_THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);
		threadPool.createThreads(LINKING_THREAD_COUNT_MAXIMUM);

		int count = 0;
		if (paraNodeIt != null) {
			Node paraNode = null;
			while ((paraNode = paraNodeIt.nextNode()) != null) {
				count++;
			}
		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(1000);
				}
			} catch (Exception e1) {
			}
		}

		paraNodeIt = null;
		threadPool = null;

		try {
			paraNodeIt = XPathAPI.selectNodeIterator(doc, XPATH_LINKING);
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		threadPool = new PooledExecutor(new LinkedQueue(), LINKING_THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);
		threadPool.createThreads(LINKING_THREAD_COUNT_MAXIMUM);

		count = 0;
		if (paraNodeIt != null) {
			Node paraNode = null;
			while ((paraNode = paraNodeIt.nextNode()) != null) {
				count++;
			}
		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(1000);
				}
			} catch (Exception e1) {
			}
		}
	}

	/**
	 * Adds drug links to document
	 * 
	 * @param doc
	 */
	public void linkDrug(Document doc) {
		NodeIterator paraNodeIt = null;
		try {
			paraNodeIt = XPathAPI.selectNodeIterator(doc, XPATH_LINKING);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), LINKING_THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(THREAD_KEEP_ALIVE_TIME);
		threadPool.createThreads(LINKING_THREAD_COUNT_MAXIMUM);
		int count = 0;
		if (paraNodeIt != null) {
			Node paraNode = null;
			while ((paraNode = paraNodeIt.nextNode()) != null) {
				count++;
			}
		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(1000);
				}
			} catch (Exception e1) {
			}
		}
	}

	/**
	 * Add RIS Info tags to content
	 * 
	 * @return success or failure
	 */
	public boolean addRISInfoTags() {
		String sourceDirName = CURRENT_DIR + FILE_SEPARATOR + getConfigProperty("RIS.CONTENT_TEMP");
		ArrayList files = FileUtil.getXMLFiles(getConfigProperty("RIS.CONTENT_TEMP"), true);
		
		if (phaseLogger != null) {
			phaseLogger.progress("Adding RIS info tags to " + files.size() + " file(s)");
		}
		
		for (int i = 0; i < files.size(); i++) {
			String inFileName = null;
			try {
				inFileName = ((File) files.get(i)).getCanonicalPath();
				String outFileName = inFileName + ".risinfo";
				FileUtil.copyFile(inFileName, outFileName);
				XMLUtil.transformSaxon(inFileName, XSLT_RISINFO, outFileName, null, null);
				FileUtil.copyFile(outFileName, inFileName);
				log.info("Applied <risinfo> tags to - " + inFileName);
			} catch (RISTransformerException e) {
				String errorMsg = String.format("Failed adding <risinfo> tags to: %s\nXSLT: %s\nError: %s", 
					inFileName, XSLT_RISINFO, e.getMessage());
				if (phaseLogger != null) {
					phaseLogger.phaseError(errorMsg, e);
				} else {
					log.error("Failed adding <risinfo> tags to - " + inFileName);
					log.error("XSLT ERRORS:\n" + e.toString());
				}
				return false;
			} catch (IOException ioe) {
				String errorMsg = String.format("IO error adding <risinfo> tags to: %s\nError: %s", 
					inFileName, ioe.getMessage());
				if (phaseLogger != null) {
					phaseLogger.phaseError(errorMsg, ioe);
				} else {
					log.error("Failed adding <risinfo> tags to - " + inFileName);
					log.error(ioe.toString());
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Validate cross-document references (IDREF/linkend attributes)
	 * Collects all IDs from all XML files, then verifies all references point to existing IDs
	 * This catches broken links that DTD validation misses when files are split
	 */
	private boolean validateCrossReferences(ArrayList files) {
		// Phase 1: Collect all IDs from all files
		HashSet<String> allIds = new HashSet<String>();
	DocumentBuilderFactory dfactory = null;
	try {
		// Use the internal JDK parser to avoid classpath conflicts with multiple Xerces versions
		dfactory = DocumentBuilderFactory.newInstance("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl", null);
		log.debug("Using internal JDK XML parser for validation");
	} catch (Exception e) {
		log.warn("Could not load internal JDK parser, using default: " + e.getMessage());
		dfactory = DocumentBuilderFactory.newInstance();
	}
	
	dfactory.setNamespaceAware(true);
	dfactory.setValidating(false);
	try {
		dfactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	} catch (Exception e) {
		log.debug("Could not set XML feature (may not be supported by this parser): " + e.getMessage());
	}
	
	log.info("Validating cross-document references across " + files.size() + " files");
	
	for (int i = 0; i < files.size(); i++) {
		try {
			File file = (File) files.get(i);
				DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
				// Set EntityResolver to redirect DTD references to local files
				docBuilder.setEntityResolver(new ChunkResolver(file.getParent()));
				Document doc = docBuilder.parse(file);
				
				// Collect all elements with 'id' attribute
				org.w3c.dom.NodeList allElements = doc.getElementsByTagName("*");
				for (int j = 0; j < allElements.getLength(); j++) {
					org.w3c.dom.Element elem = (org.w3c.dom.Element) allElements.item(j);
					String idValue = elem.getAttribute("id");
					if (idValue != null && !idValue.isEmpty()) {
						allIds.add(idValue);
					}
				}
			} catch (Exception e) {
				log.warn("Could not collect IDs from " + ((File)files.get(i)).getName() + ": " + e.getMessage());
			}
		}
		
		log.info("Collected " + allIds.size() + " unique IDs from all content files");
		
		// Phase 2: Check all references
		ArrayList<String> brokenLinks = new ArrayList<String>();
		for (int i = 0; i < files.size(); i++) {
			try {
				File file = (File) files.get(i);
				DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
				// Set EntityResolver to redirect DTD references to local files
				docBuilder.setEntityResolver(new ChunkResolver(file.getParent()));
				Document doc = docBuilder.parse(file);
				
				// Check all 'linkend' attributes (DocBook cross-references)
				org.w3c.dom.NodeList linkElements = doc.getElementsByTagName("*");
				for (int j = 0; j < linkElements.getLength(); j++) {
					org.w3c.dom.Element elem = (org.w3c.dom.Element) linkElements.item(j);
					String linkend = elem.getAttribute("linkend");
					if (linkend != null && !linkend.isEmpty()) {
						if (!allIds.contains(linkend)) {
							String brokenLink = file.getName() + " -> " + linkend + " (element: " + elem.getTagName() + ")";
							brokenLinks.add(brokenLink);
							log.error("BROKEN REFERENCE: " + brokenLink);
						}
					}
				}
			} catch (Exception e) {
				log.warn("Could not validate references in " + ((File)files.get(i)).getName() + ": " + e.getMessage());
			}
		}
		
		if (brokenLinks.size() > 0) {
			String errorMsg = "Found " + brokenLinks.size() + " broken cross-references:\n";
			for (int i = 0; i < Math.min(10, brokenLinks.size()); i++) {
				errorMsg += "  - " + brokenLinks.get(i) + "\n";
			}
			if (brokenLinks.size() > 10) {
				errorMsg += "  ... and " + (brokenLinks.size() - 10) + " more";
			}
			
			if (phaseLogger != null) {
				phaseLogger.phaseError(errorMsg);
			}
			log.error(errorMsg);
			return false;
		}
		
		log.info("All cross-document references validated successfully");
		return true;
	}

	/**
	 * Split content files
	 */
	public boolean splitContentFiles() {
		String sourceDirName = CURRENT_DIR + FILE_SEPARATOR + getConfigProperty("RIS.CONTENT_TEMP");
		ArrayList files = FileUtil.getXMLFiles(getConfigProperty("RIS.CONTENT_TEMP"), true);

		if (phaseLogger != null) {
			phaseLogger.progress("Splitting/chunking " + files.size() + " content file(s)");
		}

		// First, validate all cross-document references (linkend attributes)
		// This catches broken links before we proceed with chunking
		try {
			if (!validateCrossReferences(files)) {
				log.error("Cross-reference validation failed - aborting split");
				return false;
			}
		} catch (Exception e) {
			log.error("===========================================");
			log.error("EXCEPTION during cross-reference validation:");
			log.error("Exception type: " + e.getClass().getName());
			log.error("Exception message: " + e.getMessage());
			log.error("Stack trace:", e);
			log.error("===========================================");
			return false;
		}

		DocumentBuilderFactory dfactory = null;
		try {
			// Use the internal JDK parser to avoid classpath conflicts
			dfactory = DocumentBuilderFactory.newInstance("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl", null);
			log.debug("Using internal JDK XML parser for content splitting");
		} catch (Exception e) {
			log.warn("Could not load internal JDK parser, using default: " + e.getMessage());
			dfactory = DocumentBuilderFactory.newInstance();
		}
		
		dfactory.setNamespaceAware(true);
		
		// Disable DTD validation during chunking because:
		// 1. Content was already validated during XSLT transformation phase (Saxon validates)
		// 2. Cross-document references are validated separately above
		// 3. DTD validation fails on cross-file IDREF attributes which are legitimate
		dfactory.setValidating(false);
		
		// Prevent parser from attempting HTTP connections to fetch external DTDs
		try {
			dfactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			dfactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dfactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		} catch (Exception e) {
			log.warn("Could not disable external entity features: " + e.getMessage());
		}
		
		DocumentBuilder docBuilder = null;
		String inFileName = null;
		File inFile = null;
		InputSource in = null;
		String baseDestDirName = getConfigProperty("RIS.CONTENT_TEMP");
		
		// Entity files are already in place at test/temp/ent/ from initial setup
		// They persist across clean operations and never need copying
		
		for (int i = 0; i < files.size(); i++) {
			try {
				inFile = (File) files.get(i);
				inFileName = inFile.getCanonicalPath();
				inFile = new File(inFileName);
				in = new InputSource(new FileInputStream(inFile));
				Document doc = null;

				docBuilder = dfactory.newDocumentBuilder();
				// Set EntityResolver to redirect DTD references to local files
				docBuilder.setEntityResolver(new ChunkResolver(inFile.getParent()));
				RISErrorHandler risErr = new RISErrorHandler();
				docBuilder.setErrorHandler(risErr);
				doc = docBuilder.parse(in);
				in = null;
				String bookIsbn = bookISBN;
				String outFileName = baseDestDirName + FILE_SEPARATOR + "book." + bookIsbn + ".xml";
				generateTOC(inFile.getCanonicalPath(), baseDestDirName + FILE_SEPARATOR + "toc." + bookIsbn + ".xml", bookIsbn);

				outFileName = inFileName + ".chunked";
				log.info("Chunking content file - " + inFileName);
				FileUtil.copyFile(inFileName, inFileName + ".risinfo");
				XMLUtil.transformSaxon(inFileName, XSLT_CHUNKER, outFileName, new String[] { "basedir" }, new String[] { baseDestDirName + FILE_SEPARATOR });
				FileUtil.copyFile(outFileName, inFileName);
			} catch (RISTransformerException e) {
				String errorMsg = String.format("Failed splitting content file: %s\nXSLT: %s\nError: %s", 
					inFileName, XSLT_CHUNKER, e.getMessage());
				if (phaseLogger != null) {
					phaseLogger.phaseError(errorMsg, e);
				} else {
					log.error("Failed splitting content file - " + inFileName);
					log.error(e.toString());
				}
				return false;
			} catch (IOException ioe) {
				String errorMsg = String.format("IO error splitting content file: %s\nError: %s", 
					inFileName, ioe.getMessage());
				if (phaseLogger != null) {
					phaseLogger.phaseError(errorMsg, ioe);
				} else {
					log.error("Failed splitting content file - " + inFileName);
					log.error(ioe.toString());
				}
				return false;
			} catch (SAXException e1) {
				String errorMsg = String.format("XML parsing error splitting content file: %s\nError: %s\nDetails: Check for missing ID references or malformed XML", 
					inFileName, e1.getMessage());
				if (phaseLogger != null) {
					phaseLogger.phaseError(errorMsg, e1);
				} else {
					log.error("Failed splitting content file for - " + inFileName);
					log.error(e1.toString());
				}
				return false;
			} catch (ParserConfigurationException e) {
				String errorMsg = String.format("Parser configuration error splitting content file: %s\nError: %s", 
					inFileName, e.getMessage());
				if (phaseLogger != null) {
					phaseLogger.phaseError(errorMsg, e);
				} else {
					log.error("Failed splitting content file for - " + inFileName);
					log.error(e.toString());
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Generate book TOC file name
	 * 
	 * @param bookFileName
	 * @param tocFileName
	 * @param bookIsbn
	 * @return
	 */
	public boolean generateTOC(String bookFileName, String tocFileName, String bookIsbn) {
		try {
			XMLUtil.transformSaxon(bookFileName, XSLT_TOC_GENERATOR, tocFileName, null, null);
			log.info("Generated TOC for book - " + bookIsbn);
		} catch (RISTransformerException e) {
			log.error("Failed generating TOC for book - " + bookIsbn);
			log.error(e.toString());
			return false;
		}
		return true;
	}

	/**
	 * Load content to XML data store and update resource meta-data in database
	 * 
	 * @param bookIsbn
	 */
	public void loadContent(String bookIsbn) {
		String sourceDirName = getConfigProperty("RIS.CONTENT_TEMP");
		File sourceDir = new File(sourceDirName);
		int tiGloballyAccessible = 0;

		// BJW--Modified 2012-02-17 to allow fo skipping textml and instead dumping the content files into 
		// other destintation folders.

		if (!skipAllDatabaseOperations && !"true".equalsIgnoreCase(getConfigProperty("RIS.SKIP_TEXTML"))) {
			log.info("Loading content to TextML and database...");
			try {
				ResourceDB resDB = new ResourceDB();
				String collectionName = resDB.getBookCollectionName(bookIsbn);

				if (collectionName == null) {
					collectionName = Main.getConfigProperty("XMLDB.DEFAULT_COLLECTION_NAME");
				}

				XMLDataStore xmlDataStore = new XMLDataStore();
				xmlDataStore.connect();
				xmlDataStore.setDocuments(sourceDir, true, collectionName);
				xmlDataStore.disconnect();
				
				if (!xmlDataStore.isLoadError()) {
					try {
						loadContentToDB(sourceDir, tiGloballyAccessible);
					} catch (Exception e) {
						log.error("Error loading content to database: " + e.getMessage(), e);
					}
				} else {
					log.error("XML load to TextML failed. Skipping writing resource to database.");
				}
			} catch (NoClassDefFoundError e) {
				log.warn("TextML library not available. Set RIS.SKIP_TEXTML=true to suppress this warning.");
				log.debug("Missing class: " + e.getMessage());
				// Still update database even if TextML fails
				log.info("Attempting to update resource in database despite TextML failure...");
				try {
					loadContentToDB(sourceDir, tiGloballyAccessible);
				} catch (Exception dbEx) {
					log.error("Error loading content to database: " + dbEx.getMessage(), dbEx);
				}
			} catch (Exception e) {
				// Check if the root cause is ClassNotFoundException
				Throwable cause = e;
				boolean isClassNotFound = false;
				while (cause != null) {
					if (cause instanceof ClassNotFoundException) {
						isClassNotFound = true;
						break;
					}
					cause = cause.getCause();
				}
				
				if (isClassNotFound) {
					log.warn("TextML library not available. Set RIS.SKIP_TEXTML=true to suppress this warning.");
					log.debug("Missing class details: " + e.getMessage());
					// Still update database even if TextML fails
					log.info("Attempting to update resource in database despite TextML failure...");
					try {
						loadContentToDB(sourceDir, tiGloballyAccessible);
					} catch (Exception dbEx) {
						log.error("Error loading content to database: " + dbEx.getMessage(), dbEx);
					}
				} else {
					log.error("Error loading content to TextML: " + e.getMessage(), e);
				}
			}
		} else if ("true".equalsIgnoreCase(getConfigProperty("RIS.SKIP_TEXTML"))) {
			log.info("Skipping TextML loading (RIS.SKIP_TEXTML=true)");
		} else if (skipAllDatabaseOperations) {
			log.info("[NO-DB MODE] Skipping TextML and database loading");
		}
		if ("true".equals(getConfigProperty("RIS.LOAD_CONTENT_TO_NON_TEXTML_PATH"))) {
			log.info("\n\n\nhere\n\n\n");
			String destRoot = getConfigProperty("RIS.DEST_NON_TEXTML_CONTENT_PATH");
			if (destRoot != null && !"".equals(destRoot)) {
				File rootFolder = new File(destRoot);
				if (rootFolder.isDirectory()) {
					// clean destination directory
					FileUtil.cleanDir(new File(rootFolder.getAbsolutePath() + "/" + bookIsbn), true);
					
					log.info("Copying content files to: " + rootFolder);
					File xmlFolder = new File(rootFolder.getAbsolutePath() + "/" + bookIsbn + "/xml");
					if (!xmlFolder.exists())
						xmlFolder.mkdir();

					File[] files = new File(sourceDirName).listFiles();
					for (File file : files) {
						if (file.isFile() && file.getName().toLowerCase().endsWith(".xml")) {
							log.info("Copying file: " + file.getAbsolutePath() + " to " + xmlFolder.getAbsolutePath() + "/" + file.getName());
							FileUtil.copyFile(file.getAbsolutePath(), xmlFolder.getAbsolutePath() + "/" + file.getName());
						}
					}

				// Also copy the book.{isbn}.xml file from the ISBN subdirectory
				File isbnSubDir = new File(sourceDirName + FILE_SEPARATOR + bookIsbn);
				if (isbnSubDir.exists() && isbnSubDir.isDirectory()) {
					File bookXmlFile = new File(isbnSubDir, "book." + bookIsbn + ".xml");
					if (bookXmlFile.exists()) {
						log.info("Copying book file: " + bookXmlFile.getAbsolutePath() + " to " + xmlFolder.getAbsolutePath() + "/" + bookXmlFile.getName());
						FileUtil.copyFile(bookXmlFile.getAbsolutePath(), xmlFolder.getAbsolutePath() + "/" + bookXmlFile.getName());
					} else {
						log.warn("Book file not found: " + bookXmlFile.getAbsolutePath());
					}
				}

			File imagesFolder = new File(rootFolder.getAbsolutePath() + "/" + bookIsbn + "/images");
			if (!imagesFolder.exists())
				imagesFolder.mkdir();

			files = new File(getConfigProperty("RIS.CONTENT_IN") + "/multimedia").listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						log.info("Copying file: " + file.getAbsolutePath() + " to " + imagesFolder.getAbsolutePath() + "/" + file.getName());
						FileUtil.copyFile(file.getAbsolutePath(), imagesFolder.getAbsolutePath() + "/" + file.getName());
					}
				}
			}

		} else {
			System.out.println(destRoot + " is NOT a folder! Modify/Correct RIS.DEST_NON_TEXTML_CONTENT_PATH property");
			log.error(destRoot + " is NOT a folder! Modify/Correct RIS.DEST_NON_TEXTML_CONTENT_PATH property");
		}
	} else {
		System.out.println("Missing required configuration property: RIS.DEST_NON_TEXTML_CONTENT_PATH");
		log.error("Could not copy content files because of missing required property: RIS.DEST_NON_TEXTML_CONTENT_PATH");
	}
			if (!skipAllDatabaseOperations) {
				try {
					loadContentToDB(sourceDir, tiGloballyAccessible);
				} catch (Exception e) {
					System.out.println("Problems Loading Database");
					e.printStackTrace();
				}
			} else {
				log.info("[NO-DB MODE] Skipping database resource creation (from non-TextML path)");
				log.info("[NO-DB MODE] In production, this would update tResource table with book metadata");
			}
		}
		
		// Add logging when skipping ALL TextML and database operations
		if (skipAllDatabaseOperations) {
			log.info("[NO-DB MODE] === Database Operations Summary ===");
			log.info("[NO-DB MODE] All database connections and operations were skipped");
			log.info("[NO-DB MODE] In production, this would:");
			log.info("[NO-DB MODE]   - Connect to TextML server (Ixia DITA CMS)");
			log.info("[NO-DB MODE]   - Upload processed XML to document collection");
			log.info("[NO-DB MODE]   - Create resource entry in tResource table");
			log.info("[NO-DB MODE]   - Update resource metadata and status");
			log.info("[NO-DB MODE] Content files remain in: " + sourceDirName);
		}
	}

	/**
	 * Load content information to Database
	 * <p>
	 * </p>
	 * 
	 * @param sourceDir
	 * @param tiGloballyAccessible
	 */
	public void loadContentToDB(File sourceDir, int tiGloballyAccessible) {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = null;
				try {
					file = new File(dir.getCanonicalPath() + Main.FILE_SEPARATOR + name);
				} catch (IOException e) {
				}
				return ((name.startsWith("book.") && name.endsWith(".xml")) || file.isDirectory());
			}
		};
		try {
			// Skip database operations if flag is set
			if (skipAllDatabaseOperations) {
				log.info("Skipping database content loading (skipAllDatabaseOperations=true)");
				return;
			}
			
			ResourceDB resourceDB = new ResourceDB();
			String pubIdStr = null;
			if (pubName != null) {
				int pubId = resourceDB.addNewPublisher(pubName, "", "", "", "");
				pubIdStr = String.valueOf(pubId);
			}
			Integer resourceId = resourceDB.getResourceInfo(bookISBN, false);
			if (resourceId == null) {
				log.info("Adding new resource with ISBN = " + bookISBN);
				resourceDB.addNewResource(bookISBN, bookTitle, bookTitle, authors, pubIdStr, 1, "0.0", 1, 0, 8, pubDate, edition, copyRightStr, tiGloballyAccessible);
			} else {
				int tiResourceReady = 1;
				int tiAllowSubscriptions = 0;
				int iResourceStatusId = 8;
				String vchUpdaterId = "risbackend";
				String vchResourceSubTitle = bookTitle;
				String dtRISReleaseDate = null;
				int tiBrandonHillStatus = 1;
				String vchResourceNLMCall = null;
				double decResourcePrice = 0, decPayPerView = 0, decSubScriptionPrice = 0;
				String vchResourceImageName = null;
				log.info("Updating existing resource with ISBN = " + bookISBN);
				resourceDB.updateResource(resourceId, bookTitle, bookTitle, vchResourceSubTitle, authors, pubName, dtRISReleaseDate, pubDate, tiBrandonHillStatus, vchResourceNLMCall, bookISBN, edition, decResourcePrice, decPayPerView, decSubScriptionPrice, vchResourceImageName, tiResourceReady, tiAllowSubscriptions, copyRightStr);
			}
			if (bookISBN.length() > 2)
				System.out.println("==== BOOK IS " + bookISBN + " ====");
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Starts log4j logger
	 */
	public void startLogger() {
		// find log4j.xml
		String log4j = System.getProperty("log4j.configuration");
		if (log4j == null) {
			if (isInDevelopmentMode) {
				log4j = CURRENT_DIR + File.separatorChar + "bin" + File.separatorChar + "log4j.xml";
			} else {
				log4j = CURRENT_DIR + File.separatorChar + "log4j.xml";
			}
			File lf = new File(log4j);
			if (lf.canRead())
				System.setProperty("log4j.configuration", lf.toURI().toASCIIString());
		}
		log = Category.getInstance(Main.class.getName());
	}

	/**
	 * Starts log4j logger
	 */
	public void startBookLogger(String isbn) {
		String log4j = System.getProperty("log4j.configuration");
		try {
			// make sure that loging is already on.
			if (log4j == null) {
				if (isInDevelopmentMode) {
					log4j = CURRENT_DIR + File.separatorChar + "\\bin\\log4j.xml";
				} else {
					log4j = CURRENT_DIR + File.separatorChar + "log4j.xml";
				}
				File lf = new File(log4j);
				if (lf.canWrite())
					System.setProperty("log4j.configuration", lf.toURI().toASCIIString());
				log = Category.getInstance(Main.class.getName());

			}

			WriterAppender tmpwrit = (WriterAppender) log.getParent().getAppender("ris.backend");

			String outFileName = null;
			if (isInDevelopmentMode) {
				outFileName = CURRENT_DIR + File.separatorChar + "logs" + File.separatorChar + isbn + "-RisBackend.log";
			} else {
				outFileName = CURRENT_DIR + File.separatorChar + "..\\logs" + File.separatorChar + isbn + "-RisBackend.log";
			}
			Writer writer = new OutputStreamWriter(new FileOutputStream(outFileName), System.getProperty("file.encoding"));
			Layout tmpLayout = tmpwrit.getLayout();
			tmpwrit.setImmediateFlush(true);
			log.info("Closing logger");
			logOrig = tmpwrit;
			log.getParent().removeAppender(tmpwrit);
			tmpwrit = new WriterAppender(tmpLayout, writer);
			tmpwrit.setName("ris.backend");
			log.getParent().addAppender(tmpwrit);

		} catch (Exception e2) {
			System.out.println("Problems changing log writer");
		}
		log.debug("new writer opened");
	}

	public void endBookLogger() {
		try {

			WriterAppender tmpwrit = (WriterAppender) log.getParent().getAppender("ris.backend");
			tmpwrit.setImmediateFlush(true);
			log.info("Closing logger");
			log.getParent().removeAppender(tmpwrit);
			tmpwrit.close();
			tmpwrit.finalize();
			tmpwrit = logOrig;
			log.getParent().addAppender(tmpwrit);
			log.debug("writer reopened");

		} catch (Exception e2) {
			System.out.println("Problems changing writter");
			e2.printStackTrace();
		}
	}

	public boolean loadFiles() {
		ArrayList files = FileUtil.getXMLFiles(getConfigProperty("RIS.CONTENT_TEMP"), true);
		if (files == null || files.size() == 0) {
			return true;
		}
		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		dfactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = dfactory.newDocumentBuilder();
			// Set EntityResolver to redirect DTD references to local files
			docBuilder.setEntityResolver(new ChunkResolver(getConfigProperty("RIS.CONTENT_TEMP")));
		} catch (ParserConfigurationException e4) {
			log.error(e4.toString());
			return false;
		}
		RISErrorHandler risErr = new RISErrorHandler();
		docBuilder.setErrorHandler(risErr);
		Document doc = null;
		// interate on all docs then all rules

		// split the content files first
		try {
			if (splitContentFiles()) {
				log.info("Content files split successfully");
			} else {
				log.error("==========================================");
				log.error("CRITICAL ERROR: Content splitting failed");
				log.error("Check errors above for details");
				log.error("==========================================");
				return false;
			}
		} catch (Exception e) {
			log.error("==========================================");
			log.error("EXCEPTION during content splitting:");
			log.error("Exception type: " + e.getClass().getName());
			log.error("Exception message: " + e.getMessage());
			log.error("Stack trace:", e);
			log.error("==========================================");
			return false;
		}
		return true;
	}

	private boolean finishLoadingFiles() {
		log.info("Entering finishLoadingFiles()");

		this.foundDiseaseList = null;
		this.foundDiseaseSynList = null;
		this.foundDrugList = null;
		this.foundDrugSynList = null;
		System.gc();

		loadContent(bookISBN);

		return true;

		
	}

	public boolean performDrugAndDiseaseLinking() {
		log.info("Entering performDrugAndDiseaseLinking()");
		// get all of the chunked xml files
		ArrayList<File> files = FileUtil.getXMLFiles(getConfigProperty("RIS.CONTENT_TEMP"), true);
		File file = null;
		String filename = null, filenameParts[] = null, chapterParts[] = null, contentFileName = null;
		Reader reader = null;
		InputSource in = null;
		Document doc = null;
		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		dfactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = null;
		LinkedHashMap[] linkMappings = null;
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd hh:mm:ss a");

		if (files == null || files.size() == 0) {
			return true;
		}

		// setup document builder
		try {
			docBuilder = dfactory.newDocumentBuilder();
			// Set EntityResolver to redirect DTD references to local files
			docBuilder.setEntityResolver(new ChunkResolver(getConfigProperty("RIS.CONTENT_TEMP")));
		} catch (ParserConfigurationException e4) {
			log.error(e4.toString());
			return false;
		}
		RISErrorHandler risErr = new RISErrorHandler();
		docBuilder.setErrorHandler(risErr);

		// remove xml files from the list that aren't either the appendix, sect1
		for (int i = files.size() - 1; i > -1; i--) {
			file = (File) files.get(i);
			filename = file.getName();
			if (filename != null) {
				if (!filename.startsWith("appendix") && !filename.startsWith("sect1") && !filename.startsWith("preface")) {
					files.remove(i);
				}
			} else {
				files.remove(i);
			}
		}

		// sort the files
		Collections.sort(files, new FileComparator());

		ArrayList chunkedDiseaseList = StringUtil.splitCollection(MetaData.getDiseaseMetaData(), 5000);
		ArrayList chunkedDiseaseSynList = StringUtil.splitCollection(MetaData.getDiseaseSynonymMetaData(), 5000);
		ArrayList chunkedDrugList = StringUtil.splitCollection(MetaData.getDrugMetaData(), 5000);
		ArrayList chunkedDrugSynList = StringUtil.splitCollection(MetaData.getDrugSynonymMetaData(), 5000);

		long timeStarted = 0, currentTime = 0;
		double percent = 0.0, oldPercent = 0.0;

		int len = files.size();

		for (int i = 0; i < len; i++) {
			try {
				log.info("Started Process linking on '" + filename + "'");
				timeStarted = System.currentTimeMillis();
				filename = files.get(i).getName();
				contentFileName = files.get(i).getCanonicalPath();
				reader = new InputStreamReader(new FileInputStream((File) files.get(i)), System.getProperty("file.encoding"));
				in = new InputSource(reader);
				doc = docBuilder.parse(in);

				linkMappings = softFilterMetaData(XMLUtil.documentToString(doc), chunkedDiseaseList, chunkedDiseaseSynList, chunkedDrugList, chunkedDrugSynList);
				if (linkMappings != null) {
					if (linkMappings.length == 4) {
						if (LINK_DRUGS) {
							softLinkDrug(doc, linkMappings[0], linkMappings[1]);
						}
						if (LINK_DISEASES) {
							softLinkDisease(doc, linkMappings[2], linkMappings[3]);
						}
					}
				}
				percent = (i * 1.0 / (len - 1)) * 100;
				currentTime = System.currentTimeMillis();
				System.out.println("Processed linking on '" + filename + "' [" + (int) percent + "%]");
				log.info("Processed linking on '" + filename + "' [" + (int) percent + "%]");
				softWriteProcessedContent(doc, contentFileName);
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		log.info("Leaving performDrugAndDiseaseLinking()");
		return true;
	}

	public void softLinkDisease(Document doc, LinkedHashMap softFoundDiseaseList, LinkedHashMap softFoundDiseaseSynList) {
		NodeIterator paraNodeIt = null;
		LinkedHashMap diseaseMetaData = metaData.getDiseaseMetaData();
		LinkedHashMap diseaseSynXMetaData = metaData.getDiseaseSynonymXDiseaseMetaData();
		LinkedHashMap diseaseSynMetaData = metaData.getDiseaseSynonymMetaData();

		try {
			paraNodeIt = XPathAPI.selectNodeIterator(doc, XPATH_SOFT_LINKING);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), LINKING_THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(1);
		threadPool.createThreads(LINKING_THREAD_COUNT_MAXIMUM);
		int count = 0;
		if (paraNodeIt != null) {
			Node paraNode = null;
			while ((paraNode = paraNodeIt.nextNode()) != null) {
				count++;
				try {
					DiseaseLinkThread aThread = new DiseaseLinkThread(softFoundDiseaseList, doc, paraNode);
					threadPool.execute(aThread);

				} catch (InterruptedException e1) {
				}
			}
		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(1000);
				}
			} catch (Exception e1) {
			}
		}
		threadPool = null;
		paraNodeIt = null;

		try {
			paraNodeIt = XPathAPI.selectNodeIterator(doc, XPATH_SOFT_LINKING);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		threadPool = new PooledExecutor(new LinkedQueue(), LINKING_THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(1);
		threadPool.createThreads(LINKING_THREAD_COUNT_MAXIMUM);
		count = 0;
		if (paraNodeIt != null) {
			Node paraNode = null;
			while ((paraNode = paraNodeIt.nextNode()) != null) {
				count++;
				try {

					DiseaseSynonymLinkThread bThread = new DiseaseSynonymLinkThread(softFoundDiseaseSynList, doc, paraNode);
					threadPool.execute(bThread);

				} catch (InterruptedException e1) {
				}
			}
		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(100);
				}
			} catch (Exception e1) {
			}
		}
	}

	public void softLinkDrug(Document doc, LinkedHashMap softFoundDrugList, LinkedHashMap softFoundDrugSynList) {
		NodeIterator paraNodeIt = null;

		LinkedHashMap drugMetaData = metaData.getDrugMetaData();
		LinkedHashMap drugSynMetaData = metaData.getDrugSynonymMetaData();
		LinkedHashMap xDrugSynMetaData = metaData.getDrugSynonymXDrugMetaData();

		try {
			paraNodeIt = XPathAPI.selectNodeIterator(doc, XPATH_SOFT_LINKING);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), LINKING_THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(1);
		threadPool.createThreads(LINKING_THREAD_COUNT_MAXIMUM);
		int count = 0;
		if (paraNodeIt != null) {
			Node paraNode = null;
			while ((paraNode = paraNodeIt.nextNode()) != null) {
				count++;
				try {
					DrugLinkThread aThread = new DrugLinkThread(softFoundDrugList, doc, paraNode);
					threadPool.execute(aThread);
				} catch (InterruptedException e1) {
				}
			}
		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(1);
				}
			} catch (Exception e1) {
			}
		}
		threadPool = null;
		paraNodeIt = null;

		try {
			paraNodeIt = XPathAPI.selectNodeIterator(doc, XPATH_SOFT_LINKING);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		threadPool = new PooledExecutor(new LinkedQueue(), LINKING_THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(1);
		threadPool.createThreads(LINKING_THREAD_COUNT_MAXIMUM);
		count = 0;
		if (paraNodeIt != null) {
			Node paraNode = null;
			while ((paraNode = paraNodeIt.nextNode()) != null) {
				count++;
				try {
					DrugSynonymLinkThread bThread = new DrugSynonymLinkThread(softFoundDrugSynList, doc, paraNode);
					threadPool.execute(bThread);
				} catch (InterruptedException e1) {
				}
			}
		}
		if (count > 0) {
			threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
			try {
				threadPool.awaitTerminationAfterShutdown();
				while (!threadPool.isTerminatedAfterShutdown()) {
					Thread.sleep(100);
				}
			} catch (Exception e1) {
			}
		}
	}

	public LinkedHashMap[] softFilterMetaData(String content, ArrayList chunkedDiseaseList, ArrayList chunkedDiseaseSynList, ArrayList chunkedDrugList, ArrayList chunkedDrugSynList) {
		log.info("Trying to find the count of disease and drug names in content...");
		content = content.toLowerCase();

		foundDiseaseList = new LinkedHashMap();
		foundDiseaseSynList = new LinkedHashMap();
		foundDrugList = new LinkedHashMap();
		foundDrugSynList = new LinkedHashMap();

		PooledExecutor threadPool = new PooledExecutor(new LinkedQueue(), THREAD_COUNT_MAXIMUM);
		threadPool.setKeepAliveTime(100);
		threadPool.createThreads(THREAD_COUNT_MAXIMUM);

		for (int i = 0; i < chunkedDrugList.size(); i++) {
			LinkedHashMap mapToUse = (LinkedHashMap) chunkedDrugList.get(i);

			SoftDrugCounterThread aThread = new SoftDrugCounterThread(mapToUse, content, foundDrugList);
			try {
				threadPool.execute(aThread);
			} catch (InterruptedException e) {
			}
		}

		for (int i = 0; i < chunkedDrugSynList.size(); i++) {
			LinkedHashMap mapToUse = (LinkedHashMap) chunkedDrugSynList.get(i);
			SoftDrugSynonymCounterThread aThread = new SoftDrugSynonymCounterThread(mapToUse, content, foundDrugSynList);
			try {
				threadPool.execute(aThread);
			} catch (InterruptedException e) {
			}
		}

		for (int i = 0; i < chunkedDiseaseList.size(); i++) {
			LinkedHashMap mapToUse = (LinkedHashMap) chunkedDiseaseList.get(i);
			SoftDiseaseCounterThread aThread = new SoftDiseaseCounterThread(mapToUse, content, foundDiseaseList);
			try {
				threadPool.execute(aThread);
			} catch (InterruptedException e) {
			}
		}
		for (int i = 0; i < chunkedDiseaseSynList.size(); i++) {
			LinkedHashMap mapToUse = (LinkedHashMap) chunkedDiseaseSynList.get(i);
			SoftDiseaseSynonymCounterThread aThread = new SoftDiseaseSynonymCounterThread(mapToUse, content, foundDiseaseSynList);
			try {
				threadPool.execute(aThread);
			} catch (InterruptedException e) {
			}
		}

		threadPool.shutdownAfterProcessingCurrentlyQueuedTasks();
		try {
			threadPool.awaitTerminationAfterShutdown();
			while (!threadPool.isTerminatedAfterShutdown()) {
				Thread.sleep(100);
			}
		} catch (Exception e1) {
		}

		try {
			foundDiseaseList = reorderList(foundDiseaseList);// reorderMapUsingOriginalMap(MetaData.getDiseaseMetaData(),
			foundDiseaseSynList = reorderList(foundDiseaseSynList);// reorderMapUsingOriginalMap(MetaData.getDiseaseSynonymMetaData(),
			foundDrugList = reorderList(foundDrugList);// reorderMapUsingOriginalMap(MetaData.getDrugMetaData(),
			foundDrugSynList = reorderList(foundDrugSynList);// reorderMapUsingOriginalMap(MetaData.getDrugSynonymMetaData(),
		} catch (Exception e1) {
			log.info("reordering problem");
			log.error(e1.toString());
		}

		log.info("Found " + foundDiseaseList.size() + " unique disease names in content.");
		log.info("Found " + foundDiseaseSynList.size() + " unique disease synonyms in content.");
		log.info("Found " + foundDrugList.size() + " unique drug names in content.");
		log.info("Found " + foundDrugSynList.size() + " unique drug synonyms in content.");

		return new LinkedHashMap[] { foundDrugList, foundDrugSynList, foundDiseaseList, foundDiseaseSynList };

	}

	public LinkedHashMap reorderList(LinkedHashMap list) {
		LinkedHashMap newList = new LinkedHashMap();
		ArrayList<Object[]> arrList = new ArrayList<Object[]>();
		ArrayList keys = new ArrayList(list.keySet());
		int len = list.size();
		for (int i = 0; i < len; i++) {
			Object key = keys.get(i);
			arrList.add(new Object[] { key, list.get(key) });
		}
		Collections.sort(arrList, new MultiWordComparator());

		for (int i = 0; i < len; i++) {
			Object[] obj = arrList.get(i);
			newList.put(obj[0], obj[1]);
		}
		return newList;
	}

	public static InputStream changeDoctype(URL url) throws IOException {
		InputStream fileIn = url.openStream();
		DoctypeChangerStream changer = new DoctypeChangerStream(fileIn);
		changer.setGenerator(new DoctypeGenerator() {
			public Doctype generate(Doctype old) {
				// Convert relative DTD path to absolute file URL to avoid resolution issues
				String dtdPath = getConfigProperty("RIS.RITTENHOUSE_DTD_PATH") + "RittDocBook.dtd";
				
				// If path is relative (starts with . or doesn't start with / or drive letter), make it absolute
				if (dtdPath.startsWith(".") || (!dtdPath.startsWith("/") && !dtdPath.matches("^[A-Za-z]:.*"))) {
					File dtdFile = new File(CURRENT_DIR + FILE_SEPARATOR + dtdPath);
					try {
						// Convert to file URL format (file:///)
						dtdPath = dtdFile.getCanonicalFile().toURI().toString();
					} catch (IOException e) {
						// Fall back to original path if conversion fails
						log.warn("Could not convert DTD path to URL: " + e.getMessage());
					}
				}
				
				return new DoctypeImpl(old.getRootElement(), old.getPublicId(), dtdPath, old.getInternalSubset());
			}
		});

		return changer;
	}

	public String getCleanIsbn(Node isbnNode) {
		if (isbnNode == null) {
			log.warn("ISBN node is null");
			return null;
		}
		Node firstChild = isbnNode.getFirstChild();
		if (firstChild == null) {
			log.warn("ISBN node has no firstChild (empty element)");
			return null;
		}
		String tmp = firstChild.getNodeValue();
		if (tmp == null) {
			log.warn("ISBN node value is null");
			return null;
		}
		tmp = tmp.replaceAll("-", "");
		tmp = tmp.replaceAll(" ", "");
		tmp = tmp.replaceAll("\n", "");
		tmp = tmp.replaceAll("\r", "");
		return tmp;
	}

	class ChunkResolver implements EntityResolver {
		private String currPath;

		ChunkResolver(String path) {
			this.currPath = path;
			if (currPath == null) {
				currPath = Main.CURRENT_DIR + Main.FILE_SEPARATOR + Main.getConfigProperty("RIS.CONTENT_TEMP");
			}
		}

		public InputSource resolveEntity(String publicID, String systemID) throws SAXException {
			if (systemID.endsWith(".xml")) {
				// Find the last separator (either / or \) to get the filename
				int lastSlash = Math.max(systemID.lastIndexOf("/"), systemID.lastIndexOf("\\"));
				String currXML;
				if (lastSlash < 0) {
					// No separator found, systemID is just a filename
					currXML = systemID;
				} else {
					// Extract filename including the separator
					currXML = systemID.substring(lastSlash);
				}
				// Ensure currPath ends with separator and currXML starts with separator
				String localSrc = currPath;
				if (!currPath.endsWith(FILE_SEPARATOR) && !currPath.endsWith("/")) {
					localSrc = currPath + FILE_SEPARATOR;
				}
				if (currXML.startsWith("/") || currXML.startsWith("\\")) {
					localSrc = localSrc + currXML.substring(1);
				} else {
					localSrc = localSrc + currXML;
				}
				try {
					String properRef = new File(localSrc).toURL().toExternalForm();
					return new InputSource(properRef);
				} catch (MalformedURLException e) {
					log.error(e.toString());
				}
			} else if (systemID.endsWith(".dtd") || systemID.endsWith(".mod") || systemID.endsWith(".ent") || systemID.endsWith(".dec")) {
				// Log DTD resolution start message once per document
				if (!dtdResolutionStarted.get()) {
					log.debug("DTD/module resolution started");
					dtdResolutionStarted.set(true);
				}
				// Handle DTD and module file resolution - redirect to project dtd folder
				// Extract DTD/module file path structure, preserving subdirectories like ent/
				String dtdPath = systemID;
				if (dtdPath.contains("dtd/")) {
					dtdPath = dtdPath.substring(dtdPath.indexOf("dtd/") + 4);
				} else if (dtdPath.contains("dtd\\")) {
					dtdPath = dtdPath.substring(dtdPath.indexOf("dtd\\") + 4);
				} else {
					// Extract filename with potential subdirectory (e.g., "ent/iso-amsa.ent")
					// Look for the last occurrence of a book directory pattern (digits)
					String[] pathParts = systemID.replace("\\", "/").split("/");
					int bookDirEnd = -1;
					for (int i = pathParts.length - 1; i >= 0; i--) {
						if (pathParts[i].matches("\\d+")) {
							// Found book directory, take everything after it
							bookDirEnd = i;
							break;
						}
					}
					if (bookDirEnd >= 0 && bookDirEnd < pathParts.length - 1) {
						// Reconstruct path from parts after book directory
						StringBuilder sb = new StringBuilder();
						for (int i = bookDirEnd + 1; i < pathParts.length; i++) {
							if (sb.length() > 0) sb.append("/");
							sb.append(pathParts[i]);
						}
						dtdPath = sb.toString();
					} else {
						// Just get the filename as fallback
						int lastSlash = Math.max(systemID.lastIndexOf("/"), systemID.lastIndexOf("\\"));
						if (lastSlash >= 0) {
							dtdPath = systemID.substring(lastSlash + 1);
						}
					}
				}
				// Build correct DTD path from project root
				String correctDtdPath = Main.CURRENT_DIR + FILE_SEPARATOR + "dtd" + FILE_SEPARATOR + dtdPath.replace("/", FILE_SEPARATOR);
				File dtdFile = new File(correctDtdPath);
				if (dtdFile.exists()) {
					try {
						// Increment success counter instead of logging each resolution
						dtdResolutionCount.set(dtdResolutionCount.get() + 1);
						String fileUrl = dtdFile.toURI().toURL().toExternalForm();
						InputSource source = new InputSource(fileUrl);
						source.setSystemId(fileUrl);
						return source;
					} catch (MalformedURLException e) {
						log.error("Error resolving DTD/module: " + e.toString());
						dtdResolutionFailures.get().add(systemID + " -> " + e.getMessage());
					}
				} else {
					// Try looking in v1.1 subdirectory if not found
					String correctDtdPathV11 = Main.CURRENT_DIR + FILE_SEPARATOR + "dtd" + FILE_SEPARATOR + "v1.1" + FILE_SEPARATOR + dtdPath.replace("/", FILE_SEPARATOR);
					File dtdFileV11 = new File(correctDtdPathV11);
					if (dtdFileV11.exists()) {
						try {
							// Increment success counter instead of logging each resolution
							dtdResolutionCount.set(dtdResolutionCount.get() + 1);
							String fileUrlV11 = dtdFileV11.toURI().toURL().toExternalForm();
							InputSource sourceV11 = new InputSource(fileUrlV11);
							sourceV11.setSystemId(fileUrlV11);
							return sourceV11;
						} catch (MalformedURLException e) {
							log.error("Error resolving DTD/module: " + e.toString());
							dtdResolutionFailures.get().add(systemID + " -> " + e.getMessage());
						}
					} else {
						log.warn("DTD/module file not found at: " + correctDtdPath + " or " + correctDtdPathV11);
						dtdResolutionFailures.get().add(systemID + " -> file not found");
					}
				}
			}
			return null;
		}
	}

	/**
	 * Log summary of DTD resolution activity and reset counters.
	 * Call this after XML parsing is complete to output condensed DTD resolution information.
	 */
	private void logDTDResolutionSummary() {
		if (dtdResolutionStarted.get()) {
			int successCount = dtdResolutionCount.get();
			List<String> failures = dtdResolutionFailures.get();
			
			if (failures.isEmpty()) {
				log.debug("Resolved " + successCount + " DTD/module files successfully");
			} else {
				log.debug("Resolved " + successCount + " DTD/module files with " + failures.size() + " failure(s):");
				for (String failure : failures) {
					log.debug("  - Failed: " + failure);
				}
			}
			
			// Reset ThreadLocal counters for next document
			dtdResolutionStarted.set(false);
			dtdResolutionCount.set(0);
			dtdResolutionFailures.get().clear();
		}
	}

	// The original maps are sorted resort the found values by using the sorted
	// originals.
	public LinkedHashMap reorderMapUsingOriginalMap(LinkedHashMap sortedMap, LinkedHashMap foundMap) {

		// Handle null sortedMap (happens when metadata is not loaded)
		if (sortedMap == null || foundMap == null) {
			log.debug("Skipping reorder - metadata not available (sortedMap or foundMap is null)");
			return (foundMap != null) ? foundMap : new LinkedHashMap();
		}

		List mapKeys = new ArrayList(sortedMap.keySet());

		LinkedHashMap someMap = new LinkedHashMap();

		int size = mapKeys.size();

		for (int i = 0; i < size; i++) {
			Object key = mapKeys.get(i);
			Object valAt = null;
			String val = sortedMap.get(key).toString();
			if (key != null) {
				valAt = foundMap.get(key);
				if (valAt != null) {
					if (valAt.toString().equals(val)) {
						someMap.put(key, val);
					}
				}
			}

		}
		return someMap;
	}

	public static String xmlToString(Node node) {
		try {
			Source source = new DOMSource(node);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.transform(source, result);
			return stringWriter.getBuffer().toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return null;
	}
}