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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrame;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrameHeader;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpMessage;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class SoapTcpMessageDecoder extends ProtocolDecoderAdapter { //CumulativeProtocolDecoder {

    @SuppressWarnings("unchecked")
    public void decode(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out)
        throws Exception {

        byte[] tempBuffer = (byte[])session.getAttribute("tempBuffer");
        Integer bufferPosition = (Integer)session.getAttribute("bufferPosition");
        Integer bufferDataLength = (Integer)session.getAttribute("bufferDataLength");
        if (tempBuffer == null) {
            tempBuffer = new byte[SoapTcpOutputStream.CHUNK_SIZE];
            bufferDataLength = buffer.limit();
            for (bufferPosition = new Integer(0); bufferPosition < bufferDataLength; bufferPosition++) {
                tempBuffer[bufferPosition] = buffer.get();
            }
            session.setAttribute("tempBuffer", tempBuffer);
            session.setAttribute("bufferPosition", bufferPosition);
            session.setAttribute("bufferDataLength", bufferDataLength);
        } else {
            bufferDataLength += buffer.limit();
            for (; bufferPosition < bufferDataLength; bufferPosition++) {
                tempBuffer[bufferPosition] = buffer.get();
            }
        }
        
        SoapTcpSessionState sessionState = (SoapTcpSessionState)session.getAttribute("sessionState");
        if (sessionState != null
            && sessionState.getStateId() == SoapTcpSessionState.SOAP_TCP_SESSION_STATE_NEW) {
            if (bufferPosition == 16) {
                out.write(IoBuffer.wrap(tempBuffer, 0, bufferPosition));
                bufferPosition = 0;
                bufferDataLength = 0;
                session.setAttribute("bufferPosition", bufferPosition);
                session.setAttribute("bufferDataLength", bufferDataLength);
                return;
            } else {
                return;
            }
        }
        
        InputStream inStream = new ByteArrayInputStream(tempBuffer, 0, bufferDataLength);
        try {
            SoapTcpFrame frame = SoapTcpUtils.readMessageFrame(inStream);
            List<SoapTcpChannel> channels = (List<SoapTcpChannel>)session.getAttribute("channels");
            for (SoapTcpChannel channel : channels) {
                if (channel.getChannelId() == frame.getChannelId()) {
                    switch (frame.getHeader().getFrameType()) {
                    case SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE:
                    case SoapTcpFrameHeader.ERROR_MESSAGE:
                    case SoapTcpFrameHeader.NULL_MESSAGE:
                        SoapTcpMessage singleFrameMessage = SoapTcpMessage.createSoapTcpMessage(frame);
                        out.write(singleFrameMessage);
                        bufferPosition = 0;
                        bufferDataLength = 0;
                        break;
                    case SoapTcpFrameHeader.MESSAGE_START_CHUNK:
                    case SoapTcpFrameHeader.MESSAGE_CHUNK:
                        channel.addFrame(frame);
                        bufferPosition = 0;
                        bufferDataLength = 0;
                        break;
                    case SoapTcpFrameHeader.MESSAGE_END_CHUNK:
                        List<SoapTcpFrame> frames = channel.getFrames();
                        SoapTcpMessage multiFrameMessage = SoapTcpMessage.createSoapTcpMessage(frames);
                        multiFrameMessage.getFrames().add(frame);
                        out.write(multiFrameMessage);
                        bufferPosition = 0;
                        bufferDataLength = 0;
                        break;
                    default:
                        return;
                    }
                }
            }
        } catch (IOException ex) {
            //
        } finally {
            session.setAttribute("bufferPosition", bufferPosition);
            session.setAttribute("bufferDataLength", bufferDataLength);
        }
    }

}
