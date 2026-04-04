/**
 * 加密服务边界情况测试类
 * <p>
 * 本测试类针对 EncryptionService 的边界情况和异常场景进行测试，
 * 确保加密服务在各种极端输入下都能正确处理，不会出现未预期的异常或安全问题。
 *
 * <h3>测试覆盖范围：</h3>
 * <ul>
 *   <li>空值处理：null 输入、空字符串、空格字符串</li>
 *   <li>超长文本：长达 100000 字符的文本加密解密</li>
 *   <li>IV 随机性：同一明文多次加密产生不同密文</li>
 *   <li>篡改检测：密文被篡改后解密应抛出异常</li>
 *   <li>编码验证：密文应为有效的 Base64 编码</li>
 *   <li>特殊字符：各种语言、Emoji、JSON、HTML、SQL 等特殊内容</li>
 * </ul>
 *
 * @see EncryptionService
 */
package com.sipc115.helix.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 加密服务边界情况和异常场景测试
 *
 * <p>
 * 加密服务使用 AES-GCM 算法，AES-GCM 是业界标准的对称加密算法，
 * 具有加密速度快、安全性高的特点。本测试类验证其在各种边界条件下的行为。
 *
 * <h3>AES-GCM 特性说明：</h3>
 * <ul>
 *   <li>每次加密使用随机 IV（初始化向量），即使相同明文也会产生不同密文</li>
 *   <li>GCM 模式提供认证加密，能检测密文篡改</li>
 *   <li>加密结果为 Base64 编码字符串，便于存储和传输</li>
 * </ul>
 */
class EncryptionServiceEdgeCaseTest {

    private EncryptionService encryptionService;

    /**
     * 测试前准备：创建加密服务实例并设置测试用密钥
     * <p>
     * 通过反射机制设置测试专用的主密钥（masterKey）。
     * 测试密钥是32字节的字符串，满足 AES-256 的密钥要求。
     * 实际生产环境中应使用配置中心的密钥管理服务。
     */
    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        try {
            java.lang.reflect.Field field = EncryptionService.class.getDeclaredField("masterKey");
            field.setAccessible(true);
            field.set(encryptionService, "test-master-key-for-unit-tests-only-32bytes");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        encryptionService.init();
    }

