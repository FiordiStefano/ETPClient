/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ETPClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Classe che gestisce il file da inviare e crea i pacchetti informazioni, dati e risposta
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
     * Metodo che crea il pacchetto dati
     *
     * @param packetIndex il numero del pacchetto
     * @param packByte il vettore contenente i dati in binario
     * @return il pacchetto Json
     */
    protected JsonObject createJsonPacket(int packetIndex, byte[] packByte) {
        // codifica dei dati da binario a base64 per evitare problemi nella conversione in Json
        String base64text = Base64.getEncoder().encodeToString(packByte);
        return Json.createObjectBuilder()
                .add("type", "data")
                .add("number", packetIndex)
                .add("text", base64text)
                .build();
    }

    /**
     * Metodo che costruisce il pacchetto
     *
     * @param packetIndex il numero del pacchetto
     * @return il pacchetto da inviare
     */
    public JsonObject buildPacket(int packetIndex) {
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
        // Incapsulamento del pacchetto in un json per il trasferimento
        JsonObject JsonPacket = createJsonPacket(packetIndex, packByte);

        return JsonPacket;
    }

    /**
     * Metodo che effettua l'hashing MD5 del file e crea il pacchetto di
     * informazioni
     *
     * @return il pacchetto di informazioni
     * @throws NoSuchAlgorithmException
     */
    public JsonObject getInfoPacket() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(ByteFile);
        byte[] messageDigestMD5 = messageDigest.digest();
        StringBuilder stringBuffer = new StringBuilder();
        for (byte bytes : messageDigestMD5) {
            stringBuffer.append(String.format("%02x", bytes & 0xff));
        }

        JsonObject JsonInfoPacket = Json.createObjectBuilder()
                .add("type", "info")
                .add("name", FileToSend.getName())
                .add("length", FileToSend.length())
                .add("checksum", stringBuffer.toString())
                .build();

        return JsonInfoPacket;
    }
}
