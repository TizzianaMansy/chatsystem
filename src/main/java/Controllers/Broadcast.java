package Controllers;

import Controllers.Database.UpdateUsers;
import Controllers.Database.CreateTables;
import Model.AppData;
import Model.ClientsList;

import java.net.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.Map;
import java.util.logging.Level;

import static Model.AppData.currentUser;
import static Model.AppData.getNonLoopbackAddress;

public class Broadcast {

    public static final int PORT = 37842;

    public static final Logger logger = Logger.getLogger("chatsystem");

    // pour avoir l'adresse de broadcast
    public static InetAddress getBroadcastAddress() {
        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback())
                    continue;
                for (InterfaceAddress interfaceAddress :
                        networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null)
                        continue;
                    return broadcast;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    // vérifier que je ne reçois pas mon propre broadcast
    public static boolean receivedFromMyself(InetAddress sender) {
        boolean SameAddress = false;
        try {
            // recupérer les interfaces du réseau
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // récupérer les inetAdresses
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();

                    // vérifier qu'aucune des adresses ip est égale à sender
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.equals(sender)) {
                        SameAddress = true;
                    }
                }
            }
            return !SameAddress;
        } catch (SocketException ex) {
            throw new RuntimeException(ex);
        }
    }

    /*envoi du broadcast (UDP)*/
    public static void sendFirstPacket() {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            String mess = "FIRST_MESSAGE";
            byte [] FirstMessage = mess.getBytes();
            DatagramPacket message= new DatagramPacket(FirstMessage, FirstMessage.length, getBroadcastAddress(), PORT);
            socket.send(message);
            // on créé la table users au même temps qu'on envoie le premier message
            CreateTables database = new CreateTables(CreateTables.createURL(Objects.requireNonNull(getNonLoopbackAddress())));
            database.tableUsers();
        }
        catch (IOException e) {
            logger.log(Level.SEVERE,"IOException: " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /*reception des message en UDP*/
    public static class Receive extends Thread {
        private static DatagramSocket socket;

        public Receive() {
            //constructeur
            try {
                socket = new DatagramSocket(PORT); // port qui va recevoir le message
            } catch (SocketException e) {
                logger.log(Level.SEVERE, "SocketException: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {

                boolean running = true;

                socket.setBroadcast(true);
                byte[] buf = new byte[256];

                while (running) {
                    DatagramPacket inPacket = new DatagramPacket(buf, buf.length);
                    socket.receive(inPacket);
                    // on récupère le message et la personne qui l'a envoyé
                    String received = new String(inPacket.getData(), 0, inPacket.getLength());
                    InetAddress sender = InetAddress.getByName(inPacket.getAddress().getHostAddress());
                    // on appelle handlereceive qui gère la réception des messages UDP
                    handleReceived(sender, received);

                    if (received.equals("end")) {
                        running = false;
                    }
                }
            } catch (IOException | SQLException e) {
                logger.log(Level.SEVERE, "IOException: " + e.getMessage());
            } finally {
                closeSocket();
            }
        }

        public static void closeSocket() {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                logger.info("Receive socket closed.");
            }
        }

        // pour traiter les différents messages qui peuvent être reçus
        static void handleReceived(InetAddress sender, String received) throws SQLException, IOException {
            if (received.equals("FIRST_MESSAGE") && (receivedFromMyself(sender))) { // réception du broadcast
                sendNickname(AppData.getNicknameCurrentUser(), sender); //j'envoie mon nickname à la personne qui souhaite se connecter
                System.out.println("[Broadcast] First packet is received");
            } else if (received.startsWith("MY_NICKNAME_")) { // réception du nickname
                String prefix = "MY_NICKNAME_";
                String nickname = received.substring(prefix.length());
                System.out.println("[Broadcast] The following nickname : "+ nickname + "is received");
                AppData.addContactList(sender, nickname); //j'ajoute l'utilisateur qui vient de m'envoyer son nickname dans ma liste de contacts
                System.out.println("[Broadcast] " +nickname+ " added to contact list");
                CreateTables database = new CreateTables(CreateTables.createURL(Objects.requireNonNull(getNonLoopbackAddress())));
                database.tableMessages(sender); //création de la table de messages correspondant à cet utilisateur
                UpdateUsers.addUser(sender, nickname, CreateTables.createURL(Objects.requireNonNull(getNonLoopbackAddress()))); //ajout de l'utilisateur dans la database
                // si l'utilisateur existe deja dans la base de données mais a changé de pseudo lors de la connexion
                if (!UpdateUsers.NicknameIsSame(nickname, CreateTables.createURL(Objects.requireNonNull(getNonLoopbackAddress())))) {
                    UpdateUsers.changeNicknameDB(sender, nickname, CreateTables.createURL(Objects.requireNonNull(getNonLoopbackAddress())));
                }
                if (!ClientsList.checkPresenceIP(sender)) { //pour vérifier si l'utilisateur est déjà dans ma liste de clients
                    Client client = new Client(sender); //s'il n'est pas dans la liste, alors je créé une instance de client, son constructeur permet la création du socket et l'ajout dans la liste de clients
                }
            } else if (received.startsWith("CHANGE_NICKNAME_")) { //lorsqu'un utilisateur veut changer son nickname
                String prefix = "CHANGE_NICKNAME_";
                String nickname = received.substring(prefix.length());
                AppData.changeContactList(sender, nickname); //changer le nickname de l'utilisateur dans la liste de contacts
                UpdateUsers.changeNicknameDB(sender, nickname, CreateTables.createURL(Objects.requireNonNull(getNonLoopbackAddress()))); //changer le nickname dans la database
            } else if (received.equals("DISCONNECTING")) {
                AppData.DeletefromContactList(sender); //si quelqu'un veut se déconnecter, on l'enlève de notre liste de contacts
            }
        }
    }

        //pour envoyer le nickname
        public static void sendNickname(String nickname, InetAddress address) {
            try {
                DatagramSocket respSocket = new DatagramSocket();
                String mess = "MY_NICKNAME_" + nickname;
                byte[] respMessage = mess.getBytes();
                DatagramPacket respPacket = new DatagramPacket(respMessage, respMessage.length, address, PORT);
                respSocket.send(respPacket);
                respSocket.close();
                System.out.println("nickname sent : " + nickname);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "IOException: " + e.getMessage());
            }


        }

        //envoyer un message à sa liste de contacts lorsqu'on se déconnecte
        public static void sendExitMessage() {

            try {
                DatagramSocket exitSocket = new DatagramSocket();
                exitSocket.setBroadcast(true);

                for (Map.Entry<InetAddress, String> pers : AppData.getContactList().entrySet()) {
                    InetAddress id = pers.getKey(); //récupérer l'adresse IP des personnes dans la liste de contacts

                    String exit = "DISCONNECTING";
                    byte[] exitMessage = exit.getBytes();
                    // on envoie le message à chaque personne de la liste
                    DatagramPacket exitPacket = new DatagramPacket(exitMessage, exitMessage.length, id, PORT);
                    exitSocket.send(exitPacket);
                }
                exitSocket.close();
                System.out.println("[Broadcast] Just sent the message to disconnect.");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "IOException: " + e.getMessage());
            }
        }

	    
// vérifier l'unicité du nickname souhaité : on renvoie faux si le nickname existe déjà
    public static boolean CheckUnicityNickname(String CurrentNickname) throws InterruptedException {
        boolean valid_name = true;

            for (Map.Entry<InetAddress, String> pers : AppData.getContactList().entrySet()) {
                String nickname = pers.getValue();
                if (nickname.equals(CurrentNickname)) {
                    valid_name = false; //nickname déjà pris
                    break;
                }
            }
        return valid_name;

    }

    // pour envoyer le pseudo une fois qu'on a vérifié qu'il est unique
    public static void setCurrentNickname (String nickname){
        currentUser.setNickname(nickname);
    }

}




