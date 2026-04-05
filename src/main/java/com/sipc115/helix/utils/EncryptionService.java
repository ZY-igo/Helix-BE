/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密服务类
 * <p>
 * 提供 AES-GCM 对称加密功能，用于保护敏感信息如 API Key、密码等。
 * 使用 AES-256-GCM 模式，具有加密和认证双重功能。
 *
 * <h3>加密算法说明：</h3>
 * <ul>
 *   <li><b>算法</b>：AES/GCM/NoPadding（AES-256-GCM）</li>
 *   <li><b>IV 长度</b>：12 字节（96 位）</li>
 *   <li><b>Tag 长度</b>：128 位</li>
 *   <li><b>密钥长度</b>：32 字节（256 位）</li>
 * </ul>
 *
 * <h3>GCM 模式特性：</h3>
 * <ul>
 *   <li>每次加密使用随机 IV，即使相同明文也产生不同密文（防止密文分析）</li>
 *   <li>提供认证加密，能检测密文篡改（AEAD）</li>
 *   <li>加密结果为 Base64 编码字符串，便于存储和传输</li>
 * </ul>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>存储外部服务连接凭据（飞书 appSecret、LLM apiKey、数据库密码）</li>
 *   <li>保护用户敏感配置信息</li>
 * </ul>
 *
 * @author Helix Team
 * @see #encrypt(String) 加密方法
 * @see #decrypt(String) 解密方法
 */
@Slf4j
@Service
public class EncryptionService {

    /**
     * AES-GCM 加密算法
     * <p>
     * AES-256 with Galois/Counter Mode, NoPadding
     */
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /**
     * GCM 初始化向量（IV）长度
     * <p>
     * 96 位（12 字节）是 NIST 推荐的标准 IV 长度，
     * 兼顾安全性和效率。
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * GCM 认证标签长度
     * <p>
     * 128 位认证标签，提供强大的篡改检测能力。
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * 主密钥
     * <p>
     * 用于 AES 加密的对称密钥。
     * 默认值仅用于开发环境，生产环境必须配置真实密钥。
     * 配置项：app.encryption.master-key
     */
    @Value("${app.encryption.master-key:default-master-key-for-dev-32bytes}")
    private String masterKey;

    /**
     * AES 密钥规格
     * <p>
     * 封装 AES 密钥的内部表示，
     * 由 masterKey 派生而来。
     */
    private SecretKeySpec secretKey;

    /**
     * 安全随机数生成器
     * <p>
     * 用于生成加密时所需的随机 IV。
     * 使用 SecureRandom 而非 Random，确保随机数的不可预测性。
     */
    private SecureRandom secureRandom;

    /**
     * 初始化加密服务
     * <p>
     * 在 Spring Bean 创建后执行初始化：
     * <ol>
     *   <li>将 masterKey 填充或截断为 32 字节（满足 AES-256 要求）</li>
     *   <li>创建 SecretKeySpec 供加密/解密使用</li>
     *   <li>初始化安全随机数生成器</li>
     * </ol>
     *
     * @see jakarta.annotation.PostConstruct
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = padKey(masterKey).getBytes(StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
        log.info("EncryptionService 初始化完成");
    }

    /**
     * 加密明文
     * <p>
     * 使用 AES-256-GCM 算法加密输入字符串。
     * <p>
     * 加密过程：
     * <ol>
     *   <li>生成 12 字节随机 IV</li>
     *   <li>使用 AES 加密明文，生成密文和认证标签</li>
 *   <li>将 IV + 密文拼接后进行 Base64 编码</li>
     * </ol>
     *
     * <h3>返回值格式：</h3>
     * <pre>
     * Base64(IV[12字节] || 密文 || GCM认证标签[16字节])
     * </pre>
     *
     * @param plainText 要加密的明文（可为 null 或空字符串）
     * @return Base64 编码的加密结果，null 输入返回 null
     * @throws RuntimeException 加密失败时抛出
     */
    public String encrypt(String plainText) {
        // null 或空字符串直接返回 null，不进行加密
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        try {
            // 步骤1：生成随机 IV（初始化向量）
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 步骤2：初始化 GCM 模式的 Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 步骤3：执行加密（doFinal 会自动添加 GCM 认证标签）
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 步骤4：拼接 IV 和密文
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            // 步骤5：Base64 编码返回
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密密文
     * <p>
     * 使用 AES-256-GCM 算法解密输入字符串。
     * <p>
     * 解密过程：
     * <ol>
     *   <li>Base64 解码输入</li>
     *   <li>提取前 12 字节作为 IV</li>
     *   <li>剩余部分为密文（含 GCM 认证标签）</li>
     *   <li>使用 IV 初始化 Cipher 并解密</li>
     *   <li>GCM 认证标签验证失败会抛出异常（防止篡改）</li>
     * </ol>
     *
     * @param encryptedText Base64 编码的加密字符串（可为 null 或空字符串）
     * @return 解密后的明文，null 输入返回 null
     * @throws RuntimeException 解密失败时抛出（可能原因：密钥错误、密文篡改、IV 错误）
     */
    public String decrypt(String encryptedText) {
        // null 或空字符串直接返回 null
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        try {
            // 步骤1：Base64 解码
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            // 步骤2：分离 IV 和密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // 步骤3：初始化 GCM 模式的 Cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 步骤4：执行解密（GCM 认证标签验证自动进行）
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 填充或截断密钥为 32 字节
     * <p>
     * AES-256 要求密钥长度为 32 字节。
     * 如果密钥长度小于 32 字节，则用 '0' 填充；
     * 如果密钥长度大于 32 字节，则截断前 32 字节。
     *
     * <h3>处理规则：</h3>
     * <ul>
     *   <li>密钥长度 >= 32：截断为前 32 字节</li>
     *   <li>密钥长度 < 32：用 '0' 填充到 32 字节</li>
     * </ul>
     *
     * <h3>安全建议：</h3>
     * <p>
     * 生产环境应配置 32 字节的真实随机密钥，
     * 而非依赖默认填充值。
     *
     * @param key 原始密钥字符串
     * @return 32 字节长度的处理后密钥
     */
    private String padKey(String key) {
        if (key.length() >= 32) {
            // 密钥长度足够，截断为前 32 字节
            return key.substring(0, 32);
        }
        // 密钥长度不足，用 '0' 填充到 32 字节
        StringBuilder sb = new StringBuilder(key);
        while (sb.length() < 32) {
            sb.append("0");
        }
        return sb.toString();
    }
}