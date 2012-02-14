package edu.gatech.cc.html2mobile;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		PrintStream out = new PrintStream(resp.getOutputStream());
		
		out.println("<html><body>");
		Enumeration<?> headers = req.getHeaders("User-Agent");
		while( headers.hasMoreElements() ) {
			String header = (String)headers.nextElement();
			out.println("User-Agent: " + header + "<br />");
		}
		out.println("</body></html>");
		
		out.flush();
		out.close();
	}
	
}
