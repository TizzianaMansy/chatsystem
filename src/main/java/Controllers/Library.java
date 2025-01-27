package Controllers;

import Model.AppData;
import Model.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static Model.AppData.getContactList;

public class Library {

    private static final Logger logger = Logger.getLogger("chatsystem");

    public Library() {
        //constructeur
    }


    /*méthodes*/

    //envoyer premier message
    public static void sendFirstMessage(){
        Broadcast.sendFirstPacket();
    }

    //envoyer son nickname quand choisi et unique
    public static void SendCurrentNickname(String CurrentNickname) throws InterruptedException {
        try {
            DatagramSocket NameSocket = new DatagramSocket();
            String Nickname = "MY_NICKNAME_" + CurrentNickname;
            byte[] NicknameMessage = Nickname.getBytes();
            for (Map.Entry<InetAddress, String> pers : AppData.getContactList().entrySet()) {
                InetAddress id = pers.getKey(); //récupérer l'adresse IP des membres de la liste de contact

                DatagramPacket NamePacket = new DatagramPacket(NicknameMessage, NicknameMessage.length, id, Broadcast.PORT);
                NameSocket.send(NamePacket);
            }
            NameSocket.close();
        }
        catch (IOException e) {
                logger.log(Level.SEVERE,"IOException: " + e.getMessage());
        }
    }

    // si l'utilisateur décide de changer de pseudo pendant l'utilisation de l'application
    // il envoie le nouveau pseudo à tous ses contacts
    public static void SendNewNickname(String NewNickname) throws InterruptedException {
        try{
            DatagramSocket NameSocket = new DatagramSocket();
            String Nickname = "CHANGE_NICKNAME_" + NewNickname;
            byte[] NicknameMessage = Nickname.getBytes();
            for (Map.Entry<InetAddress, String> pers : AppData.getContactList().entrySet()) {
                InetAddress id = pers.getKey(); //récupérer l'adresse IP des membres de la liste de contact

                DatagramPacket NamePacket = new DatagramPacket(NicknameMessage, NicknameMessage.length, id, Broadcast.PORT);
                NameSocket.send(NamePacket);
            }
            NameSocket.close();
        }
        catch (IOException e) {
            logger.log(Level.SEVERE,"IOException: " + e.getMessage());
        }
    }

    //récupérer liste avec les utilisateurs connectés
    public static ArrayList<Model.User> GetConnectedUserList() {
        ArrayList<Model.User> connectedUsers = new ArrayList<User>();

        // pour accéder à la liste des contacts connectés
        if (AppData.getContactList() != null) {
            for (Map.Entry<InetAddress, String> pers : AppData.getContactList().entrySet()) {
                InetAddress id = pers.getKey();
                String nickname = pers.getValue();
                // on créé un user par contact
                User user = new User();
                user.setNickname(nickname);
                user.setId(id);
                connectedUsers.add(user);

            }
        }
        return connectedUsers;
    }

    // pour vider la liste de contacts
    public static void clearContactList() {
        getContactList().clear();
    }

    // pour récupérer le pseudo de l'utilisateur
    public static String getNameUser(Model.User user){
      return user.getNickname();
    }

    // pour récupérer l'adresse IP de l'utilisateur
    public static InetAddress getIpUser (Model.User user) {
        return user.getId();
    }
}
