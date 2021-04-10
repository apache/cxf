package org.apache.cxf.attachment;

import java.util.Collections;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CustomDataSourceTest {

    private static final String firstID = "id1";
    private static final String secondID = "id2";
	private static final String thirdID = "id3";

    @Test
    public void testNoDataSource() throws Exception {
        DataSource ds = new CustomDataSource(firstID,
                Collections.singleton(new AttachmentImpl(firstID, new DataHandler((DataSource) null) {
                    @Override
                    public DataSource getDataSource() {
                        return null;
                    }
                })));
        try {
            ds.getNamehistory();
            fail();
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains(firstID));
        }
    }
	


    @Test
    public void testNoAttachment() throws Exception {
        DataSource ds = new CustomDataSource(firstID, Collections.singleton(new AttachmentImpl(secondID)));
        try {
            ds.getName();
            fail();
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains(firstID));
            assertTrue(message, message.contains(secondID));
        }
    }

}

public void testNoDataSource() throws Exception {
        DataSource ds = new CustomDataSource(thirdID,
                Collections.singleton(new AttachmentImpl(firstID, new DataHandler((DataSource) null) {
                    @Override
                    public DataSource getDataSource() {
                        return null;
                    }
                })));
        try {
            ds.getIdhistory();
            fail();
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains(firstID));
        }
    }