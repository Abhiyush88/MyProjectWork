import java.io.IOException;
 
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory; //*new
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory; //*new
import org.eclipse.jetty.server.ServerConnector; //*new


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileServer 
{
    private static String BASE_DIR ;// "/home/naveen/code/media"; // set this to whatever your base media directory is
    private static File dirBase;
    private static String ipAddr;
    private static String rtspDir;
    private static final int RTSP_PORT = 8554;
    
    public static void main( String[] args ) throws Exception
    {
        // Create a basic jetty server object that will listen on port 8080.
        // Note that if you set this to port 0 then a randomly available port
        // will be assigned that you can either look in the logs for the port,
        // or programmatically obtain it for use in test cases.
        
        ipAddr = args[0];
        String UserInput = args[1];

        int length = UserInput.length();
        if (UserInput.charAt(length - 1) == '/')
        {
            BASE_DIR = UserInput.substring(0, length - 1);
        }
        else
        {
            BASE_DIR = UserInput;
        }
        dirBase = new File(BASE_DIR);  
        Path path = Paths.get(BASE_DIR);
        if (Files.notExists(path))
        {
            System.out.println("ERROR: INVALID PATH");
            System.exit(-1);
        }
       
        rtspDir = "rtsp://" + ipAddr + ":" + RTSP_PORT + "/";
        
        Server server = new Server(); // take out the 8080
        
        // *----------------------------------------------------------------- NEW
        // Common HTTP configuration.
        HttpConfiguration config = new HttpConfiguration();

        // HTTP/1.1 support.   
        HttpConnectionFactory http1 = new HttpConnectionFactory(config);

        // HTTP/2 cleartext support.
        HTTP2CServerConnectionFactory http2c = new HTTP2CServerConnectionFactory(config);

        ServerConnector connector = new ServerConnector(server, http1, http2c);
        connector.setPort(8080);
        server.addConnector(connector);
        // *----------------------------------------------------------------
        
        // The ServletHandler is a dead simple way to create a context handler
        // that is backed by an instance of a Servlet.
        // This handler then needs to be registered with the Server object.
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
 
        // Passing in the class for the Servlet allows jetty to instantiate an
        // instance of that Servlet and mount it on a given context path.
 
        // IMPORTANT:
        // This is a raw Servlet, not a Servlet that has been configured
        // through a web.xml @WebServlet annotation, or anything similar.
        handler.addServletWithMapping(HelloServlet.class, "/*");
 
        // Start things up!
        server.start();
 
        // The use of server.join() the will make the current thread join and
        // wait until the server is done executing.
        // See
        // http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#join()
        server.join();
    }
 
    @SuppressWarnings("serial")
    public static class HelloServlet extends HttpServlet
    {
        @Override
        protected void doGet( HttpServletRequest request,
                              HttpServletResponse response ) throws ServletException,
                                                            IOException
        {
           
           
            if (!request.getRequestURL().toString().contains("browse.xml"))
                return;
           
            response.setContentType("text/xml");
            response.setStatus(HttpServletResponse.SC_OK);
            
            PrintWriter writer = response.getWriter();
            
            writer.println("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>");
            writer.println("<root>");
            
            System.out.println(request.getQueryString());
            
            System.out.println(request.getRequestURL());
            
            /* if (request.getQueryString().substring(0,5).contentEquals("dir=~")) // upon inital connect from the client, show the base media directory */
            if ((request.getQueryString().substring(0,5).contentEquals("dir=~")) || (request.getQueryString().length() == 7)) // upon inital connect from the client, show the base media directory
            {
                
                listFiles(dirBase, writer);
            }
            
            else // show the requested directory
            {
                String browseDir = request.getQueryString().substring(4);
                String delimiter = "[%2F]";
                String[] dirParts = browseDir.split(delimiter);
                
                if (browseDir.contains(".."))
                {
                    browseDir = "";
                    for (int i=0; i<(dirParts.length-4); i++)
                    {
                        browseDir += dirParts[i] + "/";
                    }
                    browseDir = browseDir.substring(0,browseDir.length()-1);
                }
                else
                {
                    browseDir = "";
                    for (int i=0; i<dirParts.length; i++)
                    {
                        browseDir += dirParts[i] + "/";
                    }
                    browseDir = browseDir.substring(0,browseDir.length()-1);
                }
                
                
                File browseFile = new File(browseDir);
                listFiles(browseFile,writer);
                
            }
            
            writer.println("</root>");
            
        }
    }
    
    
    static void listFiles(File aFile, PrintWriter aWriter) { // files have uri following rtsp://[ip address]:[port]/[path]
        
        int spc_count = -1;
        
        spc_count++;
        String spcs = "";
        for (int i = 0; i < spc_count; i++) {
            spcs += " ";
        }
        if (aFile.isFile()) {
            //aWriter.println("<blockquote>");
            //aWriter.println("<p>" + spcs + "[FILE] " + aFile.getName() + "</p>");
            //aWriter.println("<br></br>");
        }   
        else if (aFile.isDirectory()) 
        {
            
            //aWriter.println(spcs + "[DIR] " + aFile.getName());
            //aWriter.println("<br></br>");
            File[] listOfFiles = aFile.listFiles();
            if (listOfFiles != null) {
                
                listOfFiles = sortFiles(listOfFiles);
                
                System.out.println(aFile.getAbsolutePath());
                
                if (!(aFile.getAbsolutePath().contentEquals(BASE_DIR)))
                {
                    aWriter.println("<element ");
                    aWriter.println("uri=\"" + aFile.getAbsolutePath() + "/..\" ");
                    aWriter.println("name=\"..\" ");
                    aWriter.println("creation_time=\"null\" ");
                    aWriter.println("uid=\"null\" ");
                    aWriter.println("mode=\"null\" ");
                    aWriter.println("size=\"" + aFile.length() + "\" ");
                    aWriter.println("access_time=\"null\" ");
                    aWriter.println("type=\"dir\" ");
                    aWriter.println("path=\"" + aFile.getAbsolutePath() + "/..\" ");
                    aWriter.println("gid=\"null\" ");
                    aWriter.println("modification_time=\"null\"");
                    aWriter.println("/>");
                }                

                //aWriter.println("<blockquote>");
                for (int i = 0; i < listOfFiles.length; i++)
                {
                    
                    //Process(listOfFiles[i], aWriter);
                    aFile = listOfFiles[i];
                    /* NAVEEN Added */
                    if (aFile.getName().equals("live555MediaServer"))
                    {
                        continue;
                    }
                    aWriter.println("<element ");
                    if (aFile.isFile()) { 
                        
                        aWriter.println("uri=\"" + rtspDir + aFile.getAbsolutePath().substring(BASE_DIR.length()+1) + "\" ");
                        aWriter.println("name=\"" + aFile.getName() + "\" ");
                        aWriter.println("creation_time=\"null\" ");
                        aWriter.println("uid=\"null\" ");
                        aWriter.println("mode=\"null\" ");
                        aWriter.println("size=\"" + aFile.length() + "\" ");
                        aWriter.println("access_time=\"null\" ");
                        aWriter.println("type=\"file\" ");
                        aWriter.println("path=\"" + aFile.getAbsolutePath() + "\" ");
                        aWriter.println("gid=\"null\" ");
                        aWriter.println("modification_time=\"null\"");
                        
                        //aWriter.println("<blockquote>");
                        //aWriter.println("<p>" + spcs + "[FILE] " + aFile.getAbsolutePath() + "</p>");
                        //aWriter.println("<br></br>");
                    } 
                    else if (aFile.isDirectory())
                    {
                        
                        aWriter.println("uri=\"" + aFile.getAbsolutePath() + "\" ");
                        aWriter.println("name=\"" + aFile.getName() + "\" ");
                        aWriter.println("creation_time=\"null\" ");
                        aWriter.println("uid=\"null\" ");
                        aWriter.println("mode=\"null\" ");
                        aWriter.println("size=\"" + aFile.length() + "\" ");
                        aWriter.println("access_time=\"null\" ");
                        aWriter.println("type=\"dir\" ");
                        aWriter.println("path=\"" + aFile.getAbsolutePath() + "\" ");
                        aWriter.println("gid=\"null\" ");
                        aWriter.println("modification_time=\"null\"");
                        
                        //aWriter.println("<blockquote>");
                        //aWriter.println(spcs + "[DIR] " + aFile.getName());
                        //aWriter.println("<br></br>");
                    
                    
                    }
                    
                    else {
                //System.out.println(spcs + " [ACCESS DENIED]");
            }
                    
                    aWriter.println("/>");
                }
            }
            else
            {
                aWriter.println("<element ");
                aWriter.println("uri=\"" + aFile.getAbsolutePath() + "/..\" ");
                aWriter.println("name=\"..\" ");
                aWriter.println("creation_time=\"null\" ");
                aWriter.println("uid=\"null\" ");
                aWriter.println("mode=\"null\" ");
                aWriter.println("size=\"" + aFile.length() + "\" ");
                aWriter.println("access_time=\"null\" ");
                aWriter.println("type=\"dir\" ");
                aWriter.println("path=\"" + aFile.getAbsolutePath() + "/..\" ");
                aWriter.println("gid=\"null\" ");
                aWriter.println("modification_time=\"null\"");
                aWriter.println("/>");
            }
        }
        //aWriter.println("</blockquote>");
        spc_count--;
    }
    
    
    
    
    
    static File[] sortFiles(File[] files) 
    {
        boolean a = true;
        File temp;
        
        while (a)
        {
            a = false;
            for (int i=0; i<files.length-1; i++)
            {
                if (files[i].getName().compareToIgnoreCase(files[i+1].getName())>0)
                {
                    temp = files[i];
                    files[i] = files[i+1];
                    files[i+1] = temp;
                    a = true;
                }
            }
        }
        
        return files;
    }
    
}

