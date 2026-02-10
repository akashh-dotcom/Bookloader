package com.rittenhouse.RIS.test;

/**
 * Title: GetDocument - a sample to get a document from a docbase
 * Description:
 *               GetDocument [url=<rmi server url and port>] user=<domain\\user> password=<password> server=<ServerName> docbase=<DocBaseName> docname=<DocumentName> path=<Path>
 *
 *               This sample demonstrates how to retrieve a document from
 *               a document base.
 *               it also shows how to trap errors that might occur while
 *               retrieving the document.
 *
 * Copyright:    Copyright (c) 2003
 * Company:      Ixiasoft inc.
 * @author       Pierre Bisaillon
 * @version 1.0
 */

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.ixia.textmlserver.ClientServices;
import com.ixia.textmlserver.Constants;
import com.ixia.textmlserver.IxiaDocBaseServices;
import com.ixia.textmlserver.IxiaDocument;
import com.ixia.textmlserver.IxiaDocumentServices;
import com.ixia.textmlserver.IxiaServerServices;
import com.ixia.textmlserver.TextmlserverError;

public class GetDocument
{
    final static String TOKEN_URL      = "URL";
    final static String TOKEN_USER     = "USER";
    final static String TOKEN_PASSWORD = "PASSWORD";
    final static String TOKEN_SERVER   = "SERVER";
    final static String TOKEN_DOCBASE  = "DOCBASE";
    final static String TOKEN_PATH     = "PATH";
    final static String TOKEN_DOCNAME  = "DOCNAME";


    final static String [] validTokens = { TOKEN_URL, TOKEN_USER, TOKEN_PASSWORD, TOKEN_SERVER, TOKEN_DOCBASE, TOKEN_PATH, TOKEN_DOCNAME };
    final static boolean[] mandatory   = { false    , true      , true          , true        , true         , true      , true          };

    private static LinkedHashMap Extract(String [] args)
    {
        LinkedHashMap retval = new LinkedHashMap(10);

        for (int i = 0; i < args.length; ++i)
        {
            StringTokenizer tokens = new StringTokenizer(args[i], "=", false);
            String token = null, value = null;

            if (tokens.hasMoreElements())
                token = tokens.nextToken();
            if (tokens.hasMoreElements())
                value = tokens.nextToken();

            if (token == null || value == null)
            {
                retval.clear();
                return retval;
            }

            boolean found = false;
            for (int j = 0; j < validTokens.length && !found; ++j)
            {
                if (validTokens[j].equalsIgnoreCase(token) &&
                    !retval.containsKey(validTokens[j]))
                {
                    retval.put(validTokens[j], value);
                    found = true;
                }
            }

            if (!found)
            {
                retval.clear();
                return retval;
            }
        }

        for (int i = 0; i < validTokens.length; ++i)
        {
            if (mandatory[i] && !retval.containsKey(validTokens[i]))
            {
                retval.clear();
                return retval;
            }
        }

        return retval;
    }

    private static void Usage()
    {
        System.out.println("GetDocument [url=<rmi server url and port>] user=<domain\\user> password=<password> server=<ServerName> docbase=<DocBaseName> docname=<DocumentName> path=<path>");
        System.out.println("\t<rmi server url and port> is the url and port number where the rmi server is located (default=rmi://localhost:1099)");
        System.out.println("\t<domain\\user> name of the user used for security purpose");
        System.out.println("\t<password> password of the user");
        System.out.println("\t<ServerName> Texmlserver name");
        System.out.println("\t<DocBaseName> Document base name");
        System.out.println("\t<DocumentName> Name of the document to retrieve");
        System.out.println("\t<path> The path where the document will be saved");
    }

    private static void WriteFile(String fileName, IxiaDocument.Content content) throws java.io.IOException
    {
        FileOutputStream f = new FileOutputStream(fileName);

        try
        {
            content.SaveTo(f);
        }
        finally
        {
            f.close();
        }
    }

