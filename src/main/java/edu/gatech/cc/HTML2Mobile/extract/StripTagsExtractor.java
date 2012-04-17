package edu.gatech.cc.HTML2Mobile.extract;

import java.io.Writer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * An extractor that removes <code>&lt;script&gt;</code>,
 * <code>&lt;style&gt;</code>, and <code>&lt;link&gt;</code>
 * tags.
 *
 */
public class StripTagsExtractor implements IExtractor {
	/**
	 * Removes <code>&lt;script&gt;</code>, <code>&lt;style&gt;</code>,
	 * and <code>&lt;link&gt;</code> tags from <code>doc</code>.
	 */
	@Override
	public void extract(Document doc, Writer out) throws ExtractorException {
		if( doc == null ) {
			throw new NullPointerException("doc is null");
		}
		Elements tags = doc.select("script, style, link");
		for( Element tag : tags ) {
			tag.remove();
		}
	}

}
