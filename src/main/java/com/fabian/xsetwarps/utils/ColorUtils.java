package com.fabian.xsetwarps.utils;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing advanced color codes
 * Compatible with Minecraft 1.8.8+
 * Supports: Legacy (&) and Hex (#RRGGBB)
 */
public class ColorUtils {

    // Pattern for hex colors: &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");
    // Pattern for hex colors with format: &#RRGGBB&format
    private static final Pattern HEX_WITH_FORMAT_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})(&[0-9a-fk-or])");
    // Pattern for gradients: <gradient:#RRGGBB:#RRGGBB>text</gradient>
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:?#([0-9A-Fa-f]{6}):?#([0-9A-Fa-f]{6})>(.*?)</gradient>", Pattern.CASE_INSENSITIVE);

    /**
     * Translate all color codes in a string to ChatColor
     * Supports: & codes and &#RRGGBB hex colors
     */
    public static String translateColors(String text) {
        if (text == null || text.isEmpty()) return text;

        // 1. Handle gradients
        String result = translateGradients(text);

        // 2. Handle hex colors with format codes (e.g., &#FFF400&lC)
        result = translateHexWithFormat(result);

        // 3. Handle plain hex colors (e.g., &#FFF400)
        result = translateHexColors(result);

        // 4. Finally translate legacy & codes
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    /**
     * Translate hex colors with format codes (&#RRGGBB&l)
     */
    private static String translateHexWithFormat(String text) {
        Matcher matcher = HEX_WITH_FORMAT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            String format = matcher.group(2);
            ChatColor color = getChatColorFromHex(hex);
            if (color != null) {
                String legacyFormat = translateFormat(format);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(color + legacyFormat));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Translate plain hex colors (&#RRGGBB)
     */
    private static String translateHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            ChatColor color = getChatColorFromHex(hex);
            if (color != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(color.toString()));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Convert hex color to closest ChatColor
     */
    private static ChatColor getChatColorFromHex(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            // Find closest legacy color
            return findClosestLegacyColor(r, g, b);
        } catch (Exception e) {
            return ChatColor.WHITE;
        }
    }

    /**
     * Find closest legacy ChatColor to RGB values
     */
    private static ChatColor findClosestLegacyColor(int r, int g, int b) {
        ChatColor[] colors = {
            ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
            ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
            ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
            ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
        };

        ChatColor closest = ChatColor.WHITE;
        int minDistance = Integer.MAX_VALUE;

        for (ChatColor color : colors) {
            int[] rgb = getChatColorRGB(color);
            int distance = (r - rgb[0]) * (r - rgb[0]) +
                          (g - rgb[1]) * (g - rgb[1]) +
                          (b - rgb[2]) * (b - rgb[2]);

            if (distance < minDistance) {
                minDistance = distance;
                closest = color;
            }
        }

        return closest;
    }

    /**
     * Get RGB values from ChatColor
     */
    private static int[] getChatColorRGB(ChatColor color) {
        switch (color) {
            case BLACK: return new int[]{0, 0, 0};
            case DARK_BLUE: return new int[]{0, 0, 170};
            case DARK_GREEN: return new int[]{0, 170, 0};
            case DARK_AQUA: return new int[]{0, 170, 170};
            case DARK_RED: return new int[]{170, 0, 0};
            case DARK_PURPLE: return new int[]{170, 0, 170};
            case GOLD: return new int[]{255, 170, 0};
            case GRAY: return new int[]{170, 170, 170};
            case DARK_GRAY: return new int[]{85, 85, 85};
            case BLUE: return new int[]{85, 85, 255};
            case GREEN: return new int[]{85, 255, 85};
            case AQUA: return new int[]{85, 255, 255};
            case RED: return new int[]{255, 85, 85};
            case LIGHT_PURPLE: return new int[]{255, 85, 255};
            case YELLOW: return new int[]{255, 255, 85};
            case WHITE: default: return new int[]{255, 255, 255};
        }
    }

    /**
     * Translate format code (e.g., &l, &n)
     */
    private static String translateFormat(String format) {
        if (format == null) return "";
        
        switch (format) {
            case "&l": return ChatColor.BOLD.toString();
            case "&n": return ChatColor.UNDERLINE.toString();
            case "&o": return ChatColor.ITALIC.toString();
            case "&m": return ChatColor.STRIKETHROUGH.toString();
            case "&k": return ChatColor.MAGIC.toString();
            case "&r": return ChatColor.RESET.toString();
            default: return ""; 
        }
    }

    /**
     * Translate gradient tags to colored text
     */
    private static String translateGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String content = matcher.group(3);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(applyGradient(content, startHex, endHex)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Apply gradient to a string between two hex colors
     */
    private static String applyGradient(String text, String startHex, String endHex) {
        int r1 = Integer.parseInt(startHex.substring(0, 2), 16);
        int g1 = Integer.parseInt(startHex.substring(2, 4), 16);
        int b1 = Integer.parseInt(startHex.substring(4, 6), 16);

        int r2 = Integer.parseInt(endHex.substring(0, 2), 16);
        int g2 = Integer.parseInt(endHex.substring(2, 4), 16);
        int b2 = Integer.parseInt(endHex.substring(4, 6), 16);

        StringBuilder sb = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = length > 1 ? (float) i / (length - 1) : 1f;
            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);
            
            // On 1.8.8, we use closest legacy color. 
            // On 1.16+, we should use hex, but for simplicity and compatibility with existing code 
            // which always find closest legacy, we do the same here.
            ChatColor color = findClosestLegacyColor(r, g, b);
            sb.append(color).append(text.charAt(i));
        }

        return sb.toString();
    }
}
