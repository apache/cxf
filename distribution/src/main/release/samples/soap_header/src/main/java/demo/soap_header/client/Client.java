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


package demo.soap_header.client;

import java.io.File;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import org.apache.headers.HeaderService;
import org.apache.headers.HeaderTester;
import org.apache.headers.InHeader;
import org.apache.headers.InHeaderResponse;
import org.apache.headers.InoutHeader;
import org.apache.headers.InoutHeaderResponse;
import org.apache.headers.OutHeader;
import org.apache.headers.OutHeaderResponse;
import org.apache.headers.SOAPHeaderData;

public final class Client {

    private static final QName SERVICE_NAME
        = new QName("http://apache.org/headers", "HeaderService");


    private Client() {
    }

    public static void main(String args[]) throws Exception {

        if (args.length == 0) {
            System.out.println("please specify wsdl");
            System.exit(1);
        }

        URL wsdlURL;
        File wsdlFile = new File(args[0]);
        if (wsdlFile.exists()) {
            wsdlURL = wsdlFile.toURI().toURL();
        } else {
            wsdlURL = new URL(args[0]);
        }
        
        HeaderService hs = new HeaderService(wsdlURL, SERVICE_NAME);
        HeaderTester proxy = hs.getSoapPort();

        invokeInHeader(proxy);
        invokeOutHeader(proxy);  
        invokeInOutHeader(proxy);  
    }
     
    private static void invokeInHeader(HeaderTester proxy) {
        // invoke inHeader operation
        System.out.println("Invoking inHeader operation");
        InHeader me = new InHeader();
        me.setRequestType("CXF user");
        SOAPHeaderData headerInfo = new SOAPHeaderData();
        headerInfo.setOriginator("CXF client");
        headerInfo.setMessage("Invoking inHeader operation");
        InHeaderResponse response = proxy.inHeader(me, headerInfo);
        System.out.println("\tinHeader invocation returned: ");
        System.out.println("\t\tResult: " + response.getResponseType());
    }

    private static void invokeOutHeader(HeaderTester proxy) {    
        // invoke outHeaderoperation
        System.out.println("Invoking outHeader operation");
        OutHeader me = new OutHeader();
        me.setRequestType("CXF user");
        Holder<OutHeaderResponse> theResponse = new Holder<OutHeaderResponse>();
        Holder<SOAPHeaderData> headerInfo = new Holder<SOAPHeaderData>();
        proxy.outHeader(me, theResponse, headerInfo);
        System.out.println("\toutHeader invocation returned: ");
        System.out.println("\t\tOut parameter: " + theResponse.value.getResponseType());
        System.out.println("\t\tHeader content:");
        System.out.println("\t\t\tOriginator: " + headerInfo.value.getOriginator());
        System.out.println("\t\t\tMessage: " + headerInfo.value.getMessage());
    }

    private static void invokeInOutHeader(HeaderTester proxy) {
        System.out.println("Invoking inoutHeader operation");
        InoutHeader me = new InoutHeader();
        me.setRequestType("CXF user");
        Holder<SOAPHeaderData> headerInfo = new Holder<SOAPHeaderData>();
        SOAPHeaderData shd = new SOAPHeaderData();
        shd.setOriginator("CXF client");
        shd.setMessage("Invoking inoutHeader operation");
        headerInfo.value = shd;
        InoutHeaderResponse response = proxy.inoutHeader(me, headerInfo);
        System.out.println("\tinoutHeader invocation returned: ");
        System.out.println("\t\tResult: " + response.getResponseType());
        System.out.println("\t\tHeader content:");
        System.out.println("\t\t\tOriginator: " + headerInfo.value.getOriginator());
        System.out.println("\t\t\tMessage: " + headerInfo.value.getMessage());

        System.exit(0);
    }
}

