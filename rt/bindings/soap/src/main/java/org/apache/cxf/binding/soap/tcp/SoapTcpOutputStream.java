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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrame;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrameContentDescription;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrameHeader;
import org.apache.cxf.io.AbstractThresholdOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.MessageObserver;

/**
 * SoapTCPOutPutStream is OutputStream for sending message in SOAP/TCP protocol.
 * It sends single message in one or more SOAP/TCP frames.
 */
public class SoapTcpOutputStream extends AbstractThresholdOutputStream {
    public static final int CHUNK_SIZE = 4096;

    private int channelId;
    private OutputStream outStream;
    private InputStream inStream;
    private boolean messageSent;
    private Message outMessage;
    private int chunkSize;
    
    private MessageObserver incomingObserver;

    public SoapTcpOutputStream(final InputStream inStream, final OutputStream outStream,
                               final Message message, final String targetWsURI,
                               final MessageObserver incomingObserver) {
        this(inStream, outStream, message, targetWsURI, incomingObserver, CHUNK_SIZE);
    }
    
    public SoapTcpOutputStream(final InputStream inStream, final OutputStream outStream,
                               final Message message, final String targetWsURI,
                               final MessageObserver incomingObserver, final int chunkSize) {
        super(chunkSize);
        this.messageSent = false;
        this.inStream = inStream;
        this.outStream = outStream;
        this.outMessage = message;
        this.wrappedStream = null;
        this.chunkSize = chunkSize;
        this.incomingObserver = incomingObserver;
        
        final List<String> mimeTypes = new ArrayList<String>();
        
        SoapMessage m = (SoapMessage)message;
        
        //mimeTypes.add("application/vnd.sun.stateful.fastinfoset");
        mimeTypes.add(m.getVersion().getContentType());
        //mimeTypes.add("multipart/related");
        
        
        final List<String> supportedParams = new ArrayList<String>();
        supportedParams.add("charset");
        if (m.getVersion() == Soap11.getInstance()) {
            supportedParams.add("SOAPAction");
        } else {
            supportedParams.add("action");
        }
        
        try {
            channelId = openChannel(targetWsURI, mimeTypes, supportedParams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private int openChannel(final String targetWsURI, final List<String> supportedMimeTypes,
                            final List<String> supportedParams) throws IOException {
        
        String openChannelMsg = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<s:Body><openChannel xmlns=\"http://servicechannel.tcp.transport.ws.xml.sun.com/\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">";
        openChannelMsg += "<targetWSURI xmlns=\"\">" + targetWsURI + "</targetWSURI>";
        
        for (String mimeType : supportedMimeTypes) {
            openChannelMsg += "<negotiatedMimeTypes xmlns=\"\">" + mimeType + "</negotiatedMimeTypes>";
        }
        for (String param : supportedParams) {
            openChannelMsg += "<negotiatedParams xmlns=\"\">" + param + "</negotiatedParams>";
        }

        openChannelMsg += "</openChannel></s:Body></s:Envelope>";
        
        SoapTcpFrameContentDescription contentDesc = new SoapTcpFrameContentDescription();
        contentDesc.setContentId(0);
        
        final Map<Integer, String> parameters = new Hashtable<Integer, String>();
        parameters.put(0, "utf-8");
        
        contentDesc.setParameters(parameters);
        
        final SoapTcpFrameHeader header =
            new SoapTcpFrameHeader(SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE, contentDesc);
        final SoapTcpFrame frame = new SoapTcpFrame();
        frame.setChannelId(0);
        frame.setHeader(header);
        try {
            frame.setPayload(openChannelMsg.getBytes("UTF-8"));
            SoapTcpUtils.writeMessageFrame(outStream, frame);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        final SoapTcpFrame response = SoapTcpUtils.readMessageFrame(inStream);
        if (!SoapTcpUtils.checkSingleFrameResponse(response, "openChannelResponse")) {
            throw new IOException("Couldn't open new channel.");
        }
        //SoapTcpUtils.printSoapTcpFrame(System.out, response);
        
        return getChannelIdFromResponse(response);
    }
    
    private int getChannelIdFromResponse(final SoapTcpFrame frame) {
        return ChannelIdParser.getChannelId(new ByteArrayInputStream(frame.getPayload()));
    }

    @Override
    public void thresholdNotReached() throws IOException {
        //Send single message if didn't send any message yet or end message if already send message
        if (messageSent) {
            SoapTcpFrameHeader header = new SoapTcpFrameHeader(SoapTcpFrameHeader.MESSAGE_END_CHUNK, null);
            header.setChannelId(channelId);
            SoapTcpFrame frame = new SoapTcpFrame();
            frame.setChannelId(channelId);
            frame.setHeader(header);
            frame.setPayload(this.buffer.toByteArray());
            SoapTcpUtils.writeMessageFrame(outStream, frame);
        } else {
            final SoapTcpFrameContentDescription contentDesc = new SoapTcpFrameContentDescription();
            contentDesc.setContentId(0);
            
            final Map<Integer, String> parameters = new Hashtable<Integer, String>();
            parameters.put(0, "utf-8");
            
            contentDesc.setParameters(parameters);
            
            final SoapTcpFrameHeader header =
                new SoapTcpFrameHeader(SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE, contentDesc);
            header.setChannelId(channelId);
            final SoapTcpFrame frame = new SoapTcpFrame();
            frame.setChannelId(channelId);
            frame.setHeader(header);
            frame.setPayload(this.buffer.toByteArray());
            SoapTcpUtils.writeMessageFrame(outStream, frame);
            messageSent = true;
        }
    }

    @Override
    public void thresholdReached() throws IOException {
        //Send start-chunk message if didn't send any message yet or message chunk if already send message
        if (messageSent) {
            SoapTcpFrameHeader header = new SoapTcpFrameHeader(SoapTcpFrameHeader.MESSAGE_CHUNK, null);
            header.setChannelId(channelId);
            SoapTcpFrame frame = new SoapTcpFrame();
            frame.setChannelId(channelId);
            frame.setHeader(header);
            frame.setPayload(this.buffer.toByteArray());
            SoapTcpUtils.writeMessageFrame(outStream, frame);
        } else {
            SoapTcpFrameContentDescription contentDesc = new SoapTcpFrameContentDescription();
            contentDesc.setContentId(0);
            
            Map<Integer, String> parameters = new Hashtable<Integer, String>();
            parameters.put(0, "utf-8");
            
            contentDesc.setParameters(parameters);
            
            SoapTcpFrameHeader header =
                new SoapTcpFrameHeader(SoapTcpFrameHeader.MESSAGE_START_CHUNK, contentDesc);
            header.setChannelId(channelId);
            SoapTcpFrame frame = new SoapTcpFrame();
            frame.setChannelId(channelId);
            frame.setHeader(header);
            frame.setPayload(this.buffer.toByteArray());
            SoapTcpUtils.writeMessageFrame(outStream, frame);
            messageSent = true;
        }
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if (messageSent) {
            InputStream inputStream = getResponse();
            Exchange exchange = outMessage.getExchange();
            Message inMessage = new MessageImpl();
            inMessage.setExchange(exchange);
            inMessage.setContent(InputStream.class, inputStream);
            
            incomingObserver.onMessage(inMessage);
        }
    }

    @Override
    protected void onFirstWrite() throws IOException {
        
    }
    
    private InputStream getResponse() {
        SoapTcpFrame responseMessage = null;
        try {
            responseMessage = SoapTcpUtils.readMessageFrame(inStream);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        if (responseMessage != null) {
            int frameType = responseMessage.getHeader().getFrameType();
            if (frameType == SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE
                || frameType == SoapTcpFrameHeader.ERROR_MESSAGE
                || frameType == SoapTcpFrameHeader.NULL_MESSAGE) {
                return new ByteArrayInputStream(responseMessage.getPayload());
            } else if (frameType == SoapTcpFrameHeader.MESSAGE_START_CHUNK) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(4 * chunkSize);
                try {
                    baos.write(responseMessage.getPayload());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                while (frameType != SoapTcpFrameHeader.MESSAGE_END_CHUNK) {
                    try {
                        SoapTcpFrame frame = SoapTcpUtils.readMessageFrame(inStream);
                        baos.write(frame.getPayload());
                    } catch (IOException e) {
                        break;
                    }
                }
                return new ByteArrayInputStream(baos.toByteArray());
            }
        }
        
        return null;
    }

}
