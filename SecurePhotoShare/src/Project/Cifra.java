package Projeto2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Cifra {

	public static void cifrarFicheiro(String name, File myFile, byte[] bytes)
			throws Exception {

		// gerar uma chave aleatoria para utilizar com o AES
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(128);
		SecretKey key = kg.generateKey();

		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.ENCRYPT_MODE, key);

		FileOutputStream fos;
		CipherOutputStream cos;

		fos = new FileOutputStream(myFile.getPath());
		cos = new CipherOutputStream(fos, c);
		
		// encripta o ficheiro usando AES
		cos.write(bytes);
		cos.close();

		// encripta a chave usando uma chave publica
		FileInputStream kfile = new FileInputStream("server/keystore/PhotoShareServer.truststore"); // keystore
		KeyStore kstore = KeyStore.getInstance("JKS");
		kstore.load(kfile, "123456".toCharArray()); // password
		Certificate cert = kstore.getCertificate(name); // alias do utilizador

		Cipher c1 = Cipher.getInstance("RSA");
		c1.init(Cipher.WRAP_MODE, cert);
		byte[] chaveAESCifrada = c1.wrap(key);

	
		FileOutputStream kos = new FileOutputStream(myFile.getPath() + ".key");
		ObjectOutputStream oos = new ObjectOutputStream(kos);
		oos.write(chaveAESCifrada);
		oos.close();

	}

	public static byte[] decifrarFicheiro(String name, File myFile, Key privateKey)
			throws Exception {
		byte[] chaveAES = new byte[256];
		FileInputStream inKey = new FileInputStream(myFile.getPath() + ".key");
		ObjectInputStream ioKey = new ObjectInputStream(inKey);
		ioKey.read(chaveAES);

		FileInputStream fis;
		fis = new FileInputStream(myFile.getPath());

		/*
		FileInputStream kfile = new FileInputStream("server/keystore/PhotoShareServer.keystore"); // keystore
		KeyStore kstore = KeyStore.getInstance("JKS");
		kstore.load(kfile, "123456".toCharArray()); // password
		Key privateKey = kstore.getKey(name, "123456".toCharArray()); // alias do utilizador
		*/
		Cipher c1 = Cipher.getInstance("RSA");
		c1.init(Cipher.UNWRAP_MODE, privateKey);
		Key chaveAESDecifrada = c1.unwrap(chaveAES, "AES", Cipher.SECRET_KEY);

		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, chaveAESDecifrada);
		
		// desencripta o ficheiro
		CipherInputStream cis = new CipherInputStream(fis, c);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		byte[] d = new byte[16];
		int j = cis.read(d);
		
		while (j != -1) {
			bytes.write(d, 0, j);
			j = cis.read(d);
		}
		
		cis.close();
		ioKey.close();
		
		return bytes.toByteArray();
	}
	
	public static void cifrarComAssinatura (File myFile, byte[] bytes) throws Exception{
		
		// busca as chaves publicas e privadas do servidor
		FileInputStream kfile = new FileInputStream("server/keystore/PhotoShareServer.keystore"); // keystore
		KeyStore kstore = KeyStore.getInstance("JKS");
		kstore.load(kfile, "123456".toCharArray()); // password
		kfile.close();
		
		// chave publica
		Certificate cert = kstore.getCertificate("PhotoShareServer"); 
		
		// chave privada
		KeyStore.PrivateKeyEntry pkEntry;
		pkEntry = (KeyStore.PrivateKeyEntry) kstore.getEntry("PhotoShareServer", new KeyStore.PasswordProtection("123456".toCharArray()));
		PrivateKey pKey = pkEntry.getPrivateKey();
		
		// gera uma assinatura destes bytes
		Signature assinatura = Signature.getInstance("MD5withRSA");
		assinatura.initSign(pKey);
		assinatura.update(bytes);
		
		File myFileSig = new File(myFile.getPath() + ".sig");
		if (myFileSig.exists())
			myFileSig.delete();
		
		FileOutputStream out = new FileOutputStream(myFileSig.getPath());
		out.write(assinatura.sign());
		out.close();
		
		// gerar uma chave aleatoria para utilizar com o AES
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(128);
		SecretKey key = kg.generateKey();

		Cipher cifra = Cipher.getInstance("AES");
		cifra.init(Cipher.ENCRYPT_MODE, key);

		// encripta o ficheiro usando AES e guarda-o no sistema de ficheiros
		if (myFile.exists())
			myFile.delete();
		
		out = new FileOutputStream(myFile.getPath());
		CipherOutputStream cos = new CipherOutputStream(out, cifra);
		
		cos.write(bytes);
		cos.close();
		out.close();

		// encripta a chave e guarda-a no sistema de ficheiros
		Cipher cifra1 = Cipher.getInstance("RSA");
		cifra1.init(Cipher.WRAP_MODE, cert);
		byte[] chaveAESCifrada = cifra1.wrap(key);

		File myFileKey = new File(myFile.getPath() + ".key");
		if (myFileKey.exists())
			myFileKey.delete();
		
		out = new FileOutputStream(myFileKey.getPath());
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.write(chaveAESCifrada);
		oos.close();
		out.close();
	}
	
	
	public static byte[] decifrarComAssinatura (File myFile) throws Exception{
		
		byte[] chaveAES = new byte[256];
		FileInputStream inKey = new FileInputStream(myFile.getPath() + ".key");
		ObjectInputStream ioKey = new ObjectInputStream(inKey);
		ioKey.read(chaveAES);

		FileInputStream fis;
		fis = new FileInputStream(myFile.getPath());

		FileInputStream kfile = new FileInputStream("server/keystore/PhotoShareServer.keystore"); // keystore
		KeyStore kstore = KeyStore.getInstance("JKS");
		kstore.load(kfile, "123456".toCharArray()); // password
		Key privateKey = kstore.getKey("PhotoShareServer", "123456".toCharArray()); // alias do utilizador
		Certificate certificate = kstore.getCertificate("PhotoShareServer");
		PublicKey pubKey = certificate.getPublicKey();
		
		Cipher c1 = Cipher.getInstance("RSA");
		c1.init(Cipher.UNWRAP_MODE, privateKey);
		Key chaveAESDecifrada = c1.unwrap(chaveAES, "AES", Cipher.SECRET_KEY);

		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, chaveAESDecifrada);
		
		// desencripta o ficheiro
		CipherInputStream cis = new CipherInputStream(fis, c);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		byte[] d = new byte[16];
		int j = cis.read(d);
		
		while (j != -1) {
			bytes.write(d, 0, j);
			j = cis.read(d);
		}
		fis.close();
		cis.close();
		ioKey.close();
		
		// le e desencripta a assinatura
		FileInputStream sis = new FileInputStream(myFile.getPath() + ".sig");
		
		byte[] sigBytes = new byte[256];
		sis.read(sigBytes);

		sis.close();
		
		Signature sig = Signature.getInstance("MD5withRSA");
		
		//verifica a assinatura dos bytes desencriptados
		sig.initVerify(pubKey);
		sig.update(bytes.toByteArray());
		
		if (sig.verify(sigBytes))
			return bytes.toByteArray();
		else{
			System.out.println("Assinatura e ficheiro nao coincidem!");
			return null;
		}
	}
}