package edu.gatech.cc.HTML2Mobile.Parser;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

public class LinkGroup {

	private static List<Element> orphans;

	static {
		orphans = new LinkedList<Element>();
	}

	private static int creationCount = 0;

	public static enum LinkGroupFormats {
		list, table, other
	}

	private String name;
	private Element hash;
	private List<Element> links;
	private LinkGroupFormats format;
	private int creationId;

	public boolean valid() {
		return links.size() >= LinkParserServlet.MIN_GROUP_SIZE;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name != null) {
			this.name = name;
		}
	}

	public LinkGroupFormats getFormat() {
		return format;
	}

	public LinkGroup(LinkGroupFormats format) {
		this(null, null, format);
	}

	public LinkGroup(Element ele, LinkGroupFormats format, boolean unused) {
		this(ele.attr("name") != null ? ele.attr("name")
				: ele.attr("id") != null ? ele.attr("id") : ele.className(),
						ele, format);
	}

	public LinkGroup(String name, Element ele, LinkGroupFormats format) {
		this.name = name;
		this.hash = ele;
		this.format = format;
		this.creationId = creationCount++;
		links = new LinkedList<Element>();
	}

	public void add(Element link) {
		if (link != null && link.html().length() > 0) {
			links.add(link);
		}
	}

	public String toXmlString() {
		StringBuilder sb = new StringBuilder();
		if (valid()) {
			sb.append("\t<linkgroup><count>").append(links.size()).append("</count>\n");
			for (Element a : links) {

				sb.append("\t\t<link>\n");
				sb.append("\t\t\t<text><![CDATA["+StringEscapeUtils.escapeXml(a.text())+"]]></text>\n");
				for(Attribute attrib : a.attributes()) {
					if(!attrib.getValue().isEmpty()) {
						sb.append("\t\t\t<" + attrib.getKey() + "><![CDATA[" + attrib.getValue() + "]]></" + attrib.getKey() + ">\n");
					}
				}
				sb.append("\t\t</link>\n");
				//sb.append("\t\t<link><![CDATA[").append(a.toString()).append("]]></link>\n");
			}
			sb.append("\t</linkgroup>\n");
		} else {
			orphans.addAll(links);
		}
		return sb.toString();
	}



	public static String orphansToXmlString() {
		StringBuilder sb = new StringBuilder();
		if (orphans.size() > 0) {
			sb.append("\t<linkgroup><count>orphans links</count>\n");
			for (Element a : orphans) {

				sb.append("\t\t<link>\n");
				sb.append("\t\t\t<text><![CDATA["+StringEscapeUtils.escapeXml(a.text())+"]]></text>\n");
				for(Attribute attrib : a.attributes()) {
					if(!attrib.getValue().isEmpty()) {
						sb.append("\t\t\t<" + attrib.getKey() + "><![CDATA[" + attrib.getValue() + "]]></" + attrib.getKey() + ">\n");
					}
				}
				sb.append("\t\t</link>\n");
				//sb.append("\t\t<link><![CDATA[").append(a.toString()).append("]]></link>\n");
			}
			sb.append("\t</linkgroup>\n");
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		if (hash != null) {
			return hash.hashCode();
		}
		if (name != null) {
			return name.hashCode();
		}
		return creationId;
	}
}
