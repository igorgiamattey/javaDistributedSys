import java.io.*;
import java.net.*;
import java.util.Random;

public class Cliente {

    private static final int min_aleatorio = Config.getNum("OFFSET.RANDOM");
    private static final int intervalo_num = Config.getNum("RANGE.RANDOM");

    private static final int min_dormir = Config.getNum("OFFSET.SLEEP");
    private static final int intervalo_dormir = Config.getNum("RANGE.SLEEP");

    public static void main(String[] args) {
        Random random = new Random();

        System.out.println("Cliente iniciado...");

        try (PrintWriter logger = new PrintWriter(new FileWriter("cliente_log.txt", true))) {
            while (true) {

                boolean isEscrita =  random.nextBoolean();
                String msgEnviar;
                String logMsg;

                if (isEscrita) {

                    int n1 = min_aleatorio + random.nextInt(intervalo_num);
                    int n2 = min_aleatorio + random.nextInt(intervalo_num);

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

                try (Socket socket = new Socket("localhost", Config.getPort("LB.PORT"));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    
                    out.println(msgEnviar);
                    String resposta = in.readLine();
                    System.out.println("Resposta do LB: " + resposta);
                    logger.println("Resposta: " + resposta);

                } catch (IOException e) {
                    System.err.println("Erro ao conectar ao Balanceador: " + e.getMessage());
                }

                int sleepTime = min_dormir + random.nextInt(intervalo_dormir);
                Thread.sleep(sleepTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}