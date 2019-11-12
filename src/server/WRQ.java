package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class WRQ {
    public static final short OP_DAT = 3;
    Utils Utilitaire = new Utils();
    public void Etat(DatagramSocket sendSocket, String string, int CodeEtat) {

        File file = new File(string);
        byte[] buf = new byte[Utilitaire.Taille_Paquet -4];

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Utilitaire.sendError(sendSocket, (short) 2, "ERREUR: On n'a pas pu créer le fichier.");
            return;
        }

        short block = 0;
        byte[] rec = new byte[Utilitaire.Taille_Paquet];
        DatagramPacket receiver = new DatagramPacket(rec, rec.length);
        do{

            int CompteurEssais = 0;
            DatagramPacket dataPacket = new DatagramPacket(rec, rec.length);

            try {
                System.out.println("Envoi ACK pour le block suivant: " + block);
                sendSocket.send(Utilitaire.ackPacket(block++));
                CompteurEssais++;
                sendSocket.setSoTimeout(((int) Math.pow(2, CompteurEssais))*1000);
                sendSocket.receive(receiver);
                int opcode = receiver.getData()[0]+receiver.getData()[1];

                switch(opcode){
                    case OP_DAT:
                        short blockNum = Utilitaire.getData(receiver);
                        System.out.println(blockNum + " <--> " + block);
                        if (blockNum == block) {
                            dataPacket = receiver;
                        } else if (blockNum == -1) {
                            dataPacket = null;//cela va faire perdre la connection
                        } else {
                            System.out.println("Duplicat.");
                            CompteurEssais = 0;
                            throw new SocketTimeoutException();
                        }
                        if (dataPacket == null) {
                            System.out.println("Connection perdue");
                            Utilitaire.sendError(sendSocket, (short) 6, "Connection perdue");
                            try {
                                output.close();
                            } catch (IOException e) {
                            }
                            file.delete();
                            break;
                        } else {
                            byte[] data = dataPacket.getData();
                            try {
                                output.write(data, 4, dataPacket.getLength() - 4);
                                System.out.println(dataPacket.getLength());
                            } catch (IOException e) {
                                Utilitaire.sendError(sendSocket, (short) 2, "Probleme d'écriture dans le fichier");
                            }
                            if (dataPacket.getLength() - 4 < 512) {
                                try {
                                    sendSocket.send(Utilitaire.ackPacket(block));
                                    System.out.println("ACK ENVOYE APRES DATA ENVOYE");
                                } catch (IOException e1) {
                                    try {
                                        sendSocket.send(Utilitaire.ackPacket(block));
                                    } catch (IOException e) {
                                    }
                                }
                                System.out.println("");
                                try {
                                    output.close();
                                } catch (IOException e) {
                                }
                                break;
                            }
                        }


                        break;
                        default:break;

                }




            } catch (SocketTimeoutException e) {
                System.out.println("SOCKET TIMEOUT EXCEPTION");
                try {
                    sendSocket.send(Utilitaire.ackPacket(block++));
                } catch (IOException e1) {
                }
            } catch (IOException e) {
                System.err.println("IO EXCEPTION.");
            } finally {
                try {
                    sendSocket.setSoTimeout(0);
                } catch (SocketException e) {

                }
            }



        }while(receiver.getLength()==516);

    }

}
