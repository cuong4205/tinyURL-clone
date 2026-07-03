package com.example.urlshortener.util;

public final class Base62Encoder {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    // Number of characters in the generated short codes.
    public static final int CODE_LENGTH = 5;

    private Base62Encoder() {
    }

    /**
     * Encodes a non-negative number into a base62 string, left-padded with the
     * alphabet's zero-character so every code is exactly CODE_LENGTH characters long.
     */
    public static String encode(long number) {
        StringBuilder sb = new StringBuilder();
        long n = number;
        if (n == 0) {
            sb.append(ALPHABET.charAt(0));
        }
        while (n > 0) {
            int remainder = (int) (n % BASE);
            sb.append(ALPHABET.charAt(remainder));
            n /= BASE;
        }
        // pad to fixed length
        while (sb.length() < CODE_LENGTH) {
            sb.append(ALPHABET.charAt(0));
        }
        return sb.reverse().toString();
    }
}
