package Controllers.Database;

import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.*;
import java.time.format.DateTimeFormatter;


import static Controllers.Database.CreateDatabase.MESSAGE_DATABSE;

public class UpdateMessages {

    private static int idCounter = 1;
    public static boolean addMessage(InetAddress Address, String Content, String url) throws SQLException {

        try {
           CreateDatabase database = new CreateDatabase(url);
           // Statement statement = database.connection.createStatement();
            String strAddress = Address.getHostAddress().replace('.', '_');
            String insertQuery = "INSERT INTO Messages_" + strAddress + "(idMessage, content, dateHeure) VALUES (?, ?, ?)";

            try (PreparedStatement preparedStatement = database.connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                // Set values for the parameters using the Users class methods
                // the preparedStatement.setString() method is used to set the values for the placeholders (?) in the SQL query
               int Id = idCounter++;
                //to add the date and time of the message
                LocalDateTime currentDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedDateTime = currentDateTime.format(formatter);
                preparedStatement.setInt(1, idCounter);
                preparedStatement.setString(2, Content);
                preparedStatement.setString(3,formattedDateTime);

                preparedStatement.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle the exception
            return false;
        }
    }
}
