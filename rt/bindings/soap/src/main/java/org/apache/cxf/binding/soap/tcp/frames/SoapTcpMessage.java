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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.cxf.binding.soap.tcp.DataCodingUtils;
import org.apache.cxf.binding.soap.tcp.SoapTcpOutputStream;

public final class SoapTcpMessage {
    private List<SoapTcpFrame> frames;
    
    private SoapTcpMessage() {
        frames = new ArrayList<SoapTcpFrame>();
    }
    
    public static SoapTcpMessage createSoapTcpMessage(SoapTcpFrame frame) {
        SoapTcpMessage soapTcpMessage = new SoapTcpMessage();
        soapTcpMessage.getFrames().add(frame);
        return soapTcpMessage;
    }
    
    public static SoapTcpMessage createSoapTcpMessage(List<SoapTcpFrame> frames) {
        SoapTcpMessage soapTcpMessage = new SoapTcpMessage();
        soapTcpMessage.getFrames().addAll(frames);
        return soapTcpMessage;
    }
    
    public static SoapTcpMessage createSoapTcpMessage(String message, int channelId) {
        SoapTcpMessage soapTcpMessage = new SoapTcpMessage();
        try {
            byte[] msgContent = message.getBytes("UTF-8");
            int numOfFrames = (int)Math.ceil((float)msgContent.length
                                             / (float)SoapTcpOutputStream.CHUNK_SIZE);
            if (numOfFrames > 1) {
                int offset = 0;
                byte[] payload = new byte[SoapTcpOutputStream.CHUNK_SIZE];
                for (int i = 1; i <= numOfFrames; i++) {
                    if (i == numOfFrames) {
                        payload = new byte[msgContent.length % SoapTcpOutputStream.CHUNK_SIZE];
                    }
                    
                    for (int j  = 0; j < payload.length; j++) {
                        payload[j] = msgContent[offset + j];
                    }
                    
                    SoapTcpFrame frame = null;
                    if (i == 1) {
                        frame = createSoapTcpFrame(SoapTcpFrameHeader.MESSAGE_START_CHUNK,
                                                   payload, channelId);
                    } else if (i < numOfFrames) {
                        frame = createSoapTcpFrame(SoapTcpFrameHeader.MESSAGE_CHUNK, payload, channelId);
                    } else {
                        frame = createSoapTcpFrame(SoapTcpFrameHeader.MESSAGE_END_CHUNK, payload, channelId);
                    }
                    
                    soapTcpMessage.frames.add(frame);
                    offset += SoapTcpOutputStream.CHUNK_SIZE;
                }
                
            } else {
                soapTcpMessage.frames.
                    add(createSoapTcpFrame(SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE, msgContent, channelId));
            }
            return soapTcpMessage;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static SoapTcpMessage createErrorMessage(int code, int subCode, String description,
                                                    int channelId) {
        SoapTcpMessage soapTcpMessage = new SoapTcpMessage();
        SoapTcpFrame frame = new SoapTcpFrame();
        SoapTcpFrameHeader header = new SoapTcpFrameHeader();
        header.setChannelId(channelId);
        header.setFrameType(SoapTcpFrameHeader.ERROR_MESSAGE);
        frame.setHeader(header);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            DataCodingUtils.writeInts4(baos, code, subCode);
            byte[] strByteArray = description.getBytes("UTF-8");
            DataCodingUtils.writeInt8(baos, strByteArray.length);
            baos.write(strByteArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        frame.setPayload(baos.toByteArray());
        soapTcpMessage.getFrames().add(frame);
        
        return soapTcpMessage;
    }
    
    public void setChannelId(int channelId) {
        for (SoapTcpFrame frame : frames) {
            frame.setChannelId(channelId);
        }
    }
    
    public int getChannelId() {
        if (frames.size() > 0) {
            return frames.get(0).getChannelId();
        }
        return -1;
    }
    
    public void setFrames(List<SoapTcpFrame> frames) {
        this.frames = frames;
    }
    
    public List<SoapTcpFrame> getFrames() {
        return frames;
    }

    public String getContent() {
        StringBuffer result = new StringBuffer();

        try {
            for (SoapTcpFrame frame : frames) {
                result.append(new String(frame.getPayload(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return result.toString();
    }
    
    public InputStream getContentAsStream() {
        int buffLength = 0;
        for (SoapTcpFrame frame : frames) {
            buffLength += frame.getPayload().length;
        }
        byte buffer[] = new byte[buffLength];
        int index = 0;
        byte payload[] = null;
        for (SoapTcpFrame frame : frames) {
            payload = frame.getPayload();
            for (int i = 0; i < payload.length; i++) {
                buffer[index] = payload[i];
                index++;
            }
        }
        return new ByteArrayInputStream(buffer);
    }
    
    private static SoapTcpFrame createSoapTcpFrame(int frameType, byte[] payload, int channelId) {
        SoapTcpFrame frame = new SoapTcpFrame();
        SoapTcpFrameHeader header = new SoapTcpFrameHeader();
        SoapTcpFrameContentDescription contentDesc = null;
        if (frameType == SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE
            || frameType == SoapTcpFrameHeader.MESSAGE_START_CHUNK) {
            contentDesc = new SoapTcpFrameContentDescription();
            contentDesc.setContentId(0);
            
            final Map<Integer, String> parameters = new Hashtable<Integer, String>();
            parameters.put(0, "utf-8");
            
            contentDesc.setParameters(parameters);
        }
        header.setChannelId(channelId);
        header.setFrameType(frameType);
        header.setContentDescription(contentDesc);
        frame.setHeader(header);
        frame.setPayload(payload);
        return frame;
    }
}
