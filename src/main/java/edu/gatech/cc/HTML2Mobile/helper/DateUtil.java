package edu.gatech.cc.HTML2Mobile.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * Static utility methods for date handling.
 */
public class DateUtil {
	// match the pieces and discard the rest, be tolerant of different seperators
	private static final String SEP = "[\\-\\s_:]";
	private static final String EXPIRE_REGEX = "^.*" +
			"(([a-zA-Z]{3}),\\s*)?" +
			"(\\d{2})" + SEP +
			"([a-zA-Z]{3})" + SEP +
			"(\\d{4})\\s+" +
			"(\\d{2})" + SEP +
			"(\\d{2})" + SEP +
			"(\\d{2})" +
			".*$";
	// replace with standardized format for datetime parsing
	private static final String EXPIRE_REPLACE = "$3-$4-$5 $6:$7:$8";
	private static final String DATE_FORMAT = "dd-MMM-yyyy HH:mm:ss";

	/** Use with EXPIRE_REPLACE to normalize date string. */
	private static final Pattern normalizeDate = Pattern.compile(EXPIRE_REGEX);

	/**
	 * Returns whether <code>date</code> is an HTTP date formatted string.
	 * 
	 * @param date the date
	 * @return if <code>date</code> is HTTP date-formatted
	 * @throws NullPointerException if <code>date</code> is <code>null</code>
	 */
	public static boolean isHttpDate(String date) {
		if( date == null ) {
			throw new NullPointerException("date is null");
		}
		return normalizeDate.matcher(date).matches();
	}

	/**
	 * Attempt to parse an HTTP date-formatted string to a UTC date-time.
	 * 
	 * @param date the date string
	 * @return the parsed date-time
	 * @throws IllegalArgumentException if <code>date</code> cannot be parsed
	 * @throws NullPointerException if <code>date</code> is <code>null</code>
	 */
	public static DateTime parseHttpDate(String date) {
		if( date == null ) {
			throw new NullPointerException("date is null");
		}

		// normalize the date string first
		Matcher match = normalizeDate.matcher(date);
		if( !match.matches() ) {
			throw new IllegalArgumentException("Not an HTTP date: " + date);
		}
		date = match.replaceFirst(EXPIRE_REPLACE);

		// now parse
		return DateTimeFormat.forPattern(DATE_FORMAT).withZoneUTC().parseDateTime(date);
	}
}
