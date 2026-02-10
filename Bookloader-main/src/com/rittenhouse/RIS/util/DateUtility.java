package com.rittenhouse.RIS.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * @author vbhatia
 */
public class DateUtility {

	/**
	 * @param dtStr
	 * @param dtFormat
	 * @return
	 */
	public static Date convertToDate(String dtStr, String dtFormat) {
		if (dtStr==null) {
            return null;
        }

        SimpleDateFormat dateFormatReceived = new SimpleDateFormat(dtFormat);
        Date retDate = null;
        TimeZone tz = TimeZone.getDefault();
        GregorianCalendar cl = new GregorianCalendar(tz);
        dateFormatReceived.setCalendar(cl);
        dateFormatReceived.setLenient(false);
        try {
			retDate = dateFormatReceived.parse(dtStr);
		} catch (ParseException e) {
			return null;
		}
        return retDate;
	}
	
	
	public static String getDBFormatSQL(Date value) {
		if (value == null) {
			return null;
		}
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
        String sql = formatter.format(value);
        return "'" + sql + "'";
    }


	
}
