package com.rittenhouse.RIS.util;

import java.util.Comparator;

public class MultiWordComparator implements Comparator {
	public int compare(Object o1, Object o2) {
		Object[] obj1 = (Object[]) o1;
		Object[] obj2 = (Object[]) o2;
		Integer i1 = (Integer) obj1[0];
		Integer i2 = (Integer) obj2[0];
		String s1 = (String) obj1[1];
		String s2 = (String) obj2[1];
		Integer len1 = s1.split(" ").length;
		Integer len2 = s2.split(" ").length;
		if (len1 == len2) {
			return i1.compareTo(i2);
		} else {
			return len2.compareTo(len1);
		}
	}
}
