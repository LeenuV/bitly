package utility;
import com.mysql.jdbc.Connection;
import java.sql.DriverManager;


public class connection {

	 private static Connection con = null;
	   
	    private static String url = "jdbc:mysql://localhost/tinyURL?autoReconnect=true";
	    private static String user = "root";    
	    private static String pass = "root@123";
	    
    private connection(String url, String user, String pass) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = (Connection) DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {

        }
    }

    public static Connection getConnection() {
        try {
            if (con == null) {
                new connection(url, user, pass);
            }
        } catch (Exception e) {
           
        }
        return con;
    }

 
    }
   
    
    

