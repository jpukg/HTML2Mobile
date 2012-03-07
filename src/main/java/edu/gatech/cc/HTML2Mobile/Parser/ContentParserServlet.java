package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import edu.gatech.cc.HTML2Mobile.JSoupServlet;


@SuppressWarnings("serial")



public class ContentParserServlet extends JSoupServlet {


	ArrayList<String> skip_tags = new ArrayList<String>();

	@Override
	public String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {
		BaseGroup group = new BaseGroup("iframe","iframegroup");
		Element body = doc.body();

		skip_tags.add("script");
		//skip_tags.add("a");

		String output="";
		//output += "<html dir=\"ltr\" lang=\"en-US\">";


		output+=this.stripSearchElement(body);

		return output;
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
