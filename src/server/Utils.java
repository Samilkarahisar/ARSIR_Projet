package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

public class Utils {

    public static final int Taille_Paquet = 516;
    public static final short OP_RRQ = 1;
    public static final short OP_WRQ = 2;
    public static final short OP_DAT = 3;
    public static final short OP_ACK = 4;
    public static final short OP_ERR = 5;
    public static String 	  mode;
    public int PORT = 69;


  public short ParseRequete(byte[] buf, StringBuffer requestedFile) {
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

    public void sendError(DatagramSocket SocketErr, short errorCode, String errMsg) {

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


    public DatagramPacket ackPacket(short block) {

        ByteBuffer buffer = ByteBuffer.allocate(Taille_Paquet);
        buffer.putShort(OP_ACK);
        buffer.putShort(block);

        return new DatagramPacket(buffer.array(), 4);
    }

    public DatagramPacket dataPacket(short block, byte[] data, int length) {

        ByteBuffer buffer = ByteBuffer.allocate(Taille_Paquet);
        buffer.putShort(OP_DAT);
        buffer.putShort(block);
        buffer.put(data, 0, length);

        return new DatagramPacket(buffer.array(), 4+length);
    }

    public short getAck(DatagramPacket ack) {
        ByteBuffer buffer = ByteBuffer.wrap(ack.getData());
        short opcode = buffer.getShort();
        if (opcode == OP_ERR) {
            System.err.println("Erreur code 5");
            return -1;
        }

        return buffer.getShort();
    }
    public short getData(DatagramPacket data) {
        ByteBuffer buffer = ByteBuffer.wrap(data.getData());
        short opcode = buffer.getShort();
        if (opcode == OP_ERR) {
            System.err.println("Erreur code 5");
            return -1;
        }

        return buffer.getShort();
    }
}
