package com.orihistoria.dictado;

import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.List;

/** Divide un texto en fragmentos de dictado, conservando la puntuación. */
public final class Segmenter {
    private Segmenter() {
    }

    public static ArrayList<String> split(String input) {
        ArrayList<String> raw = new ArrayList<>();
        if (input == null) {
            return raw;
        }

        String text = input.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (text.isEmpty()) {
            return raw;
        }

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);

            if (isBoundary(text, i, c)) {
                flush(current, raw);
            }
        }
        flush(current, raw);

        int maxLength = Math.max(500, TextToSpeech.getMaxSpeechInputLength() - 100);
        ArrayList<String> result = new ArrayList<>();
        for (String fragment : raw) {
            splitOversized(fragment, maxLength, result);
        }
        return result;
    }

    private static boolean isBoundary(String text, int index, char c) {
        if (c == '\n') {
            return true;
        }

        char previous = index > 0 ? text.charAt(index - 1) : '\0';
        char next = index + 1 < text.length() ? text.charAt(index + 1) : '\0';

        if (c == ',') {
            // No corta números decimales escritos como 12,5.
            return !(Character.isDigit(previous) && Character.isDigit(next));
        }

        if (c == ';' || c == ':') {
            return true;
        }

        if (c == '!' || c == '?') {
            return true;
        }

        if (c == '.') {
            // No corta números decimales ni siglas como EE.UU. en el primer punto.
            if (Character.isDigit(previous) && Character.isDigit(next)) {
                return false;
            }
            return next == '\0'
                    || Character.isWhitespace(next)
                    || next == '"'
                    || next == '\''
                    || next == '”'
                    || next == '’'
                    || next == ')'
                    || next == ']';
        }

        return false;
    }

    private static void flush(StringBuilder current, List<String> output) {
        String fragment = current.toString().trim();
        current.setLength(0);
        if (!fragment.isEmpty()) {
            output.add(fragment);
        }
    }

    private static void splitOversized(String fragment, int maxLength, List<String> output) {
        String remaining = fragment.trim();
        while (remaining.length() > maxLength) {
            int cut = remaining.lastIndexOf(' ', maxLength);
            if (cut < maxLength / 2) {
                cut = maxLength;
            }
            String part = remaining.substring(0, cut).trim();
            if (!part.isEmpty()) {
                output.add(part);
            }
            remaining = remaining.substring(cut).trim();
        }
        if (!remaining.isEmpty()) {
            output.add(remaining);
        }
    }
}
