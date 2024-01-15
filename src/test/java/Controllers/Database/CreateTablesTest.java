package Controllers.Database;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Connection;

    public class CreateTablesTest extends TestCase {
        /**
         * Create the test case
         *
         * @param testName name of the test case
         */
        public CreateTablesTest(String testName) {
            super(testName);
        }

        public static Test suite() {
            return new TestSuite(CreateTablesTest.class);
        }

        // Méthode pour vérifier si une table existe dans la base de données
        private boolean tableExists(CreateTables createTables, String Table) throws SQLException {
            DatabaseMetaData metaData = createTables.connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, Table, null);
            return tables.next();
        }
        public static String TestUrl = "jdbc:sqlite:BDDTest.db";
        public void testConnect () throws UnknownHostException { //find a way to make an assert
            CreateTables dbTest = new CreateTables(TestUrl);
            // on vérifie que la connection n'est pas nulle
            assertNotNull("Connection should not be null", dbTest.connection);

            // Check if the connection is open
            try {
                assertFalse("Connection should be open", dbTest.connection.isClosed());
            } catch (SQLException e) {
                fail("SQLException: " + e.getMessage());
            }

        }
        public void testTableUsers () throws UnknownHostException, SQLException{
            CreateTables dbTest = new CreateTables(TestUrl);
            dbTest.tableUsers();
            assertTrue(tableExists(dbTest,"Users"));
            dbTest.closeConnection();
        }

        public void testTableMessage () throws UnknownHostException, SQLException{
            InetAddress senderAddress = InetAddress.getByName("101.26.81.12");
            CreateTables dbTest = new CreateTables(TestUrl);
            dbTest.tableMessages(senderAddress);
            assertTrue(tableExists(dbTest,"Messages_101_26_81_12"));
            dbTest.closeConnection();
        }
    }