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
package org.apache.cxf.transport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.cxf.io.CachedOutputStream;
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
    
    String initTestData(int packetSize) {
        String temp = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+?><[]/0123456789";
        String result = new String();
        for (int i = 0; i <  1024 * packetSize / temp.length(); i++) {
            result = result + temp;
        }
        return result;
    }
    

}
    
   
