package com.trab1;

import java.io.*;
import java.net.*;
import java.util.Random;

public class Client {
	
	public static void main(String[] args) {
		
		String servidor = "localhost";
		int porta = 5001;
	
		System.out.println("Iniciando o cliente...");
		
		try (Socket socket = new Socket(servidor, porta)) {
			
			System.out.println("Conectado ao servidor.");
			
			BufferedReader entrada = new BufferedReader(
			new InputStreamReader(socket.getInputStream()));
			PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
			
			Random random = new Random();
			int numero = random.nextInt(1_000_000) + 1;
			System.out.println("Número sorteado: " + numero);
					
			saida.println(numero);
			
			String resposta = entrada.readLine();
			System.out.println("Resposta ao servidor: " + resposta);
			
		}	catch (IOException e) {
				e.printStackTrace();
		}
	}
	
	private static int generateRandNum() {
		
	}
}