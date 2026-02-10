package com.rittenhouse.RIS;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Category;

import com.ixia.textmlserver.ClientServices;
import com.ixia.textmlserver.ClientServicesFactory;
import com.ixia.textmlserver.Constants;
import com.ixia.textmlserver.IxiaDocBaseServices;
import com.ixia.textmlserver.IxiaDocument;
import com.ixia.textmlserver.IxiaDocumentServices;
import com.ixia.textmlserver.IxiaServerServices;
import com.ixia.textmlserver.TextmlserverError;

/**
 * TextML XML datastore facade
 * 
 * @author vbhatia
 */
public class XMLDataStore {

	// logger
	protected static Category log = Category.getInstance(XMLDataStore.class.getName());

	private String url = "rmi://localhost:1099";
	private String user = "localhost";
	private String password = "";
	private String server = "localhost";
	private String docbase = "RIS Document Base_Staging";
	private String domain = "FLYINGSTAR";

	private ClientServices clientService = null;
	private IxiaDocumentServices docService = null;

	private boolean loadError = false;

	public XMLDataStore() {
		this.url = Main.getConfigProperty("XMLDB.URL");
		this.user = Main.getConfigProperty("XMLDB.USER");
		this.password = Main.getConfigProperty("XMLDB.PASSWORD");
		this.server = Main.getConfigProperty("XMLDB.SERVER");
		this.docbase = Main.getConfigProperty("XMLDB.DOCBASE");
		this.domain = Main.getConfigProperty("XMLDB.DOMAIN");
	}

	/**
	 * Connect to XML data store
	 */
	public void connect() {
		if (docService == null) {
			LinkedHashMap parms = new LinkedHashMap(1);
			parms.put("ServerURL", url);
			try {
				log.debug("Trying to connect with XML data store");
				// Get the ClientServices
				this.clientService = ClientServicesFactory.getInstance("RMI", parms);
				clientService.Login(domain, user, password);

				// Get the server Services
				IxiaServerServices serverService = clientService.ConnectServer(server);
				// then, the DocbaseServices
				IxiaDocBaseServices docbaseService = serverService.ConnectDocBase(docbase);
				// then, the DocumentServices
				this.docService = docbaseService.DocumentServices();
				
				log.debug("Successfully connected to XML data store");
			} catch (Exception e) {
				log.error("Error connecting to XML data store");
				log.error(e.toString());
				loadError = true;
			}
		}
	}

	/**
	 * Getting documents from XML data store
	 * 
	 * @param documentNames
	 * @param dirPath
	 */
	public void getDocuments(String[] documentNames, String dirPath) {
		IxiaDocumentServices.Result[] results = docService.GetDocuments(documentNames, Constants.TEXTML_DOCUMENT_CONTENT);
		if (results != null) {
			for (int i = 0; i < results.length; i++) {
				try {
					results[i].document.GetContent().SaveTo(dirPath + Main.FILE_SEPARATOR + documentNames[i]);
				} catch (IOException e) {
					log.error("Error adding document to XML data store");
					log.error(e.toString());
					loadError = true;
				}
			}
		}
	}

	/**
	 * Putting documents to XML data store
	 * 
	 * @param file
	 *            directory path
	 * @param recurse
	 */
	public void setDocuments(File file, boolean recurse, String collectionName) {
		try {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					File file = null;
					try {
						file = new File(dir.getCanonicalPath() + Main.FILE_SEPARATOR + name);
					} catch (IOException e) {
					}
					return (name.endsWith(".xml") || name.endsWith(".XML") || file.isDirectory());
				}
			};
			File[] files = file.listFiles(filter);

			ArrayList documents = new ArrayList(files.length);

			for (int i = 0; i < files.length; ++i) {
				String fName = files[i].getCanonicalPath();
				if (files[i].isDirectory()) {
					if (recurse) {
						setDocuments(files[i], true, collectionName);
					}
				} else {
					IxiaDocument document = IxiaDocument.getInstance();
					document.SetName(files[i].getName());
					if (collectionName != null && collectionName.length() > 0)
						document.SetCollection(collectionName);
					document.SetMimeType("text/xml");
					document.AttachContent(IxiaDocument.MakeContentFromFile(files[i]));
					documents.add(document);
				}
			}

			IxiaDocument[] docList = new IxiaDocument[documents.size()];
			docList = (IxiaDocument[]) documents.toArray(docList);

			String collName = collectionName;
			if (collName == null || collName.length() == 0) {
				collName = "root";
			}
			log.info("Adding " + String.valueOf(docList.length) + " documents to " + collName + " collection, from " + file.getCanonicalPath());

			TextmlserverError[] err = docService.SetDocuments(docList, Constants.TEXTML_ADD_DOCUMENT | Constants.TEXTML_REPLACE_DOCUMENT | Constants.TEXTML_INDEX_DOCUMENT, Constants.TEXTML_DOCUMENT);

			int countError = 0;
			if (err != null) {
				for (int i = 0; i < err.length; ++i) {
					if (err[i] != null) {
						++countError;
						if (countError == 1) {
							log.error(err[i].getMessage());
							loadError = true;
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Error adding document to XML data store");
			log.error(e.toString());
			loadError = true;
		} 
	}

	/**
	 * disconnect from XML data store
	 */
	public void disconnect() {
		clientService.Logout();
		this.clientService = null;
		this.docService = null;
		log.debug("Disconnected from XML data store");
	}

	/**
	 * @return Returns the loadError.
	 */
	public boolean isLoadError() {
		return loadError;
	}

	/**
	 * @param loadError
	 *            The loadError to set.
	 */
	public void setLoadError(boolean loadError) {
		this.loadError = loadError;
	}
}