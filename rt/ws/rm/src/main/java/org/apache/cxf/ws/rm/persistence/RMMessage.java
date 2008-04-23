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

import java.math.BigInteger;


public class RMMessage {
    
    private byte[] content;
    private BigInteger messageNumber;
    private String to;
    
    /**
     * Returns the message number of the message within its sequence.
     * @return the message number
     */
    public BigInteger getMessageNumber() {
        return  messageNumber;
    }
    
    /**
     * Sets the message number of the message within its sequence.
     * @param messageNumber the message number
     */
    public void setMessageNumber(BigInteger mn) {
        messageNumber = mn;
    }
    

    /**
     * Returns the content of the message as an input stream.
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }


    /**
     * Sets the message content as an input stream.
     * @param content the message content
     */
    public void setContent(byte[] c) {
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


    
    
    
    
    
    
}
