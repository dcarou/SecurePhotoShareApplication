package Projeto2;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class MAC {

	public static String generateMAC(String toHash, String password)
			throws InvalidKeyException, NoSuchAlgorithmException {
		byte[] bytesToHash = toHash.getBytes();
		byte[] pass = password.getBytes();
		SecretKey key = new SecretKeySpec(pass, "HmacSHA256");

		Mac m;
		m = Mac.getInstance("HmacSHA256");
		m.init(key);
		m.update(bytesToHash);

		return DatatypeConverter.printHexBinary(m.doFinal());
	}

	/**
	 * Funcao para calcular o hash de uma string, devolve o hash da mesma em
	 * hexadecimal.
	 * 
	 * @param string
	 *            string a qual calcular o hash
	 * @return string com o hash da primeira string, com o seu valor em
	 *         hexadecimal
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */

	public static String generateMACnoPass(String string) throws NoSuchAlgorithmException,
			InvalidKeyException {

		byte[] toHash = string.getBytes();
		SecretKey key = new SecretKeySpec(toHash, "HmacSHA256");

		Mac m;
		m = Mac.getInstance("HmacSHA256");
		m.init(key);
		m.update(toHash);

		return DatatypeConverter.printHexBinary(m.doFinal());

	}

	/**
	 * Verifica se o hash de uma string corresponde ao hash fornecido
	 * 
	 * @param string
	 *            - a string a qual se ira verificar o hash
	 * @param hash
	 *            - uma string com uma hash em hexadecimal
	 * @return o valor de verdade da comparacao
	 */

	public static boolean checkMAC(String string, String hash) {
		try {
			if (generateMACnoPass(string).compareTo(hash) == 0)
				return true;
		} catch (InvalidKeyException e) {
			System.err.println("Ocorreu um erro!" + e);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Ocorreu um erro!" + e);
		}
		return false;
	}

	public static boolean checkIfMAC(String password) {
		if (password.length() < 64)
			return false;
		return true;
	}
}
