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

package org.apache.cxf.binding.soap.tcp;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrame;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrameHeader;

public class SoapTcpChannel {
    private int channelId;
    private String wsURI;
    private List<SoapTcpFrame> frames;
    
    public SoapTcpChannel(int channelId, String wsURI) {
        this.channelId = channelId;
        this.wsURI = wsURI;
        frames = new ArrayList<SoapTcpFrame>();
    }
    
    public boolean addFrame(SoapTcpFrame frame) {
        if (frame != null && frame.getChannelId() == channelId) {
            if (frame.getHeader().getFrameType() == SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE) {
                frames.clear();
            }
            frames.add(frame);
            return true;
        }
        return false;
    }
    
    public List<SoapTcpFrame> getFrames() {
        return frames;
    }
    
    public void clearFrameBuffer() {
        frames.clear();
    }


    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getWsURI() {
        return wsURI;
    }

    public void setWsURI(String wsURI) {
        this.wsURI = wsURI;
    }
}