    private static void WriteFile(String fileName, String strContent) throws java.io.IOException
    {
        OutputStreamWriter f = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-16LE");

        try
        {
            f.write("\ufeff");
            f.write(strContent);
        }
        finally
        {
            f.close();
        }
    }

    public static void main(String[] args)
    {
        String  url = "rmi://localhost:1099";
        
        //[url=<rmi server url and port>] user=<domain\\user> password=<password> server=<ServerName> docbase=<DocBaseName> docname=<DocumentName> path=<path>
        String params[] = new String[]{"url=rmi://localhost:1099", "user=FLYINGSTAR\\vbhatia","password=romanJava0", "server=rittdev01", "docbase=RittenHouse", "path=.\\","docname=/Medicine/1-882742-32-X.xml"};

        // parse command line

        LinkedHashMap map = Extract(params);

        // validate parameters

        if (map.isEmpty())
        {
            Usage();
            return;
        }

        if (map.containsKey(TOKEN_URL))
            url = (String) map.get(TOKEN_URL);

        String user = (String) map.get(TOKEN_USER);

        if (user.indexOf("\\") == -1)
        {
            Usage();
            return;
        }

        String documentName = (String) map.get(TOKEN_DOCNAME);
        String path         = (String) map.get(TOKEN_PATH);

        // Prepare parameters for ClientServicesFactory

        LinkedHashMap parms = new LinkedHashMap(1);
        parms.put("ServerURL", url);

        try
        {
            // Get the ClientServices
            ClientServices cs = com.ixia.textmlserver.ClientServicesFactory.getInstance("RMI", parms);

            // extract domain from user
            String domain   = user.substring(0, user.indexOf("\\"));
            String userName = user.substring(user.indexOf("\\") + 1);

            // and login

            cs.Login(domain, userName, (String) map.get(TOKEN_PASSWORD));

            try
            {
                // Get the server Services
                IxiaServerServices ss = cs.ConnectServer((String) map.get(TOKEN_SERVER));
                // then, the DocbaseServices
                IxiaDocBaseServices docbase = ss.ConnectDocBase((String) map.get(TOKEN_DOCBASE));
                // then, the DocumentServices
                IxiaDocumentServices ds = docbase.DocumentServices();

                // we're now ready to get the document

                String [] documents = new String[1];
                documents[0] = documentName;

                IxiaDocumentServices.Result [] result = ds.GetDocuments(documents, Constants.TEXTML_DOCUMENT_CONTENT | Constants.TEXTML_DOCUMENT_PROPERTIES, Constants.TEXTML_DOCUMENT);

                if (result == null)
                {
                    System.out.println("An unexpected error occured");
                    return;
                }
                else if (result[0].error != null)
                {
                    if (result[0].error.GetErrorCode() == TextmlserverError.TEXTML_E_TRANSACTION_LOG)
                    {
                        Iterator events = result[0].error.GetEvents();

                        int errorCode = 0;

                        while (events.hasNext() && errorCode == 0)
                        {
                            TextmlserverError.Event event = (TextmlserverError.Event) events.next();
                            errorCode = event.GetType();
                        }

                        if (errorCode == TextmlserverError.Event.TEXTML_EVENT_DOCNOTEXIST)
                        {
                            // try to get the document as a system document.
                            result = ds.GetDocuments(documents, Constants.TEXTML_DOCUMENT_CONTENT | Constants.TEXTML_DOCUMENT_PROPERTIES, Constants.TEXTML_SYSTEM_DOCUMENT);
                        }
                    }
                }

                if (result[0].error != null)
                {
                    System.err.println("The following error occured while getting " + documentName);
                    System.err.println(result[0].error.getMessage());
                    return;
                }
                
                result[0].document.GetContent().SaveTo(System.out);


            }
            finally
            {
                // don't forget to logout
                cs.Logout();
            }
        }
        catch (Exception e)
        {
            System.err.println("Exception occured: ");
            e.printStackTrace(System.err);
        }

    }
}


