package edu.gatech.cc.HTML2Mobile.extract;

import java.io.IOException;
import java.io.Writer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.gatech.cc.HTML2Mobile.Parser.BaseGroup;

/**
 * Extractor that pulls <code>&lt;iframe&gt;</code> elements.
 */
public class IFrameExtractor implements IExtractor {
	@Override
	public void extract(Document doc, Writer out) throws ExtractorException {
		if( doc == null ) {
			throw new NullPointerException("doc is null");
		}
		if( out == null ) {
			throw new NullPointerException("out is null");
		}

		try {

			Elements eles = doc.select("iframe");
			BaseGroup group = new BaseGroup("iframe", "iframegroup");
			for( Element a : eles ) {
				group.addElement(a);
			}
			// FIXME would be better to append directly into the Writer
			// instead of building the whole string first
			out.append(group.toString());

		} catch( IOException e ) {
			throw new ExtractorException(e);
		} catch( IllegalStateException e ) {
			// can be thrown by JSoup
			throw new ExtractorException(e);
		}
	}
}
