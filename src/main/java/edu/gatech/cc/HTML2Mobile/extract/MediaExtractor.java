package edu.gatech.cc.HTML2Mobile.extract;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Extractor for various media elements, e.g. <code>&lt;img&gt;</code>.
 */
public class MediaExtractor implements IExtractor {

	@Override
	public void extract(Document doc, Writer out) throws ExtractorException {
		if( doc == null ) {
			throw new NullPointerException("doc is null");
		}
		if( out == null ) {
			throw new NullPointerException("out is null");
		}

		try {
			out.append("<media>\n");

			// Grab Pictures
			Elements imgs = doc.select("img[src]");
			out.append("\t<pictures><count>");
			out.append(String.valueOf(imgs.size()));
			out.append("</count>\n");
			for(Element media : imgs) {
				out.append("\t\t<picture>\n");
				out.append(printAttributes(media.attributes()));
				out.append("\t\t</picture>\n");
			}
			out.append("\t</pictures>\n");

			/*
			 * Do we want to support embedded youtube videos as well? (Flash)
			 * I think they are doing it in an iframe now so we may be able
			 * to force it to "fallback" to html5. Check the source of:
			 * 
			 * http://www.youtube.com/embed/kPdYpST_yoE?version=3&amp;rel=1&amp;fs=1&amp;showsearch=0&amp;showinfo=1&amp;iv_load_policy=1&amp;wmode=transparent
			 */
			// Grab Videos(HTML5)
			Elements vids = doc.select("video");
			Elements youtubeVids = doc.select("embed[src*=http://www.youtube.com], iframe[src*=http://www.youtube.com]");
			out.append("\t<videos><count>");
			out.append(String.valueOf(vids.size() + youtubeVids.size()));
			out.append("</count>\n");
			for(Element media : vids) {
				out.append("\t\t<video type='html5'>\n");
				out.append(printAttributes(media.attributes()));
				for(Element source : media.children()) {
					out.append("\t\t\t<").append(source.tagName()).append(">\n<![CDATA[");
					out.append(printAttributes(source.attributes()));
					out.append("]]></").append(source.tagName()).append(">\n");
				}
				out.append("\t\t</video>\n");
			}

			// Grab Videos(Youtube)
			for(Element media : youtubeVids) {
				out.append("\t\t<video type='youtube'>\n<![CDATA[");
				out.append(printAttributes(media.attributes()));
				out.append("]]>\t</video>\n");
			}

			out.append("\t</videos>\n");

			// Grab Audio(HTML5)
			Elements aud = doc.select("audio");
			out.append("\t<audios><count>");
			out.append(String.valueOf(aud.size()));
			out.append("</count>\n");
			for(Element media : aud) {
				out.append("\t\t<audio>\n");
				//				out.append("<![CDATA[>");
				out.append(printAttributes(media.attributes()));
				for(Element source : media.children()) {
					out.append("\t\t\t<" + source.tagName() + ">\n<![CDATA[");
					out.append(printAttributes(source.attributes()));
					out.append("]]>\t\t\t</" + source.tagName() + ">\n");
				}
				//				out.append("]]>");
				out.append("\t\t</audios>\n");
			}
			out.append("\t</audios>\n");

			out.append("</media>\n");
		} catch( IOException e ) {
			throw new ExtractorException(e);
		} catch( IllegalStateException e ) {
			// can be thrown by JSoup
			throw new ExtractorException(e);
		}
	}

	private String printAttributes(Attributes attribs) {
		StringBuilder toRet = new StringBuilder();
		for(Attribute attrib : attribs) {
			if(!attrib.getValue().isEmpty()) {
				toRet.append("\t\t<" + attrib.getKey() + "><![CDATA[" + attrib.getValue() + "]]></" + attrib.getKey() + ">\n");
				toRet.append("\t\t<").append(attrib.getKey()).append("><![CDATA[")
				.append(StringEscapeUtils.escapeXml(attrib.getValue()))
				.append("]]></").append(attrib.getKey()).append(">\n");//+ ">" + StringEscattrib.getValue() + "</" + attrib.getKey() + ">\n");
			}
		}
		return toRet.toString();
	}
}
