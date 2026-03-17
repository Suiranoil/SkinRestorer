package net.lionarius.skinrestorer.util;

import java.io.BufferedReader;
import java.io.IOException;

public final class StringUtils {

    private StringUtils() {}

    public static String readString(BufferedReader reader) throws IOException {
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }

        return response.toString();
    }

    public static boolean isValidPlayerName(String playerName) {
        return playerName.length() <= 16
                && playerName.chars().filter(i -> i <= 32 || i >= 127).findAny().isEmpty();
    }
}