    /**
     * 测试 null 输入返回 null
     * <p>
     * 验证加密和解密服务对 null 输入的处理。
     * null 不是有效的输入，应该直接返回 null 而不是抛出异常。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>encrypt(null) 返回 null</li>
     *   <li>decrypt(null) 返回 null</li>
     * </ul>
     *
     * <h3>设计理由：</h3>
     * <p>
     * 加密和解密操作需要字符串作为输入，null 通常表示"无数据"或"未设置"。
     * 让操作直接返回 null 而不是抛出异常，可以简化调用方的空值判断逻辑。
     */
    @Test
    @DisplayName("测试null输入返回null")
    void testNullInputReturnsNull() {
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.decrypt(null));
    }

    /**
     * 测试空字符串输入返回 null
     * <p>
     * 验证加密和解密服务对空字符串的处理。
     * 空字符串虽然没有实际内容，但也是一个有效的 Java 字符串。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>encrypt("") 返回 null</li>
     *   <li>decrypt("") 返回 null</li>
     * </ul>
     *
     * <h3>设计理由：</h3>
     * <p>
     * 空字符串加密没有实际意义，反而会浪费计算资源。
     * 返回 null 可以让调用方区分"无数据"和"有数据但为空"。
     */
    @Test
    @DisplayName("测试空字符串输入返回null")
    void testEmptyStringInputReturnsNull() {
        assertNull(encryptionService.encrypt(""));
        assertNull(encryptionService.decrypt(""));
    }

    /**
     * 测试只包含空格的字符串
     * <p>
     * 验证加密服务能正确处理纯空格字符串。
     * 空格虽然看起来"没有内容"，但实际上是有效的字符数据。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>空格字符串能被正确加密</li>
     *   <li>解密后能得到原始的空格字符串</li>
     * </ul>
     *
     * <h3>测试场景：</h3>
     * <p>
     * 用户可能在配置中输入空格作为默认值或占位符，
     * 加密服务需要能正确处理这种边缘情况。
     */
    @Test
    @DisplayName("测试只包含空格的字符串")
    void testWhitespaceOnlyString() {
        String whitespace = "   ";
        String encrypted = encryptionService.encrypt(whitespace);
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(whitespace, decrypted);
    }

    /**
     * 测试超长字符串加密解密
     * <p>
     * 验证加密服务能处理长达 100,000 字符的长文本。
     * 这模拟了大型配置文件、JSON 内容或长文本数据的加密场景。
     *
     * <h3>测试数据：</h3>
     * <ul>
     *   <li>输入：100,000 个连续字符 "A"</li>
     *   <li>预期：加密后的密文能正确解密回原始内容</li>
     * </ul>
     *
     * <h3>性能考量：</h3>
     * <p>
     * AES-GCM 的加密速度很快，但对于极大量的数据，
     * 仍需考虑分段加密以避免内存压力。本测试验证基本的大数据处理能力。
     */
    @Test
    @DisplayName("测试超长字符串加密解密")
    void testVeryLongString() {
        String longText = "A".repeat(100000);
        String encrypted = encryptionService.encrypt(longText);
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(longText, decrypted);
    }

    /**
     * 测试重复加密产生不同密文（验证 IV 随机性）
     * <p>
     * 验证 AES-GCM 每次加密使用不同的 IV（初始化向量），
     * 即使对相同的明文进行多次加密，产生的密文也不同。
     * 这是 GCM 模式的重要安全特性。
     *
     * <h3>测试数据：</h3>
     * <ul>
     *   <li>明文："consistent-text"（固定内容）</li>
     *   <li>操作：连续加密 3 次</li>
     * </ul>
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>encrypted1 != encrypted2 != encrypted3（三个密文都不相等）</li>
     *   <li>解密后都能得到原始明文 "consistent-text"</li>
     * </ul>
     *
     * <h3>安全意义：</h3>
     * <p>
     * 如果相同明文总是产生相同密文，攻击者可以通过对比密文判断是否重复执行相同操作。
     * 随机 IV 确保即使是相同内容，每次加密结果也不同，防止信息泄露。
     *
     * @see EncryptionService
     */
    @Test
    @DisplayName("测试重复加密产生不同密文（IV机制）")
    void testRepeatableEncryption() {
        String plainText = "consistent-text";
        String encrypted1 = encryptionService.encrypt(plainText);
        String encrypted2 = encryptionService.encrypt(plainText);
        String encrypted3 = encryptionService.encrypt(plainText);

        assertNotEquals(encrypted1, encrypted2);
        assertNotEquals(encrypted2, encrypted3);
        assertNotEquals(encrypted1, encrypted3);

        assertEquals(plainText, encryptionService.decrypt(encrypted1));
        assertEquals(plainText, encryptionService.decrypt(encrypted2));
        assertEquals(plainText, encryptionService.decrypt(encrypted3));
    }

    /**
     * 测试解密被篡改的密文抛出异常
     * <p>
     * 验证 AES-GCM 的认证机制能检测密文篡改。
     * 当密文被修改后，解密过程会失败并抛出异常。
     *
     * <h3>测试方法：</h3>
     * <ol>
     *   <li>加密原始明文得到密文</li>
     *   <li>将密文的最后 5 个字符替换为 "XXXXX"</li>
     *   <li>尝试解密被篡改的密文</li>
     * </ol>
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>解密被篡改的密文应抛出 RuntimeException</li>
     * </ul>
     *
     * <h3>安全意义：</h3>
     * <p>
     * GCM 模式在解密时会验证 Authentication Tag。
     * 如果密文被篡改，Tag 验证会失败，抛出异常而不是返回错误的明文。
     * 这确保了数据完整性和真实性，防止主动攻击。
     */
    @Test
    @DisplayName("测试解密被篡改的密文抛出异常")
    void testTamperedCipherTextThrowsException() {
        String plainText = "original-text";
        String encrypted = encryptionService.encrypt(plainText);

        String tampered = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";

        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(tampered);
        });
    }

    /**
     * 测试 Base64 编码的密文格式
     * <p>
     * 验证加密后的密文是有效的 Base64 编码字符串。
     * Base64 编码确保二进制数据可以用 ASCII 字符串表示，便于存储和传输。
     *
     * <h3>验证规则：</h3>
     * <ul>
     *   <li>密文只包含：A-Z、a-z、0-9、+、/、=</li>
     *   <li>正则表达式：^[A-Za-z0-9+/=]+$</li>
     * </ul>
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>加密后的字符串符合 Base64 格式</li>
     *   <li>该字符串能正确解密回原始明文</li>
     * </ul>
     *
     * <h3>应用场景：</h3>
     * <p>
     * Base64 编码的密文可以直接存储在 JSON、XML 配置文件中，
     * 或通过 URL 传递，不会出现字符编码问题。
     */
    @Test
    @DisplayName("测试Base64编码的密文解密")
    void testBase64EncodedCipherText() {
        String plainText = "test-data";
        String encrypted = encryptionService.encrypt(plainText);

        assertTrue(encrypted.matches("^[A-Za-z0-9+/=]+$"));

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    /**
     * 测试各种特殊字符组合
     * <p>
     * 验证加密服务能正确处理各种特殊字符，包括：
     * <ul>
     *   <li>符号：!@#$%^&*() 等</li>
     *   <li>中文：中文测试</li>
     *   <li>日文：日本語テスト</li>
     *   <li>Emoji：🎉🎊🎁 和组合 Emoji 👨‍👩‍👧‍👦</li>
     *   <li>HTML 标签：&lt;html&gt;tags&lt;/html&gt;</li>
     *   <li>JSON 格式：{"json": "value"}</li>
     *   <li>SQL 语句：SELECT * FROM table</li>
     * </ul>
     *
     * <h3>测试策略：</h3>
     * <p>
     * 对每个测试用例分别进行加密和解密，
     * 验证解密后的内容与原始输入完全一致。
     *
     * <h3>字符编码说明：</h3>
     * <p>
     * Java 内部使用 UTF-16 编码字符串，加密时需要正确处理字符编码。
     * 本测试验证各种 Unicode 字符都能被正确处理。
     */
    @Test
    @DisplayName("测试各种特殊字符组合")
    void testVariousSpecialCharacters() {
        String[] testCases = {
            "!@#$%^&*()",
            "中文测试",
            "日本語テスト",
            "🎉🎊🎁",
            "emoji: 👨‍👩‍👧‍👦 family",
            "<html>tags</html>",
            "{\"json\": \"value\"}",
            "SQL: SELECT * FROM table"
        };

        for (String testCase : testCases) {
            String encrypted = encryptionService.encrypt(testCase);
            String decrypted = encryptionService.decrypt(encrypted);
            assertEquals(testCase, decrypted, "Failed for: " + testCase);
        }
    }
}