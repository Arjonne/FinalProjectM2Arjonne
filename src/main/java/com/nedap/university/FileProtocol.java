package com.nedap.university;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the protocol for creating packet input from files and the other way around for the transmission between
 * client and server.
 */
public final class FileProtocol {
    // filePath for sending files using localHost:
    public static final String CLIENT_FILEPATH = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/example_files/";
    public static final String SERVER_FILEPATH = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/localserver/";
//    public static final String SERVER_FILEPATH = "System.getProperty(\"user.dir\")";


    /**
     * Create request file based on input from TUI.
     *
     * @param fileName is the filename that the user has typed in the TUI.
     * @return the data in the form of a byte array (which is needed for datagram packet) of the file.
     */
    public static byte[] createRequestFile(String fileName) {
        byte[] request = fileName.getBytes();
        return request;
    }

    /**
     * Create an actual file path to be able to find stored files in a specific folder.
     *
     * @param filePathString is the string representation of the file path.
     * @return the actual file path in which files can be found.
     */
    public static File createFilePath(String filePathString) {
        File filePath = new File(filePathString);
        return filePath;
    }

    /**
     * Create a byte array (which is the input for a Datagram Packet) from the file that needs to be sent.
     *
     * @param fileName is the name of the file that needs to be sent.
     * @return the byte representation of the file in an array.
     */
    public static byte[] fileToBytes(String filePath, String fileName) {
        File file = new File(filePath + fileName);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] fileInByteArray = new byte[(int) file.length()];
            for (int i = 0; i < fileInByteArray.length; i++) {
                byte nextByte = (byte) fileInputStream.read();
                fileInByteArray[i] = nextByte;
            }
            return fileInByteArray;
        } catch (IOException e) {
            System.out.println("Could not copy byte representation of file into a new byte array.");
            return null;
        }
    }

    /**
     * Create a file from the byte array that is sent.
     *
     * @param fileName is the name of the transmitted file.
     * @param fileData is the data of the file in bytes.
     * @return the complete file.
     */
    public static File bytesToFile(String fileName, byte[] fileData) {
        File file = new File(fileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            //todo in case of creating total file with header, change offset and length in next function:
            for (byte byteOfFileData : fileData) {
                fileOutputStream.write(byteOfFileData);
            }
            return file;
        } catch (IOException e) {
            System.out.println("Could not write byte representation of file to actual file.");
            return null;
        }
    }

    /**
     * Get the file size of the file to be transmitted.
     *
     * @param fileName is the name of the file to be transmitted.
     * @return the size of the file.
     */
    public static int getFileSize(String filePath, String fileName) {
        byte[] fileToSendInBytes = fileToBytes(filePath, fileName);
        int fileSize = fileToSendInBytes.length;
        return fileSize;
    }

    /**
     * Create a list of all available files on the server.
     *
     * @param filePath is the path with folder in which the files are stored.
     * @return the list of filenames.
     */
    public static List<String> createListOfFileNames(File filePath) {
        File[] listOfFiles = filePath.listFiles();
        List<String> listOfFileNames = new ArrayList<>();
        if (listOfFiles.length != 0) {
            for (File file : listOfFiles) {
                String fileName = file.getName();
                listOfFileNames.add(fileName);
            }
        }
        return listOfFileNames;
    }

    /**
     * Check if file already exists in the folder of interest.
     *
     * @param fileNameToCheck is the name of the file you want to check.
     * @param filePath        is the path with folder in which the files are stored.
     * @return true if file already exists, false if not.
     */
    public static boolean checkIfFileExists(String fileNameToCheck, File filePath) {
        List<String> listOfFileNames = createListOfFileNames(filePath);
        if (!listOfFileNames.isEmpty()) {
            for (String fileName : listOfFileNames) {
                if (fileName.equals(fileNameToCheck)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create a list of file names that can be sent in a Datagram Packet.
     *
     * @param listOfFileNames is the list of file names to be sent.
     * @return the byte representation of the list.
     */
    public static byte[] createByteArrayOfFileNameList(List<String> listOfFileNames) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(out);
            outputStream.writeObject(listOfFileNames);
            outputStream.close();
        } catch (IOException e) {
            System.out.println("Not able to create a byte array of the list of file names.");
        }
        byte[] byteArrayOfFileNameList = out.toByteArray();
        return byteArrayOfFileNameList;
    }

    /**
     * Create a readable list of file names from the byte array input stream.
     *
     * @param byteArrayOfFileNameList is the list of file names represented in a byte array.
     * @return a readable list of file names (String representation).
     */
    public List<String> readByteArrayWithFileNameList(byte[] byteArrayOfFileNameList) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOfFileNameList));
//            List<String> listOfFileNames = inputStream.readObject();
//            return listOfFileNames;
        } catch (IOException e) {
            e.printStackTrace(); // todo
        }
        return null;
    }
}
