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

import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;

//The following wsdl file is used.
//wsdlLocation = "/trunk/testutils/src/main/resources/wsdl/hello_world_rpc_lit.wsdl"
@WebServiceProvider(portName = "SoapPortProviderRPCLit3", serviceName = "SOAPServiceProviderRPCLit",
                      targetNamespace = "http://apache.org/hello_world_rpclit",
 wsdlLocation = "/wsdl/hello_world_rpc_lit.wsdl")
@ServiceMode (value = javax.xml.ws.Service.Mode.PAYLOAD)
@HandlerChain(file = "./handlers_invocation.xml", name = "TestHandlerChain")
public class HWSourcePayloadProvider implements Provider<Source> {
    boolean doneStax;
    @Resource 
    WebServiceContext ctx;

    public HWSourcePayloadProvider() {
    
    }
    
    public Source invoke(Source request) {   
        QName qn = (QName)ctx.getMessageContext().get(MessageContext.WSDL_OPERATION);
        if (qn == null) {
            throw new RuntimeException("No Operation Name");
        }
        
        try {
            System.out.println(request.getClass().getName());
            String input = getSourceAsString(request);
            System.out.println(input);  
            
            if (input.indexOf("ServerLogicalHandler") >= 0) {
                return map(request.getClass());
            }

        } catch (Exception e) {
            System.out.println("Received an exception while parsing the source");
            e.printStackTrace();
        }
        return null;
    }
    
    private Source map(Class<? extends Source> class1) 
        throws Exception {
        
        InputStream greetMeInputStream = getClass()
            .getResourceAsStream("resources/GreetMeRpcLiteralRespBody.xml");
        if (DOMSource.class.equals(class1)) {
            return new DOMSource(XMLUtils.parse(greetMeInputStream));
        } else if (StaxSource.class.equals(class1)) {
            if (doneStax) {
                XMLReader reader = XMLReaderFactory.createXMLReader();
                return new SAXSource(reader, new InputSource(greetMeInputStream));
            } else {
                doneStax = true;
                return new StaxSource(StaxUtils.createXMLStreamReader(greetMeInputStream));
            }
        } else if (StreamSource.class.equals(class1)) {
            StreamSource source = new StreamSource();
            source.setInputStream(greetMeInputStream);
            return source;
        }
        //java 6 javax.xml.transform.stax.StAXSource
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(greetMeInputStream);
        return class1.getConstructor(XMLStreamReader.class).newInstance(reader);
    }

    public static String getSourceAsString(Source s) throws Exception {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            Writer out = new StringWriter();
            StreamResult streamResult = new StreamResult();
            streamResult.setWriter(out);
            transformer.transform(s, streamResult);
            return streamResult.getWriter().toString();
            
        } catch (TransformerException te) {
            if ("javax.xml.transform.stax.StAXSource".equals(s.getClass().getName())) {
                //on java6, we will get this class if "stax" is configured
                //for the preferred type. However, older xalans don't know about it
                //we'll manually do it
                XMLStreamReader r = (XMLStreamReader)s.getClass().getMethod("getXMLStreamReader").invoke(s);
                return XMLUtils.toString(StaxUtils.read(r).getDocumentElement());
            }
            throw te;
        }
    }
}
