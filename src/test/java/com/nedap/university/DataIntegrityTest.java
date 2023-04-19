package com.nedap.university;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to check if the file that is sent is equal to the file that is received.
 */
public class DataIntegrityTest {
    String filePath_example_files;
    String filePath_example_files_backup;
    String pdf;
    String png;
    String txt;

    /**
     * Before each test, two file path names and three actual file names are being created:
     * The path /.../example_files/ includes files that are actually received via transmission by testing the
     * transmission using the Raspberry Pi (by first uploading the files to the Raspberry Pi, then removing the files
     * from this folder and finally downloading these files back to this folder from the Raspberry Pi);
     * The path /.../example_files_backup/ includes source files that are used to test transmission.
     * The file pdf includes a .pdf file;
     * The file png includes a .png file;
     * The file txt includes a .txt file.
     */
    @BeforeEach
    public void setUp() {
        filePath_example_files = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/example_files/";
        filePath_example_files_backup = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/example_files_backup/";
        pdf = "medium.pdf";
        png = "test2085.png";
        txt = "test.txt";
    }

    /**
     * Test whether the hash code of the PDF in the example_files folder is equal to the hash code of the PDF in the
     * example_files_backup folder, to see whether the transmission has resulted in an exact copy of the original file.
     */
    @Test
    public void testHashCodePDF() throws IOException {
        File pdfInExampleFolder = FileProtocol.getFile(filePath_example_files, pdf);
        File pdfInBackupFolder = FileProtocol.getFile(filePath_example_files_backup, pdf);
        long hashCodePdfInExampleFolder = Files.mismatch(pdfInExampleFolder.toPath(), pdfInBackupFolder.toPath());
        assertEquals(-1L, hashCodePdfInExampleFolder);
    }

    /**
     * Test whether the hash code of the PNG in the example_files folder is equal to the hash code of the PNG in the
     * example_files_backup folder, to see whether the transmission has resulted in an exact copy of the original file.
     */
    @Test
    public void testHashCodePNG() throws IOException {
        File pngInExampleFolder = FileProtocol.getFile(filePath_example_files, png);
        File pngInBackupFolder = FileProtocol.getFile(filePath_example_files_backup, png);
        long hashCodePngInExampleFolder = Files.mismatch(pngInExampleFolder.toPath(), pngInBackupFolder.toPath());
        assertEquals(-1L, hashCodePngInExampleFolder);
    }

    /**
     * Test whether the hash code of the TXT in the example_files folder is equal to the hash code of the TXT in the
     * example_files_backup folder, to see whether the transmission has resulted in an exact copy of the original file.
     */
    @Test
    public void testHashCodeTXT() throws IOException {
        File txtInExampleFolder = FileProtocol.getFile(filePath_example_files, txt);
        File txtInBackupFolder = FileProtocol.getFile(filePath_example_files_backup, txt);
        long hashCodeTxtInExampleFolder = Files.mismatch(txtInExampleFolder.toPath(), txtInBackupFolder.toPath());
        assertEquals(-1L, hashCodeTxtInExampleFolder);
    }

    /**
     * Test whether the method .getFileSize actually returns the correct file size of the transmitted file, by checking
     * whether it is the same as the file size of the original file.
     */
    @Test
    public void testFileSize() {
        int pdfSize = 475231;
        int getSize = FileProtocol.getFileSize(filePath_example_files, pdf);
        assertEquals(pdfSize, getSize);
    }

    /**
     * Test whether the checksum that is calculated over the byte representation of the total file is equal for both
     * the file in the examples_files folder and the one in the examples_files_backup folder, and thus, if checksum
     * calculation and checking was performed correctly.
     */
    @Test
    public void testChecksumTotalFile() {
        byte[] pdfInExampleFolderInBytes = FileProtocol.fileToBytes(filePath_example_files, pdf);
        byte[] pdfInBackupFolderInBytes = FileProtocol.fileToBytes(filePath_example_files_backup, pdf);
        int checksumPdfInExampleFolder = DataIntegrityCheck.calculateChecksum(pdfInExampleFolderInBytes);
        int checksumPdfInBackupFolder = DataIntegrityCheck.calculateChecksum(pdfInBackupFolderInBytes);
        assertTrue(DataIntegrityCheck.areChecksumOfTwoFilesTheSame(checksumPdfInExampleFolder, checksumPdfInBackupFolder));
    }
}
