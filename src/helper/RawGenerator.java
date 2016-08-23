package helper;


import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Rakhmatullah Yoga S
 */
public class RawGenerator {
    private String host;
    private String user;
    private String pass;
    private String db_name;
    private Connection connection;
    private Statement statement;
    
    public RawGenerator() {
        try {
            host = "localhost";
            user = "root";
            pass = "";
            db_name = "news_aggregator";
            String connectionURL = "jdbc:mysql://" + host + "/" + db_name;
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = (Connection) DriverManager.getConnection(connectionURL, user, pass);
            statement = (Statement) connection.createStatement();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            Logger.getLogger(RawGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void generate() {
        try {
            ResultSet result = statement.executeQuery("SELECT * FROM `artikel` WHERE ID_ARTIKEL IN (SELECT ID_ARTIKEL FROM `artikel_kategori_verified` WHERE ID_KELAS = 3) ORDER BY RAND()");
            int i=0;
            while(result.next()) {
                PrintWriter writer = new PrintWriter("./data/articles/artikel"+i+".txt", "UTF-8");
                writer.println(result.getString("JUDUL"));
                writer.println(result.getString("FULL_TEXT"));
                writer.close();
                i++;
            }
        } catch (SQLException | FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(RawGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        RawGenerator gen = new RawGenerator();
        gen.generate();
    }
}
