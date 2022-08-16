/*
 * Trabalho da disciplina DCC042 - Redes de Computadores 2022-1
 */
package trabalhodcc042;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import javax.swing.JFileChooser;


/**
 * @author Débora izabel Rocha Duarte - 201776029
 * @author Laís Figueiredo Linhares - 201735030
 */
public class Servidor {
    
    private void isPronto(int porta, String host) {

        System.out.println("Escolhendo arquivo para enviar");
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(host);
            String nomeArquivo;

            JFileChooser jfc = new JFileChooser(); // Arquivo para enviar
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY); // Seleciona somente arquivos (sem diretórios)
            if (jfc.isMultiSelectionEnabled()) { // Apenas um arquivo por vez
                jfc.setMultiSelectionEnabled(false);
            }

            int r = jfc.showOpenDialog(null);
            if (r == JFileChooser.APPROVE_OPTION) { // Na escolha de um arquivo
                File f = jfc.getSelectedFile();
                nomeArquivo = f.getName();
                byte[] fileNameBytes = nomeArquivo.getBytes(); // Converte o nome do arquivo para bytes
                DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, address, porta); // Pacote com o nome do arquivo
                socket.send(fileStatPacket); // Envia o pacote com o nome do arquivo

                byte[] fileByteArray = leFileToByteArray(f); //ler com sotaque frances
                enviaArquivo(socket, fileByteArray, address, porta); //Método que faz o envio
            }
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
     
    private void enviaArquivo(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int porta) throws IOException {
        System.out.println("Enviando arquivo...");
        int numSequencia = 0;
        boolean flag; // Fim do arquivo?
        int seqAck = 0; // Datagrama enviado corretamente?

        for (int i = 0; i < fileByteArray.length; i = i + 1021) {
            numSequencia += 1;

            // Cria mensagem
            byte[] mensagem = new byte[1024]; // Primeiros bytes para controle (ordem e integridade)
            mensagem[0] = (byte) (numSequencia >> 8);
            mensagem[1] = (byte) (numSequencia);

            if ((i + 1021) >= fileByteArray.length) { // Fim do arquivo?
                flag = true;
                mensagem[2] = (byte) (1); // Sim, fim do arquivo.
            } else {
                flag = false;
                mensagem[2] = (byte) (0); // não, ainda não é o fim do arquivo
            }

            if (!flag) {
                System.arraycopy(fileByteArray, i, mensagem, 3, 1021);
            } else { // Se é o último datagrama
                System.arraycopy(fileByteArray, i, mensagem, 3, fileByteArray.length - i);
            }

            DatagramPacket sendPacket = new DatagramPacket(mensagem, mensagem.length, address, porta); // The data to be sent
            socket.send(sendPacket); // Sending the data
            System.out.println("Número de sequencia enviado: " + numSequencia);

            boolean ackRec; // Datagrama chegou?

            while (true) {
                byte[] ack = new byte[2]; // Cria novo pacote para o ACk do datagrama
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length); //ackpack :)

                try {
                    socket.setSoTimeout(50); // Esperando o servidor enviar o ack
                    socket.receive(ackpack);
                    seqAck = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff); // Numero sequencia
                    ackRec = true; // Ack recebido
                } catch (SocketTimeoutException e) {
                    System.out.println("tempo expirado esperando pelo ack");
                    ackRec = false; // Ack não recebido
                }

                // Se deu tudo certo próximo pacote pode ser enviado
                if ((seqAck == numSequencia) && (ackRec)) {
                    System.out.println("Ack received: Sequence Number = " + seqAck);
                    break;
                } // pacote não recebido, então, enviado
                else {
                    socket.send(sendPacket);
                    System.out.println("Reenviando: Numero de Sequencia = " + numSequencia);
                }
            }
        }
    }
      
    private static byte[] leFileToByteArray(File file) {
        FileInputStream arqEntrada = null;
        // cria um vetro de bytes com o tamanho do arquivo
        byte[] bArray = new byte[(int) file.length()];
        try {
            arqEntrada = new FileInputStream(file);
            arqEntrada.read(bArray);
            arqEntrada.close();

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
        return bArray;
    }
    
    public static void main(String[] args) {
        int porta = 1025;
        String cliente = "127.0.0.1"; // local host
        Servidor servidor= new Servidor();
        servidor.isPronto(porta, cliente);
    }
    
    
}
