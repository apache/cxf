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

package demo.jms_greeter.client;

import java.io.File;

import javax.xml.namespace.QName;
import org.apache.cxf.jms_greeter.JMSGreeterPortType;
import org.apache.cxf.jms_greeter.JMSGreeterService;


public final class Client {

    private static final QName SERVICE_NAME =
        new QName("http://cxf.apache.org/jms_greeter", "JMSGreeterService");
    private static final QName PORT_NAME =
        new QName("http://cxf.apache.org/jms_greeter", "GreeterPort");

    private Client() {
    }

    public static void main(String[] args) throws Exception {

        args = new String[1];
        args[0] = "./wsdl/jms_greeter.wsdl";
        if (args.length == 0) {
            System.out.println("please specify wsdl");
            System.exit(1);
        }

        File wsdl = new File(args[0]);

        JMSGreeterService service = new JMSGreeterService(wsdl.toURL(), SERVICE_NAME);
        JMSGreeterPortType greeter = (JMSGreeterPortType)service.getPort(PORT_NAME, JMSGreeterPortType.class);

        System.out.println("Invoking sayHi...");
        System.out.println("server responded with: " + greeter.sayHi());
        System.out.println();

        System.out.println("Invoking greetMe...");
        System.out.println("server responded with: " + greeter.greetMe(System.getProperty("user.name")));
        System.out.println();

        System.out.println("Invoking greetMeOneWay...");
        greeter.greetMeOneWay(System.getProperty("user.name"));
        System.out.println("No response from server as method is OneWay");
        System.out.println();

        System.exit(0);
    }
}
