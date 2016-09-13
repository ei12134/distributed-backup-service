package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import core.Dbs;

public class Encryption {

	public static final String CRYPTO_RSA_ALGORITHM = "RSA";
	public static final String CRYPTO_AES_ALGORITHM = "AES";
	public static final int CRYPTO_KEY_SIZE = 4096;
	public static final int CRYPTO_MIN_CYPHER_BYTE_LENGTH = (CRYPTO_KEY_SIZE / 8) - 11;
	public static final int CRYPTO_MAX_CYPHER_BYTE_LENGTH = (CRYPTO_KEY_SIZE / 8);
	public static final String CRYPTO_FOLDER = "crypto";
	public static final String CRYPTO_SHARED_KEY_FILE = "/shared.key";
	public static final String CRYPTO_PEER_PRIVATE_KEY_FILE = "peer_private.key";
	public static final String CRYPTO_PEER_PUBLIC_KEY_FILE = "peer_public.key";

	private final PublicKey peerPublicKey;
	private final PrivateKey peerPrivateKey;
	private final SecretKey sharedKey;
	private Cipher cipher;

	public Encryption() throws Exception {
		cipher = Cipher.getInstance(CRYPTO_RSA_ALGORITHM);

		if (!hasSharedKey()) {
			if (!generateSharedKey())
				throw new Exception("Failed to generate the LAN's shared key");
			// throw new Exception("The LAN's shared key is missing");
		}

		if (!hasPeerKeys()) {
			generatePeerKey();
		}

		sharedKey = readSharedKey(CRYPTO_SHARED_KEY_FILE);
		peerPublicKey = readPublicKey(Dbs.peer.getFolder() + File.separator
				+ CRYPTO_FOLDER + File.separator + CRYPTO_PEER_PUBLIC_KEY_FILE);
		peerPrivateKey = readPrivateKey(Dbs.peer.getFolder() + File.separator
				+ CRYPTO_FOLDER + File.separator + CRYPTO_PEER_PRIVATE_KEY_FILE);
	}

	public SecretKey getSharedKey() {
		return sharedKey;
	}

	public String getSharedKeyString() {
		return Base64.getEncoder().encodeToString(sharedKey.getEncoded());
	}

	public PrivateKey getPeerPrivateKey() {
		return peerPrivateKey;
	}

	public PublicKey getPeerPublicKey() {
		return peerPublicKey;
	}

