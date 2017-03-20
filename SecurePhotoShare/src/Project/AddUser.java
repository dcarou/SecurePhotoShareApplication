package Projeto2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class AddUser {

	/**
	 * Adiciona um utilizador a lista, guardando apenas o hash da password
	 * 
	 * @param args
	 *            1o argumento = username, 2o argumento = password
	 */
	public static void main(String[] args) {

		if (args.length != 3 || args[0] == null || args[1] == null || args[2] == null){
			System.out.println("Argumentos errados! Sintaxe �: AddUser <username> <password> <password MAC>");
			System.exit(1);
		}

		String username = args[0];
		username.toLowerCase();
		String password = args[1];
		String filePassword = args[2];
		String hashedPassword = null;

		try {
			hashedPassword = MAC.generateMACnoPass(password);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// verifica se ja existe nos ficheiros

		String linha;
		BufferedReader in;
		BufferedWriter out;

		try {
			in = new BufferedReader(new FileReader("users.txt"));
			linha = in.readLine();
			StringBuilder sb = new StringBuilder();
			StringBuilder sbMac = new StringBuilder();
			boolean done = false;

			// le ficheiro de utilizadores
			while (linha != null) {
				sbMac.append(linha + "\n");
				String[] temp = linha.split(":");
				if (temp[0].compareTo(username) == 0) {
					sb.append(username + ":" + hashedPassword + "\n");
					done = true;
				} else {
					sb.append(linha + "\n");
				}
				linha = in.readLine();

			}
			in.close();

			if (!done)
				sb.append(username + ":" + hashedPassword + "\n");
			
			// verifica se existe ficheiro de password de protecao MAC
			// caso nao exista pede input ao utilizador para criar um novo ou sair
			File file = new File("cifra.tmp");
			if (!file.exists()){
				Scanner s = new Scanner(System.in);
				String a = "ab";
				while(a.compareTo("s") != 0 && a.compareTo("c") != 0 ){
					System.out.println("Ficheiro de utilizador nao protegido por MAC.");
					System.out.println("Pressione c para calcular um MAC ou pressione s para sair e carregue ENTER.");
					a = s.nextLine();
				}
				s.close();
				
				if(a.compareTo("s") == 0)
					System.exit(0);
				
				
				Files.write(file.toPath(), MAC.generateMAC(sb.toString(), filePassword).getBytes());
				in.close();
			}else{
				in = new BufferedReader(new FileReader("cifra.tmp"));
				
				if (MAC.generateMAC(sbMac.toString(), filePassword).compareTo(in.readLine()) == 0){
					Files.write(file.toPath(), MAC.generateMAC(sb.toString(), filePassword).getBytes());
					in.close();
				}else{
					System.err.println("Password incorrecta!");
					in.close();
					System.exit(-1);
				}
			}
			

			// escreve o novo ficheiro de users
			out = new BufferedWriter(new FileWriter("users.txt"));
			out.write(sb.toString());
			out.close();
			

		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

	}

}
