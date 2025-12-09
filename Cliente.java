import java.io.*;
import java.net.*;
import java.util.Random;

public class Cliente {
    public static void main(String[] args) {
        Random random = new Random();
        
        System.out.println("Cliente iniciado...");

        try (PrintWriter logger = new PrintWriter(new FileWriter("cliente_log.txt", true))) {
            while (true) {
                // Gera dois números aleatórios entre 2 e 1.000.000
                int n1 = 2 + random.nextInt(999999);
                int n2 = 2 + random.nextInt(999999);

                // Log local
                String msg = n1 + ";" + n2;
                logger.println("Enviado: " + msg);
                logger.flush();

                // Envia para o Balanceador
                try (Socket socket = new Socket("localhost", 8000);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                    out.println(msg);
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