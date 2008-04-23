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

package org.apache.cxf.systest.mtom;

import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.mtom_xop.TestMtomImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {

    protected void run() {
        Object implementor = new TestMtomImpl();
        String address = "http://localhost:9036/mime-test";
        try {
            EndpointImpl jaxep = (EndpointImpl) javax.xml.ws.Endpoint.publish(address, implementor);
            Endpoint ep = jaxep.getServer().getEndpoint();
            ep.getInInterceptors().add(new TestMultipartMessageInterceptor());
            ep.getOutInterceptors().add(new TestAttachmentOutInterceptor());
            
            SOAPBinding jaxWsSoapBinding = (SOAPBinding) jaxep.getBinding();
            jaxWsSoapBinding.setMTOMEnabled(true);

        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String args[]) {
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
