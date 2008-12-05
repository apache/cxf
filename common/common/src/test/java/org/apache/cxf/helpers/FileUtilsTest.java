package org.apache.cxf.helpers;

import java.io.File;


import org.junit.Assert;
import org.junit.Test;

public class FileUtilsTest extends Assert {
    
    
    @Test
    public void testTempIODirExists() throws Exception {

        try {
            System.setProperty("java.io.tmpdir", "dummy");
            FileUtils.createTempFile("foo", "bar");
        } catch (RuntimeException e) {
            assertTrue(e.toString().contains("please set java.io.tempdir to an existing directory"));
        }    
    }
}
