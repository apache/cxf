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
package org.apache.cxf.systest.jms;

import javax.xml.ws.Endpoint;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {


    protected void run()  {
        Object implementor = new GreeterImplTwoWayJMS();        
        Object impl2 =  new GreeterImplQueueOneWay();
        Object impl3  = new GreeterImplTopicOneWay();
        Object impleDoc = new GreeterImplDoc();
        Object impl4 = new GreeterByteMessageImpl();
        Endpoint.publish(null, impleDoc);
        String address = "http://localhost:9000/SoapContext/SoapPort";
        Endpoint.publish(address, implementor);
        Endpoint.publish("http://testaddr.not.required/", impl2);
        Endpoint.publish("http://testaddr.not.required.topic/", impl3);
        Endpoint.publish("http://testaddr.not.required.byte/", impl4);
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
