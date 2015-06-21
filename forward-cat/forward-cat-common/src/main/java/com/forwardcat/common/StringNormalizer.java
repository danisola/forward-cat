package com.forwardcat.common;

public class StringNormalizer {

    public static String onlyLowerCaseWords(String text) {
        return text.toLowerCase().replaceAll("[^\\w]", "");
    }

    private StringNormalizer() {
        // Non-instantiable
    }
}
