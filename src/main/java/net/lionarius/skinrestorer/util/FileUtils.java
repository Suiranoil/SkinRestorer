package net.lionarius.skinrestorer.util;

import java.io.*;

public class FileUtils {

    public static String readFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return StringUtils.readString(reader);
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean writeFile(File path, String fileName, String content) {
        try {
            if (!path.exists())
                path.mkdirs();

            File file = new File(path, fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
