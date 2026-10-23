package com.xcompwiz.mystcraft.symbol;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * Bridges 1.12 Mystcraft symbol identifiers to modern Identifier rules.
 *
 * <p>Legacy Mystcraft deliberately used mixed-case paths such as
 * {@code mystcraft:TerrainNormal}. Modern Identifier paths reject uppercase
 * characters, so the exact legacy string must remain the persistence/NBT identity.
 * This class derives a lower-case port-only lookup key without rewriting the NBT.</p>
 */
public final class LegacySymbolId {
    private LegacySymbolId() {}

    public static String qualify(String legacyId) {
        if (legacyId == null) return "";
        String value = legacyId.trim();
        if (value.isEmpty()) return "";
        return value.indexOf(':') >= 0 ? value : Mystcraft.MOD_ID + ":" + value;
    }

    public static Identifier canonicalKey(String legacyId) {
        String qualified = qualify(legacyId);
        if (qualified.isEmpty()) return null;
        int split = qualified.indexOf(':');
        String namespace = sanitize(qualified.substring(0, split));
        String path = sanitizePath(qualified.substring(split + 1));
        if (namespace.isEmpty() || path.isEmpty()) return null;
        return Identifier.tryParse(namespace + ":" + path);
    }

    private static String sanitize(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        return out.toString();
    }

    private static String sanitizePath(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        return out.toString();
    }
}
