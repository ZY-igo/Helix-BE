package com.sipc115.helix.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码加密工具类
 * <p>
 * 使用 AES-256-GCM 加密算法，提供安全的密码加密、解密和验证功能
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
public class PasswordEncryptor {

    /**
     * 单例实例
     */
    private static final PasswordEncryptor instance = new PasswordEncryptor();

    /**
     * 获取单例实例
     * 
     * @return 密码加密工具实例
     */
    public static PasswordEncryptor getInstance() {
        return instance;
    }

    /**
     * AES-256 密钥长度
     */
    private static final int KEY_SIZE = 256;

    /**
     * 算法名称
     */
    private static final String ALGORITHM = "AES";
    
    /**
     * 转换模式
     */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    
    /**
     * GCM 初始化向量长度（96 bits）
     */
    private static final int GCM_IV_LENGTH = 12;
    
    /**
     * GCM 标签长度（128 bits）
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * 默认密钥（实际应用中应该从配置中心或环境变量读取）
     */
    private static final String DEFAULT_SECRET_KEY = "Helix2026SecureKeyForPasswordEncryption!";

    /**
     * 加密密钥
     */
    private SecretKey secretKey;

    /**
     * 私有构造函数
     */
    private PasswordEncryptor() {
        this.secretKey = generateKeyFromPassword(DEFAULT_SECRET_KEY);
    }

    /**
     * 加密密码
     * @param plainPassword 明文密码
     * @return Base64 编码的密文 (包含 IV + 加密数据)
     */
    public String encrypt(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return "";
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 加密数据
            byte[] encryptedBytes = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));

            // 将 IV 和加密数据组合在一起
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            // Base64 编码返回
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    /**
     * 解密密码
     * @param encryptedPassword Base64 编码的密文
     * @return 明文密码
     */
    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return "";
        }

        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedPassword);

            // 提取 IV
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            // 提取加密数据
            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 解密数据
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }

    /**
     * 验证密码是否匹配
     * @param plainPassword 明文密码
     * @param encryptedPassword Base64 编码的密文
     * @return 是否匹配
     */
    public boolean validate(String plainPassword, String encryptedPassword) {
        try {
            String decrypted = decrypt(encryptedPassword);
            return plainPassword != null && plainPassword.equals(decrypted);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从密码字符串生成密钥
     */
    private SecretKey generateKeyFromPassword(String password) {
        try {
            // 使用 SHA-256 对密码进行哈希处理，生成固定长度的密钥
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate key from password", e);
        }
    }

    /**
     * 生成新的随机密钥（用于初始化或轮换密钥）
     */
    public SecretKey generateNewKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE, new SecureRandom());
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate new key", e);
        }
    }

    /**
     * 将密钥转换为 Base64 字符串（用于存储或传输）
     */
    public String encodeKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * 从 Base64 字符串恢复密钥
     */
    public SecretKey decodeKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    /**
     * 更新密钥（用于密钥轮换）
     */
    public void rotateKey(String newSecretKey) {
        if (newSecretKey == null || newSecretKey.isEmpty()) {
            throw new IllegalArgumentException("New secret key cannot be empty");
        }
        this.secretKey = generateKeyFromPassword(newSecretKey);
    }
}
