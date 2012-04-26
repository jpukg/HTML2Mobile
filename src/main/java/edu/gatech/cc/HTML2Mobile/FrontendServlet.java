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
import org.jsoup.nodes.Element;

import com.sun.jersey.api.client.ClientResponse;

import edu.gatech.cc.HTML2Mobile.extract.ContentExtractor;
import edu.gatech.cc.HTML2Mobile.extract.ExtractionController;
import edu.gatech.cc.HTML2Mobile.extract.ExtractorException;
import edu.gatech.cc.HTML2Mobile.extract.FormExtractor;
import edu.gatech.cc.HTML2Mobile.extract.IFrameExtractor;
import edu.gatech.cc.HTML2Mobile.extract.LinkExtractor;
import edu.gatech.cc.HTML2Mobile.extract.MediaExtractor;
import edu.gatech.cc.HTML2Mobile.extract.StripTagsExtractor;
import edu.gatech.cc.HTML2Mobile.helper.DebugUtil;
import edu.gatech.cc.HTML2Mobile.proxy.LinkProxyExtractor;
import edu.gatech.cc.HTML2Mobile.proxy.LinkRewriter;
import edu.gatech.cc.HTML2Mobile.proxy.ProxyException;
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
		PrintWriter writer = resp.getWriter();

		if( DEBUG ) {
			DebugUtil.dumpRequestInfo(req);
		}

		URL url = this.getProxiedURL(req);
		if(url == null) {
			writer.println("<html>");
			writer.println("<head>");
			writer.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />");
			writer.println("<link rel='stylesheet' href='http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.css' />");
			writer.println("<script src='http://code.jquery.com/jquery-1.6.4.min.js'></script>");
			writer.println("<script src='http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.js'></script>");
			writer.println("</head>");
			writer.println("<body>");
			writer.println("<h1>Welcome to HTML2Mobile!</h1>");
			writer.println("<p>Please enter a URL to continue:</p>");
			writer.println("<form method='get'>");
			writer.println("<input name='url' type='text' placeholder='http://t-square.gatech.edu' x-webkit-speech />");
			writer.println("<input name='Submit' type='submit' value='Go!'/>");
			writer.println("</form>");
			writer.println("</body>");
			writer.println("</html>");
			return;
		}

		RequestProxy proxy = new RequestProxy();
		ClientResponse response = proxy.proxyRequest(url, req, resp);

		// parse the document
		String contents = response.getEntity(String.class);
		Document doc = Jsoup.parse(contents);

		// expand iframe elements
		this.expandIFrames(doc, proxy, req, resp);

		// run the main extract/transform logic
		String output = this.process(doc, req);

		// write out the new contents
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
			return null;
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
	 * Fetches contents of <code>&lt;iframe&rt;</code> elements in <code>doc</code>
	 * and appends the <code>&lt;body&gt;</code> to the <code>&lt;iframe&rt;</code>
	 * element.
	 * 
	 * <p>This method modifies <code>doc</code> in place.  It does not recurse
	 * if there are <code>&lt;iframe&rt;</code> elements in the fetched content.
	 * Errors are printed to the console but otherwise suppressed.
	 * </p>
	 * 
	 * @param doc   the top-level page document
	 * @param proxy the request proxy
	 * @param req   the current servlet request
	 * @param resp  the current servlet response
	 * @throws NullPointerException if any argument is <code>null</code>
	 */
	protected void expandIFrames(Document doc, RequestProxy proxy, HttpServletRequest req,
			HttpServletResponse resp) {
		if( doc == null ) {
			throw new NullPointerException("doc is null");
		}
		if( proxy == null ) {
			throw new NullPointerException("proxy is null");
		}
		if( req == null ) {
			throw new NullPointerException("req is null");
		}
		if( resp == null ) {
			throw new NullPointerException("resp is null");
		}

		for( Element iframe : doc.select("iframe") ) {
			String srcAttr = iframe.attr("src");

			if( srcAttr == null ) {
				continue;
			}

			// add protocol if not present
			if( !LinkRewriter.hasHost(srcAttr) ) {
				srcAttr = req.getScheme() + "://" + srcAttr;
			} else if ( srcAttr.startsWith("//") ) {
				srcAttr = req.getScheme() + ":" + srcAttr;
			}

			try {
				// request frame contents
				URL srcURL = new URL(srcAttr);
				ClientResponse frameResponse = proxy.proxyRequest(srcURL, req, resp);

				// append body to iframe element in the original document
				Document frameDoc = Jsoup.parse(frameResponse.getEntity(String.class));
				iframe.appendChild(frameDoc.body());

				// record errors but don't fail
			} catch( ProxyException e ) {
				System.err.println("Failed to fetch frame contents from: " + srcAttr);
				System.err.println("ProxyException message: " + e.getMessage());
			} catch( MalformedURLException e ) {
				System.err.println("Failed to fetch frame contents from: " + srcAttr);
				System.err.println("MalformedURLException message: " + e.getMessage());
			} catch( IllegalArgumentException e ) {
				System.err.println("Failed to parse frame contents from: " + srcAttr);
				System.err.println("IllegalArgumentException message: " + e.getMessage());

				// print this one in case it wasn't Jsoup as expected
				e.printStackTrace();
			}
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
				new StripTagsExtractor(),
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
	 * @param req the servlet request
	 * @returns the final output document contents
	 * 
	 * @throws ExtractorException if there is a problem extracting content
	 * @throws TransformerException if there is a problem transforming the output
	 * @throws NullPointerException if either argument is <code>null</code>
	 */
	public String process(Document doc, HttpServletRequest req) {
		String extracted = this.extract(doc, req);
		if( DEBUG ) {
			System.out.println("EXTRACT RESULT:\n" + extracted);
		}
		String xslParam = req.getParameter("xmlOnly");
		if( xslParam != null ) {
			return extracted;
		}
		String transformed = this.transform(new StringBuffer(extracted));
		if( DEBUG ) {
			System.out.println("TRANSFORMED RESULT:\n" + transformed);
		}
		return transformed;
	}
}
