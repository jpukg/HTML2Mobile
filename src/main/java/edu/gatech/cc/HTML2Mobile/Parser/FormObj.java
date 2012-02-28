package edu.gatech.cc.HTML2Mobile.Parser;

import org.jsoup.nodes.Element;

public class FormObj {

	private String name;
	private String action;
	private String method;
	private String html;

	public FormObj(Element e) {
		this(e.attr("action"), e.attr("method"), e.html(), e.attr("name"));
	}

	public FormObj(String action, String method, String html) {
		this(action, method, html, null);
	}

	public FormObj(String action, String method, String html, String name) {
		this.action = action;
		this.method = method;
		this.html = html;
		this.name = name;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Form: ").append(name == null?"<no name>" : name).append("\n");
		sb.append("\tAction: ").append(action).append("\n");
		sb.append("\tMethod: ").append(method).append("\n");
		sb.append(">>html: ").append("\n");
		sb.append("------------------------------\n");
		sb.append(html);
		sb.append("------------------------------");
		return sb.toString();
	}

	public String toHtml2Mobile() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html2mobile_form").append(name == null?"" : " name='"+name+"'");
		sb.append(" action='").append(action).append("'");
		sb.append(" method='").append(method).append("'>");
		sb.append("\t\t<code>").append("\n");
		sb.append("\t<![CDATA[\n");
		sb.append(html);
		sb.append("\n\t\t]]>\n\t</code>");
		return sb.append("</html2mobile_form>").toString();
	}

	public String toHtml2Mobile_2() {
		StringBuilder sb = new StringBuilder();
		sb.append("<form>\n\t");
		sb.append(name == null?"" : "\t<name>"+name+"</name>\n\t");
		sb.append("<action><![CDATA[").append(action).append("]]></action>\n\t");
		sb.append("<method><![CDATA[").append(method).append("]]></method>\n\t");
		sb.append("\t\t<code>").append("\n");
		sb.append("\t<![CDATA[\n");
		sb.append(html);
		sb.append("\n\t\t]]>\n\t</code>\n");
		return sb.append("</form>\n").toString();
	}
}
