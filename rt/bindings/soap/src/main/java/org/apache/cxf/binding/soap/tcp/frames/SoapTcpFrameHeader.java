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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.cxf.binding.soap.tcp.DataCodingUtils;

public class SoapTcpFrameHeader {

    //Message Frame Types
    public static final int SINGLE_FRAME_MESSAGE = 0;
    public static final int MESSAGE_START_CHUNK = 1;
    public static final int MESSAGE_CHUNK = 2;
    public static final int MESSAGE_END_CHUNK = 3;
    public static final int ERROR_MESSAGE = 4;
    public static final int NULL_MESSAGE = 5;
    
    private int channelId;
    private int frameType;
    private SoapTcpFrameContentDescription contentDescription;
    
    public SoapTcpFrameHeader(final int frameType, final SoapTcpFrameContentDescription contentDescription) {
        this.frameType = frameType;
        this.contentDescription = contentDescription;
    }
    
    public SoapTcpFrameHeader() {
        this.frameType = NULL_MESSAGE;
        this.contentDescription = null;
    }
    
    public int getChannelId() {
        return channelId;
    }
    
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
    
    public int getFrameType() {
        return frameType;
    }
    
    public void setFrameType(int frameType) {
        this.frameType = frameType;
    }

    public SoapTcpFrameContentDescription getContentDescription() {
        return contentDescription;
    }

    public void setContentDescription(SoapTcpFrameContentDescription contentDescription) {
        this.contentDescription = contentDescription;
    }
    
    public void write(final OutputStream output) throws IOException {
        DataCodingUtils.writeInts4(output, channelId, frameType);
        if ((frameType == SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE
            || frameType == SoapTcpFrameHeader.MESSAGE_START_CHUNK) && contentDescription != null) {
            contentDescription.write(output);
        }
    }
}
