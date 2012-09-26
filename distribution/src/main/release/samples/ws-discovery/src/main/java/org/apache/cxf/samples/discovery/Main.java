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

package org.apache.cxf.samples.discovery;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import javax.xml.ws.Endpoint;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        //find a randomish port to use.   The clients will
        //use WS-Discovery to find these services so 
        //it really doesn't matter what port we publish them
        //on (or what URL or anything like that)
        ServerSocket sock = new ServerSocket();
        InetSocketAddress s = new InetSocketAddress(InetAddress.getLocalHost(), 0);
        sock.bind(s);
        int port = sock.getLocalPort();
        sock.close();
        
        String address = "http://localhost:" + port + "/Greeter";
        System.out.println("Publishing on " + address);
        Endpoint.publish(address, new GreeterImpl(port));
    }
}
