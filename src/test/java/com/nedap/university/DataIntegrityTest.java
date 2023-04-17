package com.nedap.university;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class DataIntegrityTest {
    String filePath_example_files;
    String filePath_example_files_backup;
    String pdf;
    String png;
    String txt;

    @BeforeEach
    public void setUp() {
        filePath_example_files = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/example_files/";
        filePath_example_files_backup = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/example_files_backup/";
        pdf = "medium.pdf";
        png = "test2085.png";
        txt = "test.txt";
    }

    @Test
    public void testHashCodePDF() {
        File pdfInExampleFolder = FileProtocol.getFile(filePath_example_files, pdf);
        File pdfInBackupFolder = FileProtocol.getFile(filePath_example_files_backup, pdf);
        int hashCodePdfInExampleFolder = pdfInExampleFolder.hashCode();
        int hashCodePdfInBackupFolder = pdfInBackupFolder.hashCode();
        assertEquals(hashCodePdfInExampleFolder, hashCodePdfInBackupFolder);
    }

    @Test
    public void testHashCodePNG() throws IOException {
        File pngInExampleFolder = FileProtocol.getFile(filePath_example_files, png);
        File pngInBackupFolder = FileProtocol.getFile(filePath_example_files_backup, png);
        long hashCodePdfInExampleFolder = Files.mismatch(pngInExampleFolder.toPath(), pngInBackupFolder.toPath());
        assertEquals(-1L, hashCodePdfInExampleFolder);
    }

    @Test
    public void testHashCodeTXT() {
        File txtInExampleFolder = FileProtocol.getFile(filePath_example_files, txt);
        File txtInBackupFolder = FileProtocol.getFile(filePath_example_files_backup, txt);
        int hashCodePdfInExampleFolder = txtInExampleFolder.hashCode();
        int hashCodePdfInBackupFolder = txtInBackupFolder.hashCode();
        assertEquals(hashCodePdfInExampleFolder, hashCodePdfInBackupFolder);
    }
}
