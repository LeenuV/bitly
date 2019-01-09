/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nic.bit;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static nic.bit.TinyURL.log;
//import static nic.bit.TinyURL.longUrl;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.javastack.kvstore.structures.btree.BplusTree;
import org.javastack.mapexpression.InvalidExpression;
import org.javastack.surbl.SURBL;
import utility.connection;

/**
 *
 * @author NIC
 */
@WebServlet(name = "tinyServlet", urlPatterns = {"/tinyServlet"})
public class tinyServlet extends HttpServlet {

    	static final Logger log = Logger.getLogger(tinyServlet.class);
	private static final long serialVersionUID = 42L;
	//
	private static final String CFG_STORAGE = "storage.dir";
	private static final String CFG_DUMP_KEY = "dump.key";
	private static final String CFG_WHITELIST = "whitelist.file";
	private static final String CFG_FLAGS = "check.flags";
	private static final String CFG_CHECK_CACHE = "check.cache.millis";
	private static final String CFG_CONN_TIMEOUT = "connection.timeout.millis";
	private static final String CFG_READ_TIMEOUT = "read.timeout.millis";
	//
	private static final String DEF_CHECKS = "WHITELIST,CONNECTION";
	//
	private Config config;
	private String dumpKey = null;
	private Set<CheckType> checkFlags;
	private int connectionTimeout, readTimeout, checkCacheExpire;
	private Persistence store;
	private Hasher hasher;
	private SURBL surbl;
	private WhiteList whiteList;
	private LinkedHashMap<String, Integer> checkCache;
        static String longUrl="";
        static String result="";
        
