package com.sipc115.helix.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkflowVersionUtils {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-zA-Z0-9]+))?$");

    private WorkflowVersionUtils() {
    }

    public static String initialVersion() {
        return "v1.0.0";
    }

    public static String nextPatchVersion(String currentVersion) {
        VersionInfo info = parse(currentVersion);
        if (info == null) {
            return currentVersion + "-patch1";
        }
        return String.format("v%d.%d.%d-patch", info.major, info.minor, info.patch + 1);
    }

    public static String nextMinorVersion(String currentVersion) {
        VersionInfo info = parse(currentVersion);
        if (info == null) {
            return "v1.1.0";
        }
        return String.format("v%d.%d.0", info.major, info.minor + 1);
    }

    public static String nextMajorVersion(String currentVersion) {
        VersionInfo info = parse(currentVersion);
        if (info == null) {
            return "v2.0.0";
        }
        return String.format("v%d.0.0", info.major + 1);
    }

    public static VersionInfo parse(String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return null;
        }

        VersionInfo info = new VersionInfo();
        info.major = Integer.parseInt(matcher.group(1));
        info.minor = Integer.parseInt(matcher.group(2));
        info.patch = Integer.parseInt(matcher.group(3));
        info.preRelease = matcher.group(4);
        return info;
    }

    public static boolean isValid(String version) {
        return parse(version) != null;
    }

    public static String format(VersionInfo info) {
        if (info == null) {
            return "v1.0.0";
        }
        String base = String.format("v%d.%d.%d", info.major, info.minor, info.patch);
        if (info.preRelease != null && !info.preRelease.isEmpty()) {
            base = base + "-" + info.preRelease;
        }
        return base;
    }

    public static class VersionInfo {
        public int major;
        public int minor;
        public int patch;
        public String preRelease;
    }
}
