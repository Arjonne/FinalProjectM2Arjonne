package com.nedap.university;

import java.io.*;

/**
 * Represents the protocol for creating packet input from files and the other way around for the transmission between
 * client and server.
 */
public final class FileProtocol {
    // filePath for sending files using localHost:
    public static final String CLIENT_FILEPATH = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/example_files/";
    public static final String SERVER_FILEPATH = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/localserver/";
//    public static final String SERVER_FILEPATH = "/home/pi/Files/";

    /**
     * Get the actual file.
     *
     * @param filePath is the path with folder where this file is stored.
     * @param fileName is the name of the file.
     * @return the actual file.
     */
    public static File getFile(String filePath, String fileName) {
        File file = new File(filePath + fileName);
        return file;
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
        File file = getFile(filePath, fileName);
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
    public static File bytesToFile(String filePathDestination, String fileName, byte[] fileData) {
        File file = new File(filePathDestination + fileName);
        try {
            if (file.createNewFile()) {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                for (byte byteOfFileData : fileData) {
                    fileOutputStream.write(byteOfFileData);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not write byte representation of file to actual file.");
            return null;
        }
        return file;
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
     * Check if there are any files stored on the server.
     *
     * @param filePath is the path with folder in which the files should be stored.
     * @return true if any files are stored, false if not.
     */
    public static boolean areFilesStoredOnServer(File filePath) {
        File[] listOfFiles = filePath.listFiles();
        return listOfFiles.length != 0;
    }

    /**
     * Create a list of all available files on the server.
     *
     * @param filePath is the path with folder in which the files are stored.
     * @return the list of filenames.
     */
    public static String createListOfFileNames(File filePath) {
        File[] listOfFiles = filePath.listFiles();
        String listedFiles = "The following files are stored on the server: \n";
        for (File file : listOfFiles) {
            String fileName = file.getName();
            listedFiles = listedFiles + fileName + "\n";
        }
        return listedFiles;
    }

    /**
     * Check if file already exists in the folder of interest.
     *
     * @param fileNameToCheck is the name of the file you want to check.
     * @param filePath        is the path with folder in which the files are stored.
     * @return true if file already exists, false if not.
     */
    public static boolean checkIfFileExists(String fileNameToCheck, File filePath) {
        File[] listOfFiles = filePath.listFiles();
        if (areFilesStoredOnServer(filePath)) {
            for (File file : listOfFiles) {
                String fileName = file.getName();
                if (fileName.equals(fileNameToCheck)) {
                    return true;
                }
            }
        }
        return false;
    }
}
