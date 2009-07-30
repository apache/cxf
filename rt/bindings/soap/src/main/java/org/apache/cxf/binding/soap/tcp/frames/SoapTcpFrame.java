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

package org.apache.cxf.binding.soap.tcp.frames;


public class SoapTcpFrame {
    private int channelId;
    private SoapTcpFrameHeader header;
    private byte[] payload;
    
    public int getChannelId() {
        return channelId;
    }
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
    public SoapTcpFrameHeader getHeader() {
        return header;
    }
    public void setHeader(SoapTcpFrameHeader header) {
        this.header = header;
    }
    public byte[] getPayload() {
        return payload;
    }
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
    
    
}
