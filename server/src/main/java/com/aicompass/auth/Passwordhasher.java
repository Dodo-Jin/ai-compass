package com.aicompass.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int ITERATIONS = 65536;
  private static final int KEY_LENGTH = 256;
  private static final int SALT_BYTES = 16;

  private final SecureRandom secureRandom = new SecureRandom();

  public String hash(String rawPassword) {
    byte[] salt = new byte[SALT_BYTES];
    secureRandom.nextBytes(salt);
    byte[] hashed = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
    return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hashed);
  }

  public boolean matches(String rawPassword, String storedPassword) {
    if (rawPassword == null || storedPassword == null || !storedPassword.contains(":")) {
      return false;
    }
    String[] parts = storedPassword.split(":", 2);
    try {
      byte[] salt = Base64.getDecoder().decode(parts[0]);
      byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
      byte[] actualHash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
      return MessageDigest.isEqual(expectedHash, actualHash);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
      SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
      return skf.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("비밀번호 해싱 중 오류가 발생했습니다.", e);
    }
  }
}
