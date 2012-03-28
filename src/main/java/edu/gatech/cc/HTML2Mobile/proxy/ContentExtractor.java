package edu.gatech.cc.HTML2Mobile.proxy;

import java.io.Writer;
import java.util.ArrayList;

import org.jsoup.nodes.Attribute;
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
public class ContentExtractor implements IExtractor {

	ArrayList<String> skip_tags = new ArrayList<String>();
	/**
	 * {@inheritDoc}
	 * Rewrites URLs in <code>doc</code>.
	 */
	@Override
	public void extract(Document doc, Writer out) throws ExtractorException {

		try {

			Element body = doc.body();

			skip_tags.add("script");

			String output="<content>";

			output+=this.stripSearchElement(body) +  "</content>";

			out.append(output);

		} catch(Exception e ) {
			throw new ExtractorException(e);
		}
	}


	public int numChildren(Element ele){
		return ele.children().size();
	}


	public String stripSearchElement(Element main){


		String output = "";


		String inner_output = "";

		if(main.ownText().length()>10) {
			inner_output+=main.ownText();
		}

		for(Element ele: main.children()){

			if(skip_tags.contains(ele.tagName())) {
				inner_output+=ele.ownText();
				continue;
			}

			if(ele.hasText()) {

				inner_output += stripSearchElement(ele);
			}



		}
		if(inner_output.isEmpty()) {
			return "";
		}

		output+=this.upperTag(main);
		output+=inner_output;
		output += this.endingTag(main);

		return output;

	}

	public String upperTag(Element ele){

		String tag = ele.tagName();

		String output  = "<" + tag;

		for(Attribute attr : ele.attributes()) {
			output+=" " + attr.getKey() + "=\"" + attr.getValue()  + "\"";
		}



		output += ">";
		return output;
	}

	public String endingTag(Element ele){
		return "</" + ele.tagName() + ">";
	}



}
