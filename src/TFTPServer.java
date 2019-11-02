package server;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;


public class TFTPServer {

    public static final int Taille_Paquet = 516;

	public static  String Dossier = "";
    public static final short OP_RRQ = 1;
    public static final short OP_WRQ = 2;
    public static final short OP_DAT = 3;
    public static final short OP_ACK = 4;
    public static final short OP_ERR = 5;
    public static String 	  mode;
    public int PORT = 69;

    public TFTPServer(int P, String D){
        PORT=P;
        Dossier=D+"\\";
    }

    public void start() throws SocketException {
        byte[] buf= new byte[Taille_Paquet];

        DatagramSocket socket= new DatagramSocket(null);
        SocketAddress localIP= new InetSocketAddress(PORT);
        socket.bind(localIP);

        System.out.println("On écoute sur le port suivant: "+PORT);

        while(true) { //ETAT DE CONSTANT ECOUT

            final InetSocketAddress AdresseClient =  EcouteClients(socket, buf);
            if (AdresseClient == null)
                continue;


            final StringBuffer FichierDemande = new StringBuffer();
            final int reqtype = ParseRequete(buf, FichierDemande);

            new Thread() {
                public void run() {
                    try {
                        DatagramSocket SocketServeur = new DatagramSocket(0);
                        SocketServeur.connect(AdresseClient);

                        if (reqtype == OP_RRQ) { //ON FAIT ENTRER LE THREAD DANS l'ETAT RRQ
                            FichierDemande.insert(0, Dossier);
                            SwitchEtat(SocketServeur, FichierDemande.toString(), OP_RRQ);
                        }
                        else if(reqtype==OP_WRQ){ //ON FAIT ENTRER LE THREAD DANS l'ETAT WRQ
                            FichierDemande.insert(0, Dossier);
                            SwitchEtat(SocketServeur,FichierDemande.toString(),OP_WRQ);
                        }
                        SocketServeur.close();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }


    private InetSocketAddress EcouteClients(DatagramSocket socket, byte[] buf) {
        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

        try {
            socket.receive(receivePacket);
        } catch (IOException e) {

        }
        InetSocketAddress client = new InetSocketAddress(receivePacket.getAddress(),receivePacket.getPort());
        return client;
    }

    private short ParseRequete(byte[] buf, StringBuffer requestedFile) {
        ByteBuffer wrap = ByteBuffer.wrap(buf);
        short opcode = wrap.getShort();
        int delimiter = -1;
        for (int i = 2; i < buf.length; i++) {
            if (buf[i] == 0) {
                delimiter = i;
                break;
            }
        }

        if (delimiter == -1) {
            System.err.println("La Requete recu est incomprehensible(delimiter -1)");
            System.exit(1);
        }

        String fileName = new String(buf, 2, delimiter-2);
        requestedFile.append(fileName);

        for (int i = delimiter+1; i < buf.length; i++) {
            if (buf[i] == 0) {
                String temp = new String(buf,delimiter+1,i-(delimiter+1));
                mode = temp;
                if (temp.equalsIgnoreCase("octet")) {
                    return opcode;
                } else {
                    System.exit(1);
                }
            }
        }
        //On a pas pu trouver de 0 qui délimite.
        System.err.println("On a pas pu trouver de 0 qui délimite.");
        System.exit(1);
        return 0;
    }


    private void SwitchEtat(DatagramSocket sendSocket, String string, int CodeEtat) {

        File file = new File(string);
        byte[] buf = new byte[Taille_Paquet -4];

        if (CodeEtat == OP_RRQ) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                System.err.println("ERREUR: Fichier inexistant. Envoi d'un paquet d'erreur..");
                sendError(sendSocket, (short)1, "");
                return;
            }

            short blockNum = 1;

            while (true) {

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
                DatagramPacket sender = dataPacket(blockNum, buf, length);

                if (receive_file(sendSocket, sender, blockNum++)) {
                } else {
                    System.err.println("Connection perdue.");
                    sendError(sendSocket,(short)0, "Connection perdue");
                    return;
                }

                if (length < 512) {
                    try {
                        in.close();
                    } catch (IOException e) {

                    }
                    break;
                }
            }
        } else if (CodeEtat == OP_WRQ) {

                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    sendError(sendSocket, (short)2, "ERREUR: On n'a pas pu créer le fichier.");
                    return;
                }

                short blockNum = 0;

                while (true) {
                    DatagramPacket dataPacket = send_file(sendSocket, ackPacket(blockNum++), blockNum);

                    if (dataPacket == null) {
                        System.out.println("Connection perdue");
                        sendError(sendSocket, (short)6, "Connection perdue");
                        try {
                            output.close();
                        } catch (IOException e) {
                        }
                        file.delete();
                        break;
                    } else {
                        byte[] data = dataPacket.getData();
                        try {
                            output.write(data, 4, dataPacket.getLength()-4);
                            System.out.println(dataPacket.getLength());
                        } catch (IOException e) {
                            sendError(sendSocket,(short)2, "Probleme d'écriture dans le fichier");
                        }
                        if (dataPacket.getLength()-4 < 512) {
                            try {
                                sendSocket.send(ackPacket(blockNum));
                            } catch (IOException e1) {
                                try {
                                    sendSocket.send(ackPacket(blockNum));
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
                }

        }

    }

    private DatagramPacket send_file(DatagramSocket sendSocket, DatagramPacket sendAck, short block) {
        int CompteurEssais = 0;
        byte[] rec = new byte[Taille_Paquet];
        DatagramPacket receiver = new DatagramPacket(rec, rec.length);

        while(true) {
            if (CompteurEssais >= 5) {
                System.err.println("On a fait 5 essais. On coupe la connection.");
                return null;
            }
            try {
                System.out.println("Envoi ACK pour le block suivant: " + block);
                sendSocket.send(sendAck);
                CompteurEssais++;
                sendSocket.setSoTimeout(((int) Math.pow(2, CompteurEssais))*1000);

                sendSocket.receive(receiver);

                short blockNum = getData(receiver);
                System.out.println(blockNum + " " + block);
                if (blockNum == block) {
                    return receiver;
                } else if (blockNum == -1) {
                    return null;
                } else {
                    System.out.println("Duplicate.");
                    CompteurEssais = 0;
                    throw new SocketTimeoutException();
                }
            } catch (SocketTimeoutException e) {
                System.out.println("SOCKET TIMEOUT EXCEPTION");
                try {
                    sendSocket.send(sendAck);
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
        }
    }

    private boolean receive_file(DatagramSocket RFSocket, DatagramPacket sender, short blockNum) {
        int CompteurEssais = 0;
        byte[] rec = new byte[Taille_Paquet];
        DatagramPacket receiver = new DatagramPacket(rec, rec.length);

        while(true) { // faire un do while
            if (CompteurEssais >= 6) {
                System.err.println("Au bout de 6 envoi échoué, la connection est coupé.");
                return false;
            }
            try {
                RFSocket.send(sender);
                System.out.println("Envoyé.");
                CompteurEssais++;

                RFSocket.setSoTimeout(((int) Math.pow(2, CompteurEssais))*1000);
                RFSocket.receive(receiver);

                short ack = getAck(receiver);

                if (ack == blockNum) {
                    return true;
                } else if (ack == -1) {
                    return false;
                } else {
                    CompteurEssais = 0;
                    throw new SocketTimeoutException();
                }

            } catch (SocketTimeoutException e) {
            } catch (IOException e) {
            } finally {
                try {
                    RFSocket.setSoTimeout(0);
                } catch (SocketException e) {
                }
            }
        }
    }


    private void sendError(DatagramSocket SocketErr, short errorCode, String errMsg) {

        ByteBuffer wrap = ByteBuffer.allocate(Taille_Paquet);
        wrap.putShort(OP_ERR);
        wrap.putShort(errorCode);
        wrap.put(errMsg.getBytes());
        wrap.put((byte) 0);

        DatagramPacket receivePacket = new DatagramPacket(wrap.array(),wrap.array().length);
        try {
            SocketErr.send(receivePacket);
        } catch (IOException e) {
            System.err.println("Problem sending error packet.");
            e.printStackTrace();
        }

    }


    private DatagramPacket ackPacket(short block) {

        ByteBuffer buffer = ByteBuffer.allocate(Taille_Paquet);
        buffer.putShort(OP_ACK);
        buffer.putShort(block);

        return new DatagramPacket(buffer.array(), 4);
    }

    private DatagramPacket dataPacket(short block, byte[] data, int length) {

        ByteBuffer buffer = ByteBuffer.allocate(Taille_Paquet);
        buffer.putShort(OP_DAT);
        buffer.putShort(block);
        buffer.put(data, 0, length);

        return new DatagramPacket(buffer.array(), 4+length);
    }

    private short getAck(DatagramPacket ack) {
        ByteBuffer buffer = ByteBuffer.wrap(ack.getData());
        short opcode = buffer.getShort();
        if (opcode == OP_ERR) {
            System.err.println("Erreur code 5");
            return -1;
        }

        return buffer.getShort();
    }
    private short getData(DatagramPacket data) {
        ByteBuffer buffer = ByteBuffer.wrap(data.getData());
        short opcode = buffer.getShort();
        if (opcode == OP_ERR) {
            System.err.println("Erreur code 5");
            return -1;
        }

        return buffer.getShort();
    }

}


