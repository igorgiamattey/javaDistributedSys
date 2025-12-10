import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Coordenador {
    private static BlockingQueue<String> filaGlobal = new LinkedBlockingQueue<>();
    
    private static final int PORTA_COORD = Config.getPort("COORD.PORT");
    
    private static final int s1 = Config.getPort("S1.PORT");
    private static final int s2 = Config.getPort("S2.PORT");
    private static final int s3 = Config.getPort("S3.PORT");
    
    private static final int[] PORTAS_SERVIDORES = {s1, s2, s3};

    public static void main(String[] args) {
        System.out.println(">>> Coordenador iniciado na porta " + PORTA_COORD);

        new Thread(new Difusor()).start();

        try (ServerSocket serverSocket = new ServerSocket(PORTA_COORD)) {
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg = in.readLine();
                
                if (msg != null) {
                    System.out.println("[Coordenador] Requisição recebida para fila: " + msg);
                    filaGlobal.put(msg);
                }
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class Difusor implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String dados = filaGlobal.take(); 
                    
                    System.out.println("[Coordenador] Iniciando difusão de escrita: " + dados);

                    for (int porta : PORTAS_SERVIDORES) {
                        enviarOrdemDeEscritaComRetentativa(porta, dados);
                    }
                    System.out.println("[Coordenador] Consistência atingida para: " + dados);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void enviarOrdemDeEscritaComRetentativa(int porta, String dados) {
            
            boolean sucesso = false;
            int intervalo_tentativas = 5000;

            while (!sucesso) {
                try (Socket s = new Socket("localhost", porta);
                     PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                    
                    out.println("COMMIT;" + dados);
                    
                    String resposta = in.readLine(); 
                    if ("OK".equals(resposta)) {
                        sucesso = true;
                    }
                } catch (IOException e) {
                    System.err.println("[ALERTA CRÍTICO] Servidor na porta " + porta + " indisponível.");
                    System.err.println("          >>> Pausando sistema até que ele retorne...");
                    try {
                        Thread.sleep(intervalo_tentativas);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}