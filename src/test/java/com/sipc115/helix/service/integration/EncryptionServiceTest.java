package com.sipc115.helix.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 加密服务单元测试类
 * <p>
 * 测试 EncryptionService 的加密和解密功能是否正确。
 * 加密服务使用 AES-GCM 算法，用于保护敏感信息如 API Key、密码等。
 *
 * <h3>测试覆盖范围：</h3>
 * <ul>
 *   <li>基本加密解密功能</li>
 *   <li>空值和空字符串处理</li>
 *   <li>长文本加密</li>
 *   <li>IV 随机性验证</li>
 *   <li>特殊字符和 Unicode 支持</li>
 *   <li>异常情况处理</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see EncryptionService
 */
class EncryptionServiceTest {

    /**
     * 加密服务实例
     * <p>
     * 在每个测试方法执行前初始化，确保测试隔离性。
     */
    private EncryptionService encryptionService;

    /**
     * 测试前置设置
     * <p>
     * 创建 EncryptionService 实例并设置测试用的 master key。
     * 使用反射注入测试密钥，避免在配置文件中存储敏感信息。
     */
    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        try {
            // 通过反射设置 masterKey 字段（用于测试环境）
            Field field = EncryptionService.class.getDeclaredField("masterKey");
            field.setAccessible(true);
            field.set(encryptionService, "test-master-key-for-unit-tests-only-32bytes");
        } catch (Exception e) {
            throw new RuntimeException("初始化加密服务失败", e);
        }
        // 调用 PostConstruct 初始化方法
        encryptionService.init();
    }

    /**
     * 测试加密和解密正常文本
     * <p>
     * 验证基本的加密和解密功能是否正常工作。
     * 测试包含中英文混合的文本。
     */
    @Test
    @DisplayName("测试加密和解密正常文本")
    void testEncryptAndDecrypt() {
        // 待加密的原始文本，包含中英文
        String originalText = "Hello World! 这是一段测试文本。";

        // 加密文本
        String encrypted = encryptionService.encrypt(originalText);

        // 验证加密后不为 null
        assertNotNull(encrypted);
        // 验证加密后的文本与原始文本不同
        assertNotEquals(originalText, encrypted);

        // 解密文本
        String decrypted = encryptionService.decrypt(encrypted);

        // 验证解密后的文本与原始文本一致
        assertEquals(originalText, decrypted);
    }

    /**
     * 测试空字符串返回 null
     * <p>
     * 加密空字符串或 null 应该返回 null，而不是返回空值或异常。
     * 这是防御性编程的体现。
     */
    @Test
    @DisplayName("测试加密空字符串返回null")
    void testEncryptEmptyString() {
        // 验证加密空字符串返回 null
        assertNull(encryptionService.encrypt(""));
        // 验证加密 null 返回 null
        assertNull(encryptionService.encrypt(null));
    }

    /**
     * 测试解密空字符串返回 null
     * <p>
     * 解密空字符串或 null 应该返回 null。
     */
    @Test
    @DisplayName("测试解密空字符串返回null")
    void testDecryptEmptyString() {
        assertNull(encryptionService.decrypt(""));
        assertNull(encryptionService.decrypt(null));
    }

    /**
     * 测试加密敏感信息（如 API Key）
     * <p>
     * 验证加密服务可以正确加密和解密敏感信息。
     * 这是加密服务的主要使用场景之一。
     */
    @Test
    @DisplayName("测试加密敏感信息如API Key")
    void testEncryptSensitiveData() {
        // 测试用的 API Key
        String apiKey = "sk-1234567890abcdef";

        // 加密
        String encrypted = encryptionService.encrypt(apiKey);
        // 解密
        String decrypted = encryptionService.decrypt(encrypted);

        // 验证加密后的 Key 与原始不同
        assertNotEquals(apiKey, encrypted);
        // 验证解密后与原始一致
        assertEquals(apiKey, decrypted);
    }

    /**
     * 测试加密长文本
     * <p>
     * 验证加密服务可以处理超过默认缓冲区大小的文本。
     * 测试文本长度约 10KB。
     */
    @Test
    @DisplayName("测试加密长文本")
    void testEncryptLongText() {
        // 创建长度约 10000 字符的长文本
        String longText = "A".repeat(10000);

        // 加密
        String encrypted = encryptionService.encrypt(longText);
        // 解密
        String decrypted = encryptionService.decrypt(encrypted);

        // 验证解密后与原始一致
        assertEquals(longText, decrypted);
    }

    /**
     * 测试解密无效密文抛出异常
     * <p>
     * 验证当密文被篡改或格式不正确时，解密操作会抛出异常。
     * 这是安全性的一部分，确保数据完整性。
     */
    @Test
    @DisplayName("测试解密无效密文抛出异常")
    void testDecryptInvalidCipherText() {
        // 无效的密文格式
        String invalidCipherText = "invalid-cipher-text";

        // 验证解密无效密文会抛出异常
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(invalidCipherText);
        });
    }

    /**
     * 测试相同明文加密后密文不同（IV 随机机制）
     * <p>
     * AES-GCM 算法每次加密会生成不同的 IV（初始化向量），
     * 即使相同的明文也会产生不同的密文，这提高了安全性。
     */
    @Test
    @DisplayName("测试相同明文加密后密文不同（IV随机）")
    void testSamePlainTextDifferentCipher() {
        // 相同的明文
        String plainText = "Same text";

        // 三次加密
        String encrypted1 = encryptionService.encrypt(plainText);
        String encrypted2 = encryptionService.encrypt(plainText);
        String encrypted3 = encryptionService.encrypt(plainText);

        // 验证三次加密的密文都不同（因为 IV 不同）
        assertNotEquals(encrypted1, encrypted2);
        assertNotEquals(encrypted2, encrypted3);
        assertNotEquals(encrypted1, encrypted3);

        // 验证三次加密都可以正确解密
        assertEquals(plainText, encryptionService.decrypt(encrypted1));
        assertEquals(plainText, encryptionService.decrypt(encrypted2));
        assertEquals(plainText, encryptionService.decrypt(encrypted3));
    }

    /**
     * 测试特殊字符加密
     * <p>
     * 验证加密服务可以处理各种特殊字符。
     */
    @Test
    @DisplayName("测试特殊字符加密")
    void testSpecialCharacters() {
        // 包含各种特殊字符的字符串
        String specialText = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        // 加密
        String encrypted = encryptionService.encrypt(specialText);
        // 解密
        String decrypted = encryptionService.decrypt(encrypted);

        // 验证解密后与原始一致
        assertEquals(specialText, decrypted);
    }

    /**
     * 测试 Unicode 字符加密
     * <p>
     * 验证加密服务可以正确处理各种语言的 Unicode 字符和表情符号。
     */
    @Test
    @DisplayName("测试Unicode字符加密")
    void testUnicodeCharacters() {
        // 包含中文、日文、韩文和表情符号的字符串
        String unicodeText = "中文测试 🎉 émojis 日本語 한국어";

        // 加密
        String encrypted = encryptionService.encrypt(unicodeText);
        // 解密
        String decrypted = encryptionService.decrypt(encrypted);

        // 验证解密后与原始一致
        assertEquals(unicodeText, decrypted);
    }
}