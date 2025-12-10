import java.io.*;
import java.net.*;
import java.util.Random;

public class Cliente {
    public static void main(String[] args) {
        Random random = new Random();
        
        System.out.println("Cliente iniciado...");

        try (PrintWriter logger = new PrintWriter(new FileWriter("cliente_log.txt", true))) {
            while (true) {

                boolean isEscrita =  random.nextBoolean();
                String msgEnviar;
                String logMsg;

                if (isEscrita) {
                    int n1 = 2 + random.nextInt(999999);
                    int n2 = 2 + random.nextInt(999999);
                    msgEnviar = "WRITE;" + n1 + ";" + n2;
                    logMsg = "REQ Escrita (" + n1 + "," + n2 + ")";
                }

                else {
                    msgEnviar = "READ";
                    logMsg = "REQ Leitura";
                }
                
                
                System.out.println("Enviando: " + msgEnviar);
                logger.println("Enviado: " + logMsg);
                logger.flush();

                // Envia para o Balanceador
                try (Socket socket = new Socket("localhost", 9090);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    
                    out.println(msgEnviar);
                    String resposta = in.readLine();
                    System.out.println("Resposta do LB: " + resposta);
                    logger.println("Resposta: " + resposta);
                    
                } catch (IOException e) {
                    System.err.println("Erro ao conectar ao Balanceador: " + e.getMessage());
                }

                // Dorme entre 20 e 50ms
                int sleepTime = 20 + random.nextInt(31);
                Thread.sleep(sleepTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}