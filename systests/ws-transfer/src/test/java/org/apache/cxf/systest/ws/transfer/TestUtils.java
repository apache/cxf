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

import java.io.PrintWriter;
import javax.xml.transform.stream.StreamSource;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.systest.ws.transfer.resolver.MyResourceResolver;
import org.apache.cxf.systest.ws.transfer.transformer.StudentPutResourceTransformer;
import org.apache.cxf.systest.ws.transfer.transformer.TeacherResourceTransformer;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.transfer.manager.MemoryResourceManager;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.Resource;
import org.apache.cxf.ws.transfer.resource.ResourceLocal;
import org.apache.cxf.ws.transfer.resource.ResourceRemote;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactoryImpl;
import org.apache.cxf.ws.transfer.shared.TransferConstants;
import org.apache.cxf.ws.transfer.shared.handlers.ReferenceParameterAddingHandler;
import org.apache.cxf.ws.transfer.validationtransformation.XSDResourceValidator;
import org.apache.cxf.ws.transfer.validationtransformation.XSLTResourceTransformer;

/**
 * Parent test for all tests in WS-Transfer System Tests.
 * 
 * @author Erich Duda
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
        LoggingInInterceptor loggingIn = new LoggingInInterceptor(new PrintWriter(System.out));
        loggingIn.setPrettyLogging(true);
        LoggingOutInterceptor loggingOut = new LoggingOutInterceptor(new PrintWriter(System.out));
        loggingOut.setPrettyLogging(true);
        factory.getInInterceptors().add(loggingIn);
        factory.getOutInterceptors().add(loggingOut);
        factory.getInFaultInterceptors().add(loggingIn);
        factory.getOutFaultInterceptors().add(loggingOut);
        return (ResourceFactory) factory.create();
    }
    
    protected static Resource createResourceClient(EndpointReferenceType ref) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Resource.class);
        factory.setAddress(ref.getAddress().getValue());
        factory.getHandlers().add(new ReferenceParameterAddingHandler(ref.getReferenceParameters()));
        LoggingInInterceptor loggingIn = new LoggingInInterceptor(new PrintWriter(System.out));
        loggingIn.setPrettyLogging(true);
        LoggingOutInterceptor loggingOut = new LoggingOutInterceptor(new PrintWriter(System.out));
        loggingOut.setPrettyLogging(true);
        factory.getInInterceptors().add(loggingIn);
        factory.getOutInterceptors().add(loggingOut);
        factory.getInFaultInterceptors().add(loggingIn);
        factory.getOutFaultInterceptors().add(loggingOut);
        return (Resource) factory.create();
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
        resource.getValidators().add(new XSDResourceValidator(
            new StreamSource(TestUtils.class.getResourceAsStream("/schema/teacher.xsd")),
            new TeacherResourceTransformer()));
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
        resourceFactory.getValidators().add(
                new XSDResourceValidator(new StreamSource(
                    TestUtils.class.getResourceAsStream("/schema/studentCreate.xsd")),
                    new XSLTResourceTransformer(new StreamSource(
                        TestUtils.class.getResourceAsStream("/xslt/studentCreate.xsl")))));
        resourceFactory.getValidators().add(
                new XSDResourceValidator(new StreamSource(
                    TestUtils.class.getResourceAsStream("/schema/teacherCreateBasic.xsd")),
                    new XSLTResourceTransformer(new StreamSource(
                        TestUtils.class.getResourceAsStream("/xslt/teacherCreateBasic.xsl")))));
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory.class);
        factory.setServiceBean(resourceFactory);
        factory.setAddress(RESOURCE_FACTORY_URL);
        // Logging Interceptors
        LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        loggingInInterceptor.setPrettyLogging(true);
        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        loggingOutInterceptor.setPrettyLogging(true);
        factory.getInInterceptors().add(loggingInInterceptor);
        factory.getOutInterceptors().add(loggingOutInterceptor);
        
        return factory.create();
    }
    
    private static Server createStudentsResource(ResourceManager resourceManager) {
        ResourceLocal resourceLocal = new ResourceLocal();
        resourceLocal.setManager(resourceManager);
        resourceLocal.getValidators().add(
                new XSDResourceValidator(new StreamSource(
                    TestUtils.class.getResourceAsStream("/schema/studentPut.xsd")),
                        new StudentPutResourceTransformer()));
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(Resource.class);
        factory.setServiceBean(resourceLocal);
        factory.setAddress(RESOURCE_STUDENTS_URL);
        // Logging Interceptors
        LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        loggingInInterceptor.setPrettyLogging(true);
        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        loggingOutInterceptor.setPrettyLogging(true);
        factory.getInInterceptors().add(loggingInInterceptor);
        factory.getOutInterceptors().add(loggingOutInterceptor);
        return factory.create();
    }
    
    private static Server createTeachersResourceFactoryEndpoint(ResourceRemote resource) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(ResourceFactory.class);
        factory.setServiceBean(resource);
        factory.setAddress(RESOURCE_TEACHERS_URL + TransferConstants.RESOURCE_REMOTE_SUFFIX);
        // Logging Interceptors
        LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        loggingInInterceptor.setPrettyLogging(true);
        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        loggingOutInterceptor.setPrettyLogging(true);
        factory.getInInterceptors().add(loggingInInterceptor);
        factory.getOutInterceptors().add(loggingOutInterceptor);
        return factory.create();
    }
    
    private static Server createTeacherResourceEndpoint(ResourceRemote resource) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(Resource.class);
        factory.setServiceBean(resource);
        factory.setAddress(RESOURCE_TEACHERS_URL);
        // Logging Interceptors
        LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        loggingInInterceptor.setPrettyLogging(true);
        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        loggingOutInterceptor.setPrettyLogging(true);
        factory.getInInterceptors().add(loggingInInterceptor);
        factory.getOutInterceptors().add(loggingOutInterceptor);
        return factory.create();
    }
    
}
