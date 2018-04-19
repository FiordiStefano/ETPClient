/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ETPClient;

import com.google.protobuf.ByteString;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import packet.protoPacket.dataPacket;
import packet.protoPacket.info;

/**
 * Classe che gestisce il file da inviare e crea i pacchetti informazioni, dati
 * e risposta
 *
 * @author Stefano Fiordi
 */
public class FileHandlerClient {

    /**
     * grandezza standard del pacchetto
     */
    private final int NumberOfBytes = 4096;
    /**
     * file da inviare
     */
    protected File FileToSend;
    /**
     * vettore di byte contenente il file
     */
    protected byte[] ByteFile;
    /**
     * numero di pacchetti totali
     */
    protected int nPackets;

    /**
     * Costruttore che calcola il numero dei pacchetti in base alla lunghezza
     * del file
     *
     * @param FileToSend il file da inviare
     * @throws IOException
     */
    public FileHandlerClient(File FileToSend) throws IOException {
        this.FileToSend = new File(FileToSend.getPath());
        // codifica del file in binario
        ByteFile = Files.readAllBytes(FileToSend.toPath());
        if (FileToSend.length() % NumberOfBytes == 0) {
            nPackets = (int) FileToSend.length() / NumberOfBytes;
        } else {
            nPackets = (int) FileToSend.length() / NumberOfBytes + 1;
        }
    }

    /**
     * Metodo che crea il pacchetto dati protobuf
     *
     * @param packetIndex il numero del pacchetto
     * @param packByte il vettore contenente i dati in binario
     * @return il pacchetto dati protobuf
     */
    protected dataPacket createProtoPacket(int packetIndex, byte[] packByte) {

        return dataPacket.newBuilder()
                .setType("data")
                .setNumber(packetIndex)
                .setText(ByteString.copyFrom(packByte))
                .build();
    }

    /**
     * Metodo che costruisce il pacchetto protobuf
     *
     * @param packetIndex il numero del pacchetto
     * @return il pacchetto da inviare
     */
    public dataPacket buildProtoPacket(int packetIndex) {
        byte[] packByte;
        if (packetIndex != nPackets - 1) {
            packByte = new byte[NumberOfBytes];
            for (int i = 0; i < NumberOfBytes; i++) {
                packByte[i] = ByteFile[i + (packetIndex * NumberOfBytes)];
            }
        } else {
            int LastBytes = (int) FileToSend.length() - (NumberOfBytes * (nPackets - 1));
            packByte = new byte[LastBytes];
            int j = 0;
            for (int i = packetIndex * NumberOfBytes; i < FileToSend.length(); i++) {
                packByte[j] = ByteFile[i];
                j++;
            }
        }

        return createProtoPacket(packetIndex, packByte);
    }

    /**
     * Metodo che effettua l'hashing MD5 del file e crea il pacchetto di
     * informazioni protobuf
     *
     * @return il pacchetto di informazioni protobuf
     * @throws NoSuchAlgorithmException
     */
    public info getProtoInfoPacket() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(ByteFile);
        byte[] messageDigestMD5 = messageDigest.digest();
        StringBuilder stringBuffer = new StringBuilder();
        for (byte bytes : messageDigestMD5) {
            stringBuffer.append(String.format("%02x", bytes & 0xff));
        }

        return info.newBuilder()
                .setType("info")
                .setName(FileToSend.getName())
                .setLength(FileToSend.length())
                .setChecksum(stringBuffer.toString())
                .build();
    }
}
