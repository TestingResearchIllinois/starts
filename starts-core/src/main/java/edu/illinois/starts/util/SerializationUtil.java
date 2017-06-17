/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;

public class SerializationUtil {

    public static boolean exists(File file) {
        return Files.exists(file.toPath());
    }

    public static void serializeData(Object data, String location) {
        try {
            File file = new File(location);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream outputStream = new ObjectOutputStream(fos);
            outputStream.writeObject(data);
            outputStream.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static Object deserializeData(String location) {
        Object data = "Serialization Unsuccessful";
        try {
            FileInputStream fis = new FileInputStream(location);
            ObjectInputStream inputStream = new ObjectInputStream(fis);
            data = inputStream.readObject();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return data;
    }
}
