package utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

public class Utils {

	public static String sha256Sum(String text) {

		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		byte[] hashedBytes = digest.digest(text
				.getBytes(StandardCharsets.UTF_8));
		String hash = String.format("%064x", new java.math.BigInteger(1,
				hashedBytes));

		return hash;
	}

	public static void randomDelay(int maxDelay) {
		try {
			Random rand = new Random();
			Thread.sleep(rand.nextInt(maxDelay));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void randomDelay(int minDelay, int maxDelay) {
		try {
			Random rand = new Random();
			Thread.sleep(minDelay + rand.nextInt(maxDelay - minDelay));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static int randomPort() {
		Random rand = new Random();
		return (49152 + rand.nextInt(65535 - 49152));
	}

	public static int knuthMorrisPratt(byte[] msg, byte[] pattern) {
		// computePrefixFunction
		int kPre = -1;
		int ps = pattern.length;

		int[] pi = new int[pattern.length];
		Arrays.fill(pi, -1);

		for (int q = 1; q < ps; q++) {
			while (kPre > -1 && pattern[kPre + 1] != pattern[q])
				kPre = pi[kPre];
			if (pattern[kPre + 1] == pattern[q])
				kPre++;
			pi[q] = kPre;
		}

		// find the index
		int ts = msg.length;
		int k = -1;
		for (int q = 0; q < ts; q++) {
			while (k > -1 && pattern[k + 1] != msg[q])
				k = pi[k];
			if (msg[q] == pattern[k + 1])
				k++;
			if (k == (pattern.length - 1))
				return (q - pattern.length + 1);
		}
		return -1;
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
	public static byte[] append(byte[] prefix, byte[] suffix) {
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
