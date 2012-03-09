package edu.gatech.cc.HTML2Mobile.proxy;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * A <code>LinkRewriter</code> that rewrites links in various ways.
 * <p>
 * A link rewrites can rewrite links to point directly to the target
 * host.  Relative-to-absolute conversion is done based on the
 * rewriter's <code>requestURL</code>.  Additionally, links can be
 * rewritten to pass through a proxy by doing the aforementioned
 * transformation, url-encoding the result and appending it to the
 * <code>proxyURI</code>.
 * </p>
 */
public class LinkRewriter {
	/** Flag to print rewrites. */
	private static final boolean DEBUG = false;

	/** Host matching pattern. */
	private static final Pattern hasHost =
			Pattern.compile("^(https?:)?//.*", Pattern.CASE_INSENSITIVE);

	/**
	 * Checks whether a URL includes a host part already.
	 * 
	 * @param url the URL to check
	 * @return if <code>url</code> has a host part
	 * @throws NullPointerException if <code>url</code> is <code>null</code>
	 */
	public static boolean hasHost(String url) {
		if( url == null ) {
			throw new NullPointerException("url is null");
		}
		return hasHost.matcher(url).matches();
	}

	//
	// instance fields / methods
	//

	/** The base URI for proxied links, something like <code>"/proxy?url="</code>. */
	protected String proxyURI;

	/** The request URL of the document we're rewriting links for. */
	protected URL requestURL;

	/**
	 * Creates a link rewriter that proxies with the given base URI.
	 * <p>
	 * When rewriting proxied links, the url-encoded target will be appended
	 * to <code>proxyURI</code>, so it should include a leading query parameter
	 * if needed, e.g. <code>"/proxy?url="</code>.
	 * </p>
	 * 
	 * @param proxyURI the proxy base URI
	 * @param requestURL the URL of the document links are being rewritten for
	 * @throws NullPointerException if any argument is <code>null</code>
	 */
	public LinkRewriter(String proxyURI, URL requestURL) {
		setProxyURI(proxyURI);
		setRequestURL(requestURL);
	}

	/**
	 * Rewrites a URL to be absolute and with a host.
	 * 
	 * @param targetURL  the resource URL to rewrite
	 * @return the rewritten URL or <code>targetURL</code> if it already includes the host
	 * @throws MalformedURLException if the rewritten URL is not well-formed
	 */
	public String rewriteDirectResource(String targetURL)
			throws MalformedURLException {
		if( requestURL == null ) {
			throw new NullPointerException("requestURL is null");
		}
		if( targetURL == null ) {
			throw new NullPointerException("targetURL is null");
		}

		// no rewrite when the host is included
		if( hasHost(targetURL) ) {
			return targetURL;
		}

		// make relative URLs absolute
		if( !targetURL.startsWith("/") ) {
			String path = requestURL.getPath();

			// strip off non-dir part
			int lastSlash = path.lastIndexOf('/');
			if( -1 != lastSlash ) {
				path = path.substring(0, lastSlash);
			}

			targetURL = path + "/" + targetURL;
		}

		// now point to the original server
		String newTarget = new URL(requestURL.getProtocol(), requestURL.getHost(),
			requestURL.getPort(), targetURL).toExternalForm();

		if( DEBUG ) {
			if( !newTarget.equals(targetURL) ) {
				System.err.println("Rewrote '" + targetURL + "' -> '" + newTarget + "'");
			}
		}

		return newTarget;
	}

	/**
	 * Rewrites a URL to be proxied.  The URL is made absolute,
	 * url-encoded, and then appended to the proxy URI.
	 * 
	 * @param requestURL the base URL of the request
	 * @param targetURL  the resource URL to rewrite
	 * @return the rewritten URL
	 * @throws MalformedURLException if the rewritten URL is not well-formed
	 */
	public String rewriteProxiedResource(String targetURL)
			throws MalformedURLException {
		try {
			String absoluteURL = rewriteDirectResource(targetURL);
			String proxyURL = proxyURI + URLEncoder.encode(absoluteURL, "UTF-8");
			if( DEBUG ) {
				System.err.println("Rewrote '" + targetURL + "' -> '" + proxyURL + "'");
			}
			return proxyURL;
		} catch( UnsupportedEncodingException e ) {
			// should not reach, UTF-8 is always supported
			throw new RuntimeException("UTF-8 not supported?");
		}
	}

	//
	// accessors
	//
	/**
	 * Returns the base URI for proxied links.
	 * @return the base URI
	 */
	public String getProxyURI() {
		return proxyURI;
	}

	/**
	 * Sets the base base URI for proxied links.
	 * <p>
	 * When rewriting proxied links, the url-encoded target will be appended
	 * to <code>proxyURI</code>, so it should include a leading query parameter
	 * if needed, e.g. <code>"/proxy?url="</code>.
	 * </p>
	 * 
	 * @param proxyURI the proxy base URI
	 * @throws NullPointerException if <code>proxyURI</code> is <code>null</code>
	 */
	public void setProxyURI(String proxyURI) {
		if( proxyURI == null ) {
			throw new NullPointerException("proxyURI is null");
		}
		this.proxyURI = proxyURI;
	}

	/**
	 * Returns the request URL that links are being rewritten for.
	 * @return the request URL
	 */
	public URL getRequestURL() {
		return requestURL;
	}

	/**
	 * Sets the request URL that links are being rewritten for.
	 * @param requestURL the request URL
	 * @throws NullPointerException if <code>requestURL</code> is <code>null</code>
	 */
	public void setRequestURL(URL requestURL) {
		this.requestURL = requestURL;
	}
}
