package com.nedap.university;

import com.nedap.university.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


import java.io.File;

/**
 * Test if the removal from a file is being performed correctly.
 */
public class RemoveFileTest {
    File filePath;
    String fileName;
    Server server;

    /**
     * Before this test can be performed, the filepath, filename and server should be created to be able to remove a
     * file and test whether this file is actually being removed.
     */
    @BeforeEach
    public void createFile() {
        String filePath_ExampleFiles = "/Users/arjonne.laar/Documents/module2/FinalProjectM2Arjonne/example_files/";
        filePath = FileProtocol.createFilePath(filePath_ExampleFiles);
        fileName = "medium.pdf";
        server = new Server();
    }

    /**
     * Test whether the file that did exist before the removal action, actually is removed afterwards.
     */
    @Test
    public void testIfFileIsActuallyRemoved() {
        assertTrue(FileProtocol.doesFileExist(fileName, filePath));
        assertTrue(server.isFileRemoved(fileName, filePath));
        assertFalse(FileProtocol.doesFileExist(fileName,filePath));
        assertFalse(server.isFileRemoved(fileName, filePath));
    }
}
