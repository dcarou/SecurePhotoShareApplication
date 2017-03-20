package Projeto2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Classes que definem o funcionamento do servidor
 * 
 * @author fc40347, fc41907, fc43612
 *
 */

public class ServerOperations {

	protected int CT_OK = 0;
	protected int CT_NOK = 1;
	protected int CT_WRONGSIG = -1;
	protected int CT_SERVERERR = -2;
	protected int CT_NOSUCHUSER = -3;
	protected int CT_NOTSUPPORTED = -4;

	ArrayList<String> users = new ArrayList<String>();
	ArrayList<String> passwords = new ArrayList<String>();

	/**
	 * Construtor para inicializar as variaveis do servidor
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */

	public ServerOperations(String password) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException {

		String linha;
		BufferedReader in;
		BufferedWriter out;

		in = new BufferedReader(new FileReader("users.txt"));
		StringBuilder sb = new StringBuilder();
		StringBuilder sbMac = new StringBuilder();
		linha = in.readLine();

		// le ficheiro de utilizadores
		while (linha != null) {
			sbMac.append(linha + "\n");
			String[] temp = linha.split(":");
			users.add(temp[0]);
			sb.append(temp[0] + ":");
			// verifica se passwords estao hashed
			if (MAC.checkIfMAC(temp[1])) {
				passwords.add(temp[1]);
				sb.append(temp[1] + "\n");
			} else {
				passwords.add(MAC.generateMACnoPass(temp[1]));
				sb.append(MAC.generateMACnoPass(temp[1]) + "\n");
			}
			linha = in.readLine();

		}

		// verifica se existe ficheiro de password de protecao MAC
		// caso nao exista pede input ao utilizador para criar um novo ou sair
		File file = new File("cifra.tmp");
		if (!file.exists()) {
			Scanner s = new Scanner(System.in);
			String a = "ab";
			while (a.compareTo("s") != 0 && a.compareTo("c") != 0) {
				System.out
						.println("Ficheiro de utilizador nao protegido por MAC.");
				System.out
						.println("Pressione c para calcular um MAC ou pressione s para sair e carregue ENTER.");
				a = s.nextLine();
			}
			s.close();
			if (a.compareTo("s") == 0)
				System.exit(0);
			System.out.println("BEGIN" + sbMac.toString() + "END");
			System.out.println(MAC.generateMAC(sb.toString(), password));
			Files.write(file.toPath(), MAC.generateMAC(sb.toString(), password)
					.getBytes());
			in.close();
		} else {
			in = new BufferedReader(new FileReader("cifra.tmp"));

			if (MAC.generateMAC(sbMac.toString(), password).compareTo(
					in.readLine()) == 0) {
				Files.write(file.toPath(),
						MAC.generateMAC(sb.toString(), password).getBytes());
			} else {
				System.err.println("Password incorrecta!");
				in.close();
				System.exit(-1);
			}
			in.close();
		}

		out = new BufferedWriter(new FileWriter("users.txt"));
		out.write(sb.toString());
		out.close();

		// caso a pasta /server nao tenha sido criada
		File server = new File("server/");
		server.mkdir();

		System.out.println("Utilizadores existentes no servidor: "
				+ users.size() + " " + users.toString());

	}

	/**
	 * Funcao que copia as ultimas fotos armazenadas por um utilizador, caso o
	 * utilizador follower esteja na sua lista de seguidores
	 * 
	 * @param requester
	 *            - o utilizador a fazer o pedido
	 * @param inStream
	 *            - stream de input
	 * @param outStream
	 *            - stream de output
	 * @throws Exception
	 */

	void copyLatestPhotos(String requester, ObjectInputStream inStream,
			ObjectOutputStream outStream) throws Exception {

		ArrayList<String> files = new ArrayList<String>();
		ArrayList<String> names = new ArrayList<String>();
		Key privateKey;
		
		for (String user : users) {
			// verifica se o utilizador que fez o pedido está a seguir este
			// utilizador
			if ((user.compareTo(requester) != 0)
					&& isFollowing(requester, user)) {
				String file = getLatestPhoto("server/" + user + "/");
				if (file != null) {
					files.add(file);
					names.add(user);
					// verifica se tem comentarios associados
					File comment = new File("server/" + user + "/" + file
							+ ".txt");
					if (comment.exists()) {
						files.add(file + ".txt");
						names.add(user);
					}
				}
			}
		}

		outStream.writeObject(files.size());
		for (int i = 0; i < files.size(); i++) {
			outStream.writeObject(files.get(i));
			outStream.writeObject(names.get(i));
			privateKey = (Key) inStream.readObject();

			// envia o ficheiro
			File myFile = new File("server/" + names.get(i) + "/"
					+ files.get(i));
			System.out.println("Enviando " + files.get(i) + "...");
			byte[] myBytes = Cifra.decifrarFicheiro(names.get(i), myFile, privateKey);
			System.out.println("Enviando ficheiro: " + myFile.getPath());
			outStream.writeObject(myBytes);
		}
	}

