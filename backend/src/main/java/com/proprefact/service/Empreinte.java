package com.proprefact.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Empreintes SHA-256 en hexadécimal. */
final class Empreinte {
    private Empreinte() {}

    static String sha256(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 est garanti par toute JVM
        }
    }

    static String sha256(String valeur) {
        return sha256(valeur.getBytes(StandardCharsets.UTF_8));
    }
}
