package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public abstract class JSoupServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// read requested URL
		String urlParam = req.getParameter("url");
		if( urlParam == null ) {
			throw new ServletException("No URL");
		}
		URL url = null;
		try {
			url = new URL(urlParam);
		} catch( MalformedURLException e ) {
			throw new ServletException("Cannot parse: " + urlParam, e);
		}

		/* Jersey Reference
		// fetch the URL
		Client client = Client.create();
		WebResource res = client.resource(urlParam);
		String contents = res.get(String.class);
		// parse the document
		Document doc = Jsoup.parse(contents); */

		Document doc = Jsoup.connect(url.toString()).get();

		String result = this.process(doc, req);

		PrintWriter writer = resp.getWriter();
		writer.write("<html><body><pre>");
		writer.write(result);
		//writer.write(result.replace("<", "&lt;").replace(">", "&gt;"));
		writer.write("</pre></body></html>");
	}

	public abstract String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException;
}