	/**
	 * Funcao que copia todas as fotos de um utilizador, caso o utilizador
	 * follower esteja a segui-lo.
	 * 
	 * @param follower
	 *            - o utilizador a fazer o pedido
	 * @param inStream
	 *            - stream de input
	 * @param outStream
	 *            - stream de output
	 * @throws Exception
	 */
	public void copyFollowedPhotos(String follower, ObjectInputStream in,
			ObjectOutputStream out) throws Exception {

		String user = (String) in.readObject();
		File[] files = {};
		Key privateKey = (Key) in.readObject();

		// verifica se o utilizador que fez o pedido está a seguir o utilizador
		// pedido
		if ((user.compareTo(follower) != 0) && isFollowing(follower, user))
			files = getUserPhotos("server/" + user + "/");

		out.writeObject(files.length);

		for (int i = 0; i < files.length; i++) {
			out.writeObject(files[i].getName());

			// envia o ficheiro
			byte[] myBytes = Cifra.decifrarFicheiro(user, files[i], privateKey);
			System.out.println("Enviando ficheiro: " + files[i].getPath()
					+ "...");
			out.writeObject(myBytes);
		}

	}

	/**
	 * Funcao que permite a um utilizador follower comentar as fotos de um
	 * utilizador fornecido pelo mesmo, caso esteja na lista de seguidores do
	 * ultimo
	 * 
	 * @param follower
	 *            - o utilizador a fazer o pedido
	 * @param inStream
	 *            - stream de input
	 * @param outStream
	 *            - stream de output
	 * @throws Exception
	 */
	void commentPhoto(String follower, ObjectInputStream in,
			ObjectOutputStream out) throws Exception {

		String user = (String) in.readObject();
		String photo = (String) in.readObject();
		
		if ((user.compareTo(follower) == 0) || isFollowing(follower, user)) {
			if (fileExists("server/" + user + "/" + photo)) {
				out.writeObject(CT_OK);

				// adiciona comentario
				Key privateKey = (Key) in.readObject();
				String comment = (String) in.readObject();
				StringBuilder sb = new StringBuilder();
				File comments = new File("server/" + user + "/", photo + ".txt");

				// verifica se ficheiro existe para ser desencriptado
				if (fileExists("server/" + user + "/" + photo + ".txt")) {
					byte[] bytes = Cifra.decifrarFicheiro(user, comments, privateKey);
					sb.append(new String(bytes, "UTF-8"));
					System.out.println("ANTES:" + sb.toString());
				}

				sb.append(comment + "\t" + follower + "\n");
				System.out.println("DEPOIS:" + sb.toString());
				Cifra.cifrarFicheiro(user, comments, sb.toString().getBytes());

			} else {
				out.writeObject(CT_NOK);
			}
			return;
		}
		out.writeObject(CT_NOK);
	}

	/**
	 * Funcao que faz com que uma lista de utilizadores fornecida pelo
	 * utilizador user passe a pertencer 'a sua lista de seguidores
	 * 
	 * @param user
	 *            - o utilizador a fazer o pedido
	 * @param inStream
	 *            - stream de input
	 * @param outStream
	 *            - stream de output
	 * @throws Exception
	 */

