package com.rittenhouse.RIS.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringUtil {

	/**
	 * Created by Brian Wright -- Technotects
	 * 12/21/2007
	 * 
	 * @param is
	 * @return
	 */
	public static String getStringFromInputStream(InputStream is) {
		OutputStream outputStream = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[4096];

			while (true) {
				int read = is.read(buffer);

				if (read == -1) {
					break;
				}

				outputStream.write(buffer, 0, read);
			}

			outputStream.close();
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return outputStream.toString();
	}

	public static String titleCase(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return str;
		}
		StringBuffer buffer = new StringBuffer(strLen);
		boolean whitespace = true;
		for (int i = 0; i < strLen; i++) {
			char ch = str.charAt(i);
			if (Character.isWhitespace(ch)) {
				buffer.append(ch);
				whitespace = true;
			} else if (whitespace) {
				buffer.append(Character.toTitleCase(ch));
				whitespace = false;
			} else {
				buffer.append(Character.toLowerCase(ch));
			}
		}
		return buffer.toString();
	}

	/**
	 * 
	 * @param text
	 * @param repl
	 * @param with
	 * @param max
	 * @param isCaseSensitive
	 * @return
	 */
	public static String replace(String text, String repl, String with, int max, boolean isCaseSensitive) {
		if (text == null || repl == null || with == null || repl.length() == 0 || max == 0) {
			return text;
		}

		String origText = text;
		String origRepl = repl;

		if (!isCaseSensitive) {
			text = text.toLowerCase();
			repl = repl.toLowerCase();
		}

		StringBuffer buf = new StringBuffer(text.length());
		int start = 0, end = 0;
		while ((end = text.indexOf(repl, start)) != -1) {
			buf.append(origText.substring(start, end)).append(with);
			start = end + repl.length();

			if (--max == 0) {
				break;
			}
		}
		buf.append(origText.substring(start));
		return buf.toString();
	}

	/**
	 * Finds index of word in string
	 * 
	 * @param source
	 *            string to search in
	 * @param toSearch
	 *            string to search for
	 * @return index
	 */
	public static int findWordIndex(String source, String toSearch) {
		// revised as it wasn't handling 3 special cases correctly
		// exact match, Start with pattern, end with pattern.
		int index = -1;
		String words[] = null;
		String patt = "(?:\\W+)\\Q" + toSearch + "\\E(?:\\W+)";
		try {
			// if source is empty don't look at it
			if (source == null)
				return -1;

			if (!isAllUpperCase(toSearch)) {
				source = source.toLowerCase();
				toSearch = toSearch.toLowerCase();
				patt = "(?:\\W+)\\Q" + toSearch + "\\E(?:\\W+)";
			}
			// two casses terms with spaces and those with out.
			if (toSearch.indexOf(" ") < 0) {
				try {
					words = source.split("\\b");
				} catch (PatternSyntaxException pse) {
				}

				if (words != null && words.length > 0) {
					for (int j = 0; j < words.length; j++) {
						if (toSearch.equals(words[j].trim())) {
							if (j + 1 >= words.length) {
								index = source.indexOf(words[j - 1] + words[j]);
							} else {
								index = source.indexOf(words[j - 1] + words[j] + words[j + 1]);
							}
							index = index + words[j - 1].length();
							return index;
						}
					}
				}
			} else {
				// see if the pattern is matched with white space on either side
				try {
					Pattern pattern = Pattern.compile(patt);
					Matcher matcher = pattern.matcher(source);
					boolean matchFound = matcher.find();

					if (matchFound) {
						index = source.indexOf(toSearch);
						return index;
					}
				} catch (PatternSyntaxException pse) {
				}

				// exact match case is not handled
				if (source.equals(toSearch)) {
					return 0;
				}

				// see if ends with search is a match
				if (source.endsWith(toSearch)) {
					String nextString = null;
					nextString = source.substring((source.length() - (toSearch.length() + 1)));
					patt = "(?:\\W+)\\Q" + toSearch + "\\E";
					try {
						Pattern pattern = Pattern.compile(patt);
						Matcher matcher = pattern.matcher(nextString);
						boolean matchFound = matcher.find();

						if (matchFound) {
							return source.length() - toSearch.length();
						}
					} catch (PatternSyntaxException pse) {
					}
				}

				if (source.startsWith(toSearch)) {
					String nextString = null;
					nextString = source.substring(0, toSearch.length() + 1);
					patt = "\\Q" + toSearch + "\\E(?:\\W+)";
					try {
						Pattern pattern = Pattern.compile(patt);
						Matcher matcher = pattern.matcher(nextString);
						boolean matchFound = matcher.find();

						if (matchFound) {
							return 0;
						}
					} catch (PatternSyntaxException pse) {
					}
				}
			}

		} catch (Exception e) {
			index = -1;
		}
		return index;
	}

	public static ArrayList splitCollection(LinkedHashMap map, int chunkSize) {
		ArrayList retList = new ArrayList();
		
		// Handle null map
		if (map == null) {
			return retList;
		}
		
		LinkedHashMap chunkMap = new LinkedHashMap();
		int chunkCount = 0;
		int itemCount = 0;
		Iterator mapIt = map.keySet().iterator();
		Integer id = null;
		String name = null;
		int i = 0;

		if (chunkSize >= map.size()) {
			retList.add(0, map);
			return retList;
		}

		while (mapIt.hasNext()) {
			id = (Integer) mapIt.next();
			name = (String) map.get(id);
			chunkMap.put(id, name);
			if (i == map.size() || chunkCount == chunkSize) {
				chunkMap = new LinkedHashMap();
				retList.add(itemCount, chunkMap);
				itemCount++;
				chunkCount = -1;
			}
			chunkCount++;
			i++;
		}
		return retList;
	}

	public static boolean isAllUpperCase(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return false;
		}
		char ch = 0;
		for (int i = 0; i < strLen; i++) {
			ch = str.charAt(i);
			if (Character.isUpperCase(ch)) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}
}