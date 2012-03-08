package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sun.jersey.api.client.ClientResponse;

import edu.gatech.cc.HTML2Mobile.helper.DebugUtil;
import edu.gatech.cc.HTML2Mobile.proxy.RequestProxy;

/** Proof of concept proxying servlet. */
public class DumbProxyServlet extends JSoupServlet {
	//
	// Static fields and methods
	private static final long serialVersionUID = 1L;

	/** Compile-time constant for logging debug info. */
	private static final boolean DEBUG = false;

	/** Constant for logging URL rewrites, separate from DEBUG. */
	private static final boolean DEBUG_REWRITES = false;

	/**
	 * Attribute name where the remote URL will be stored, to go away soon.
	 * @deprecated
	 */
	@Deprecated
	protected static final String ATTR_REMOTE_URL = "proxyURL";

	/** Host matching pattern. */
	private static final Pattern hasHost =
			Pattern.compile("^(https?:)?//.*", Pattern.CASE_INSENSITIVE);

	/**
	 * Checks if the given url includes a host part.
	 * 
	 * @param url the url to check
	 * @return <code>true</code> if the URL has a host part
	 */
	public static boolean hasHost(String url) {
		if( url == null ) {
			throw new NullPointerException("url is null");
		}
		return hasHost.matcher(url).matches();
	}

	/**
	 * Rewrites a URL to be absolute and with a host.
	 * 
	 * @param requestURL the base URL of the request
	 * @param targetURL  the resource URL to rewrite
	 * @return the rewritten URL or <code>targetURL</code> if it already includes the host
	 * @throws MalformedURLException if the rewritten URL is not well-formed
	 */
	public static String rewriteDirectResource(URL requestURL, String targetURL)
			throws MalformedURLException {
		// no rewrite when the host is included
		if( hasHost.matcher(targetURL).matches() ) {
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
		URL newTarget = new URL(requestURL.getProtocol(), requestURL.getHost(), requestURL.getPort(), targetURL);
		return newTarget.toExternalForm();
	}

	//
	// Instance fields / methods

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO: move into JSoupServlet
		if( DEBUG ) {
			DebugUtil.dumpRequestInfo(req);
		}

		URL url = this.getProxiedURL(req);

		RequestProxy proxy = new RequestProxy();
		ClientResponse response = proxy.proxyRequest(url, req, resp);
		req.setAttribute(ATTR_REMOTE_URL, req.getAttribute(RequestProxy.ATTR_REMOTE_URL));

		// parse the document
		String contents = response.getEntity(String.class);
		Document doc = Jsoup.parse(contents);

		// this implementation rewrites various element urls
		String output = this.process(doc, req);

		// write out the new contents
		PrintWriter writer = resp.getWriter();
		writer.write(output);
		writer.flush();
	}

	/**
	 * Reads and parses the <code>url</code> parameter.  Also assigns the
	 * result to the {@link #ATTR_REMOTE_URL} attribute of <code>req</code>.
	 * 
	 * @param req the proxy servlet request
	 * @return the parsed URL
	 * @throws ServletException if the <code>url</code> parameter is missing or unparseable
	 */
	protected URL getProxiedURL(HttpServletRequest req) throws ServletException {
		// read requested URL
		String urlParam = req.getParameter("url");
		if( urlParam == null ) {
			throw new ServletException("No URL");
		}

		// add protocol if not present
		if( !hasHost(urlParam) ) {
			try {
				urlParam = new URI(req.getRequestURI()).getScheme() + "://" + urlParam;
			} catch( URISyntaxException e ) {
				throw new ServletException(e);
			}
		}

		// now parse
		try {
			URL url = new URL(urlParam);
			if( DEBUG ) {
				System.out.println("URL=" + url.toExternalForm());
			}
			req.setAttribute(ATTR_REMOTE_URL, url);
			return url;
		} catch( MalformedURLException e ) {
			throw new ServletException("Cannot parse: " + urlParam, e);
		}
	}

	/**
	 * {@inheritDoc}
	 * This implementation rewrites various URLs to either proxy through us or to
	 * hit the remote server directly.
	 */
	@Override
	public String process(Document doc, HttpServletRequest req) throws ServletException, IOException {
		URL url = (URL)req.getAttribute(ATTR_REMOTE_URL);
		String requestURI = req.getRequestURI() + "?url=";

		// rewrite elements that need to proxy through us
		for( Element el : doc.select("a[href], form[action], iframe[src]") ) {
			String node = el.nodeName();
			String attr;
			if( "a".equalsIgnoreCase(node) ) {
				attr = "href";
			} else if( "form".equalsIgnoreCase(node) ) {
				attr = "action";
			} else if( "iframe".equalsIgnoreCase(node) ) {
				attr = "src";
			} else {
				throw new ServletException("Unexpected node type: " + node);
			}

			String oldVal = el.attr(attr);
			String newVal = requestURI + URLEncoder.encode(rewriteDirectResource(url, oldVal), "UTF-8");
			el.attr(attr, newVal);

			if( DEBUG_REWRITES ) {
				System.out.println("Rewrote: '" + oldVal + "' -> '" + newVal + "'");
			}
		}

		// rewrite elements that should not proxy through us
		for( Element el : doc.select("img[src], script[src], link[href]") ) {
			String node = el.nodeName().toLowerCase();
			String attr;
			if( "link".equals(node) ) {
				attr = "href";
			} else if ( "img".equals(node) || "script".equals(node) ) {
				attr = "src";
			} else {
				throw new ServletException("Unexpected node type: " + node);
			}

			String oldVal = el.attr(attr);
			String newVal = rewriteDirectResource(url, oldVal);
			el.attr(attr, newVal);

			if( DEBUG_REWRITES ) {
				System.out.println("Rewrote: '" + oldVal + "' -> '" + newVal + "'");
			}
		}

		return doc.toString();
	}
}
