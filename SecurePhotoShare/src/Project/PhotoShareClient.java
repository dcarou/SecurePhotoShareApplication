package Projeto2;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Classe cliente
 * 
 * @author fc40346,fc41907,fc43612
 *
 */
public class PhotoShareClient {

	private static String localUserName;

	private static KeyStore keyStore;
	private static KeyStore trustStore;

	public static void main(String[] args) {

		Socket socket = null;
		ObjectInputStream in;
		ObjectOutputStream out;
		String auth;
		String[] address;
		boolean result;
		Key privateKey;

		// depura os argumentos
		if (args.length < 5 || args[1] == null || args[3] == null)
			System.exit(0);

		auth = args[1];
		localUserName = args[1].split(":")[0];
		address = args[3].split(":");

		if (address.length != 2)
			System.exit(0);

		try {

			File folder = new File("client/");
			folder.mkdir();

			// inicializa keystore e truststore
			FileInputStream fis;
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			fis = new FileInputStream("client/" + localUserName + "/" + localUserName + ".keystore");
			keyStore.load(fis, "123456".toCharArray());
			privateKey = keyStore.getKey(localUserName, "123456".toCharArray());
			fis.close();

			trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			fis = new FileInputStream("client/keystore/PhotoShareClient.truststore");
			trustStore.load(fis, "123456".toCharArray());
			fis.close();

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SUNX509");
			kmf.init(keyStore, "123456".toCharArray());

			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance("SUNX509");
			tmf.init(trustStore);

			// configura o socket para se ligar usando SSL
			SSLContext sslc = SSLContext.getInstance("SSL");
			sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
					new java.security.SecureRandom());

			SocketFactory sf = sslc.getSocketFactory();

			// inicia ligacao ao socket do servidor
			socket = sf.createSocket(address[0], Integer.parseInt(address[1]));

			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());

			// autenticacao com o servidor
			out.writeObject(auth);
			result = ((boolean) in.readObject());
			System.out.println("Autenticado? " + result);

			// caso esteja autenticado
			if (result) {
				char comando = args[4].charAt(1);

				switch (comando) {
				// enviar fotos
				case 'p':
					ClientOperations.sendPhotos(args, out, in, localUserName);
					break;
				// seguir utilizadores
				case 'f':
					ClientOperations.followUserIds(args, out, in);
					break;
				// listar fotos seguidores
				case 'l':
					ClientOperations.listFollowedPhotos(args, out, in);
					break;
				// comentar fotos
				case 'c':
					ClientOperations.commentPhoto(args, out, in, localUserName, privateKey);
					break;
				// descarregar fotos mais recentes dos seguidores
				case 'n':
					ClientOperations.copyLatestPhotos(out, in, localUserName, privateKey);
					break;
				// descarregar fotos seguidores
				case 'g':
					ClientOperations.copyFollowedPhotos(args, out, in,
							localUserName, privateKey);
					break;
				default:
					break;
				}
			}

			in.close();
			out.close();
			socket.close();

		} catch (UnknownHostException e) {
			System.err.println("Ocorreu um erro! Host Desconhecido." + e);
			// e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Ocorreu um erro!" + e);
			// e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Ocorreu um erro!" + e);
			// e.printStackTrace();
		} catch (KeyStoreException e) {
			System.err.println("Ocorreu um erro!" + e);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Ocorreu um erro!" + e);
		} catch (CertificateException e) {
			System.err.println("Ocorreu um erro!" + e);
		} catch (KeyManagementException e) {
			System.err.println("Ocorreu um erro!" + e);
		} catch (UnrecoverableKeyException e) {
			System.err.println("Ocorreu um erro!" + e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
