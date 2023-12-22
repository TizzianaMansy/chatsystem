package Controllers.Database;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static Controllers.Database.CreateDatabase.url;

public class UpdateUsersTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public UpdateUsersTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(UpdateUsersTest.class);
    }

    public void testAddUsers () throws UnknownHostException {
        CreateDatabase.Connect(url);
        InetAddress senderAddress = InetAddress.getByName("101.26.81.12");
        String Nickname = "Mary";
        UpdateUsers.addUser(senderAddress,Nickname);
    }

    public void testChangeStatus () throws UnknownHostException {
        CreateDatabase.Connect(url);
        InetAddress senderAddress = InetAddress.getByName("101.26.81.12");
        UpdateUsers.changeStatus(senderAddress);
    }
}