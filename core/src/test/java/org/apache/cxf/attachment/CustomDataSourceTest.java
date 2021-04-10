package org.apache.cxf.attachment;

import java.util.Collections;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CustomDataSourceTest {
    public void testGetName() {
        DataSource ds = new CustomDataSource("esa","2t5");
        assertEquals("esa",ds.getName());
    }

    public void testSetName() {
        DataSource ds = new CustomDataSource("esa","2t5");
        ds.setName("Pekka")
        assertEquals("Pekka",ds.getName());
    }
}