	void followUserIds(String user, ObjectInputStream in, ObjectOutputStream out)
			throws Exception {

		String linha;
		String followerString = null;

		// verifica os utilizadores que ja sao seguidores
		ArrayList<String> followed = new ArrayList<String>();

		// acesso ao ficheiro de seguidores
		File followers = new File("server/" + user + "/followers.txt");

		if (followers.exists()) {
			byte[] followerBytes = Cifra.decifrarComAssinatura(followers);
			if (followerBytes != null) {
				followerString = new String(followerBytes, "UTF-8");
				out.writeObject(CT_OK);
			} else {
				// detectou uma assinatura inconsistente!
				// avisa o utilizador que o ficheiro de seguidores
				// vai ser reiniciado
				System.out
						.println("Assinaturas nao coincidem! A reiniciar ficheiro de seguidores...");
				out.writeObject(CT_WRONGSIG);
			}
		} else {
			out.writeObject(CT_OK);
			System.out.println("Ficheiro de seguidores nao existe!");
			// cria directoria do utilizador just in case
			File dir = new File("server/" + user);
			dir.mkdir();
		}

		int size = (int) in.readObject();

		// prepara a nova string do ficheiro de seguidores
		StringBuilder sb = new StringBuilder();

		if (followerString != null) {
			// le a string do ficheiro de seguidores
			BufferedReader bufIn = new BufferedReader(new StringReader(
					followerString));
			while ((linha = bufIn.readLine()) != null) {
				followed.add(linha);
			}
			bufIn.close();
			sb.append(followerString);
		}

		for (int i = 0; i < size; i++) {
			try {
				String toFollow = (String) in.readObject();
				toFollow = toFollow.toLowerCase();

				if (users.contains(toFollow) && toFollow.compareTo(user) != 0) {
					if (followed.contains(toFollow)) {
						out.writeObject(CT_NOK);
					} else {
						out.writeObject(CT_OK);
						sb.append(toFollow + "\n");
					}
				} else {
					out.writeObject(CT_NOSUCHUSER);
				}

			} catch (ClassNotFoundException e) {
				System.err
						.println("Nao conseguiu adicionar o utilizador na posicao "
								+ i);
				out.writeObject(CT_SERVERERR);
				return;
			}
		}
		System.out.println("ficheiro a guardar: " + sb.toString());

		// grava o novo ficheiro
		Cifra.cifrarComAssinatura(followers, sb.toString().getBytes());

	}

	/**
	 * Funcao que autentica um utilizador
	 * 
	 * @requires uma string do estilo "utilizador:password"
	 * @param login
	 *            - String de autenticacao
	 * @return valor de verdade da autenticacao
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */

	boolean authentication(String login) throws InvalidKeyException,
			NoSuchAlgorithmException {
		String[] temp = login.split(":");
		for (int i = 0; i < users.size(); i++) {
			if (temp[0].compareTo(users.get(i)) == 0
					&& MAC.generateMACnoPass(temp[1]).compareTo(
							passwords.get(i)) == 0)
				return true;
		}
		return false;
	}

	/**
	 * Permite ao utilizador user enviar fotos do seu computador para o servidor
	 * 
	 * @param user
	 *            - o utilizador a fazer o pedido
	 * @param inStream
	 *            - stream de input
	 * @param outStream
	 *            - stream de output
	 * @throws Exception
	 */
	void sendPhotos(String user, ObjectInputStream in, ObjectOutputStream out)
			throws Exception {

		int size = (int) in.readObject();

		for (int i = 0; i < size; i++) {
			try {
				String filename = (String) in.readObject();

				// caso apareça um ficheiro de formato nao suportado
				if (!(filename.endsWith(".jpg") || filename.endsWith(".png")
						|| filename.endsWith(".bmp") || filename
							.endsWith(".gif"))) {
					out.writeObject(CT_NOTSUPPORTED);
					break;
				}

				File myFile = new File("server/" + user + "/" + filename);
				myFile.getParentFile().mkdir();

				// verifica se o ficheiro ja existe. caso seja verdade, envia
				// NOK ao cliente e passa para o proximo ficheiro na lista
				if (myFile.exists()) {
					System.out.println("Ficheiro " + myFile.getPath()
							+ " ja existe!");
					out.writeObject(CT_NOK);
				} else {
					out.writeObject(CT_OK);
					byte[] bytes = (byte[]) in.readObject();
					Cifra.cifrarFicheiro(user, myFile, bytes);
					// permissoes do ficheiro
					myFile.setReadOnly();
				}

			} catch (ClassNotFoundException e) {
				System.err
						.println("Nao conseguiu escrever o ficheiro na posicao "
								+ i);
			}
		}

		out.writeObject(size);

	}

	/**
	 * Fornece ao utilizador follower uma listagem dos ficheiros de um outro
	 * utilizador fornecido por ele, caso o primeiro pertenca a lista de
	 * seguidores do ultimo
	 * 
	 * @param follower
	 *            - o utilizador a fazer o pedido
	 * @param inStream
	 *            - stream de input
	 * @param outStream
	 *            - stream de output
	 * @throws Exception
	 */

	void listFollowedPhotos(String follower, ObjectInputStream in,
			ObjectOutputStream out) throws Exception {

		String user = (String) in.readObject();

		if (user.compareTo(follower) == 0) {
			out.writeObject(getFileList(user, "server/" + user + "/"));
			return;
		}

		if (isFollowing(follower, user)) {
			out.writeObject(getFileList(user, "server/" + user + "/"));
			return;
		}

		out.writeObject("Utilizador não esta na lista de seguidores.");
	}

