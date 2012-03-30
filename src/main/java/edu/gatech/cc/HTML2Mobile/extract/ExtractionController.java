package edu.gatech.cc.HTML2Mobile.extract;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoup.nodes.Document;

/**
 * Controller for extracting content from a JSoup {@link Document}.
 */
public class ExtractionController {
	/** The content extractors. */
	protected List<IExtractor> extractors;

	/**
	 * Creates an extraction controller.
	 * @param extractors the initial extractor list
	 * @throws NullPointerException if <code>extractors</code> is <code>null</code>
	 *                              or contains a <code>null</code> element
	 */
	public ExtractionController(IExtractor... extractors) {
		if( extractors == null ) {
			throw new NullPointerException("extractors is null");
		}
		this.extractors = createExtractorList(extractors.length);
		addExtractors(extractors);
	}

	/**
	 * Instantiates the list implementation.
	 * 
	 * @param size the initial size, a negative value
	 *             should be interpreted as using the implementation default
	 * @return the list implementation
	 */
	protected List<IExtractor> createExtractorList(int size) {
		if( size < 0 ) {
			return new ArrayList<IExtractor>();
		} else {
			return new ArrayList<IExtractor>(size);
		}
	}

	/**
	 * Extracts the contents and returns the result.
	 * 
	 * @param doc the document to extract content from
	 * @return the extraction result
	 * @throws ExtractorException if thrown by a child extractor
	 * @throws NullPointerException if <code>doc</code> is <code>null</code>
	 */
	public String extract(Document doc) throws ExtractorException {
		if( doc == null ) {
			throw new NullPointerException("doc is null");
		}

		StringWriter writer = new StringWriter();
		writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<html2mobile>\n");
		for( IExtractor extractor : this.extractors ) {
			extractor.extract(doc, writer);
		}
		writer.append("\n</html2mobile>\n");

		return writer.toString();
	}

	/**
	 * Adds one or more extractors.
	 * @param extractors the extractors to add
	 * @throws NullPointerException if any extractor is <code>null</code>
	 */
	public void addExtractors(IExtractor... extractors) {
		if( extractors != null ) {
			for( IExtractor extractor : extractors ) {
				if( extractor == null ) {
					throw new NullPointerException("an extractor is null");
				}
				this.extractors.add(extractor);
			}
		}
	}

	/**
	 * Removes one or more extractors.
	 * @param extractors the extractors to remove
	 */
	public void removeExtractors(IExtractor... extractors) {
		if( extractors != null ) {
			this.extractors.removeAll(Arrays.asList(extractors));
		}
	}

	/**
	 * Returns an unmodifiable view of the extractors.
	 * @return the extractors
	 */
	public List<IExtractor> getExtractors() {
		return Collections.unmodifiableList(extractors);
	}
}
