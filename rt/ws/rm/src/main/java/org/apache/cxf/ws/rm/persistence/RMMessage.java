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
package org.apache.cxf.ws.rm.persistence;

import java.io.IOException;
import java.io.InputStream;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;

public class RMMessage {
    
    private CachedOutputStream content;
    private long messageNumber;
    private String to;
    
    /**
     * Returns the message number of the message within its sequence.
     * @return the message number
     */
    public long getMessageNumber() {
        return  messageNumber;
    }
    
    /**
     * Sets the message number of the message within its sequence.
     * @param messageNumber the message number
     */
    public void setMessageNumber(long mn) {
        messageNumber = mn;
    }
    

    /**
     * Returns the content of the message as an input stream.
     * @return the content
     * @deprecated
     */
    public byte[] getContent() {
        byte[] bytes = null;
        try {
            bytes = content != null ? content.getBytes() : null;
        } catch (IOException e) {
            // ignore and treat it as null
        }
        return bytes;
    }


    /**
     * Sets the message content as an input stream.
     * @param content the message content
     * @deprecated
     */
    public void setContent(byte[] c) {
        content = new CachedOutputStream();
        content.holdTempFile();
        try {
            content.write(c);
        } catch (IOException e) {
            // ignore
        }
    }
    
    /**
     * Sets the message content using the input stream.
     * @param in
     * @throws IOException
     */
    public void setContent(InputStream in) throws IOException {
        content = new CachedOutputStream();
        content.holdTempFile();
        IOUtils.copy(in, content);
    }

    /**
     * Sets the message content using the cached output stream.
     * @param c
     */
    public void setContent(CachedOutputStream c) {
        content = c;
    }
    
    /**
     * Returns the to address of this message.
     * @return the to address
     */
    public String getTo() {
        return to;
    }
    
    
    /**
     * Sets the to address of this message.
     * @param t the to address
     */
    public void setTo(String t) {
        to = t;
    }

    /**
     * Returns the input stream of this message content.
     * @return
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        return content != null ? content.getInputStream() : null;
    }
    
    /**
     * Returns the associated cached output stream.
     * @return
     */
    public CachedOutputStream getCachedOutputStream() {
        return content;
    }
    
    /**
     * Returns the length of the message content in bytes.
     * 
     * @return
     */
    public int getSize() {
        return content != null ? content.size() : -1;
    }
}
