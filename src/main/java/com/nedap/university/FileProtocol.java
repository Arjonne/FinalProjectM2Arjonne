package com.nedap.university;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class FileProtocol {

    /**
     * Create a byte array (which is the input for a Datagram Packet) from the file that needs to be sent.
     *
     * @param fileName is the name of the file that needs to be sent.
     * @return the byte representation of the file in an array.
     */
    public byte[] fileToPacket(String fileName) {
        File file = new File(fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileInByteArray = new byte[(int) file.length()];
        fileInputStream.read(fileInByteArray);
        return fileInByteArray;
    }

    /**
     * Create a file from the byte array that is sent.
     *
     * @param fileName is the name of the transmitted file.
     * @param fileData is the data of the file in bytes.
     * @return the complete file.
     */
    public File packetToFile(String fileName, byte[] fileData) {
        File file = new File(fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        //todo in case of creating total file with header, change offset and length in next function:
        File completeFile = fileOutputStream.write(fileData);
        return completeFile;
    }

    /**
     * Create a list of all available files on the server.
     *
     * @param filePath is the path with folder in which the files are stored.
     * @return the list of filenames.
     */
    public List<String> createListOfFileNames(File filePath) {
        File[] listOfFiles = filePath.listFiles();
        List<String> listOfFileNames = new ArrayList<>();
        if (listOfFiles.length == 0) {
            String message = "No files available on the server.";
        } else {
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
    public boolean checkIfFileExists(String fileNameToCheck, File filePath) {
        List<String> listOfFileNames = new ArrayList<>();
        listOfFileNames = createListOfFileNames(filePath);
        for (String fileName : listOfFileNames) {
            if (fileName.equals(fileNameToCheck)) {
                return true;
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
    public byte[] createByteArrayOfFileNameList(List<String> listOfFileNames) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(out);
        outputStream.writeObject(listOfFileNames);
        outputStream.close();
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
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOfFileNameList));
        List<String> listOfFileNames = inputStream.readObject();
        return listOfFileNames;
    }
}
