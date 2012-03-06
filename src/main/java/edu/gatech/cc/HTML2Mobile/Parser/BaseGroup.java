package edu.gatech.cc.HTML2Mobile.Parser;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

public class BaseGroup {

	private List<Element> elements;

	private String outsideTag = "outside";
	private String insideTag = "inside";


	public BaseGroup(String in,String out){
		elements = new ArrayList<Element>();
		outsideTag = out;
		insideTag = in;
	}

	public void addElement(Element e){
		elements.add(e);
	}


	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append("\t<" + outsideTag + ">\n");
		for (Element a : elements) {

			sb.append("\t\t<"+insideTag+">\n");

			sb.append("\t\t\t<text>"+a.html()+"</text>\n");
			for(Attribute attrib : a.attributes()) {
				if(!attrib.getValue().isEmpty()) {
					sb.append("\t\t\t<" + attrib.getKey() + ">" + attrib.getValue() + "</" + attrib.getKey() + ">\n");
				}
			}

			sb.append("\t\t</"+insideTag+">\n");
			//
		}
		sb.append("\t</" + outsideTag + ">\n");

		return sb.toString();

	}



}
