package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.sun.jersey.api.client.ClientResponse;

import edu.gatech.cc.HTML2Mobile.helper.DebugUtil;
import edu.gatech.cc.HTML2Mobile.proxy.LinkProxyExtractor;
import edu.gatech.cc.HTML2Mobile.proxy.LinkRewriter;
import edu.gatech.cc.HTML2Mobile.proxy.RequestProxy;

/** Proof of concept proxying servlet. */
public class DumbProxyServlet extends JSoupServlet {
	//
	// Static fields and methods
	private static final long serialVersionUID = 1L;

	/** Compile-time constant for logging debug info. */
	private static final boolean DEBUG = false;

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
	 * Reads and parses the <code>url</code> parameter.
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
		if( !LinkRewriter.hasHost(urlParam) ) {
			urlParam = req.getScheme() + "://" + urlParam;
		} else if ( urlParam.startsWith("//") ) {
			urlParam = req.getScheme() + ":" + urlParam;
		}

		// now parse
		try {
			URL url = new URL(urlParam);
			if( DEBUG ) {
				System.out.println("URL=" + url.toExternalForm());
			}
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
		URL url = (URL)req.getAttribute(RequestProxy.ATTR_REMOTE_URL);
		String requestURI = req.getRequestURI() + "?url=";

		ExtractionController extraction = new ExtractionController(
			new LinkProxyExtractor(requestURI, url));

		extraction.extract(doc);

		return doc.toString();
	}
}
