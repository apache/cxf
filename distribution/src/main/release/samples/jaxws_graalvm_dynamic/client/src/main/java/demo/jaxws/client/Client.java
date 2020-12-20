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

package demo.jaxws.client;

import java.io.File;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.common.spi.GeneratedNamespaceClassLoader;
import org.apache.cxf.common.spi.NamespaceClassCreator;
import org.apache.cxf.endpoint.dynamic.ExceptionClassCreator;
import org.apache.cxf.endpoint.dynamic.ExceptionClassLoader;
import org.apache.cxf.jaxb.FactoryClassCreator;
import org.apache.cxf.jaxb.FactoryClassLoader;
import org.apache.cxf.jaxb.WrapperHelperClassLoader;
import org.apache.cxf.jaxb.WrapperHelperCreator;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.spi.WrapperClassCreator;
import org.apache.cxf.jaxws.spi.WrapperClassLoader;
import org.apache.cxf.wsdl.ExtensionClassCreator;
import org.apache.cxf.wsdl.ExtensionClassLoader;
import org.apache.handlers.AddNumbers;
import org.apache.handlers.AddNumbersFault;

public final class Client {
    private Client() {
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.err.println("please provide wsdl");
            System.exit(0);
        }

        final Bus bus = new ExtensionManagerBus();
        BusFactory.setDefaultBus(bus);

        bus.setExtension(new WrapperHelperClassLoader(bus), WrapperHelperCreator.class);
        bus.setExtension(new ExtensionClassLoader(bus), ExtensionClassCreator.class);
        bus.setExtension(new ExceptionClassLoader(bus), ExceptionClassCreator.class);
        bus.setExtension(new WrapperClassLoader(bus), WrapperClassCreator.class);
        bus.setExtension(new FactoryClassLoader(bus), FactoryClassCreator.class);
        bus.setExtension(new GeneratedNamespaceClassLoader(bus), NamespaceClassCreator.class);

        run(args, bus);

        System.exit(0);
    }

    public static void run(String[] args, final Bus bus) {
        File wsdl = new File(args[0]);

        final JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(AddNumbers.class);
        factory.setWsdlURL(wsdl.toURI().toString());
        final AddNumbers client = (AddNumbers)factory.create();

        try {
            int number1 = 10;
            int number2 = 20;

            System.out.printf("Invoking addNumbers(%d, %d)\n", number1, number2);
            int result = client.addNumbers(number1, number2);
            System.out.printf("The result of adding %d and %d is %d.\n\n", number1, number2, result);

            number1 = 3;
            number2 = 5;

            System.out.printf("Invoking addNumbers(%d, %d)\n", number1, number2);
            result = client.addNumbers(number1, number2);
            System.out.printf("The result of adding %d and %d is %d.\n\n", number1, number2, result);

            number1 = -10;
            System.out.printf("Invoking addNumbers(%d, %d)\n", number1, number2);
            result = client.addNumbers(number1, number2);
            System.out.printf("The result of adding %d and %d is %d.\n", number1, number2, result);

        } catch (AddNumbersFault ex) {
            System.out.printf("Caught AddNumbersFault: %s\n", ex.getFaultInfo().getMessage());
        }
    }
}
