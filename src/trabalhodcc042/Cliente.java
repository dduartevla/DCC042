/*
 * Trabalho da disciplina DCC042 - Redes de Computadores 2022-1
 */
package trabalhodcc042;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import javax.swing.JOptionPane;
import java.net.InetAddress;
import java.util.Formatter;
import java.util.Random;

/**
 * @author Débora izabel Rocha Duarte - 201776029
 * @author Laís Figueiredo Linhares - 201735030
 */
public class Cliente {
    
    static File fSaida;
    static Formatter resultado;
    static String localResultado = "resultado.txt";
    
    public static void main(String[] args) {
        System.out.println("Pronto para receber!");
        int porta = 1025; //porta 
        String localArquivo = "arquivoSaida.txt"; // Arquivo de saída
        criaArquivo(porta, localArquivo); // Cria o arquivo
    }
     
    public static void criaArquivo (int porta, String localArquivo){
        try{
            resultado = new Formatter(localResultado);
            DatagramSocket socket = new DatagramSocket(porta);
            byte[] entradaNome = new byte[1024]; // Guarda dados do Datagram com o nome do arquivo
            DatagramPacket entradaNomePacket = new DatagramPacket(entradaNome, entradaNome.length);
            socket.receive(entradaNomePacket); // Recebe o datagrama com o nome do arquivo
            System.out.println("Recebendo entrada");
            byte [] data = entradaNomePacket.getData(); // Lê o nome em bytes
            String arqNome = new String(data, 0, entradaNomePacket.getLength()); // Converte o nome em uma String
            
            System.out.println("Criando arquivo");
            File f = new File (localArquivo); // Criando o arquivo
            FileOutputStream arqSaida = new FileOutputStream(f); //escreve no arquivo
            
            recebeArquivo(arqSaida, socket); // Receiving the file
                        
        }catch(Exception ex){
            ex.printStackTrace();
            System.exit(1);
        }   
    }
    
    private static void recebeArquivo(FileOutputStream arqSaida, DatagramSocket socket) throws IOException {
        System.out.println("Recebendo arquivo");
        boolean flag; // Chegou ao final do arquivo
        int numSequencia = 0; 
        int fimSequencia = 0; 
        
        while (true) {
            byte[] mensagem = new byte[1024]; // Armazena os dados recebidos
            byte[] paraEscrita = new byte[1021]; //Dados para escrever no arquivo

            // Recebe o pacote e pega os dados
            DatagramPacket pacoteRecebido = new DatagramPacket(mensagem, mensagem.length);
            socket.receive(pacoteRecebido);
            mensagem = pacoteRecebido.getData(); // Dados para escrita no arquivo

            // Pega o endereço para enviar o ack
            InetAddress address = pacoteRecebido.getAddress();
            int port = pacoteRecebido.getPort();

            // pega número de sequencia
            numSequencia = ((mensagem[0] & 0xff) << 8) + (mensagem[1] & 0xff);
            // vefirifica se chegou ao final do arquivo
            flag = (mensagem[2] & 0xff) == 1;
            
            Random rand = new Random();
            int controle = rand.nextInt();
            
            // Se numero de sequencia é igual o  último + 1 está correto
            // pega os dados da mensagem e escreve o ack informando que chegou corretamente
            if (numSequencia == (fimSequencia + 1) && controle%2 != 0) { //controle serve para gerar alguns descartes

                // coloca o ultimo numero de sequencia como fim da sequencia
                fimSequencia = numSequencia;

                // Pega os dados da mensagem
                System.arraycopy(mensagem, 3, paraEscrita, 0, 1021);

                //Escreve os dados no arquivo e imprime o numero de sequencia
                arqSaida.write(paraEscrita);
                System.out.println("Recebido. Numero de Sequencia: " + fimSequencia);
                resultado.format("Recebido. Numero de Sequencia: " + fimSequencia);

                //Envis o ACK
                enviaAck(fimSequencia, socket, address, port);
            } else {
                System.out.println("Número de sequência esperado: " + (fimSequencia + 1) + ". Número recebido: " + numSequencia + ". Descartando");
                resultado.format("Número de sequência esperado: " + (fimSequencia + 1) + ". Número recebido: " + numSequencia + ". Descartando");
                // Reevia o ACK
                enviaAck(fimSequencia, socket, address, port);
            }
            // Verifica se é o ultimo datagrama
            if (flag) {
                arqSaida.close();
                resultado.close();
                break;
            }
        }
    }

    private static void enviaAck(int fimSequencia, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // envia ACK
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (fimSequencia >> 8);
        ackPacket[1] = (byte) (fimSequencia);
        // o datagrama para enviar
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement);
        System.out.println("ACK enviado: Número de Sequencia = " + fimSequencia);
        resultado.format("ACK enviado: Número de Sequencia = " + fimSequencia);
    }
    
}
