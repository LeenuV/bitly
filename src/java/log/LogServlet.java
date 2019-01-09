package log;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;


public class LogServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	public void init(ServletConfig sfg)
	{
		String logpath = sfg.getInitParameter("path");
		ServletContext ctx = sfg.getServletContext();
		String realpath = ctx.getRealPath("/");
		String fullPath = realpath+logpath;
		
		File f=new File(fullPath);
		 if(f.exists())
		 {
			 PropertyConfigurator.configure(fullPath);
		 }
		 else
		 {
			 BasicConfigurator.configure();
		 }
		
	}
	
	

}
