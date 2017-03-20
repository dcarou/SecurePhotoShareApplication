package Projeto2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * Classe com as operacoes relativas ao cliente
 * 
 * @author fc40346,fc41907,fc43612
 *
 */
public class ClientOperations {

	// opcodes para as diversas operacoes
	private static final int OC_sendPhotos = 1;
	private static final int OC_listFollowedPhotos = 2;
	private static final int OC_copyFollowedPhotos = 3;
	private static final int OC_commentPhoto = 4;
	private static final int OC_followUsers = 5;
	private static final int OC_copyLatestPhotos = 6;
	
	private static final int CT_OK = 0;
	private static final int CT_NOK = 1;
	private static final int CT_WRONGSIG = -1;
	private static final int CT_SERVERERR = -2;
	private static final int CT_NOSUCHUSER = -3;
	private static final int CT_NOTSUPPORTED = -4;
	//nao usado
	//private static final int CT_NOK = 1;

	/**
	 * Funcao que copia as fotos dos utilizadores que localUsername se encontra
	 * a seguir para a sua pasta
	 * 
	 * @param args
	 *            - argumentos funcao main
	 * @param out
	 *            - stream de escrita
	 * @param in
	 *            - stream de leitura
	 * @param localUsername
	 *            - nome do utilizador
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static void copyFollowedPhotos(String[] args, ObjectOutputStream out,
			ObjectInputStream in, String localUsername, Key privateKey) throws Exception{

		String user = args[5];

		out.writeObject(OC_copyFollowedPhotos);
		out.writeObject(user);
		//verifica qual a chave que tem de fornecer ao servidor
		if(user.compareTo(localUsername) == 0)
			out.writeObject(privateKey);
		else{
			out.writeObject(getPrivateKey(user));
		}
		
		// cria pastas caso não existam
		File clientFolder = new File("client/" + localUsername + "/");

		if (!clientFolder.exists())
			clientFolder.mkdir();

		int size = (int) in.readObject();
		System.out.println("Existem " + size
				+ " ficheiros partilhados com o utilizador " + localUsername
				+ ".");

		for (int i = 0; i < size; i++) {
			try {
				String filename = (String) in.readObject();
				File myFile = new File("client/" + localUsername + "/" + user
						+ "/" + filename);
				System.out.println("Writing file " + "client/" + localUsername
						+ "/" + user + "/" + filename);
				myFile.getParentFile().mkdir();

				byte[] bytes = (byte[]) in.readObject();
				Files.write(myFile.toPath(), bytes);

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.err
						.println("Nao conseguiu escrever o ficheiro na posicao "
								+ i);
				return;
			}
		}
	}

	/**
	 * Copia as fotos mais recentes dos utilizadores que localUsername se
	 * encontra a seguir
	 * 
	 * @param out
	 *            - buffer de escrita
	 * @param in
	 *            - buffer de leitura
	 * @param localUsername
	 *            - nome do utilizador
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static void copyLatestPhotos(ObjectOutputStream out, ObjectInputStream in,
			String localUsername, Key privateKey) throws Exception {

		out.writeObject(OC_copyLatestPhotos);

		// cria pastas caso não existam
		File clientFolder = new File("client/" + localUsername + "/");
		clientFolder.mkdir();
		File latestFolder = new File("client/" + localUsername + "/latest/");
		latestFolder.mkdir();
		
		int size = (int) in.readObject();
		System.out.println("Existem " + size
				+ " ficheiros partilhados com o utilizador " + localUsername
				+ ".");

		for (int i = 0; i < size; i++) {
			try {
				String filename = (String) in.readObject();
				String user = (String) in.readObject();
				
				//verifica qual a chave que tem de fornecer ao servidor
				if(user.compareTo(localUsername) == 0)
					out.writeObject(privateKey);
				else{
					out.writeObject(getPrivateKey(user));
				}	
				
				File myFile = new File("client/" + localUsername + "/latest/"
						+ user + "/" + filename);
				System.out.println("A escrever ficheiro: " + "client/"
						+ localUsername + "/latest/" + user + "/" + filename);
				myFile.getParentFile().mkdir();

				byte[] bytes = (byte[]) in.readObject();
				Files.write(myFile.toPath(), bytes);

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.err
						.println("Nao conseguiu escrever o ficheiro na posicao "
								+ i);
				return;
			}
		}
	}

	/**
	 * Envia as fotos do localUsername para o servidor
	 * 
	 * @param args
	 *            - argumentos da funcao main
	 * @param out
	 *            - buffer de escrita
	 * @param in
	 *            - buffer de leitura
	 * @param localUsername
	 *            - nome do utilizador
	 * @return - true se as fotos foram copiadas com exito para o servidor,
	 *         false caso contario
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static boolean sendPhotos(String[] args, ObjectOutputStream out,
			ObjectInputStream in, String localUsername) throws IOException,
			ClassNotFoundException {

		// envia codigo da operacao para o servidor
		out.writeObject(OC_sendPhotos);

		ArrayList<File> files = new ArrayList<File>();

		System.out.println("Local username is: " + localUsername);

		for (int i = 5; i < args.length; i++) {
			File file = new File("client/" + localUsername + "/" + args[i]);
			if (file.exists()) {
				if(file.getPath().endsWith(".jpg") || file.getPath().endsWith(".png") 
						|| file.getPath().endsWith(".bmp") || file.getPath().endsWith(".gif")){
					files.add(file);
					System.out.println("A Adicionar " + args[i]
							+ " para a lista a enviar...");	
				}else{
					System.out.println(args[i] + "não é um tipo de ficheiro suportado.");
				}
			}
		}

		// envia numero de ficheiros a receber pelo servidor
		out.writeObject(files.size());

		for (int i = 0; i < files.size(); i++) {

			out.writeObject(files.get(i).getName());

			int status = (int) in.readObject();
			if (status == CT_OK) {
				// le o ficheiro a enviar
				byte[] myBytes = new byte[(int) files.get(i).length()];
				BufferedInputStream bis = new BufferedInputStream(
						new FileInputStream(files.get(i)));
				bis.read(myBytes, 0, myBytes.length);
				bis.close();

				System.out.println("Enviando ficheiro: "
						+ files.get(i).getPath());
				out.writeObject(myBytes);
			} else if (status == CT_NOK){
				System.out.println("Ficheiro " + files.get(i).getName()
						+ " já existe no servidor!");
			} else if (status == CT_NOTSUPPORTED){
				System.out.println("Ficheiro " + files.get(i).getName()
						+ " não suportado pelo servidor!");
			}
		}
		
		return (((int) in.readObject()) == files.size());
	}

	/**
	 * Funcao que permite ao utilizador seguir outros utilizadores
	 * 
	 * @param args
	 *            - argumentos funcao main
	 * @param out
	 *            - buffer de escrita
	 * @param in
	 *            - buffer de leitura
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static void followUserIds(String[] args, ObjectOutputStream out,
			ObjectInputStream in) throws IOException, ClassNotFoundException {

		// envia codigo da operacao para o servidor
		out.writeObject(OC_followUsers);

		ArrayList<String> users = new ArrayList<String>();

		for (int i = 5; i < args.length; i++)
			users.add(args[i]);

		int status = (int) in.readObject();
		if(status == CT_WRONGSIG)
			System.out.println("Erro de consistencia no servidor! "
					+ "Ficheiro de seguidores vai ser reiniciado...");
		
		// envia numero de ficheiros a receber pelo servidor
		out.writeObject(users.size());

		for (int i = 0; i < users.size(); i++) {

			out.writeObject(users.get(i));

			status = (int) in.readObject();
			if (status == CT_OK) {
				System.out.println("Utilizador " + users.get(i)
						+ " adicionado à lista de seguidores.");
			} else if (status == CT_NOK){
				System.out.println("Utilizador " + users.get(i)
						+ " já está na lista de seguidores!");
			} else if (status == CT_SERVERERR){
				System.out.println("Erro no servidor! Abortando operacao...");
				return;
			} else if (status == CT_NOSUCHUSER){
				System.out.println("Utilizador " + users.get(i) + " nao existe no servidor!");
			}
		}
	}

	/**
	 * Funcao que lista as fotos dos utilizadores que o utilizador se encontra a
	 * seguir
	 * 
	 * @param args
	 *            - argumentos funcao main
	 * @param out
	 *            - buffer de escrita
	 * @param in
	 *            - buffer de leitura
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static void listFollowedPhotos(String[] args, ObjectOutputStream out,
			ObjectInputStream in) throws IOException, ClassNotFoundException {

		// envia codigo da operacao para o servidor
		out.writeObject(OC_listFollowedPhotos);
		out.writeObject(args[5]);
		System.out.println((String) in.readObject());

	}

	/**
	 * Funcao que permite comentar uma foto
	 * 
	 * @param args
	 *            - argumentos funcao main
	 * @param out
	 *            - buffer de escrita
	 * @param in
	 *            - buffer de leitura
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	static void commentPhoto(String[] args, ObjectOutputStream out,
			ObjectInputStream in, String localUsername, Key privateKey) throws Exception {

		// envia codigo da operacao para o servidor
		out.writeObject(OC_commentPhoto);

		// envia user ID a verificar
		out.writeObject(args[6]);
		// envia foto a verificar
		out.writeObject(args[7]);
		if ((int) in.readObject() == CT_OK) {
			// envia comentario
			//verifica qual a chave que tem de fornecer ao servidor
			if(args[6].compareTo(localUsername) == 0)
				out.writeObject(privateKey);
			else{
				out.writeObject(getPrivateKey(args[6]));
			}
			out.writeObject(args[5]);
		} else {
			System.out
					.println("Foto nao existe ou utilizador nao tem permissao!");
		}
	}
	
	static Key getPrivateKey (String username) throws NoSuchAlgorithmException, 
	CertificateException, IOException, KeyStoreException, UnrecoverableKeyException{
		
		FileInputStream fis;
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		fis = new FileInputStream("client/" + username + "/" + username + ".keystore");
		keyStore.load(fis, "123456".toCharArray());
		Key privateKey = keyStore.getKey(username, "123456".toCharArray());
		fis.close();
		
		return privateKey;
	}
}
