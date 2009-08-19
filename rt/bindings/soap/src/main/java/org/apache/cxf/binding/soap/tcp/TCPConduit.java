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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrame;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrameContentDescription;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrameHeader;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.policy.Assertor;

public class TCPConduit
    extends AbstractConduit
    implements Configurable, Assertor {

    private static final Logger LOG = LogUtils.getL7dLogger(TCPConduit.class);
    
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private String endPointAddress;
    
    public TCPConduit(EndpointReferenceType t) throws IOException {
        super(t);
        
        String hostName = null;
        int port = 0;
        
        String address = t.getAddress().getValue();
        if (address.contains("soap.tcp://")) {
            endPointAddress = address;
            int beginIndex = address.indexOf("://");
            int endIndex = address.indexOf(":", beginIndex + 1);
            hostName = address.substring(beginIndex + 3, endIndex);
            beginIndex = endIndex;
            endIndex = address.indexOf("/", beginIndex);
            port = Integer.parseInt(address.substring(beginIndex + 1, endIndex));
            //System.out.println("hostName: " + hostName);
            //System.out.println("port: " + port);
        }

        socket = new Socket(hostName, port);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        
        out.write(SoapTcpProtocolConsts.MAGIC_IDENTIFIER.getBytes("US-ASCII"));
        DataCodingUtils.writeInts4(out, SoapTcpProtocolConsts.PROTOCOL_VERSION_MAJOR,
                                   SoapTcpProtocolConsts.PROTOCOL_VERSION_MINOR,
                                   SoapTcpProtocolConsts.CONNECTION_MANAGEMENT_VERSION_MAJOR,
                                   SoapTcpProtocolConsts.CONNECTION_MANAGEMENT_VERSION_MINOR);
        out.flush();
        
        final int version[] = new int[4];
        DataCodingUtils.readInts4(in, version, 4);
        
        //System.out.println("serverProtocolVersionMajor = " + version[0]);
        //System.out.println("serverProtocolVersionMinor = " + version[1]);
        //System.out.println("serverConnectionManagementVersionMajor = " + version[2]);
        //System.out.println("serverConnectionManagementVersionMinor = " + version[3]);
        
        initSession();
    }
    
    public TCPConduit(EndpointInfo ei) throws IOException {
        this(ei.getTarget());
    }

    private void initSession() throws IOException {
        final String initSessionMessage = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<s:Body><initiateSession xmlns=\"http://servicechannel.tcp.transport.ws.xml.sun.com/\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"/></s:Body></s:Envelope>";
        byte[] initSessionMessageBytes = null;
        try {
            initSessionMessageBytes = initSessionMessage.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        final SoapTcpFrameContentDescription contentDesc = new SoapTcpFrameContentDescription();
        contentDesc.setContentId(0);
        
        Map<Integer, String> parameters = new Hashtable<Integer, String>();
        parameters.put(0, "utf-8");
        
        contentDesc.setParameters(parameters);
        
        final SoapTcpFrameHeader header =
            new SoapTcpFrameHeader(SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE, contentDesc);
        SoapTcpFrame frame = new SoapTcpFrame();
        frame.setHeader(header);
        frame.setChannelId(0);
        frame.setPayload(initSessionMessageBytes);
        try {
            SoapTcpUtils.writeMessageFrame(out, frame);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        final SoapTcpFrame response = SoapTcpUtils.readMessageFrame(in);
        if (!SoapTcpUtils.checkSingleFrameResponse(response, "initiateSessionResponse")) {
            throw new IOException("Could not initiate SOAP/TCP connection.");
        }
        //SoapTcpUtils.printSoapTcpFrame(System.out, response);
    }
    
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    public String getBeanName() {
        // TODO Auto-generated method stub
        return null;
    }

    public void assertMessage(Message message) {
        // TODO Auto-generated method stub

    }

    public boolean canAssert(QName type) {
        // TODO Auto-generated method stub
        return false;
    }

    public void prepare(Message message) throws IOException {
        final SoapTcpOutputStream soapTcpOutputStream =
            new SoapTcpOutputStream(in, out, message, endPointAddress, incomingObserver);
        message.setContent(OutputStream.class, soapTcpOutputStream);
    }

    @Override
    public void close(Message message) {
        
    }
    
    @Override
    public void close() {
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
}
