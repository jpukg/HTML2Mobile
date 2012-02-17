package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class DumbProxyServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String url = req.getParameter("url");
		if( url == null ) {
			throw new ServletException("No URL");
		}

		Client client = Client.create();
		WebResource res = client.resource(url);
		String contents = res.get(String.class);

		resp.getWriter().write(contents);
	}

}
