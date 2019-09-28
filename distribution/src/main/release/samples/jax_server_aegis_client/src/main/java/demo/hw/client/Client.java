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

package demo.hw.client;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;

public final class Client {

    private Client() {
    }

    public static void main(String[] args) throws Exception {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        String serviceURL;
        if (args != null && args.length > 0 && !"".equals(args[0])) {
            serviceURL = args[0];
        } else {
            serviceURL = "http://localhost:9000/Hello";
        }
        factory.setServiceName(new QName("http://server.hw.demo/", "HelloWorldService"));
        factory.setAddress(serviceURL);
        factory.setWsdlURL(serviceURL + "?wsdl");
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
        HelloWorld client = factory.create(HelloWorld.class);
        System.out.println("Invoke sayHi()....");
        System.out.println(client.sayHi(System.getProperty("user.name")));
        System.exit(0);
    }

}
