package edu.gatech.cc.HTML2Mobile.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import edu.gatech.cc.HTML2Mobile.helper.DateUtil;

/**
 * Jersey client filter that repairs certain date-related headers.
 * 
 * In certain contexts, e.g. App Engine, headers can be rewritten
 * such that dates like <code>'Tue, 10-....'</code> will be split
 * into two entries by the comma.  A <code>HeaderRepairFilter</code>
 * will recombine these entries back into a single header entry.
 */
public class HeaderRepairFilter extends ClientFilter {

	/** For matching broken 'expires=' entries. */
	protected static final Pattern isPartialExpires =
			Pattern.compile(".*expires=\\w+$", Pattern.CASE_INSENSITIVE);

	/** Date headers to be processed. */
	protected final Set<String> dateHeaders = new HashSet<String>(
			Arrays.asList("date", "expires", "last-modified"));

	/**
	 * {@inheritDoc}
	 * Repairs the response headers before returning them.
	 */
	@Override
	public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
		ClientResponse response = getNext().handle(cr);

		repairHeaders(response);

		return response;
	}

	/**
	 * Repairs the headers in-place on the given response.
	 * @param response the response
	 */
	protected void repairHeaders(ClientResponse response) {
		if( response == null ) {
			throw new NullPointerException("response is null");
		}

		for( Entry<String, List<String>> entry : response.getHeaders().entrySet() ) {
			String header = entry.getKey();
			List<String> values = entry.getValue();

			List<String> newValues = values;
			if( isDateHeader(header) && values.size() > 1 ) {
				newValues = repairDateValues(values);
			} else {
				header = header.trim().toLowerCase();
				if( header.startsWith("set-cookie") ) {
					newValues = repairCookieValues(values);
				}
			}

			// update if changed
			if( newValues.size() != values.size() ) {
				entry.setValue(newValues);
			}
		}
	}

	/**
	 * Recombines 'set-cookie' header entries if one ends with
	 * a broken 'expires=' entry.
	 * 
	 * @param values the value to repair
	 * @return the new header values
	 */
	protected List<String> repairCookieValues(List<String> values) {
		if( values == null ) {
			throw new NullPointerException("values is null");
		}

		int nvals = values.size();
		ArrayList<String> newVals = new ArrayList<String>(nvals);
		for( int i = 0; i < nvals; ++i ) {
			String val = values.get(i);

			if( isPartialExpires.matcher(val).matches() ) {
				if( ++i >= nvals ) {
					System.err.println("WARN: No next value to merge with.");
				} else {
					val = val + ", " + values.get(i);
				}
			}

			newVals.add(val);
		}
		return newVals;
	}

	/**
	 * Merges two date header values.
	 * 
	 * @param values the values, does nothing  if length < 2
	 * @return the new values
	 */
	protected List<String> repairDateValues(List<String> values) {
		if( values == null ) {
			throw new NullPointerException("values is null");
		}

		int nvals = values.size();
		if( nvals < 2 ) {
			return values;
		}
		if( nvals != 2 ) {
			System.err.println("Unexpected number of date values: " + values);
		}

		ArrayList<String> newVals = new ArrayList<String>(2);
		String newVal = values.get(0) + ", " + values.get(1);
		if( DateUtil.isHttpDate(newVal) ) {
			newVals.add(newVal);
		} else {
			System.err.println("Combined value is not a date: " + newVal);
			newVals.add(values.get(0));
			newVals.add(values.get(1));
		}

		return newVals;
	}

	/** Add an additional date header. */
	public boolean addDateHeader(String header) {
		if( header == null ) {
			throw new NullPointerException("header is null");
		}
		return dateHeaders.add(header.trim().toLowerCase());
	}

	/** Remove a date header. */
	public boolean removeDateHeader(String header) {
		if( header == null ) {
			throw new NullPointerException("header is null");
		}
		return dateHeaders.remove(header);
	}

	/**
	 * Checks whether the given header is a date header that may
	 * need to be repaired.
	 * 
	 * @param header
	 * @return
	 */
	public boolean isDateHeader(String header) {
		if( header == null ) {
			throw new NullPointerException("header is null");
		}
		return dateHeaders.contains(header.trim().toLowerCase());
	}
}
