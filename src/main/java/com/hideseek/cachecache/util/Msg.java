package com.hideseek.cachecache.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Convertit les chaînes utilisant les codes couleur legacy (§a, §c, §l, ...)
 * en Component Adventure correctement colorés.
 */
public final class Msg {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    private Msg() {}

    public static Component of(String legacyText) {
        return SERIALIZER.deserialize(legacyText);
    }
}
