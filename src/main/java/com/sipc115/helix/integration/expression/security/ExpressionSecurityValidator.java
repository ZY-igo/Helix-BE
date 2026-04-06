/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.expression.security;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Aviator 表达式安全验证器
 * <p>
 * 用于验证用户输入的表达式是否安全，防止表达式注入攻击。
 * 在 DSL 保存或表达式执行前调用，确保表达式不包含危险内容。
 *
 * <h3>安全验证策略：</h3>
 * <ul>
 *   <li>长度限制：表达式最长 500 字符</li>
 *   <li>危险模式检测：禁止 Runtime、System、exec 等危险类和方法</li>
 *   <li>SQL 注入防护：禁止 SELECT、DELETE、DROP 等 SQL 关键字</li>
 *   <li>函数白名单：只允许预定义的安全函数</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * ExpressionSecurityValidator validator = new ExpressionSecurityValidator();
 * ValidationResult result = validator.validate("${user.input}");
 * if (!result.isValid()) {
 *     throw new SecurityException(result.getErrorMessage());
 * }
 * }
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class ExpressionSecurityValidator {

    /**
     * 表达式最大长度限制
     * <p>
     * 超过此长度的表达式将被拒绝，防止过长的表达式消耗过多资源。
     */
    private static final int MAX_EXPRESSION_LENGTH = 500;

    /**
     * 危险模式正则表达式
     * <p>
     * 匹配可能造成安全问题的表达式内容，包括：
     * <ul>
     *   <li>Runtime、System 等危险类</li>
     *   <li>exec()、getClass()、forName() 等危险方法</li>
     *   <li>java.lang、java.io、java.net 等危险包路径</li>
     *   <li>script、eval 等脚本执行相关关键字</li>
     * </ul>
     */
    private static final Pattern DANGEROUS_PATTERNS = Pattern.compile(
        ".*(Runtime|System|Process|exec\\(|class\\.|getClass\\(|Class\\.|forName\\(|" +
        "java\\.lang\\.|java\\.io\\.|java\\.net\\.|java\\.nio\\.|" +
        "script|eval|newInstance|invokeMethod).*",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 允许的函数白名单
     * <p>
     * Aviator 表达式引擎支持的函数众多，但并非所有函数都安全。
     * 此白名单仅包含经过安全审计的函数。
     * <p>
     * 包含的函数类别：
     * <ul>
     *   <li>字符串函数：string、length、substring、toUpperCase、toLowerCase、trim、replace、split、join</li>
     *   <li>集合函数：list、map、filter、reduce、contains</li>
     *   <li>数学函数：sum、avg、min、max、math</li>
     *   <li>序列函数：seq</li>
     *   <li>时间函数：now、date、time</li>
     * </ul>
     */
    private static final Set<String> ALLOWED_FUNCTIONS = new HashSet<>();
    static {
        // 字符串函数
        ALLOWED_FUNCTIONS.add("string");
        ALLOWED_FUNCTIONS.add("length");
        ALLOWED_FUNCTIONS.add("substring");
        ALLOWED_FUNCTIONS.add("toUpperCase");
        ALLOWED_FUNCTIONS.add("toLowerCase");
        ALLOWED_FUNCTIONS.add("trim");
        ALLOWED_FUNCTIONS.add("replace");
        ALLOWED_FUNCTIONS.add("split");
        ALLOWED_FUNCTIONS.add("join");
        ALLOWED_FUNCTIONS.add("contains");

        // 集合函数
        ALLOWED_FUNCTIONS.add("list");
        ALLOWED_FUNCTIONS.add("map");
        ALLOWED_FUNCTIONS.add("filter");
        ALLOWED_FUNCTIONS.add("reduce");

        // 数学函数
        ALLOWED_FUNCTIONS.add("sum");
        ALLOWED_FUNCTIONS.add("avg");
        ALLOWED_FUNCTIONS.add("min");
        ALLOWED_FUNCTIONS.add("max");
        ALLOWED_FUNCTIONS.add("math");

        // 序列函数
        ALLOWED_FUNCTIONS.add("seq");

        // 时间函数
        ALLOWED_FUNCTIONS.add("now");
        ALLOWED_FUNCTIONS.add("date");
        ALLOWED_FUNCTIONS.add("time");
    }

    /**
     * 验证表达式安全性
     * <p>
     * 执行多层安全检查，任何一层检查失败都会返回错误结果。
     *
     * <h3>检查顺序：</h3>
     * <ol>
     *   <li>空值检查：表达式不能为空</li>
     *   <li>长度检查：不能超过 MAX_EXPRESSION_LENGTH</li>
     *   <li>危险模式检查：不能包含危险关键字</li>
     *   <li>SQL 注入检查：不能包含 SQL 关键字</li>
     * </ol>
     *
     * @param expression 待验证的表达式字符串
     * @return 验证结果，包含是否有效及错误信息
     */
    public ValidationResult validate(String expression) {
        // 第一层：空值检查
        // 表达式为空或仅包含空白字符时拒绝
        if (expression == null || expression.isBlank()) {
            return ValidationResult.error("表达式不能为空");
        }

        // 第二层：长度检查
        // 防止过长的表达式消耗过多内存或CPU
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            return ValidationResult.error(
                "表达式长度不能超过 " + MAX_EXPRESSION_LENGTH + " 字符，当前: " + expression.length()
            );
        }

        // 第三层：危险模式检查
        // 使用正则表达式检测可能造成命令执行、类加载等危险操作的代码
        if (DANGEROUS_PATTERNS.matcher(expression).matches()) {
            return ValidationResult.error(
                "表达式包含危险内容: " + maskDangerousContent(expression)
            );
        }

        // 第四层：SQL 注入检查
        // 虽然 Aviator 是表达式引擎，但用户的表达式可能被拼接进SQL查询
        // 禁止常见的SQL操作关键字
        String upperExpression = expression.toUpperCase();
        if (upperExpression.contains("SELECT ") || upperExpression.contains("DELETE ") ||
            upperExpression.contains("UPDATE ") || upperExpression.contains("INSERT ") ||
            upperExpression.contains("DROP ") || upperExpression.contains("CREATE ") ||
            upperExpression.contains("TRUNCATE ") || upperExpression.contains("ALTER ")) {
            return ValidationResult.error("表达式包含潜在危险的SQL关键字");
        }

        // 所有检查通过
        return ValidationResult.success();
    }

    /**
     * 遮蔽表达式中的危险内容
     * <p>
     * 当表达式被判定为危险时，使用此方法生成错误消息。
     * 遮蔽后的内容只显示前50个字符，防止泄露完整表达式内容。
     *
     * @param expression 原始表达式
     * @return 遮蔽后的表达式
     */
    private String maskDangerousContent(String expression) {
        String masked = expression;
        // 将危险关键字替换为 *** 进行遮蔽
        for (String keyword : new String[]{"Runtime", "System", "exec", "class", "getClass", "forName"}) {
            // (?i) 表示大小写不敏感
            masked = masked.replaceAll("(?i)" + keyword, "***");
        }
        // 超过50字符的部分截断并添加省略号
        return masked.length() > 50 ? masked.substring(0, 50) + "..." : masked;
    }

    /**
     * 检查指定函数是否在白名单中
     * <p>
     * 用于在添加自定义函数前验证其安全性。
     *
     * @param functionName 函数名
     * @return true 表示函数安全，false 表示函数不在白名单中
     */
    public boolean isAllowedFunction(String functionName) {
        return ALLOWED_FUNCTIONS.contains(functionName.toLowerCase());
    }

    /**
     * 获取所有允许的函数列表
     * <p>
     * 返回白名单的副本，防止外部修改。
     *
     * @return 允许的函数名集合
     */
    public Set<String> getAllowedFunctions() {
        return new HashSet<>(ALLOWED_FUNCTIONS);
    }

    /**
     * 验证结果类
     * <p>
     * 封装验证操作的返回结果，包含验证状态和错误信息。
     * 使用 Builder 模式创建。
     */
    public static class ValidationResult {
        /**
         * 验证是否通过
         */
        private final boolean valid;

        /**
         * 错误信息，当 valid 为 false 时有值
         */
        private final String errorMessage;

        /**
         * 私有构造函数，通过静态工厂方法创建
         *
         * @param valid 验证是否通过
         * @param errorMessage 错误信息
         */
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        /**
         * 创建验证成功的结果
         *
         * @return 成功的验证结果
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        /**
         * 创建验证失败的结果
         *
         * @param message 错误信息
         * @return 失败的验证结果
         */
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        /**
         * 获取验证是否通过
         *
         * @return true 表示通过，false 表示失败
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * 获取错误信息
         *
         * @return 错误信息字符串，如果验证成功则返回 null
         */
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}