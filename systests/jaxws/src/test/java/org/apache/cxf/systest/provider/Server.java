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

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {

    protected void run() {
        Object implementor = new HWSourcePayloadProvider();
        String address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8";
        Endpoint ep = Endpoint.create(implementor);
        ep.publish(address);
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SourceDataBinding.PREFERRED_FORMAT, "dom");
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-dom";
        ep = Endpoint.create(implementor);
        ep.setProperties(map);
        ep.publish(address);
        
        map.put(SourceDataBinding.PREFERRED_FORMAT, "sax");
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-sax";
        ep = Endpoint.create(implementor);
        ep.setProperties(map);
        ep.publish(address);

        map.put(SourceDataBinding.PREFERRED_FORMAT, "stax");
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-stax";
        ep = Endpoint.create(implementor);
        ep.setProperties(map);
        ep.publish(address);

        map.put(SourceDataBinding.PREFERRED_FORMAT, "cxf.stax");
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-cxfstax";
        ep = Endpoint.create(implementor);
        ep.setProperties(map);
        ep.publish(address);

        map.put(SourceDataBinding.PREFERRED_FORMAT, "stream");
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-stream";
        ep = Endpoint.create(implementor);
        ep.setProperties(map);
        ep.publish(address);

               
        implementor = new HWSoapMessageProvider();
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit1";
        Endpoint.publish(address, implementor);

        implementor = new HWDOMSourceMessageProvider();
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit2";
        Endpoint.publish(address, implementor);
        
        implementor = new HWDOMSourcePayloadProvider();
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit3";
        Endpoint.publish(address, implementor);

        implementor = new HWSAXSourceMessageProvider();
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit4";
        Endpoint.publish(address, implementor);

        implementor = new HWStreamSourceMessageProvider();
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit5";
        Endpoint.publish(address, implementor);

        implementor = new HWSAXSourcePayloadProvider();
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit6";
        Endpoint.publish(address, implementor);

        implementor = new HWStreamSourcePayloadProvider();
        address = "http://localhost:9008/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit7";
        Endpoint.publish(address, implementor);
    
    }

    public static void main(String[] args) {
        try {
            Server s = new Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
