package org.apache.cxf.jaxrs.impl;

import org.junit.Test;

public class ReaderInterceptorMBRTest {
    @Test(expected = IllegalArgumentException.class)
    public void testReaderInterceptorMBRWithNullMessage() {
        new ReaderInterceptorMBR(null, null);
    }

}