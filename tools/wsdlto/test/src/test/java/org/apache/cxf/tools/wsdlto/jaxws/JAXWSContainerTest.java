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

package org.apache.cxf.tools.wsdlto.jaxws;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.FrontEndGenerator;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.apache.cxf.tools.validator.ServiceValidator;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.JAXWSContainer;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.WSDLToJavaProcessor;
import org.junit.Test;

public class JAXWSContainerTest extends ProcessorTestBase {

    @Test    
    public void testCodeGen() {
        try {
            JAXWSContainer container = new JAXWSContainer(null);
            ToolContext context = new ToolContext();

            // By default we only generate the SEI/Types/Exception classes/Service Class(client stub)
            // Uncomment to generate the impl class
            // context.put(ToolConstants.CFG_IMPL, "impl");
        
            // Uncomment to compile the generated classes
            // context.put(ToolConstants.CFG_COMPILE, ToolConstants.CFG_COMPILE);
            
            // Where to put the compiled classes
            // context.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");

            // Where to put the generated source code
            context.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());

            context.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));

            // Delegate jaxb to generate the type classes
            context.put(DataBindingProfile.class, PluginLoader.getInstance().getDataBindingProfile("jaxb"));
            context.put(FrontEndProfile.class, PluginLoader.getInstance().getFrontEndProfile("jaxws"));

            // In case you want to remove some generators
            List<String> generatorNames = Arrays.asList(new String[]{ToolConstants.CLT_GENERATOR,
                                                                     ToolConstants.SVR_GENERATOR,
                                                                     ToolConstants.IMPL_GENERATOR,
                                                                     ToolConstants.ANT_GENERATOR,
                                                                     ToolConstants.SERVICE_GENERATOR,
                                                                     ToolConstants.FAULT_GENERATOR,
                                                                     ToolConstants.SEI_GENERATOR});
            FrontEndProfile frontend = context.get(FrontEndProfile.class);
            List<FrontEndGenerator> generators = frontend.getGenerators();
            for (FrontEndGenerator generator : generators) {
                assertTrue(generatorNames.contains(generator.getName()));
            }

            container.setContext(context);
            // Now shoot
            container.execute();

            // At this point you should be able to get the
            // SEI/Service(Client stub)/Exception classes/Types classes
            assertNotNull(output.list());
            assertEquals(1, output.list().length);

            assertTrue(new File(output, "org/apache/cxf/w2j/hello_world_soap_http/Greeter.java").exists());
            assertTrue(new File(output,
                                "org/apache/cxf/w2j/hello_world_soap_http/SOAPService.java").exists());
            assertTrue(new File(output,
                                "org/apache/cxf/w2j/hello_world_soap_http/NoSuchCodeLitFault.java").exists());
            assertTrue(new File(output,
                                "org/apache/cxf/w2j/hello_world_soap_http/types/SayHi.java").exists());
            assertTrue(new File(output,
                                "org/apache/cxf/w2j/hello_world_soap_http/types/GreetMe.java").exists());

            // Now you can get the JavaModel from the context.
            JavaModel javaModel = context.get(JavaModel.class);

            Map<String, JavaInterface> interfaces = javaModel.getInterfaces();
            assertEquals(1, interfaces.size());

            JavaInterface intf = interfaces.values().iterator().next();
            assertEquals("http://cxf.apache.org/w2j/hello_world_soap_http", intf.getNamespace());
            assertEquals("Greeter", intf.getName());
            assertEquals("org.apache.cxf.w2j.hello_world_soap_http", intf.getPackageName());

            List<JavaMethod> methods = intf.getMethods();
            assertEquals(6, methods.size());
            Boolean methodSame = false;
            for (JavaMethod m1 : methods) {
                if (m1.getName().equals("testDocLitFault")) {
                    methodSame = true;
                    break;
                }
            }
            assertTrue(methodSame);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test    
    public void testSuppressCodeGen() {
        try {
            JAXWSContainer container = new JAXWSContainer(null);
            ToolContext context = new ToolContext();

            // Do not generate any artifacts, we just want the code model.
            context.put(ToolConstants.CFG_SUPPRESS_GEN, "suppress");

            // Where to put the generated source code
            context.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());

            context.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/hello_world.wsdl"));

            // Delegate jaxb to generate the type classes
            context.put(DataBindingProfile.class, PluginLoader.getInstance().getDataBindingProfile("jaxb"));
            context.put(FrontEndProfile.class, PluginLoader.getInstance().getFrontEndProfile("jaxws"));

            container.setContext(context);
            // Now shoot
            container.execute();

            // At this point you should be able to get the
            // SEI/Service(Client stub)/Exception classes/Types classes
            assertNotNull(output.list());
            assertEquals(0, output.list().length);

            // Now you can get the JavaModel from the context.
            Map<QName, JavaModel> map = CastUtils.cast((Map)context.get(WSDLToJavaProcessor.MODEL_MAP));
            JavaModel javaModel = map.get(new QName("http://cxf.apache.org/w2j/hello_world_soap_http",
                                                    "SOAPService"));
            assertNotNull(javaModel);

            Map<String, JavaInterface> interfaces = javaModel.getInterfaces();
            assertEquals(1, interfaces.size());

            JavaInterface intf = interfaces.values().iterator().next();
            String interfaceName = intf.getName();
            assertEquals("Greeter", interfaceName);
            assertEquals("http://cxf.apache.org/w2j/hello_world_soap_http", intf.getNamespace());
            assertEquals("org.apache.cxf.w2j.hello_world_soap_http", intf.getPackageName());

            List<JavaMethod> methods = intf.getMethods();
            assertEquals(6, methods.size());

            Boolean methodSame = false;
            JavaMethod m1 = null;
            for (JavaMethod m2 : methods) {
                if (m2.getName().equals("testDocLitFault")) {
                    methodSame = true;
                    m1 = m2;
                    break;
                }
            }
            assertTrue(methodSame);
            
            assertEquals(2, m1.getExceptions().size());
            assertEquals("BadRecordLitFault", m1.getExceptions().get(0).getName());
            assertEquals("NoSuchCodeLitFault", m1.getExceptions().get(1).getName());

            String address = null;

            for (JavaServiceClass service : javaModel.getServiceClasses().values()) {
                if ("SOAPService_Test1".equals(service.getName())) {
                    continue;
                }
                List<JavaPort> ports = (List<JavaPort>) service.getPorts();
                for (JavaPort port : ports) {
                    if (interfaceName.equals(port.getPortType())) {
                        address = port.getBindingAdress();
                        break;
                    }
                }
                if (!"".equals(address)) {
                    break;
                }
            }
            assertEquals("http://localhost:9000/SoapContext/SoapPort", address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testGetServceValidator() throws Exception {
        JAXWSContainer container = new JAXWSContainer(null);
        List<ServiceValidator> validators = container.getServiceValidators();
        assertNotNull(validators);
        assertTrue(validators.size() > 0);
    }

    protected String getLocation(String wsdlFile) throws URISyntaxException {
        return getClass().getResource(wsdlFile).toString();
    }
}
