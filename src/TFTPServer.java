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


public class TFTPServer {

    public static final int Taille_Paquet = 516;
	public static  String Dossier = "";
    public static final short OP_RRQ = 1;
    public static final short OP_WRQ = 2;
    public static final short OP_DAT = 3;
    public static String  mode;
    public int PORT = 69;
    public boolean EtatServeurDemarre=true;
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
        do{ //ETAT DE CONSTANT ECOUT

            DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(receivePacket);
            } catch (IOException e) {
            }
            final InetSocketAddress AdresseClient = new InetSocketAddress(receivePacket.getAddress(),receivePacket.getPort());

            if (AdresseClient == null)
                continue;

            Utils Util = new Utils();
            final StringBuffer FichierDemande = new StringBuffer();
            final int reqtype = Util.ParseRequete(buf, FichierDemande);
            System.out.println(reqtype);
            new Thread() {
                public void run() {
                    try {
                        DatagramSocket SocketServeur = new DatagramSocket(0);
                        SocketServeur.connect(AdresseClient);

                        FichierDemande.insert(0, Dossier);
                        RRQ EtatRRQ = new RRQ();
                        WRQ EtatWRQ = new WRQ();
                        switch(reqtype){
                            case OP_RRQ:
                                EtatRRQ.Etat(SocketServeur, FichierDemande.toString(), OP_RRQ);
                                System.out.println("iciici");
                                break;
                            case OP_WRQ:
                                EtatWRQ.Etat(SocketServeur,FichierDemande.toString(),OP_WRQ);
                                break;
                            default: break;
                        }
                        SocketServeur.close();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }while(EtatServeurDemarre);

    }






}


