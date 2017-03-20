package Projeto2;

/***************************************************************************
 *   Seguranca e Confiabilidade 2014/15
 *   @author fc40347, fc41907, fc43612
 *
 ***************************************************************************/

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.Semaphore;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Servidor para armazenar e gerir fotos e os seus comentarios
 * 
 * @author fc40347, fc41907, fc43612
 *
 */

public class PhotoShareServer {

	private static KeyStore keyStore;
	private static KeyStore trustStore;

	// codigos de operacoes que o servidor reconhece
	private final int OC_sendPhotos = 1;
	private final int OC_listFollowedPhotos = 2;
	private final int OC_copyFollowedPhotos = 3;
	private final int OC_commentPhoto = 4;
	private final int OC_followUsers = 5;
	private final int OC_copyLatestPhotos = 6;

	// outras variaveis relevantes
	private final int timeout = 3000;
	private ServerOperations so;
	private final int maxReads = 1024;

	// estados enviados pelos protocolos de comunicacao
	protected int CT_OK = 0;
	protected int CT_NOK = 1;

	public static void main(String[] args) {

		System.out.println("servidor online: main");

		if (args.length == 2 && args[0] != null && args[1] != null
				&& args[0].matches("-?\\d+(\\.\\d+)?")) {
			PhotoShareServer server = new PhotoShareServer();
			server.startServer(Integer.parseInt(args[0]), args[1]);
		} else
			System.out.println("Porto para o socket nao inserido!");
	}

	public void startServer(int socketPort, String password) {

		ServerSocket sSoc = null;
		Semaphore semaphore = new Semaphore(maxReads);

		// inicializa o servidor
		try {

			// inicializa keystore e truststore
			FileInputStream fis;
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

			fis = new FileInputStream("server/keystore/PhotoShareServer.keystore");
			keyStore.load(fis, "123456".toCharArray());
			fis.close();

			trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

			fis = new FileInputStream("server/keystore/PhotoShareServer.truststore");
			trustStore.load(fis, "123456".toCharArray());
			fis.close();

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SUNX509");
			kmf.init(keyStore, "123456".toCharArray());

			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance("SUNX509");
			tmf.init(trustStore);

			SSLContext sslc = SSLContext.getInstance("SSL");
			sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
					new java.security.SecureRandom());

			ServerSocketFactory sSocF = sslc.getServerSocketFactory();

			sSoc = sSocF.createServerSocket(socketPort);
		} catch (IOException e) {
			System.err.println("Erro a criar o socket." + e);
			System.exit(-1);
		} catch (KeyStoreException | NoSuchAlgorithmException
				| CertificateException | UnrecoverableKeyException
				| KeyManagementException e) {
			System.err.println("Erro nos certificados : " + e);
		}

		try {
			so = new ServerOperations(password);
		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException e1) {
			System.err.println("Erro a ler o ficheiro de utilizadores." + e1);
			System.exit(-1);
		}

		// fica a espera de ligacoes
		while (true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc,
						semaphore);
				newServerThread.start();
			} catch (IOException e) {
				System.err.println("Ocorreu um erro!" + e);
			}

		}
	}

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;
		private Semaphore semaphore = null;

		ServerThread(Socket inSoc, Semaphore sem) {
			socket = inSoc;
			this.semaphore = sem;

			try {
				socket.setSoTimeout(timeout);
			} catch (SocketException e) {
				System.err.println("Died! Erro a lidar com sockets: " + e);
			}
		}

		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(
						socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(
						socket.getInputStream());
				String auth = null;

				try {
					auth = (String) inStream.readObject();

					// autenticacao
					boolean authenticated = so.authentication(auth);
					outStream.writeObject(authenticated);

					if (authenticated) {
						// recebe o opcode da operacao a realizar
						int opcode = (int) inStream.readObject();

						String user = auth.split(":")[0];
						user.toLowerCase();
						System.out.println("Recebido um pedido do utilizador :"
								+ user);

						System.out.print("Operacao seleccionada: ");

						switch (opcode) {
						case OC_sendPhotos:
							System.out.println("Enviar fotos para o servidor.");

							// tenta adquirir todos as permissoes para conseguir
							// escrever
							semaphore.acquireUninterruptibly(maxReads);
							so.sendPhotos(user, inStream, outStream);
							semaphore.release(maxReads);
							break;
						case OC_followUsers:
							semaphore.acquire();
							System.out.println("Seguir um utilizador.");
							so.followUserIds(user, inStream, outStream);
							semaphore.release();
							break;
						case OC_listFollowedPhotos:
							semaphore.acquire();
							System.out
									.println("Listar fotos dos seus seguidores.");
							so.listFollowedPhotos(user, inStream, outStream);
							semaphore.release();
							break;
						case OC_copyFollowedPhotos:
							semaphore.acquire();
							System.out
									.println("Copiar as fotos dos seus seguidores.");
							so.copyFollowedPhotos(user, inStream, outStream);
							semaphore.release();
							break;
						case OC_commentPhoto:
							semaphore.acquire();
							System.out.println("Comentar uma foto.");
							so.commentPhoto(user, inStream, outStream);
							semaphore.release();
							break;
						case OC_copyLatestPhotos:
							semaphore.acquire();
							System.out
									.println("Copiar as fotos mais recentes dos seus seguidores.");
							so.copyLatestPhotos(user, inStream, outStream);
							semaphore.release();
							break;
						default:
							System.out.println("OPERACAO NAO RECONHECIDA");
							break;
						}
					} else {
						System.out
								.println("Utilizador nao existe/password errada.");
					}
				} catch (ClassNotFoundException | ClassCastException e1) {
					e1.printStackTrace();
					System.err.println("Died! " + e1);
				} catch (SocketTimeoutException e3) {
					e3.printStackTrace();
					e3.printStackTrace();
					System.err.println("Died! Timeout numa comunicacao: " + e3);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.err.println("Ocorreu um erro!" + e);
				} catch (InvalidKeyException e) {
					e.printStackTrace();
					System.err.println("Ocorreu um erro!" + e);
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
					System.err.println("Ocorreu um erro!" + e);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Died! " + e);
				}
				outStream.close();
				inStream.close();
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Died! Erro de I/O: " + e);
			}
		}
	}

}