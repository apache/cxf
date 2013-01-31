/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Test;

public class CachedOutputStreamTest extends Assert {    
    
    @Test
    public void testResetOut() throws IOException {
        CachedOutputStream cos = new CachedOutputStream();        
        String result = initTestData(16);
        cos.write(result.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cos.resetOut(out, true);
        String test = out.toString();        
        assertEquals("The test stream content isn't same ", test , result);
        cos.close();
    }
    
    @Test
    public void testDeleteTmpFile() throws IOException {
        CachedOutputStream cos = new CachedOutputStream();        
        //ensure output data size larger then 64k which will generate tmp file
        String result = initTestData(65);
        cos.write(result.getBytes());
        //assert tmp file is generated
        File tempFile = cos.getTempFile();
        assertNotNull(tempFile);
        assertTrue(tempFile.exists());
        cos.close();
        //assert tmp file is deleted after close the CachedOutputStream
        assertFalse(tempFile.exists());
    }

    @Test
    public void testDeleteTmpFile2() throws IOException {
        CachedOutputStream cos = new CachedOutputStream();        
        //ensure output data size larger then 64k which will generate tmp file
        String result = initTestData(65);
        cos.write(result.getBytes());
        //assert tmp file is generated
        File tempFile = cos.getTempFile();
        assertNotNull(tempFile);
        assertTrue(tempFile.exists());
        InputStream in = cos.getInputStream();
        cos.close();
        //assert tmp file is not deleted when the input stream is open
        assertTrue(tempFile.exists());
        in.close();
        //assert tmp file is deleted after the input stream is closed
        assertFalse(tempFile.exists());
    }

    @Test
    public void testEncryptAndDecryptWithDeleteOnClose() throws IOException {
        CachedOutputStream cos = new CachedOutputStream();
        cos.setThreshold(4);
        // need a 8-bit cipher so that all bytes are flushed when the stream is flushed.
        cos.setCipherTransformation("RC4");
        
        final String text = "Hello Secret World!";
        cos.write(text.getBytes("UTF-8"));
        cos.flush();
        
        File tmpfile = cos.getTempFile();
        assertNotNull(tmpfile);
        
        final String enctext = readFromStream(new FileInputStream(tmpfile));
        assertFalse("text is not encoded", text.equals(enctext));

        InputStream fin = cos.getInputStream();

        assertTrue("file is deleted", tmpfile.exists());
        
        final String dectext = readFromStream(fin);
        assertEquals("text is not decoded correctly", text, dectext);

        // the file is deleted when cos is closed while all the associated inputs are closed
        assertTrue("file is deleted", tmpfile.exists());
        cos.close();
        assertFalse("file is not deleted", tmpfile.exists());
    }

    @Test
    public void testEncryptAndDecryptWithDeleteOnInClose() throws IOException {
        CachedOutputStream cos = new CachedOutputStream();
        cos.setThreshold(4);
        // need a 8-bit cipher so that all bytes are flushed when the stream is flushed.
        cos.setCipherTransformation("RC4");
        
        final String text = "Hello Secret World!";
        cos.write(text.getBytes("UTF-8"));
        cos.flush();
        
        File tmpfile = cos.getTempFile();
        assertNotNull(tmpfile);
        
        final String enctext = readFromStream(new FileInputStream(tmpfile));
        assertFalse("text is not encoded", text.equals(enctext));

        InputStream fin = cos.getInputStream();

        cos.close();
        assertTrue("file is deleted", tmpfile.exists());
        
        // the file is deleted when cos is closed while all the associated inputs are closed
        final String dectext = readFromStream(fin);
        assertEquals("text is not decoded correctly", text, dectext);
        assertFalse("file is not deleted", tmpfile.exists());
    }

    @Test
    public void testUseBusProps() throws Exception {
        Bus oldbus = BusFactory.getThreadDefaultBus(false); 
        try {
            CachedOutputStream cos = new CachedOutputStream(64);
            cos.write("Hello World!".getBytes());
            cos.flush();
            assertNull("expects no tmp file", cos.getTempFile());
            cos.close();
            
            IMocksControl control = EasyMock.createControl();
            
            Bus b = control.createMock(Bus.class);
            EasyMock.expect(b.getProperty("bus.io.CachedOutputStream.Threshold")).andReturn("4");
            EasyMock.expect(b.getProperty("bus.io.CachedOutputStream.MaxSize")).andReturn(null);
            EasyMock.expect(b.getProperty("bus.io.CachedOutputStream.CipherTransformation")).andReturn(null);
        
            BusFactory.setThreadDefaultBus(b);
            
            control.replay();

            cos = new CachedOutputStream();
            cos.write("Hello World!".getBytes());
            cos.flush();
            assertNotNull("expects a tmp file", cos.getTempFile());
            cos.close();
            
            control.verify();
        } finally {
            BusFactory.setThreadDefaultBus(oldbus);
        }
    }
    
    private static String readFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            byte[] b = new byte[100];
            for (;;) {
                int n = is.read(b, 0, b.length);
                if (n < 0) {
                    break;
                }
                buf.write(b, 0, n);
            }
        } finally {
            is.close();
        }
        return new String(buf.toByteArray(), "UTF-8");
    }
    
    String initTestData(int packetSize) {
        String temp = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+?><[]/0123456789";
        String result = new String();
        for (int i = 0; i <  1024 * packetSize / temp.length(); i++) {
            result = result + temp;
        }
        return result;
    }
    

}
    
   
