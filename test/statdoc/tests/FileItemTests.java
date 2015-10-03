package statdoc.tests;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import statdoc.items.FileItem;
import statdoc.items.StatdocItemHub;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class FileItemTests {

    @Test
    public void relativeFileSystemLinkTest() {
        
        Path file = FileSystems.getDefault().getPath("test/statdoc/tests/testfile.do");
        Path rootDir = FileSystems.getDefault().getPath("test/outputdir");

        StatdocItemHub hub = new StatdocItemHub();
        hub.sourceDir = rootDir;
        
        FileItem fi = hub.createFile(file, "none");
        
        System.out.println( "filelink: " + fi.getFileLink() );
        
        assertEquals("../statdoc/tests/testfile.do", fi.getFileLink() );
    }


    @Test
    public void absoluteFileSystemLinkTest() {
        
        Path file = FileSystems.getDefault().getPath("test/statdoc/tests/testfile.do");
        Path rootDir = FileSystems.getDefault().getPath("xxx/outputdir");

        StatdocItemHub hub = new StatdocItemHub();
        hub.sourceDir = rootDir;
        
        FileItem fi = hub.createFile(file, "none");
        
        System.out.println( "filelink: " + fi.getFileLink() );
        
        assertEquals("../../test/statdoc/tests/testfile.do", fi.getFileLink() );
    }

    

    @Test
    public void linkTest() {
        
        Path file = FileSystems.getDefault().getPath("test/statdoc/tests/testfile.do");
        Path rootDir = FileSystems.getDefault().getPath("test/statdoc");

        StatdocItemHub hub = new StatdocItemHub();
        hub.sourceDir = rootDir;
        
        FileItem fi = hub.createFile(file, "none");
        
        System.out.println( "link: " + fi.getLink() );
        
        assertEquals("files/tests_testfile.do.html", fi.getLink() );
    }


    @Test
    public void nameTest() {
        
        Path file = FileSystems.getDefault().getPath("test/statdoc/tests/testfile.do");
        Path rootDir = FileSystems.getDefault().getPath("test/statdoc");

        StatdocItemHub hub = new StatdocItemHub();
        hub.sourceDir = rootDir;
        
        FileItem fi = hub.createFile(file, "none");
        
        System.out.println( "name: " + fi.getName() );
        
        assertEquals("testfile.do", fi.getName() );
    }


    @Test
    public void fullNameTest() {
        
        Path file = FileSystems.getDefault().getPath("test/statdoc/tests/testfile.do");
        Path rootDir = FileSystems.getDefault().getPath("test/statdoc");

        StatdocItemHub hub = new StatdocItemHub();
        hub.sourceDir = rootDir;
        
        FileItem fi = hub.createFile(file, "none");
        
        System.out.println( "fullname: " + fi.getFullName() );
        
        assertEquals("tests/testfile.do", fi.getFullName() );
    }
}
