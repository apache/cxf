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

package demo.ws_addressing.client;

import java.io.File;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ObjectFactory;

import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.PingMeFault;
import org.apache.hello_world_soap_http.SOAPService;

import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;


public final class Client {

    private static final QName SERVICE_NAME =
        new QName("http://apache.org/hello_world_soap_http", "SOAPService");
    private static final ObjectFactory WSA_OBJECT_FACTORY =
        new ObjectFactory();
    private static final String USER_NAME = System.getProperty("user.name");


    private Client() {
    }

    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            System.out.println("please specify wsdl");
            System.exit(1);
        }

        try {
            URL wsdlURL;
            File wsdlFile = new File(args[0]);
            if (wsdlFile.exists()) {
                wsdlURL = wsdlFile.toURI().toURL();
            } else {
                wsdlURL = new URL(args[0]);
            }

            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = Client.class.getResource("/client.xml");
            Bus bus = bf.createBus(busFile.toString());
            BusFactory.setDefaultBus(bus);


            SOAPService service = new SOAPService(wsdlURL, SERVICE_NAME);
            Greeter port = service.getSoapPort();

            implicitPropagation(port);

            explicitPropagation(port);

            implicitPropagation(port);

        } catch (UndeclaredThrowableException ex) {
            ex.getUndeclaredThrowable().printStackTrace();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }  finally {
            System.exit(0);
        }
    }

    /**
     * A series of invocations with implicitly propogated
     * Message Addressing Properties.
     */
    private static void implicitPropagation(Greeter port) {
        System.out.println();
        System.out.println("Implicit MessageAddressingProperties propagation");
        System.out.println("------------------------------------------------");

        System.out.println("Invoking sayHi...");
        String resp = port.sayHi();
        System.out.println("Server responded with: " + resp + "\n");

        System.out.println("Invoking greetMe...");
        resp = port.greetMe(USER_NAME);
        System.out.println("Server responded with: " + resp + "\n");

        System.out.println("Invoking greetMeOneWay...");
        port.greetMeOneWay(USER_NAME);
        System.out.println("No response from server as method is OneWay\n");

        try {
            System.out.println("Invoking pingMe, expecting exception...");
            port.pingMe();
        } catch (PingMeFault ex) {
            System.out.println("Expected exception occurred: " + ex);
        }
    }


    private static AddressingProperties createMaps() {
        // get Message Addressing Properties instance
        AddressingProperties maps = new AddressingProperties();

        // set MessageID property
        AttributedURIType messageID = WSA_OBJECT_FACTORY.createAttributedURIType();
        messageID.setValue("urn:uuid:" + System.currentTimeMillis());
        maps.setMessageID(messageID);
        return maps;
    }

    /**
     * A series of invocations with explicitly propogated
     * Message Addressing Properties.
     */
    private static void explicitPropagation(Greeter port) {
        System.out.println();
        System.out.println("Explicit MessageAddressingProperties propagation");
        System.out.println("------------------------------------------------");



        // associate MAPs with request context
        Map<String, Object> requestContext =
            ((BindingProvider)port).getRequestContext();
        requestContext.put(CLIENT_ADDRESSING_PROPERTIES, createMaps());

        System.out.println("Invoking sayHi...");
        String resp = port.sayHi();
        System.out.println("Server responded with: " + resp + "\n");

        // set the RelatesTo property to the initial message ID, so that
        // the series of invocations are explicitly related
        //RelatesToType relatesTo = WSA_OBJECT_FACTORY.createRelatesToType();
        //relatesTo.setValue(messageID.getValue());
        //maps.setRelatesTo(relatesTo);

        System.out.println("Invoking greetMe...");
        requestContext.put(CLIENT_ADDRESSING_PROPERTIES, createMaps());
        resp = port.greetMe(USER_NAME);
        System.out.println("Server responded with: " + resp + "\n");

        System.out.println("Invoking greetMeOneWay...");
        requestContext.put(CLIENT_ADDRESSING_PROPERTIES, createMaps());
        port.greetMeOneWay(USER_NAME);
        System.out.println("No response from server as method is OneWay\n");

        // disassociate MAPs from request context
        requestContext.remove(CLIENT_ADDRESSING_PROPERTIES);
    }
}
