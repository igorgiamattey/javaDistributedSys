import java.io.*;
import java.net.*;
import java.util.Random;

public class Balanceador {
    private static final int PORTA_LB = 8000;
    // Portas dos servidores
    private static final int[] PORTAS_SERVIDORES = {8001, 8002, 8003};

    public static void main(String[] args) {
        System.out.println(">>> Balanceador rodando na porta " + PORTA_LB);
        
        try (ServerSocket serverSocket = new ServerSocket(PORTA_LB)) {
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                // Nova thread para não bloquear o aceite de conexões
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

                String request = in.readLine(); // "N1;N2"
                if (request != null) {
                    // Sorteia servidor aleatório (Chance igual para os 3)
                    int indice = random.nextInt(3);
                    int portaDestino = PORTAS_SERVIDORES[indice];

                    System.out.println("[LB] Redirecionando " + request + " para Servidor na porta " + portaDestino);
                    
                    try (Socket sServidor = new Socket("localhost", portaDestino);
                         PrintWriter outServidor = new PrintWriter(sServidor.getOutputStream(), true);
                         BufferedReader inServidor = new BufferedReader(new InputStreamReader(sServidor.getInputStream()))) {
                        
                        // Envia como REQ (Requisição Inicial)
                        outServidor.println("REQ;" + request);
                        
                        // Aguarda o servidor confirmar que recebeu e repassou ao coordenador
                        inServidor.readLine(); 
                    }
                    outCliente.println("ENVIADO");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}