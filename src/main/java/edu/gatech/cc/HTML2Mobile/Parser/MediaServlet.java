package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.gatech.cc.HTML2Mobile.JSoupServlet;

@SuppressWarnings("serial")
public class MediaServlet extends JSoupServlet {

	@Override
	public String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {
		StringBuilder toRet = new StringBuilder("<media>\n");

		// Grab Pictures
		Elements imgs = doc.select("img[src]");
		toRet.append("\t<pictures><count>").append(imgs.size()).append("</count>\n");
		for(Element media : imgs) {
			toRet.append("\t\t<picture>\n");
			toRet.append(printAttributes(media.attributes()));
			toRet.append("\t\t</picture>\n");
		}
		toRet.append("\t</pictures>\n");

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
		toRet.append("\t<videos><count>").append(vids.size() + youtubeVids.size()).append("</count>\n");
		for(Element media : vids) {
			toRet.append("\t\t<video type='html5'>\n");
			toRet.append(printAttributes(media.attributes()));
			for(Element source : media.children()) {
				toRet.append("\t\t\t<").append(source.tagName()).append(">\n");
				toRet.append(printAttributes(source.attributes()));
				toRet.append("</").append(source.tagName()).append(">\n");
			}
			toRet.append("\t\t</video>\n");
		}

		// Grab Videos(Youtube)
		for(Element media : youtubeVids) {
			toRet.append("\t\t<video type='youtube'>\n");
			toRet.append(printAttributes(media.attributes()));
			toRet.append("\t</video>\n");
		}

		toRet.append("\t</videos>\n");

		// Grab Audio(HTML5)
		Elements aud = doc.select("audio");
		toRet.append("\t<audios><count>").append(aud.size()).append("</count>\n");
		for(Element media : aud) {
			toRet.append("\t\t<audio>\n");
			toRet.append(printAttributes(media.attributes()));
			for(Element source : media.children()) {
				toRet.append("\t\t\t<" + source.tagName() + ">\n");
				toRet.append(printAttributes(source.attributes()));
				toRet.append("\t\t\t</" + source.tagName() + ">\n");
			}
			toRet.append("\t\t</audios>\n");
		}
		toRet.append("\t</audios>\n");

		toRet.append("</media>\n");
		return toRet.toString();
	}

	private String printAttributes(Attributes attribs) {
		StringBuilder toRet = new StringBuilder();
		for(Attribute attrib : attribs) {
			if(!attrib.getValue().isEmpty()) {
				toRet.append("\t\t<" + attrib.getKey() + ">" + attrib.getValue() + "</" + attrib.getKey() + ">\n");
			}
		}
		return toRet.toString();
	}
}
