import java.io.*;
import java.net.*;
import java.util.Random;

public class Balanceador {
    private static final int PORTA_LB = Config.getPort("LB.PORT");

    private static final int num_servidores = Config.getNum("NUMBER.SERVERS");

    private static final int s1 = Config.getPort("S1.PORT");
    private static final int s2 = Config.getPort("S2.PORT");
    private static final int s3 = Config.getPort("S3.PORT");

    private static final int[] PORTAS_SERVIDORES = {s1, s2, s3};

    public static void main(String[] args) {
        System.out.println(">>> Balanceador rodando na porta " + PORTA_LB);
        
        try (ServerSocket serverSocket = new ServerSocket(PORTA_LB)) {
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                new Thread(new LoadBalancerWorker(clienteSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class LoadBalancerWorker implements Runnable {
        private Socket clienteSocket;
        private Random random = new Random();

        public LoadBalancerWorker(Socket s) {
            this.clienteSocket = s;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                 PrintWriter outCliente = new PrintWriter(clienteSocket.getOutputStream(), true)) {

                String request = in.readLine();

                if (request != null) {
                    int indice = random.nextInt(num_servidores);
                    int portaDestino = PORTAS_SERVIDORES[indice];

                    System.out.println("[LB] Redirecionando " + request + " para Servidor na porta " + portaDestino);
                    
                    try (Socket sServidor = new Socket("localhost", portaDestino);
                         PrintWriter outServidor = new PrintWriter(sServidor.getOutputStream(), true);
                         BufferedReader inServidor = new BufferedReader(new InputStreamReader(sServidor.getInputStream()))) {
                        
                        outServidor.println("REQ;" + request);
                        
                        String respostaServidor = inServidor.readLine();
                        
                        if (request.startsWith("READ")) {
                            outCliente.println("Total de Linhas no Servidor " + portaDestino + ": " + respostaServidor);
                        }
                        else {
                            outCliente.println("PROCESSANDO_ESCRITA");
                        }
                    } catch (IOException e) {
                        outCliente.println("ERRO: Servidor destino indisponível");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}