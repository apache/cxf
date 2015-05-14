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


package org.apache.cxf.systest.ws.transfer;

import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.systest.ws.transfer.resolver.MyResourceResolver;
import org.apache.cxf.systest.ws.transfer.validator.StudentPutResourceValidator;
import org.apache.cxf.systest.ws.transfer.validator.TeacherResourceValidator;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.transfer.manager.MemoryResourceManager;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.Resource;
import org.apache.cxf.ws.transfer.resource.ResourceLocal;
import org.apache.cxf.ws.transfer.resource.ResourceRemote;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactoryImpl;
import org.apache.cxf.ws.transfer.shared.TransferConstants;
import org.apache.cxf.ws.transfer.validationtransformation.XSDResourceTypeIdentifier;
import org.apache.cxf.ws.transfer.validationtransformation.XSLTResourceTransformer;

/**
 * Parent test for all tests in WS-Transfer System Tests.
 */
public final class TestUtils {
    
    public static final String RESOURCE_STUDENTS_URL = "http://localhost:8080/ResourceStudents";
    
    public static final String RESOURCE_FACTORY_URL = "http://localhost:8080/ResourceFactory";
    
    public static final String RESOURCE_TEACHERS_URL = "http://localhost:8081/ResourceTeachers";
    
    private static Server resourceFactoryServer;
    
    private static Server studentsResourceServer;
    
    private static Server teachersResourceFactoryServer;
    
    private static Server teachersResourceServer;
    
    private TestUtils() {
        
    }
    
    protected static ResourceFactory createResourceFactoryClient() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory.class);
        factory.setAddress(RESOURCE_FACTORY_URL);
        return (ResourceFactory) factory.create();
    }
    
    protected static Resource createResourceClient(EndpointReferenceType ref) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Resource.class);
        factory.setAddress(ref.getAddress().getValue());
        Resource proxy = (Resource) factory.create();

        // Add reference parameters
        AddressingProperties addrProps = new AddressingProperties();
        addrProps.setTo(ref);
        ((BindingProvider) proxy).getRequestContext().put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProps);

        return proxy;
    }
    
    protected static void createStudentsServers() {
        UIDManager.reset();
        ResourceManager studentsResourceManager = new MemoryResourceManager();
        resourceFactoryServer = createResourceFactory(studentsResourceManager);
        studentsResourceServer = createStudentsResource(studentsResourceManager);
    }
    
    protected static void createTeachersServers() {
        ResourceManager teachersResourceManager = new MemoryResourceManager();
        ResourceRemote resource = new ResourceRemote();
        resource.setManager(teachersResourceManager);
        resource.getResourceTypeIdentifiers().add(new XSDResourceTypeIdentifier(
                new StreamSource(TestUtils.class.getResourceAsStream("/schema/teacher.xsd")),
                new XSLTResourceTransformer(
                        new StreamSource(TestUtils.class.getResourceAsStream("/xslt/teacherDefaultValues.xsl")),
                        new TeacherResourceValidator())));
        teachersResourceFactoryServer = createTeachersResourceFactoryEndpoint(resource);
        teachersResourceServer = createTeacherResourceEndpoint(resource);
    }
    
    protected static void destroyStudentsServers() {
        resourceFactoryServer.destroy();
        studentsResourceServer.destroy();
    }
    
    protected static void destroyTeachersServers() {
        teachersResourceFactoryServer.destroy();
        teachersResourceServer.destroy();
    }
    
    private static Server createResourceFactory(ResourceManager resourceManager) {
        ResourceFactoryImpl resourceFactory = new ResourceFactoryImpl();
        resourceFactory.setResourceResolver(
                new MyResourceResolver(RESOURCE_STUDENTS_URL, resourceManager, RESOURCE_TEACHERS_URL));
        resourceFactory.getResourceTypeIdentifiers().add(
                new XSDResourceTypeIdentifier(
                        new StreamSource(TestUtils.class.getResourceAsStream("/schema/studentCreate.xsd")),
                        new XSLTResourceTransformer(
                                new StreamSource(TestUtils.class.getResourceAsStream("/xslt/studentCreate.xsl")))));
        resourceFactory.getResourceTypeIdentifiers().add(
                new XSDResourceTypeIdentifier(
                        new StreamSource(TestUtils.class.getResourceAsStream("/schema/teacherCreateBasic.xsd")),
                        new XSLTResourceTransformer(
                                new StreamSource(
                                        TestUtils.class.getResourceAsStream("/xslt/teacherCreateBasic.xsl")))));
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory.class);
        factory.setServiceBean(resourceFactory);
        factory.setAddress(RESOURCE_FACTORY_URL);
        
        return factory.create();
    }
    
    private static Server createStudentsResource(ResourceManager resourceManager) {
        ResourceLocal resourceLocal = new ResourceLocal();
        resourceLocal.setManager(resourceManager);
        resourceLocal.getResourceTypeIdentifiers().add(
                new XSDResourceTypeIdentifier(
                        new StreamSource(TestUtils.class.getResourceAsStream("/schema/studentPut.xsd")),
                        new XSLTResourceTransformer(
                                new StreamSource(TestUtils.class.getResourceAsStream("/xslt/studentPut.xsl")),
                                new StudentPutResourceValidator())));
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(Resource.class);
        factory.setServiceBean(resourceLocal);
        factory.setAddress(RESOURCE_STUDENTS_URL);
        return factory.create();
    }
    
    private static Server createTeachersResourceFactoryEndpoint(ResourceRemote resource) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(ResourceFactory.class);
        factory.setServiceBean(resource);
        factory.setAddress(RESOURCE_TEACHERS_URL + TransferConstants.RESOURCE_REMOTE_SUFFIX);
        return factory.create();
    }
    
    private static Server createTeacherResourceEndpoint(ResourceRemote resource) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(Resource.class);
        factory.setServiceBean(resource);
        factory.setAddress(RESOURCE_TEACHERS_URL);
        return factory.create();
    }
    
}
