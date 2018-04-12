/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ETPClient;

import com.google.protobuf.InvalidProtocolBufferException;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import packet.protoPacket.dataPacket;
import packet.protoPacket.info;
import packet.protoPacket.resp;

/**
 * Programma client per il trasferimento di file utilizzando il protocollo ETP
 *
 * @author Stefano Fiordi
 */
public class ETPClient extends JFrame {

    JFileChooser fileChooser;
    JProgressBar progressBar;
    Socket socket;
    BufferedWriter out;
    BufferedReader in;
    FileHandlerClient fhc;
    Thread thClient;

    /**
     * Costruttore nel quale viene inizializzata la GUI
     */
    public ETPClient() {
        int width = (Toolkit.getDefaultToolkit().getScreenSize().width * 34) / 100;
        int heigth = (Toolkit.getDefaultToolkit().getScreenSize().height * 40) / 100;
        int x = (Toolkit.getDefaultToolkit().getScreenSize().width * 33) / 100;
        int y = (Toolkit.getDefaultToolkit().getScreenSize().height * 20) / 100;
        super.setBounds(x, y, width, heigth);
        super.setResizable(false);
        super.setTitle("ETPClient");
        super.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        super.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (thClient != null && thClient.isAlive()) {
                    if (JOptionPane.showConfirmDialog(ETPClient.this, "Sei sicuro di voler annullare il trasferimento?", "Esci", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                } else {
                    System.exit(0);
                }
            }
        });
        fileChooser = new JFileChooser();
        this.add(fileChooser, BorderLayout.PAGE_START);
        fileChooser.setApproveButtonText("Upload");
        fileChooser.setApproveButtonToolTipText("Invia il file selezionato");
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        progressBar.setValue(0);
        progressBar.setString("0%");
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        this.add(progressBar, BorderLayout.PAGE_END);
        fileChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                    if (fileChooser.getSelectedFile() != null && fileChooser.getSelectedFile().exists()) {
                        if (!(thClient != null && thClient.isAlive())) {
                            if (JOptionPane.showConfirmDialog(fileChooser, "Vuoi trasferire questo file: " + fileChooser.getSelectedFile().getName() + "?", "Conferma", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                                ETPTransfer();
                                thClient.start();
                            }
                        } else {
                            JOptionPane.showMessageDialog(fileChooser, "Impossibile inviare più di un file alla volta");
                        }
                    }
                } else {
                    if (thClient != null && thClient.isAlive()) {
                        if (JOptionPane.showConfirmDialog(ETPClient.this, "Sei sicuro di voler annullare il trasferimento?", "Esci", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                            System.exit(0);
                        }
                    } else {
                        System.exit(0);
                    }
                }
            }
        });
    }

    /**
     * Metodo che si occupa del collegamento al server tramite la socket e del
     * trasferimento del file
     */
    protected void ETPTransfer() {
        thClient = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Inizializzazione della socket
                    socket = new Socket("127.0.0.1", 4000);
                    // Inizializzazione della stream dalla socket
                    //out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(fileChooser, "Impossibile connettersi al server");
                    return;
                }

                try {
                    // Creazione delle stream di JSON per leggere e scrivere nella stream della socket
                    //JsonWriter writer = Json.createWriter(out);
                    //JsonReader reader = Json.createReader(in);

                    File FileToSend = fileChooser.getSelectedFile();
                    try {
                        fhc = new FileHandlerClient(FileToSend);
                        // invio del pacchetto informazioni
                        fhc.getProtoInfoPacket().writeDelimitedTo(socket.getOutputStream());
                        //writer.writeObject(fhc.getInfoPacket());
                        //out.flush();
                    } catch (IOException | NoSuchAlgorithmException ex) {
                        JOptionPane.showMessageDialog(fileChooser, "File non valido");
                        return;
                    }
                    // lettura del pacchetto di risposta
                    resp ProtoInfoRespPacket = resp.parseDelimitedFrom(socket.getInputStream());
                    //JsonObject JsonInfoRespPacket = reader.readObject();
                    //System.out.println(JsonInfoRespPacket.toString());

                    if (ProtoInfoRespPacket.getResp().equals("ok")) {
                        progressBar.setVisible(true);
                        OUTER:
                        for (int i = ProtoInfoRespPacket.getIndex(); i < fhc.nPackets; i++) {
                            try {
                                //writer = Json.createWriter(out);
                                // invio del pacchetto dati
                                fhc.buildProtoPacket(i).writeDelimitedTo(socket.getOutputStream());
                                //writer.writeObject(fhc.buildPacket(i));
                                //System.out.println("Invio pacchetto " + i + "...");
                                //out.flush();

                                //reader = Json.createReader(in);
                                // lettura del pacchetto di risposta
                                resp ProtoRespPacket = resp.parseDelimitedFrom(socket.getInputStream());
                                //JsonObject JsonRespPacket = reader.readObject();
                                switch (ProtoRespPacket.getResp()) {
                                    case "wp":
                                        i = ProtoRespPacket.getIndex() - 1;
                                        break;
                                    case "mrr":
                                        //System.out.println("Errore durante l'invio del pacchetto");
                                        JOptionPane.showMessageDialog(fileChooser, "Errore nell'invio del pacchetto");
                                        break OUTER;
                                    default:
                                        //System.out.println(" Pacchetto " + i + " inviato correttamente");
                                        float percent = 100f / fhc.nPackets * (i + 1);
                                        progressBar.setValue((int) percent);
                                        progressBar.setString((int) percent + "%");
                                        break;
                                }
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(fileChooser, "Errore di comunicazione");
                                return;
                            }
                        }

                        progressBar.setValue(100);
                        progressBar.setString("100%");

                        JOptionPane.showMessageDialog(fileChooser, "File trasferito correttamente");

                        progressBar.setValue(0);
                        progressBar.setString("0%");
                        progressBar.setVisible(false);
                    } else {
                        JOptionPane.showMessageDialog(fileChooser, "Il file esiste gia'");
                    }
                    // chiusura delle stream di Json
                    //writer.close();
                    //reader.close();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(fileChooser, "Errore di connessione");
                    progressBar.setValue(0);
                    progressBar.setString("0%");
                    progressBar.setVisible(false);
                }
                try {
                    // Chiusura della socket
                    socket.close();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(fileChooser, "Errore nella chiusura della socket");
                }
            }
        });
    }

    /**
     * Main che crea un'istanza della classe
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        new ETPClient().setVisible(true);
    }

}
