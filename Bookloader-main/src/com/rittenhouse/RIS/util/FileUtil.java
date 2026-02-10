package com.rittenhouse.RIS.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.log4j.Category;

import com.rittenhouse.RIS.Main;

public class FileUtil {
	
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");

	public static final String CURRENT_DIR = System.getProperty("user.dir");
	
	//logger
	protected static Category log = Category.getInstance(FileUtil.class.getName());
	
	/**
	 * Clean directory, preserving the 'dtd' subdirectory which contains DTD files
	 */
	public static void cleanDir(File sourceDir, boolean recurse) {
		File[] files = null;
		if (sourceDir.isDirectory()) {
			files = sourceDir.listFiles();
			for (int i = 0; i < files.length; i++) {
				File delFile = files[i];
				// Skip the 'dtd' directory - it contains DTD/entity/module files that should persist
				if (delFile.isDirectory() && delFile.getName().equals("dtd")) {
					continue;
				}
				if (delFile.isDirectory()) {
					FileUtil.cleanDir(delFile, recurse);
					delFile.delete();
				} else {
					delFile.delete();
				}
			}
		}
	}
	
	/**
	 * Copies content file from one directory to another
	 * 
	 * @param inDir
	 * @param outDir
	 */
	public static void copyXMLFiles(File inDir, File outDir, boolean recurse) {
		FilenameFilter xmlFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = null;
				try {
					String path = dir.getCanonicalPath();
					file = new File(path + Main.FILE_SEPARATOR + name);
				} catch (IOException e) {}
				return (name.endsWith(".xml") || name.endsWith(".XML") || file.isDirectory());
			}
		};
		File[] files = null;
		if (inDir.isDirectory()) {
			files = inDir.listFiles(xmlFilter);
			log.info("Moving folder" + inDir.toString() + " to " + outDir.toString());
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory() && recurse) {
					String destDirName = null;
					try {
						destDirName = outDir.getCanonicalPath() + FILE_SEPARATOR + files[i].getName();
					} catch (IOException e) {}
					File destDir = new File(destDirName);
					if (!destDir.exists())
						destDir.mkdirs();
					copyXMLFiles(files[i], destDir, true);
				} else {
					String inFileName = null;
					String outFileName = null;
					InputStream fin = null;
			        OutputStream fout = null;
			        try {
						inFileName = inDir.getCanonicalPath() + FILE_SEPARATOR + files[i].getName();
						outFileName = outDir.getCanonicalPath() + FILE_SEPARATOR + files[i].getName();
					    
			            fin = new FileInputStream(inFileName);
			            fout = new FileOutputStream(outFileName);
			            copy(fin, fout);
			            fout.close();
			            fin.close();
						log.debug("Moved file " + inFileName + " to " + outFileName);
					} catch (IOException e) {
						log.error("Failed moving file " + inFileName + " to " + outFileName);
					}
				}
			}
		}
	}
	
	
	/**
	 * @param inDir
	 * @param outDir
	 * @param recurse
	 */
	public static void moveNonXMLFiles(File inDir, File outDir, boolean recurse) {
		FilenameFilter nonXmlFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (!name.endsWith(".xml") && !name.endsWith(".XML"));
			}
		};
		File[] files = null;
		if (inDir.isDirectory()) {
			files = inDir.listFiles(nonXmlFilter);
			log.info("Moving folder" + inDir.toString() + " to " + outDir.toString());
			for (int i = 0; i < files.length; i++) {
				// Skip the 'dtd' directory - it contains DTD/entity/module files that should persist
				if (files[i].isDirectory() && files[i].getName().equals("dtd")) {
					log.debug("Skipping 'dtd' directory - DTD files preserved");
					continue;
				}
				if (files[i].isDirectory() && recurse) {
					String destDirName = null;
					try {
						destDirName = outDir.getCanonicalPath() + FILE_SEPARATOR;
					} catch (IOException e) {}
					File destDir = new File(destDirName);
					if (!destDir.exists())
						destDir.mkdirs();
					moveNonXMLFiles(files[i], destDir, true);
				} else {
					String inFileName = null;
					String outFileName = null;
					InputStream fin = null;
			        OutputStream fout = null;
			        try {
						inFileName = inDir.getCanonicalPath() + FILE_SEPARATOR + files[i].getName();
						outFileName = outDir.getCanonicalPath() + FILE_SEPARATOR + files[i].getName();
					    
			            fin = new FileInputStream(inFileName);
			            fout = new FileOutputStream(outFileName);
			            copy(fin, fout);
			            fout.close();
			            fin.close();

						if (!files[i].getName().endsWith(".xml") && !files[i].getName().endsWith(".XML"))
							files[i].delete();
						log.debug("Moved file " + inFileName + " to " + outFileName);
					} catch (IOException e) {
						log.error("Failed moving file " + inFileName + " to " + outFileName);
					} 
				}
			}
		}
	}
	
	public static void copy(InputStream in, OutputStream out) 
	   throws IOException {
	    synchronized (in) {
	      synchronized (out) {
	        BufferedInputStream bin = new BufferedInputStream(in);
	        BufferedOutputStream bout = new BufferedOutputStream(out);
	  
	        while (true) {
	          int datum = bin.read();
	          if (datum == -1) break;
	          bout.write(datum);
	        }
	        bout.flush();
	      }
	    }
	  }



	/**
	 * Copy file, forces creation of destination directories
	 * 
	 * @param srcFileName
	 * @param destFileName
	 */
	public static void copyFile(String srcFileName, String destFileName) {
		File destFile = new File(destFileName);
		File destDir = destFile.getParentFile();
		if (!destDir.exists()) {
			destDir.mkdirs();
		}
		InputStream fin = null;
        OutputStream fout = null;
        try {
            fin = new FileInputStream(srcFileName);
            fout = new FileOutputStream(destFileName);
            copy(fin, fout);
            fout.close();
            fin.close();
		} catch (IOException e1) {
			log.error("Failed copying file " + srcFileName + " to "+ destFileName);
		} 
	}
	
	/**
	 * Recursively copy a directory and its contents
	 * @param sourceDir Source directory to copy from
	 * @param destDir Destination directory to copy to
	 * @throws IOException if copy fails
	 */
	public static void copyDirectory(File sourceDir, File destDir) throws IOException {
		copyDirectory(sourceDir, destDir, 0);
	}
	
	/**
	 * Recursively copy a directory with depth limit to prevent infinite recursion
	 * @param sourceDir Source directory to copy from
	 * @param destDir Destination directory to copy to
	 * @param depth Current recursion depth
	 * @throws IOException if copy fails
	 */
	private static void copyDirectory(File sourceDir, File destDir, int depth) throws IOException {
		final int MAX_DEPTH = 20; // Prevent infinite recursion from circular references
		
		if (depth > MAX_DEPTH) {
			throw new IOException("Maximum directory depth exceeded (" + MAX_DEPTH + "), possible circular reference");
		}
		
		if (!sourceDir.exists() || !sourceDir.isDirectory()) {
			throw new IOException("Source directory does not exist or is not a directory: " + sourceDir);
		}
		
		// Prevent copying into itself
		try {
			String sourceCanonical = sourceDir.getCanonicalPath();
			String destCanonical = destDir.getCanonicalPath();
			if (destCanonical.startsWith(sourceCanonical + File.separator)) {
				throw new IOException("Cannot copy directory into itself: " + sourceDir);
			}
		} catch (IOException e) {
			log.warn("Could not verify canonical paths: " + e.getMessage());
		}
		
		if (!destDir.exists()) {
			destDir.mkdirs();
		}
		
		File[] files = sourceDir.listFiles();
		if (files != null) {
			for (File file : files) {
				File destFile = new File(destDir, file.getName());
				if (file.isDirectory()) {
					copyDirectory(file, destFile, depth + 1);
				} else {
					copyFile(file.getAbsolutePath(), destFile.getAbsolutePath());
				}
			}
		}
	}
	
	/**
	 * @param dirName
	 * @return
	 */
	public static File[] getXMLFiles(String dirName) {
		File files[] = null;
		String sourceDirName = dirName;
		File sourceDir = new File(sourceDirName);
		FilenameFilter xmlFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = null;
				try {
					String path = dir.getCanonicalPath();
					file = new File(path + Main.FILE_SEPARATOR + name);
				} catch (IOException e) {}
				return (name.endsWith(".xml") || name.endsWith(".XML"));
			}
		};
		if (sourceDir.isDirectory()) {
			files = sourceDir.listFiles(xmlFilter);
		}
		return files;
	}
	
	/**
	 * @param dirName
	 * @param recurse
	 * @return
	 */
	public static ArrayList getXMLFiles(String dirName, boolean recurse){
		File files[] = null;
		ArrayList filesList = new ArrayList();
		File sourceDir = new File(dirName);
		File[] allFiles = sourceDir.listFiles();
		for(int j=0;j<allFiles.length;j++){
			File file = allFiles[j];
			if (file.isDirectory()){
				try {
					File tempFiles[] = getXMLFiles(file.getCanonicalPath());
					if (tempFiles!=null){
						for(int k=0;k<tempFiles.length;k++){
							filesList.add(tempFiles[k]);
						}
					}
				} catch (IOException e) {}
			} else {
				String fileName = allFiles[j].getName();
				if (fileName.endsWith(".xml") || fileName.endsWith(".XML")){
					filesList.add(allFiles[j]);
				}
			}
		}
		return filesList;
	}
	
	
	
	public static void addSpaceBeforeNewLine(String fileName){
	    File file = new File(fileName);
	    String str="";
        StringBuffer records=new StringBuffer();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), System.getProperty("file.encoding")));
	        while((str=reader.readLine())!=null){
	            if (str==null)
	                break;
	            records.append(str + " ");
	            records.append(System.getProperty("line.separator"));
	        }
	        String toWrite = records.toString();
	        FileOutputStream fos = new FileOutputStream(file);
	        fos.write(toWrite.getBytes(System.getProperty("file.encoding")));
	        fos.flush();
	        fos.close();
        } catch (UnsupportedEncodingException e) {
            log.error("Error adding fixing new lines - " + e.toString());
        } catch (FileNotFoundException e) {
            log.error("Error adding fixing new lines - " + e.toString());
        } catch (IOException e) {
            log.error("Error adding fixing new lines - " + e.toString());
        }
	}
}
