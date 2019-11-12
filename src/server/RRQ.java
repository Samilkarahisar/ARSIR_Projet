package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class RRQ {
    public static final short OP_ACK = 4;
    Utils Utilitaire = new Utils();
    public void Etat(DatagramSocket Socket, String string, int CodeEtat) {

        File file = new File(string);
        byte[] buf = new byte[Utilitaire.Taille_Paquet -4];
        FileInputStream in = null;

        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.err.println("ERREUR: Fichier inexistant. Envoi d'un paquet d'erreur..");
            Utilitaire.sendError(Socket, (short)1, "");
            return;
        }

        short blockNum = 1;
        byte[] rec = new byte[Utilitaire.Taille_Paquet];
        DatagramPacket receiver = new DatagramPacket(rec, rec.length);

        while(true) {
            int length;
            try {
                length = in.read(buf);
            } catch (IOException e) {
                System.err.println("Nous n'avons pas pu lire le fichier");
                return;
            }
            if (length == -1) {
                length = 0;
            }

            DatagramPacket sender = Utilitaire.dataPacket(blockNum, buf, length);
            int CompteurEssais = 0;

            try {
                Socket.send(sender);
                System.out.println("Envoyé.");
                CompteurEssais++;
                Socket.setSoTimeout(((int) Math.pow(2, CompteurEssais))*1000);
                Socket.receive(receiver);


                int opcode = receiver.getData()[0]+receiver.getData()[1];

                switch(opcode){
                    case OP_ACK:
                        short ack = Utilitaire.getAck(receiver);
                        if (ack == blockNum) {
                            blockNum++;
                            System.out.println("GIRDIK");
                        } else if (ack == -1) {
                            System.err.println("Connection perdue.");
                            Utilitaire.sendError(Socket,(short)0, "Connection perdue");
                            return;
                        } else {
                            CompteurEssais = 0;
                            break;
                        }

                        break;
                    default:
                        break;
                }

            } catch (SocketTimeoutException e) {
            } catch (IOException e) {
            } finally {
                try {
                    Socket.setSoTimeout(0);
                } catch (SocketException e) {
                }
            }


            if (length < 512) {
                try {
                    in.close();
                } catch (IOException e) {

                }
                break;
            }
        }

    }
}
