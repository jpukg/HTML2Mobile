package edu.gatech.cc.HTML2Mobile.proxy;

import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import edu.gatech.cc.HTML2Mobile.ExtractorException;
import edu.gatech.cc.HTML2Mobile.IExtractor;

/**
 * An extractor that rewrites links for proxy purposes.
 * <p>
 * Note this extractor does not produce any output, it
 * simply modifies URLs in the document.
 * </p>
 */
public class LinkProxyExtractor implements IExtractor {
	/** The rewriter. */
	protected LinkRewriter rewriter;

	/**
	 * Creates a link proxy extractor with no initial rewriter.
	 * <p><strong>
	 * You must set a rewriter before calling <code>extract(Document, Writer)</code>.
	 * </strong></p>
	 */
	public LinkProxyExtractor() {	}

	/**
	 * Creates a link proxy extractor with a given rewriter.
	 * @param rewriter the rewriter
	 */
	public LinkProxyExtractor(LinkRewriter rewriter) {
		this.rewriter = rewriter;
	}

	/**
	 * Convenience constructor that creates the rewriter using the given arguments.
	 * @see LinkRewriter#LinkRewriter(String, URL)
	 */
	public LinkProxyExtractor(String proxyURI, URL requestURL) {
		this.rewriter = new LinkRewriter(proxyURI, requestURL);
	}

	/**
	 * {@inheritDoc}
	 * Rewrites URLs in <code>doc</code>.
	 */
	@Override
	public void extract(Document doc, Writer out) throws ExtractorException {
		if( rewriter == null ) {
			throw new ExtractorException("No link rewriter has been set.");
		}

		try {
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
					throw new ExtractorException("Unexpected node type: " + node);
				}

				el.attr(attr, rewriter.rewriteProxiedResource(el.attr(attr)));
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
					throw new ExtractorException("Unexpected node type: " + node);
				}

				el.attr(attr, rewriter.rewriteDirectResource(el.attr(attr)));
			}
		} catch( MalformedURLException e ) { // thrown by LinkRewriter methods
			throw new ExtractorException(e);
		}
	}

	public LinkRewriter getRewriter() {
		return rewriter;
	}

	public void setRewriter(LinkRewriter rewriter) {
		this.rewriter = rewriter;
	}
}
