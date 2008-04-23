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

import java.io.File;
import java.net.URL;
import javax.xml.namespace.QName;

import org.apache.hello_world_xml_http.bare.Greeter;
import org.apache.hello_world_xml_http.bare.XMLService;
import org.apache.hello_world_xml_http.bare.types.MyComplexStructType;

// import org.apache.hello_world_xml_http.bare.PingMeFault;

public final class Client {

    private static final QName SERVICE_NAME = new QName("http://apache.org/hello_world_xml_http/bare",
            "XMLService");

    private static final QName PORT_NAME = 
        new QName("http://apache.org/hello_world_xml_http/bare", "XMLPort");

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
            wsdlURL = wsdlFile.toURL();
        } else {
            wsdlURL = new URL(args[0]);
        }

        System.out.println(wsdlURL);
        XMLService service = new XMLService(wsdlURL, SERVICE_NAME);
        Greeter greeter = (Greeter) service.getPort(PORT_NAME, Greeter.class);

        System.out.println("Invoking sayHi...");
        System.out.println("server responded with: " + greeter.sayHi());
        System.out.println();

        System.out.println("Invoking greetMe...");
        System.out.println("server responded with: " + greeter.greetMe(System.getProperty("user.name")));
        System.out.println();

        MyComplexStructType argument = new MyComplexStructType();
        MyComplexStructType retVal = null;

        String str1 = "this is element 1";
        String str2 = "this is element 2";
        int int1 = 42;

        argument.setElem1(str1);
        argument.setElem2(str2);
        argument.setElem3(int1);
        System.out.println("Invoking sendReceiveData...");

        retVal = greeter.sendReceiveData(argument);

        System.out.println("Response from sendReceiveData operation :");
        System.out.println("Element-1 : " + retVal.getElem1());
        System.out.println("Element-2 : " + retVal.getElem2());
        System.out.println("Element-3 : " + retVal.getElem3());
        System.out.println();

        /*
         * try { System.out.println("Invoking pingMe, expecting exception...");
         * port.pingMe(); } catch (PingMeFault ex) {
         * System.out.println("Expected exception: PingMeFault has occurred: " +
         * ex.getMessage()); }
         */
        System.exit(0);
    }

}
