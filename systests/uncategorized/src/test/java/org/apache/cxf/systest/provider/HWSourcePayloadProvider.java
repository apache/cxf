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

import javax.jws.HandlerChain;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

//The following wsdl file is used.
//wsdlLocation = "/trunk/testutils/src/main/resources/wsdl/hello_world_rpc_lit.wsdl"
@WebServiceProvider(portName = "SoapPortProviderRPCLit3", serviceName = "SOAPServiceProviderRPCLit",
                      targetNamespace = "http://apache.org/hello_world_rpclit",
 wsdlLocation = "/wsdl/hello_world_rpc_lit.wsdl")
@ServiceMode (value = javax.xml.ws.Service.Mode.PAYLOAD)
@HandlerChain(file = "./handlers_invocation.xml", name = "TestHandlerChain")
public class HWSourcePayloadProvider implements Provider<Source> {
    
    public HWSourcePayloadProvider() {
    
    }
    
    public Source invoke(Source request) {        
        try {
            String input = getSourceAsString(request);
            System.out.println(input);  
            
            if (input.indexOf("ServerLogicalHandler") >= 0) {
                StreamSource source = new StreamSource();
                
                InputStream greetMeInputStream = getClass()
                    .getResourceAsStream("resources/GreetMeRpcLiteralRespBody.xml");

                source.setInputStream(greetMeInputStream);
                return source;               
            }

        } catch (Exception e) {
            System.out.println("Received an exception while parsing the source");
            e.printStackTrace();
        }
        return null;
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
