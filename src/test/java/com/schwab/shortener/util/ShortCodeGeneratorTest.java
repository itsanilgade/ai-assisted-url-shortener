package com.schwab.shortener.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

    @Test
    void generatesUrlSafeCodesWithHighPracticalUniqueness() {
        ShortCodeGenerator generator = new ShortCodeGenerator();
        var seen = new HashSet<String>();

        for (int i = 0; i < 1_000; i++) {
            String code = generator.next();
            assertTrue(code.matches("[A-Za-z0-9]{8}"));
            seen.add(code);
        }

        assertTrue(seen.size() > 990);
    }
}
