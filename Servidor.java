import java.io.*;
import java.net.*;
import java.util.Random;

public class Servidor {
    private int id;
    private int porta;

    public Servidor(int id) {
        this.id = id;
        // Define porta baseada no ID (8001, 8002, 8003)
        this.porta = 8000 + id;
    }

    public void iniciar() throws IOException {
        ServerSocket serverSocket = new ServerSocket(this.porta);
        System.out.println("Servidor " + id + " ouvindo na porta " + porta);

        while (true) {
            Socket socket = serverSocket.accept();
            // Requisito: Instanciar thread sob demanda para qualquer requisição
            new Thread(new Worker(socket, id)).start();
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Digite o ID deste servidor (1, 2 ou 3): ");
        int id = Integer.parseInt(reader.readLine());
        new Servidor(id).iniciar();
    }

    private static class Worker implements Runnable {
        private Socket socket;
        private int serverId;
        private Random random = new Random();

        public Worker(Socket s, int id) {
            this.socket = s;
            this.serverId = id;
        }

        private int mdc(int a, int b) {
            return b == 0 ? a : mdc(b, a % b);
        }

        @Override
        public void run() {
            try {
                // Requisito: Thread dorme 100-200ms ANTES de processar
                int sleepTime = 100 + random.nextInt(101);
                Thread.sleep(sleepTime);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String msg = in.readLine();
                if (msg == null) return;

                String[] partes = msg.split(";");
                String tipo = partes[0]; // REQ ou COMMIT

                if (tipo.equals("REQ")) {
                    // --- FLUXO 1: Chegou do Balanceador ---
                    // Ação: Repassar para o Coordenador para garantir ordem global
                    String n1 = partes[1];
                    String n2 = partes[2];
                    
                    System.out.println("[S" + serverId + "] Recebido do LB (" + n1 + "," + n2 + "). Repassando ao Coordenador.");
                    enviarParaCoordenador(n1 + ";" + n2);
                    
                    // Libera conexão com LB (O processamento real acontecerá assincronamente via Coordenador)
                    out.println("RECEBIDO");

                } else if (tipo.equals("COMMIT")) {
                    // --- FLUXO 2: Chegou do Coordenador ---
                    // Ação: Calcular MDC e escrever no arquivo local
                    int n1 = Integer.parseInt(partes[1]);
                    int n2 = Integer.parseInt(partes[2]);
                    
                    int resultado = mdc(n1, n2);
                    String linha = "O MDC entre " + n1 + " e " + n2 + " é " + resultado;
                    
                    escreverArquivo(linha);
                    System.out.println("[S" + serverId + "] ESCREVENDO: " + linha);
                    
                    // Confirma para o Coordenador que escreveu
                    out.println("OK");
                }
                
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void enviarParaCoordenador(String dados) {
            try (Socket s = new Socket("localhost", 7000);
                 PrintWriter pw = new PrintWriter(s.getOutputStream(), true)) {
                pw.println(dados);
            } catch (IOException e) {
                System.err.println("[S" + serverId + "] Erro ao contatar Coordenador.");
            }
        }

        private synchronized void escreverArquivo(String linha) {
            // Requisito: Servidor escreve apenas no seu arquivo local
            try (FileWriter fw = new FileWriter("server" + serverId + ".txt", true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(linha);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}