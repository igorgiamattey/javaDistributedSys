import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Coordenador {
    // Fila Global para garantir a ordem das escritas
    private static BlockingQueue<String> filaGlobal = new LinkedBlockingQueue<>();
    private static final int PORTA_COORD = 7000;
    // Portas dos 3 servidores de aplicação
    private static final int[] PORTAS_SERVIDORES = {8001, 8002, 8003};

    public static void main(String[] args) {
        System.out.println(">>> Coordenador iniciado na porta " + PORTA_COORD);

        // Thread que processa a fila e manda todos escreverem (Sincronização)
        new Thread(new Difusor()).start();

        // Loop principal: Recebe requisições dos Servidores (vindas do LB) e enfileira
        try (ServerSocket serverSocket = new ServerSocket(PORTA_COORD)) {
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg = in.readLine();
                
                if (msg != null) {
                    System.out.println("[Coordenador] Requisição recebida para fila: " + msg);
                    filaGlobal.put(msg); // Adiciona na fila (FIFO)
                }
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Classe responsável por tirar da fila e garantir que TODOS escrevam
    static class Difusor implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 1. Pega a próxima transação da fila (Bloqueia se vazia)
                    String dados = filaGlobal.take(); 
                    
                    System.out.println("[Coordenador] Iniciando difusão de escrita: " + dados);

                    // 2. Envia comando de ESCRITA (COMMIT) para os 3 servidores sequencialmente
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
            
            // Loop infinito até conseguir escrever neste servidor específico
            while (!sucesso) {
                try (Socket s = new Socket("localhost", porta);
                     PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                    
                    out.println("COMMIT;" + dados);
                    
                    String resposta = in.readLine(); 
                    if ("OK".equals(resposta)) {
                        sucesso = true; // Servidor confirmou, pode sair do loop
                    }
                } catch (IOException e) {
                    System.err.println("[ALERTA CRÍTICO] Servidor na porta " + porta + " indisponível.");
                    System.err.println("          >>> Pausando sistema até que ele retorne...");
                    try {
                        Thread.sleep(5000); // Tenta novamente a cada 5 segundos
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}