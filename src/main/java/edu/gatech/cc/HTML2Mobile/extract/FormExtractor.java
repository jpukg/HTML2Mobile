package edu.gatech.cc.HTML2Mobile.extract;

import java.io.IOException;
import java.io.Writer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.gatech.cc.HTML2Mobile.Parser.FormObj;

/**
 * Extractor that pulls <code>&lt;form&gt;</code> elements.
 */
public class FormExtractor implements IExtractor {
	@Override
	public void extract(Document doc, Writer out) throws ExtractorException {
		if( doc == null ) {
			throw new NullPointerException("doc is null");
		}
		if( out == null ) {
			throw new NullPointerException("out is null");
		}

		try {
			Elements forms = doc.select("form");
			out.append("\n\t<forms><count>").append(String.valueOf(forms.size())).append("</count>\n");
			for (Element e : forms) {
				FormObj fobj = new FormObj(e);
				out.append("\t").append(fobj.toHtml2Mobile_2()).append("\n");
			}
			out.append("\n\t\t</forms>");
		} catch( IOException e ) {
			throw new ExtractorException(e);
		} catch( IllegalStateException e ) {
			// can be thrown by JSoup
			throw new ExtractorException(e);
		}
	}
}