	/**
	 * Devolve uma String formatada com uma lista de ficheiros do user
	 * 
	 * @param user
	 *            - nome do utilizador cujos ficheiros estao a ser listados
	 * @param dir
	 *            - diretoria do utilizador
	 * @return String formatada com uma lista de ficheiros do user, String vazia
	 *         caso nao existam ficheiros
	 */

	static String getFileList(String user, String dir) {
		StringBuilder sb = new StringBuilder();

		File folder = new File(dir);

		if (folder.isDirectory()) {
			sb.append("Ficheiros de " + user + ":\n");
			for (final File fileEntry : folder.listFiles()) {
				if (!fileEntry.getName().endsWith(".txt")
						&& !fileEntry.getName().endsWith(".key")
						&& !fileEntry.getName().endsWith(".sig"))
					sb.append(fileEntry.getName() + "\t"
							+ (new Date(fileEntry.lastModified())).toString()
							+ "\n");
			}
		}
		if (sb.length() == 0)
			sb.append("Utilizador nao tem ficheiros no servidor!\n");
		return sb.toString();
	}

	/**
	 * Verifica se um ficheiro existe numa data diretoria
	 * 
	 * @param filename
	 *            - nome do ficheiro a verificar
	 * @return - valor de verdade da verificacao
	 */

	static boolean fileExists(String filename) {

		File file = new File(filename);
		return file.exists();

	}

	/**
	 * Devolve as fotos de uma diretoria (+ os respetivos comentarios)
	 * 
	 * @param dir
	 *            - diretoria a verificar
	 * @return um array de Files com informacao de cada foto (+ comentarios)
	 * @requires presume que o utilizador a quem vai ser enviada a info tem
	 *           permissao para a obter
	 */

	static File[] getUserPhotos(String dir) {
		ArrayList<File> files = new ArrayList<File>();
		File folder = new File(dir);
		System.out.println("Getting files from " + folder.getPath());

		if (folder.listFiles() != null) {
			for (final File fileEntry : folder.listFiles()) {
				if (!fileEntry.getName().matches("followers.txt")
						&& !fileEntry.getName().endsWith(".key")
						&& !fileEntry.getName().endsWith(".sig")) {
					files.add(fileEntry);
				}
			}
		}

		File[] toReturn = new File[files.size()];
		for (int i = 0; i < files.size(); i++)
			toReturn[i] = files.get(i);

		return toReturn;
	}

	/**
	 * Devolve a ultima foto da diretoria dir
	 * 
	 * @param dir
	 *            - diretoria a verificar
	 * @return String com o nome da foto
	 * @requires presume que o utilizador a quem vai ser enviada a info tem
	 *           permissao para a obter
	 * */

	static String getLatestPhoto(String dir) {
		File folder = new File(dir);
		long date = 0;
		File latest = null;

		System.out.println("Getting latest file from " + dir);

		if (folder.listFiles() != null) {
			for (final File fileEntry : folder.listFiles()) {
				// verifica se nao tem extensoes do sistema
				if (!fileEntry.getName().endsWith(".txt")
						&& !fileEntry.getName().endsWith(".key")
						&& !fileEntry.getName().endsWith(".sig")) {
					if (fileEntry.lastModified() > date) {
						date = fileEntry.lastModified();
						latest = fileEntry;
					}
				}
			}
		}

		if (latest == null)
			return null;
		else {
			System.out.println("Retrieved " + latest.getName());
			return latest.getName();
		}
	}

	/**
	 * Verifica se o utilizador follower esta a seguir o utilizador user
	 * 
	 * @param follower
	 *            - utilizador
	 * @param user
	 *            - utilizador
	 * @return valor de verdade da verificacao
	 * @throws Exception
	 */

	static boolean isFollowing(String follower, String user) throws Exception {

		// acesso ao ficheiro de seguidores
		File followers = new File("server/" + user + "/followers.txt");

		String followerString;
		String linha;

		if (followers.exists()) {
			byte[] followerBytes = Cifra.decifrarComAssinatura(followers);
			if (followerBytes != null)
				followerString = new String(followerBytes, "UTF-8");
			else
				return false;

		} else {
			System.out.println("Ficheiro de seguidores nao existe!");
			return false;
		}

		// verifica se follower esta mesmo a seguir user
		if (followerString != null) {
			// le a string do ficheiro de seguidores
			BufferedReader bufIn = new BufferedReader(new StringReader(
					followerString));
			while ((linha = bufIn.readLine()) != null) {
				if (linha.compareTo(follower) == 0) {
					bufIn.close();
					return true;
				}
			}
			bufIn.close();
		}
		return false;
	}

}
