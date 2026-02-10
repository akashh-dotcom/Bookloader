package com.rittenhouse.RIS.util;

import java.io.File;
import java.util.Comparator;

public class FileComparator implements Comparator<File> {
	public int compare(File f1, File f2) {
		if (f1 != null && f2 != null) {
			if (f1.getName() != null && f2.getName() != null) {
				return f1.getName().compareTo(f2.getName());
			}
		}
		return 0;
	}
}