        String UPLOAD_DIRECTORY = "C:/uploads";
        String excelFilePath = "";

        
        public void init() throws ServletException {
		try {
			init0();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private void init0() throws NoSuchAlgorithmException, InstantiationException, IllegalAccessException,
			IOException, InvalidExpression, BplusTree.InvalidDataException, ClassNotFoundException {
		// Config Source
          
		final String configSource = System.getProperty(Config.PROP_CONFIG, Config.DEF_CONFIG_FILE);
                BasicConfigurator.configure();
                log.info("Hi this is log message");
		log.info("ConfigSource: " + configSource);
		config = new Config(configSource);

		// Storage Directory
		final String defStoreDir = getServletContext().getRealPath("/WEB-INF/storage/");
		String storeDir = config.get(CFG_STORAGE);
		if (storeDir == null) {
			storeDir = defStoreDir;
			config.put(CFG_STORAGE, storeDir);
		}
		log.info("StoragePath: " + storeDir);
		connectionTimeout = Math.max(config.getInt(CFG_CONN_TIMEOUT, Constants.DEF_CONNECTION_TIMEOUT), 1000);
		readTimeout = Math.max(config.getInt(CFG_READ_TIMEOUT, Constants.DEF_READ_TIMEOUT), 1000);
		log.info("Timeouts connection=" + connectionTimeout + "ms read=" + readTimeout + "ms");

		// Dump Key
		dumpKey = config.get(CFG_DUMP_KEY);
		if (dumpKey == null) {
			dumpKey = generateRandomKey(64);
			log.info("Generated random dump.key=" + dumpKey);
			writeKey(new File(storeDir, "dump.key"), dumpKey);
		}

		// Check Flags
		checkFlags = CheckType.parseFlags(config.get(CFG_FLAGS, DEF_CHECKS));
		checkCacheExpire = (Math.max(config.getInt(CFG_CHECK_CACHE, Constants.DEF_CHECK_CACHE_EXPIRE), 1000) / 1000);
		log.info("Check flags=" + checkFlags + " cache=" + checkCacheExpire + "seconds");
		// Message Digester
		hasher = new Hasher();
		// WhiteList Check
		if (checkFlags.contains(CheckType.WHITELIST)) {
			// WhiteList File
			final String defWhiteListFile = "file:///"
					+ new File(storeDir, "whitelist.conf").getAbsolutePath();
                        System.out.println("%%%%%%%%%%%%%%%%%"+defWhiteListFile);
			final String whiteListFile = config.get(CFG_WHITELIST, defWhiteListFile);
			log.info("WhiteListFile: " + whiteListFile);
			whiteList = new WhiteList(whiteListFile) //
					.setConnectionTimeout(connectionTimeout) //
					.setReadTimeout(readTimeout);
			whiteList.load();
		}
		// SURBL Check
		if (checkFlags.contains(CheckType.SURBL)) {
			surbl = new SURBL(storeDir) //
					.setConnectionTimeout(connectionTimeout) //
					.setReadTimeout(readTimeout);
			surbl.load();
		}
		// Storage
	/*	try {
			final String defaultClass = PersistentKVStore.class.getName();
			final Class<?> clazz = Class.forName(config.get("storage.class", defaultClass));
			store = (Persistence) clazz.newInstance();
			log.info("Storage class=" + clazz.getName());
			store.configure(config.getSubview("storage"));
			store.open();
		} catch (IOException e) {
			//closeSilent(store);
			throw e;
		}*/
		// Check cache
		if (!checkFlags.isEmpty()) {
			checkCache = new LinkedHashMap<String, Integer>() {
				private static final long serialVersionUID = 42L;

				@Override
				protected boolean removeEldestEntry(final Map.Entry<String, Integer> eldest) {
					return size() > 128;
				}
			};
		}
	}

	@Override
	public void destroy() {
		//closeSilent(store);
	}
        
        	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		try {
			MDC.put(Constants.MDC_IP, request.getRemoteAddr());
			MDC.put(Constants.MDC_ID, getNewID());
			doGet0(request, response);
		} finally {
			//MDC.clear();
		}
	}

	private void doGet0(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
                final PrintWriter out = response.getWriter();
		final String pathInfo = request.getPathInfo();
                System.out.println("Entered url "+pathInfo);
                
                   try{
                Connection conn=connection.getConnection();
                 PreparedStatement ps=conn.prepareStatement("select longUrl from tinyurl_data where shortUrl=?");
                 ps.setString(1,pathInfo);
                 ResultSet rs=ps.executeQuery();
                 if(rs.next())
                 {
                     response.sendRedirect(rs.getString("longUrl"));
                 }
                 else
                 {
                     out.println("Not found");
                 }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                
		/*if (dumpKey != null) {
			if (pathInfo.startsWith("/dump/")) {
				if (pathInfo.substring(6).equals(dumpKey)) {
					response.setContentType("text/csv; charset=ISO-8859-1");
					store.dump(response.getOutputStream());
					return;
				}
				final PrintWriter out = response.getWriter();
				sendError(response, out, HttpServletResponse.SC_FORBIDDEN, "Invalid Key");
				return;
			}
		}
		final String key = getPathInfoKey(pathInfo);
		if (key != null) {
			final TinyData meta = store.get(key);
			if (meta != null) {
				log.info("Found id=" + key + " url=" + meta.getURL());
				// Found - send response
				response.sendRedirect(meta.getURL());
				return;
			}
		}
		final PrintWriter out = response.getWriter();
		sendError(response, out, HttpServletResponse.SC_NOT_FOUND, "Not Found");*/
	}


	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
            try {
			
			doPost0(request, response);
		} finally {
			//MDC.clear();
		}
	}

	private void doPost0(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
            final PrintWriter out = response.getWriter();
             
             String fileName="";
             if(ServletFileUpload.isMultipartContent(request)){
                 
	             try {
	
	                 List<FileItem> multiparts = new ServletFileUpload(
	                                          new DiskFileItemFactory()).parseRequest(request);

	                 for(FileItem item : multiparts){
	 
	                     if(!item.isFormField()){

	                         fileName = new File(item.getName()).getName();
	                         item.write( new File(UPLOAD_DIRECTORY + File.separator + fileName));
                                 excelFilePath="C:/uploads/"+fileName;
	                         System.out.println("name is "+fileName);    
	                     }
	                 }
	                //File uploaded successfully
                         System.out.println("File Uploaded Successfully");
	             } catch (Exception ex) {
                         System.out.println("File Upload Failed due to");
	             }         
	         }
	         
                 System.out.println("File path "+excelFilePath);
             
	         FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
	 		
	 		Workbook workbook = new XSSFWorkbook(inputStream);
	 		Sheet firstSheet = workbook.getSheetAt(0);
	 		Iterator<Row> iterator = firstSheet.iterator();
	 		
	 		while (iterator.hasNext()) {
	 			Row nextRow = iterator.next();
	 			Iterator<Cell> cellIterator = nextRow.cellIterator();
	 			
	 			while (cellIterator.hasNext()) {
	 				Cell cell = cellIterator.next();
                                        String url=cell.getStringCellValue();
                                        System.out.print(url);
                         if ((url == null) || (url.length() < Constants.MIN_URL_LENGTH)) {
                             out.println("Invalid url is "+url);
			//sendError(response, out, HttpServletResponse.SC_BAD_REQUEST, "Invalid URL Parameter");
			//return;
                        } 
                           String key = hasher.hashURL(cell.getStringCellValue());
                        // String key="abc";
                            System.out.println("url is "+url+" and key is"+key);
                            out.println("url is "+url+" and short url is "+request.getRequestURL()+"/"+key);
                            tinyServlet.saveData(response, request, url, key);                          
                            int collision = 0;
                        while (true) { // Handle possible collisions
                               // final TinyData meta = store.get(key);
                                 final TinyData meta = null;
                                // Dont exists
                                if (meta == null)
                                        break;
                                // Duplicated
                                if (url.equals(meta.getURL())) {
                                        //sendResponse(response, request,out, url, key, collision, false);
                                    System.out.println("response is "+response);
                                        //return;
                                }
                                // Collision
                                if (++collision > Constants.MAX_COLLISION) {
                                        log.error("Too many collisions { url=" + url + " id=" + key + " }");
                                        //sendError(response, out, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"ERROR: Unable to Short URL");
                                        System.out.println("ERROR: Unable to Short URL");
                                       // return;
                                }
                                key = hasher.hashURL(Integer.toString(collision) + ":" + url);
                                System.out.println("url is "+url+" and new key is"+key);
                               }
	 			
	 			}
	 			System.out.println();
	 		}
	 		
	 		workbook.close();
	 		inputStream.close();

            
            
	}
        
        
private static final void saveData(final HttpServletResponse response, final HttpServletRequest request,
			final String longUrl, final String key) {
                 
		final String res = "{ \"id\": \"" + key + "\" }";
             //   System.out.print("res is"+res);
                java.util.Date date = new java.util.Date();
             try{
                Connection conn=connection.getConnection();
                 PreparedStatement ps1=conn.prepareStatement("select * from tinyurl_data where longUrl=?");
                 ps1.setString(1,longUrl);
                 ResultSet rs1=ps1.executeQuery();
                 if(rs1.next()==false)
                 {
                     PreparedStatement ps=conn.prepareStatement("Insert into tinyurl_data (longUrl,shortUrl,IP,browserInfo,dateTime) values(?,?,?,?,?)");
                     String shortkey[]=res.split(":");
                     String shortUrl=request.getRequestURL()+"/r/"+shortkey[1].replaceAll("[-+.^:,\\s+}]","");
                     shortUrl = "/"+key;
                     ps.setString(1, longUrl);
                     ps.setString(2,shortUrl);
                     ps.setString(3,request.getRemoteAddr());
                     ps.setString(4,request.getHeader("User-Agent"));
                     ps.setString(5,date.toString());
                     System.out.println("Query "+ps);
                     ps.execute();
                 }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
          /*   try{
               Connection conn=connection.getConnection();
               PreparedStatement ps=conn.prepareStatement("select shortUrl from tinyurl_data where longUrl=?");
               ps.setString(1,longUrl);
               ResultSet rs=ps.executeQuery();
               if(rs.next())
               {
                   result=rs.getString("shortUrl");
               }
             }
             catch(Exception e)
                {
                    e.printStackTrace();
                }
             
		// Send Response
		response.setContentType("application/json");*/
              
		/*out.println(result);
                System.out.println("Result from db "+result);
		log.log((collision > 0 ? Level.WARN : Level.INFO), "Mapping url=" + url + " id=" + key
				+ " Response: " + res + " collition=" + collision + (isNew ? " (new)" : " (reuse)"));*/
	}

private static final String generateRandomKey(final int len) throws UnsupportedEncodingException {
         
		final char[] alpha = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
		final SecureRandom r = new SecureRandom();
		final StringBuilder sb = new StringBuilder(len);
		final byte[] b = new byte[len];
		r.nextBytes(b);
		for (int i = 0; (i < b.length) && (sb.length() < len); i++) {
			final char c = alpha[(b[i] & 0x7F) % alpha.length];
			if (c >= '2' && c <= '9') {
				sb.append(c);
			} else if (c >= 'A' && c <= 'H') {
				sb.append(c);
			} else if (c >= 'J' && c <= 'N') {
				sb.append(c);
			} else if (c >= 'P' && c <= 'Z') {
				sb.append(c);
			} else if (c >= 'a' && c <= 'k') {
				sb.append(c);
			} else if (c >= 'm' && c <= 'z') {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static final void writeKey(final File f, final String key) throws IOException {
            
		FileWriter wr = null;
		try {
                     System.out.print("writing key is"+key);
                     System.out.print("file name is"+f);
			wr = new FileWriter(f);
			wr.write(key);
		} finally {
			//closeSilent(wr);
			f.setExecutable(false, false);
			f.setWritable(false, false);
			f.setReadable(false, false);
			f.setWritable(true, true);
			f.setReadable(true, true);
		}
	}

private static final String getNewID() {
		return UUID.randomUUID().toString();
	}









}
