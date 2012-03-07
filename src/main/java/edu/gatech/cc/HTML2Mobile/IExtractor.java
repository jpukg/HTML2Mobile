package edu.gatech.cc.HTML2Mobile;

import java.io.Writer;

import org.jsoup.nodes.Document;

/**
 * An extractor that extracts content from an HTML document.
 */
public interface IExtractor {
	/**
	 * Extracts content from <code>doc</code> and writes it to <code>out</code>.
	 * 
	 * @param doc the parsed HTML document
	 * @param out the output writer for the result
	 * @throws ExtractorException   if there is any problem extracting content
	 * @throws NullPointerException if any argument is <code>null</code>
	 */
	public void extract(Document doc, Writer out) throws ExtractorException;
}
