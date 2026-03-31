package com.sipc115.helix.utils;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 生成 8 位字符长度的唯一 ID，格式：时间戳 (4 位) + 机器标识 (2 位) + 序列号 (2 位) = 62 进制编码
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Component
public class SnowflakeIdGenerator {

    /**
     * 单例实例
     */
    private static final SnowflakeIdGenerator instance = new SnowflakeIdGenerator();

    /**
     * 获取单例实例
     * 
     * @return 雪花算法 ID 生成器实例
     */
    public static SnowflakeIdGenerator getInstance() {
        return instance;
    }

    /**
     * 起始时间戳 (2026-01-01 00:00:00 UTC)
     */
    private static final long START_TIMESTAMP = 1735689600000L;

    /**
     * 时间戳部分位数 (约 8.5 年)
     */
    private static final long TIMESTAMP_BITS = 28;
    
    /**
     * 机器标识部分位数 (64 台机器)
     */
    private static final long MACHINE_BITS = 6;
    
    /**
     * 序列号部分位数 (1024/ms)
     */
    private static final long SEQUENCE_BITS = 10;

    /**
     * 时间戳掩码
     */
    private static final long TIMESTAMP_MASK = ~(-1L << TIMESTAMP_BITS);
    
    /**
     * 机器标识掩码
     */
    private static final long MACHINE_MASK = ~(-1L << MACHINE_BITS);
    
    /**
     * 序列号掩码
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器标识偏移量
     */
    private static final long MACHINE_SHIFT = SEQUENCE_BITS;
    
    /**
     * 时间戳偏移量
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_BITS;

    /**
     * 62 进制字符集 (0-9, A-Z, a-z)
     */
    private static final char[] BASE62_CHARS =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    /**
     * 机器 ID
     */
    private long machineId;
    
    /**
     * 序列号
     */
    private long sequence = 0L;
    
    /**
     * 上次时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 私有构造函数
     */
    private SnowflakeIdGenerator() {
        this.machineId = generateMachineId();
    }

    /**
     * 生成下一个唯一 ID（8 位字符）
     */
    public synchronized String nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards! Refusing to generate id.");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        long id = ((timestamp - START_TIMESTAMP) & TIMESTAMP_MASK) << TIMESTAMP_SHIFT
                | (machineId & MACHINE_MASK) << MACHINE_SHIFT
                | (sequence & SEQUENCE_MASK);

        return encodeBase62(id, 8);
    }

    /**
     * 批量生成指定数量的唯一 ID
     */
    public String[] nextIds(int count) {
        String[] ids = new String[count];
        for (int i = 0; i < count; i++) {
            ids[i] = nextId();
        }
        return ids;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 将 long 值转换为固定长度的 62 进制字符串
     */
    private String encodeBase62(long value, int minLength) {
        StringBuilder sb = new StringBuilder();
        while (value > 0 || sb.length() < minLength) {
            sb.append(BASE62_CHARS[(int) (value % 62)]);
            value /= 62;
        }
        return sb.reverse().toString();
    }

    /**
     * 将 62 进制字符串解码为 long 值
     */
    public long decodeBase62(String base62) {
        long result = 0;
        for (char c : base62.toCharArray()) {
            result = result * 62 + indexOf(c);
        }
        return result;
    }

    private int indexOf(char c) {
        for (int i = 0; i < BASE62_CHARS.length; i++) {
            if (BASE62_CHARS[i] == c) {
                return i;
            }
        }
        throw new IllegalArgumentException("Invalid character: " + c);
    }

    /**
     * 生成机器 ID（基于 MAC 地址和 PID）
     */
    private long generateMachineId() {
        try {
            StringBuilder sb = new StringBuilder();

            // 获取 MAC 地址
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);

            if (network != null) {
                byte[] mac = network.getHardwareAddress();
                if (mac != null && mac.length >= 2) {
                    sb.append(String.format("%02X%02X", mac[mac.length - 2], mac[mac.length - 1]));
                } else {
                    sb.append("0000");
                }
            } else {
                sb.append("0000");
            }

            // 添加进程 ID
            String pidName = ManagementFactory.getRuntimeMXBean().getName();
            long pid = Long.parseLong(pidName.split("@")[0]);
            sb.append(String.format("%04X", pid & 0xFFFF));

            // 取后 6 位作为机器 ID
            String machineStr = sb.toString();
            return Long.parseLong(machineStr.substring(machineStr.length() - 6), 16) & MACHINE_MASK;

        } catch (Exception e) {
            // 降级方案：使用随机数
            return (long) (Math.random() * 64) & MACHINE_MASK;
        }
    }

    /**
     * 重置序列号（用于测试）
     */
    public synchronized void reset() {
        sequence = 0L;
        lastTimestamp = -1L;
    }
}
