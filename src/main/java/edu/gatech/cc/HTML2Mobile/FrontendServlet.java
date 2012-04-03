package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.sun.jersey.api.client.ClientResponse;

import edu.gatech.cc.HTML2Mobile.extract.ContentExtractor;
import edu.gatech.cc.HTML2Mobile.extract.ExtractionController;
import edu.gatech.cc.HTML2Mobile.extract.ExtractorException;
import edu.gatech.cc.HTML2Mobile.extract.FormExtractor;
import edu.gatech.cc.HTML2Mobile.extract.IFrameExtractor;
import edu.gatech.cc.HTML2Mobile.extract.LinkExtractor;
import edu.gatech.cc.HTML2Mobile.extract.MediaExtractor;
import edu.gatech.cc.HTML2Mobile.helper.DebugUtil;
import edu.gatech.cc.HTML2Mobile.proxy.LinkProxyExtractor;
import edu.gatech.cc.HTML2Mobile.proxy.LinkRewriter;
import edu.gatech.cc.HTML2Mobile.proxy.RequestProxy;
import edu.gatech.cc.HTML2Mobile.transform.TransformController;
import edu.gatech.cc.HTML2Mobile.transform.XSLTransformer;

/**
 * HTML2Mobile front-end servlet.
 */
public class FrontendServlet extends HttpServlet {
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

		// run the main extract/transform logic
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
	 * Runs the extractors and returns the result.
	 * 
	 * @param doc the document to extract content from
	 * @param req the servlet request
	 * @return the extracted XML document
	 * @throws ExtractorException if thrown by child extractors
	 * @throws NullPointerException if either argument is <code>null</code>
	 */
	protected String extract(Document doc, HttpServletRequest req) {
		if( doc == null ) {
			throw new NullPointerException("doc is null");
		}
		if( req == null ) {
			throw new NullPointerException("req is null");
		}

		URL url = (URL)req.getAttribute(RequestProxy.ATTR_REMOTE_URL);
		String requestURI = req.getRequestURI() + "?url=";

		ExtractionController extraction = new ExtractionController(
				new LinkProxyExtractor(requestURI, url),
				new LinkExtractor(),
				new ContentExtractor(ContentExtractor.COUNT), // FIXME settings?
				new FormExtractor(),
				new IFrameExtractor(),
				new MediaExtractor());

		return extraction.extract(doc);
	}

	/**
	 * Transforms the XML document to the final output.
	 * 
	 * @param contents the extracted contents to transform
	 * @return the transformed contents
	 */
	protected String transform(StringBuffer contents) {
		if( contents == null ) {
			throw new NullPointerException("contents is null");
		}

		TransformController transformer = new TransformController(new XSLTransformer());

		transformer.transform(contents);
		return contents.toString();
	}

	/**
	 * Processes the document, first extracting and then transforming it.
	 * 
	 * @param doc the parsed HTML document
	 * @param req the servelet request
	 * @returns the final output document contents
	 * 
	 * @throws ExtractorException if there is a problem extracting content
	 * @throws TransformerException if there is a problem transforming the output
	 * @throws NullPointerException if either argument is <code>null</code>
	 */
	public String process(Document doc, HttpServletRequest req) {
		StringBuffer extracted = new StringBuffer(this.extract(doc, req));
		//return extracted.toString();
		return this.transform(extracted);
	}
}
