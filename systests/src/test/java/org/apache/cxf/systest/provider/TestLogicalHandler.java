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
package org.apache.cxf.systest.provider;


import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;


public class TestLogicalHandler implements LogicalHandler<LogicalMessageContext> {
    public boolean handleMessage(LogicalMessageContext ctx) {
        try {
            LogicalMessage msg = ctx.getMessage();
            Source payload = msg.getPayload();
            String request = getSourceAsString(payload);
            // System.out.println(getSourceAsString(payload));

            // Make sure SOAP handler has changed the value successfully.
            if (request.indexOf("ServerSOAPHandler") >= 0) {
                InputStream greetMeInputStream = getClass()
                    .getResourceAsStream("resources/GreetMeRpcLiteralReqLogical.xml");
                StreamSource source = new StreamSource();
                source.setInputStream(greetMeInputStream);
                msg.setPayload(source);
            } else if (request.indexOf("TestGreetMeResponse") >= 0) {
                InputStream greetMeInputStream = getClass()
                    .getResourceAsStream("resources/GreetMeRpcLiteralRespLogical.xml");
                StreamSource source = new StreamSource();
                source.setInputStream(greetMeInputStream);
                msg.setPayload(source);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    public boolean handleFault(LogicalMessageContext ctx) {
        return true;
    }
    public void close(MessageContext arg0) {
    }
    
    public static String getSourceAsString(Source s) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        Writer out = new StringWriter();
        StreamResult streamResult = new StreamResult();
        streamResult.setWriter(out);
        transformer.transform(s, streamResult);
        return streamResult.getWriter().toString();
    }
}
