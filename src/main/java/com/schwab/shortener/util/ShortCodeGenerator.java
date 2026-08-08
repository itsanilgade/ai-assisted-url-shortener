package com.schwab.shortener.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates opaque URL-safe short codes using a cryptographically strong RNG. */
@Component
public class ShortCodeGenerator {

    private static final char[] ALPHABET =
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        char[] value = new char[CODE_LENGTH];
        for (int index = 0; index < value.length; index++) {
            value[index] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(value);
    }
}