	public boolean generateSharedKey() {
		URL url = this.getClass().getClassLoader().getResource("");
		File sharedKeyFile = null;

		sharedKeyFile = new File(url.getPath() + CRYPTO_SHARED_KEY_FILE);

		try {
			final SecretKey secretKey = KeyGenerator.getInstance(
					CRYPTO_AES_ALGORITHM).generateKey();

			// Create files to store public and private key
			if (sharedKeyFile.getParentFile() != null) {
				sharedKeyFile.getParentFile().mkdirs();
			}
			sharedKeyFile.createNewFile();

			// Saving the Public key in a file
			ObjectOutputStream sharedKeyOS = new ObjectOutputStream(
					new FileOutputStream(sharedKeyFile));

			sharedKeyOS.writeObject(secretKey);

			sharedKeyOS.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Generate key which contains a pair of private and public key using 4096
	 * bytes. Store the set of keys in peer_private.key and peer_public.key
	 * files.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void generatePeerKey() {
		try {
			final KeyPairGenerator keyGen = KeyPairGenerator
					.getInstance(CRYPTO_RSA_ALGORITHM);
			keyGen.initialize(4096);
			final KeyPair key = keyGen.generateKeyPair();

			File privateKeyFile = new File(Dbs.peer.getFolder()
					+ File.separator + CRYPTO_FOLDER + File.separator
					+ CRYPTO_PEER_PRIVATE_KEY_FILE);
			File publicKeyFile = new File(Dbs.peer.getFolder() + File.separator
					+ CRYPTO_FOLDER + File.separator
					+ CRYPTO_PEER_PUBLIC_KEY_FILE);

			// Create files to store public and private key
			if (privateKeyFile.getParentFile() != null) {
				privateKeyFile.getParentFile().mkdirs();
			}
			privateKeyFile.createNewFile();

			if (publicKeyFile.getParentFile() != null) {
				publicKeyFile.getParentFile().mkdirs();
			}
			publicKeyFile.createNewFile();

			// Saving the Public key in a file
			ObjectOutputStream publicKeyOS = new ObjectOutputStream(
					new FileOutputStream(publicKeyFile));
			publicKeyOS.writeObject(key.getPublic());
			publicKeyOS.close();

			// Saving the Private key in a file
			ObjectOutputStream privateKeyOS = new ObjectOutputStream(
					new FileOutputStream(privateKeyFile));
			privateKeyOS.writeObject(key.getPrivate());
			privateKeyOS.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * The method checks if it has the shared LAN key keys
	 * 
	 * @return flag indicating the the key was generated.
	 * @throws URISyntaxException
	 */
	public boolean hasSharedKey() throws URISyntaxException {

		URL url = this.getClass().getResource(CRYPTO_SHARED_KEY_FILE);

		if (url == null) {
			return false;
		}

		File sharedKeyFile = new File(url.toURI());

		if (sharedKeyFile.exists() && sharedKeyFile.isFile()) {
			return true;
		}
		return false;
	}

	/**
	 * The method checks if the pair of public and private key has been
	 * generated.
	 * 
	 * @return flag indicating if the pair of keys were generated.
	 * @throws URISyntaxException
	 */
	public boolean hasPeerKeys() throws URISyntaxException {

		File privateKey = new File(Dbs.peer.getFolder() + File.separator
				+ CRYPTO_FOLDER + File.separator + CRYPTO_PEER_PRIVATE_KEY_FILE);
		File publicKey = new File(Dbs.peer.getFolder() + File.separator
				+ CRYPTO_FOLDER + File.separator + CRYPTO_PEER_PUBLIC_KEY_FILE);

		if (privateKey.exists() && publicKey.exists()) {
			return true;
		}
		return false;
	}

	/**
	 * Encrypt the plain text using public key.
	 * 
	 * @param plainText
	 *            : original plain text
	 * @param key
	 *            :The public key
	 * @return Encrypted text
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws java.lang.Exception
	 */
	public byte[] encrypt(byte[] plainText, PublicKey key) throws Exception {
		cipher = Cipher.getInstance(CRYPTO_RSA_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
		byte[] encrypted = blockCipher(plainText, Cipher.ENCRYPT_MODE);
		return encoder.encode(encrypted);
	}

	/**
	 * Decrypt text using private key.
	 * 
	 * @param encriptedText
	 *            :encrypted text
	 * @param key
	 *            :The private key
	 * @return plain text
	 * @throws java.lang.Exception
	 */
	public byte[] decrypt(byte[] encriptedText, PrivateKey key)
			throws Exception {
		cipher = Cipher.getInstance(CRYPTO_RSA_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, key);
		java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();
		return blockCipher(decoder.decode(encriptedText), Cipher.DECRYPT_MODE);
	}

	/**
	 * Encrypt the plain text using the shared AES key.
	 * 
	 * @param plainText
	 *            : original plain text
	 * @param key
	 *            : The AES shared key
	 * @return Encrypted text
	 * @throws Exception
	 */
	public byte[] encryptAES(byte[] plainText, SecretKey key) throws Exception {
		cipher = Cipher.getInstance(CRYPTO_AES_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
		byte[] encrypted = cipher.doFinal(plainText);
		return encoder.encode(encrypted);
	}

	/**
	 * Decrypt text using the shared AES key.
	 * 
	 * @param encriptedText
	 *            : encrypted text
	 * @param key
	 *            : The AES shared key
	 * @return plain text
	 * @throws java.lang.Exception
	 */
	public byte[] decryptAES(byte[] encriptedText, SecretKey key)
			throws Exception {
		cipher = Cipher.getInstance(CRYPTO_AES_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, key);
		java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();
		return cipher.doFinal(decoder.decode(encriptedText));
	}

	private byte[] blockCipher(byte[] bytes, int mode)
			throws IllegalBlockSizeException, BadPaddingException {
		// string initialize 2 buffers.
		// scrambled will hold intermediate results
		byte[] scrambled = new byte[0];

		// result will hold the total final result
		byte[] result = new byte[0];

		// if we encrypt we use 484 byte long blocks. Decryption requires 512
		// byte long blocks (because of RSA)
		int length = (mode == Cipher.ENCRYPT_MODE) ? CRYPTO_MIN_CYPHER_BYTE_LENGTH
				: CRYPTO_MAX_CYPHER_BYTE_LENGTH;

		// another buffer. this one will hold the bytes that have to be modified
		// in this step
		byte[] buffer = new byte[(bytes.length > length ? length : bytes.length)];

		for (int i = 0; i < bytes.length; i++) {
			// if we filled our buffer array we have our block ready for
			// decryption or encryption
			if ((i > 0) && (i % length == 0)) {
				// execute the operation
				scrambled = cipher.doFinal(buffer);
				// add the result to our total result.
				result = append(result, scrambled);
				// here we calculate the length of the next buffer required
				int newlength = length;

				// if the new length would be longer than remaining bytes in the
				// bytes array we shorten it.
				if (i + length > bytes.length) {
					newlength = bytes.length - i;
				}
				// clean the buffer array
				buffer = new byte[newlength];
			}
			// copy byte into our buffer.
			buffer[i % length] = bytes[i];
		}

		// this step is needed if we had a trailing buffer. should only happen
		// when encrypting.
		// example: we encrypt 512 bytes. 484 bytes per run means we "forgot"
		// the last 10 bytes. they are in the buffer array
		scrambled = cipher.doFinal(buffer);

		// final step before we can return the modified data.
		result = append(result, scrambled);

		return result;
	}

	/**
	 * Read the shared key stored in a key file
	 * 
	 * @param filePath
	 *            :filePath of the key file
	 * @return key :The public key
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws URISyntaxException
	 */
	public SecretKey readSharedKey(String filePath)
			throws FileNotFoundException, IOException, ClassNotFoundException,
			URISyntaxException {
		File f = new File(this.getClass().getResource(CRYPTO_SHARED_KEY_FILE)
				.toURI());
		ObjectInputStream inputStream = new ObjectInputStream(
				new FileInputStream(f.getAbsolutePath()));
		final SecretKey secretKey = (SecretKey) inputStream.readObject();
		inputStream.close();
		return secretKey;
	}

	/**
	 * Read the public key stored in a key file
	 * 
	 * @param filePath
	 *            :filePath of the key file
	 * @return key :The public key
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public PublicKey readPublicKey(String filePath)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream inputStream = new ObjectInputStream(
				new FileInputStream(filePath));
		final PublicKey publicKey = (PublicKey) inputStream.readObject();
		inputStream.close();
		return publicKey;
	}

	/**
	 * Read the private key stored in a key file
	 * 
	 * @param filePath
	 *            :filePath of the key file
	 * @return key :The private key
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public PrivateKey readPrivateKey(String filePath)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream inputStream = new ObjectInputStream(
				new FileInputStream(filePath));
		final PrivateKey privateKey = (PrivateKey) inputStream.readObject();
		inputStream.close();
		return privateKey;
	}

	/**
	 * Compute a keyed-hash message authentication code (HMAC)
	 * 
	 * @param key
	 *            : shared key string
	 * @param data
	 *            : the data input
	 * @return
	 * @throws Exception
	 */
	public String encode(String key, String data) throws Exception {
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"),
				"HmacSHA256");
		sha256_HMAC.init(secret_key);

		return Base64.getEncoder().encodeToString(
				sha256_HMAC.doFinal(data.getBytes("UTF-8")));
	}

	/**
	 * Appends a suffix byte array to a prefix byte array
	 * 
	 * @param prefix
	 *            :prefix byte array
	 * @param suffix
	 *            :suffix byte array
	 * @return toReturn :the appended byte array
	 */
	private byte[] append(byte[] prefix, byte[] suffix) {
		byte[] toReturn = new byte[prefix.length + suffix.length];
		for (int i = 0; i < prefix.length; i++) {
			toReturn[i] = prefix[i];
		}
		for (int i = 0; i < suffix.length; i++) {
			toReturn[i + prefix.length] = suffix[i];
		}
		return toReturn;
	}
}