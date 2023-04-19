package com.nedap.university;

import java.io.*;

/**
 * Represents the protocol for creating packet input from files and the other way around for the transmission between
 * client and server.
 */
public final class FileProtocol {
    // filePath for sending files using localHost:
    public static final String CLIENT_FILEPATH = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/example_files/";
//    public static final String SERVER_FILEPATH = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/localserver/";
    public static final String SERVER_FILEPATH = "/home/pi/Files/";

    /**
     * Get the actual file.
     *
     * @param filePath is the path with folder where this file is stored.
     * @param fileName is the name of the file.
     * @return the actual file.
     */
    public static File getFile(String filePath, String fileName) {
        return new File(filePath + fileName);
    }

    /**
     * Create an actual file path to be able to find stored files in a specific folder.
     *
     * @param filePathString is the string representation of the file path.
     * @return the actual file path in which files can be found.
     */
    public static File createFilePath(String filePathString) {
        return new File(filePathString);
    }

    /**
     * Create the byte representation of a file.
     *
     * @param filePath is the path where the file of interest is stored.
     * @param fileName is the name of the file of interest.
     * @return the byte representation of the file.
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
            fileInputStream.close();
            return fileInByteArray;
        } catch (IOException e) {
            System.out.println("Could not copy byte representation of file into a new byte array.");
            return null;
        }
    }

    /**
     * Create a file from the byte array that is sent.
     *
     * @param filePathDestination is the file path where the file needs to be stored.
     * @param fileName            is the name of the transmitted file.
     * @param fileData            is the data of the file in bytes.
     * @return the actual file.
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
     * @param filePath is the path where the file of interest is stored.
     * @param fileName is the name of the file of interest.
     * @return the size of the file of interest.
     */
    public static int getFileSize(String filePath, String fileName) {
        byte[] fileToSendInBytes = fileToBytes(filePath, fileName);
        if (fileToSendInBytes != null) {
            return fileToSendInBytes.length;
        } else {
            return -1;
        }
    }

    /**
     * Check if there are any files stored in the folder of interest.
     *
     * @param filePath is the path with folder in which files should be stored.
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
     * @return the list of filenames, or null if no files are stored.
     */
    public static String createListOfFileNames(File filePath) {
        File[] listOfFiles = filePath.listFiles();
        if (areFilesStoredOnServer(filePath)) {
        String listedFiles = "\nThe following files are stored on the server: \n";
            for (File file : listOfFiles) {
                String fileName = file.getName();
                listedFiles = listedFiles + fileName + "\n";
            }
            return listedFiles;
        } else {
            String listedFiles = "\nNo files to show: there are stored on the server yet. \n";
            return listedFiles;
        }
    }

    /**
     * Check if file already exists in the folder of interest.
     *
     * @param fileNameToCheck is the name of the file you want to check.
     * @param filePath        is the path with folder in which the files are stored.
     * @return true if file already exists, false if not.
     */
    public static boolean doesFileExist(String fileNameToCheck, File filePath) {
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
