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

package org.apache.cxf.tools.misc.processor;

import java.io.File;
import java.util.Iterator;

import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.misc.WSDLToService;
import org.apache.cxf.transport.jms.AddressType;
import org.junit.Before;
import org.junit.Test;


public class WSDLToServiceProcessorTest extends ProcessorTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(Bus.class, BusFactory.getDefaultBus());
    }

    @Test
    public void testNewService() throws Exception {
        String[] args = new String[] {"-transport", "soap", "-e", "serviceins", "-p", "portins", "-n",
                                      "Greeter_SOAPBinding", "-a",
                                      "http://localhost:9000/newservice/newport", "-d",
                                      output.getCanonicalPath(),
                                      getLocation("/misctools_wsdl/hello_world.wsdl")};
        WSDLToService.main(args);

        File outputFile = new File(output, "hello_world-service.wsdl");
        assertTrue("New wsdl file is not generated", outputFile.exists());
        WSDLToServiceProcessor processor = new WSDLToServiceProcessor();
        processor.setEnvironment(env);
        try {
            processor.parseWSDL(outputFile.getAbsolutePath());
            Service service = processor.getWSDLDefinition().getService(
                                                                       new QName(processor
                                                                           .getWSDLDefinition()
                                                                           .getTargetNamespace(),
                                                                                 "serviceins"));
            if (service == null) {
                fail("Element wsdl:service serviceins Missed!");
            }
            Iterator it = service.getPort("portins").getExtensibilityElements().iterator();
            if (service == null) {
                fail("Element wsdl:port portins Missed!");
            }
            boolean found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof SOAPAddress) {
                    SOAPAddress soapAddress = (SOAPAddress)obj;
                    if (soapAddress.getLocationURI() != null
                        && soapAddress.getLocationURI().equals("http://localhost:9000/newservice/newport")) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                fail("Element soap:address of service port Missed!");
            }
        } catch (ToolException e) {
            fail("Exception Encountered when parsing wsdl, error: " + e.getMessage());
        }
    }

    @Test
    public void testNewServiceSoap12() throws Exception {
        String[] args = new String[] {"-soap12", "-transport", "soap",
                                      "-e", "SOAPService", "-p", "SoapPort", "-n",
                                      "Greeter_SOAPBinding", "-a",
                                      "http://localhost:9000/SOAPService/SoapPort", "-d",
                                      output.getCanonicalPath(),
                                      getLocation("/misctools_wsdl/hello_world_soap12.wsdl")};
        WSDLToService.main(args);

        File outputFile = new File(output, "hello_world_soap12-service.wsdl");
        assertTrue("New wsdl file is not generated", outputFile.exists());
        WSDLToServiceProcessor processor = new WSDLToServiceProcessor();
        processor.setEnvironment(env);
        try {
            processor.parseWSDL(outputFile.getAbsolutePath());
            Service service = processor.getWSDLDefinition().getService(
                                                                       new QName(processor
                                                                           .getWSDLDefinition()
                                                                           .getTargetNamespace(),
                                                                                 "SOAPService"));
            if (service == null) {
                fail("Element wsdl:service serviceins Missed!");
            }
            Iterator it = service.getPort("SoapPort").getExtensibilityElements().iterator();
            if (service == null) {
                fail("Element wsdl:port portins Missed!");
            }

            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof SOAP12Address) {
                    SOAP12Address soapAddress = (SOAP12Address)obj;
                    assertNotNull(soapAddress.getLocationURI());
                    assertEquals("http://localhost:9000/SOAPService/SoapPort", soapAddress.getLocationURI());
                    break;
                }
            }
        } catch (ToolException e) {
            fail("Exception Encountered when parsing wsdl, error: " + e.getMessage());
        }

    }

    @Test
    public void testDefaultLocation() throws Exception {

        String[] args = new String[] {"-transport", "soap", "-e", "serviceins", "-p", "portins", "-n",
                                      "Greeter_SOAPBinding", "-d", output.getCanonicalPath(),
                                      getLocation("/misctools_wsdl/hello_world.wsdl")};
        WSDLToService.main(args);

        File outputFile = new File(output, "hello_world-service.wsdl");
        assertTrue("New wsdl file is not generated", outputFile.exists());
        WSDLToServiceProcessor processor = new WSDLToServiceProcessor();
        processor.setEnvironment(env);
        try {
            processor.parseWSDL(outputFile.getAbsolutePath());
            Service service = processor.getWSDLDefinition().getService(
                                                                       new QName(processor
                                                                           .getWSDLDefinition()
                                                                           .getTargetNamespace(),
                                                                                 "serviceins"));
            if (service == null) {
                fail("Element wsdl:service serviceins Missed!");
            }
            Iterator it = service.getPort("portins").getExtensibilityElements().iterator();
            if (service == null) {
                fail("Element wsdl:port portins Missed!");
            }
            boolean found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof SOAPAddress) {
                    SOAPAddress soapAddress = (SOAPAddress)obj;
                    if (soapAddress.getLocationURI() != null
                        && soapAddress.getLocationURI().equals("http://localhost:9000/serviceins/portins")) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                fail("Element soap:address of service port Missed!");
            }
        } catch (ToolException e) {
            fail("Exception Encountered when parsing wsdl, error: " + e.getMessage());
        }
    }

    @Test
    public void testJMSNewService() throws Exception {
        String[] args = new String[] {"-transport", "jms", "-e", "serviceins", "-p", "portins", "-n",
                                      "HelloWorldPortBinding", "-jpu", "tcp://localhost:91919", "-jcf",
                                      "org.activemq.jndi.ActiveMQInitialContextFactory", "-jfn",
                                      "ConnectionFactory", "-jdn",
                                      "dynamicQueues/test.cxf.jmstransport.queue", "-jmt", "text", "-jmc",
                                      "false", "-jsn", "cxf_Queue_subscriber", "-d",
                                      output.getCanonicalPath(),
                                      getLocation("/misctools_wsdl/jms_test.wsdl")};
        WSDLToService.main(args);
        File outputFile = new File(output, "jms_test-service.wsdl");
        assertTrue("New wsdl file is not generated", outputFile.exists());
        WSDLToServiceProcessor processor = new WSDLToServiceProcessor();
        processor.setEnvironment(env);
        try {
            processor.parseWSDL(outputFile.getAbsolutePath());
            Service service = processor.getWSDLDefinition().getService(
                                                                       new QName(processor
                                                                           .getWSDLDefinition()
                                                                           .getTargetNamespace(),
                                                                                 "serviceins"));
            if (service == null) {
                fail("Element wsdl:service serviceins Missed!");
            }
            Iterator it = service.getPort("portins").getExtensibilityElements().iterator();
            if (service == null) {
                fail("Element wsdl:port portins Missed!");
            }
            boolean found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof AddressType) {
                    AddressType jmsAddress = (AddressType)obj;
                    if (!(jmsAddress.getDestinationStyle() != null
                          && "queue".equalsIgnoreCase(jmsAddress.getDestinationStyle().toString()))) {
                        break;
                    }
                    if (!(jmsAddress.getJndiDestinationName() != null && jmsAddress.getJndiDestinationName()
                        .equals("dynamicQueues/test.cxf.jmstransport.queue"))) {
                        break;
                    }

                    assertEquals(2, jmsAddress.getJMSNamingProperty().size());
                    assertEquals("java.naming.factory.initial",
                                 jmsAddress.getJMSNamingProperty().get(0).getName());
                    assertEquals("tcp://localhost:91919",
                                 jmsAddress.getJMSNamingProperty().get(1).getValue());

                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Element jms:address of service port Missed!");
            }
        } catch (ToolException e) {
            fail("Exception Encountered when parsing wsdl, error: " + e.getMessage());
        }
    }

    @Test
    public void testJMSDefaultValue() throws Exception {
        String[] args = new String[] {"-transport", "jms", "-e", "serviceins", "-p", "portins", "-n",
                                      "HelloWorldPortBinding",
                                      "-d", output.getCanonicalPath(),
                                      getLocation("/misctools_wsdl/jms_test.wsdl")};
        WSDLToService.main(args);
        File outputFile = new File(output, "jms_test-service.wsdl");
        assertTrue("New wsdl file is not generated", outputFile.exists());
        WSDLToServiceProcessor processor = new WSDLToServiceProcessor();
        processor.setEnvironment(env);
        try {
            processor.parseWSDL(outputFile.getAbsolutePath());
            Service service = processor.getWSDLDefinition().getService(
                                                                       new QName(processor
                                                                           .getWSDLDefinition()
                                                                           .getTargetNamespace(),
                                                                                 "serviceins"));
            if (service == null) {
                fail("Element wsdl:service serviceins Missed!");
            }
            Iterator it = service.getPort("portins").getExtensibilityElements().iterator();
            if (service == null) {
                fail("Element wsdl:port portins Missed!");
            }
            boolean found = false;
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof AddressType) {
                    AddressType jmsAddress = (AddressType)obj;
                    if (!(jmsAddress.getDestinationStyle() != null
                          && "queue".equalsIgnoreCase(jmsAddress.getDestinationStyle().toString()))) {
                        break;
                    }
                    if (!(jmsAddress.getJndiDestinationName() != null && jmsAddress.getJndiDestinationName()
                        .equals("dynamicQueues/test.cxf.jmstransport.queue"))) {
                        break;
                    }

                    assertEquals(2, jmsAddress.getJMSNamingProperty().size());
                    assertEquals("java.naming.factory.initial",
                                 jmsAddress.getJMSNamingProperty().get(0).getName());
                    assertEquals("tcp://localhost:61616",
                                 jmsAddress.getJMSNamingProperty().get(1).getValue());

                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Element jms:address of service port Missed!");
            }
        } catch (ToolException e) {
            fail("Exception Encountered when parsing wsdl, error: " + e.getMessage());
        }
    }


    @Test
    public void testServiceExist() throws Exception {

        WSDLToServiceProcessor processor = new WSDLToServiceProcessor();

        env.put(ToolConstants.CFG_WSDLURL, getLocation("/misctools_wsdl/hello_world.wsdl"));
        env.put(ToolConstants.CFG_TRANSPORT, new String("http"));
        env.put(ToolConstants.CFG_SERVICE, new String("SOAPService_Test1"));
        env.put(ToolConstants.CFG_PORT, new String("SoapPort_Test1"));
        env.put(ToolConstants.CFG_BINDING_ATTR, new String("Greeter_SOAPBinding"));

        processor.setEnvironment(env);

        try {
            processor.process();
            fail("Do not catch expected tool exception for service and port exist");
        } catch (Exception e) {
            if (!(e instanceof ToolException && e.toString()
                .indexOf("Input service and port already exist in imported contract") >= 0)) {
                fail("Do not catch tool exception for service and port exist, "
                     + "catch other unexpected exception!");
            }
        }
    }

    @Test
    public void testBindingNotExist() throws Exception {

        WSDLToServiceProcessor processor = new WSDLToServiceProcessor();

        env.put(ToolConstants.CFG_WSDLURL, getLocation("/misctools_wsdl/hello_world.wsdl"));
        env.put(ToolConstants.CFG_TRANSPORT, new String("http"));
        env.put(ToolConstants.CFG_BINDING_ATTR, new String("BindingNotExist"));
        env.put(ToolConstants.CFG_SERVICE, new String("serviceins"));
        env.put(ToolConstants.CFG_PORT, new String("portins"));

        processor.setEnvironment(env);

        try {
            processor.process();
            fail("Do not catch expected tool exception for  binding not exist!");
        } catch (Exception e) {
            if (!(e instanceof ToolException && e.toString()
                .indexOf("Input binding does not exist in imported contract") >= 0)) {
                fail("Do not catch tool exception for binding not exist, "
                     + "catch other unexpected exception!");
            }
        }
    }
}
