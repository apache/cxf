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

package org.apache.cxf.common.annotation;



import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class AnnotationProcessorTest extends Assert {

    AnnotatedGreeterImpl greeterImpl = new AnnotatedGreeterImpl(); 
    AnnotationProcessor processor = new AnnotationProcessor(greeterImpl); 
    List<Class<? extends Annotation>> expectedAnnotations = new ArrayList<Class<? extends Annotation>>(); 

    AnnotationVisitor visitor = EasyMock.createMock(AnnotationVisitor.class);
    
    @Before
    public void setUp() { 
        EasyMock.checkOrder(visitor, false); 
    } 

    @Test
    public void testVisitClass() { 

        expectedAnnotations.add(WebService.class);

        prepareCommonExpectations(visitor);
        visitor.visitClass((Class<?>)EasyMock.eq(AnnotatedGreeterImpl.class), 
                           (Annotation)EasyMock.isA(WebService.class));

        runProcessor(visitor);
    } 

    @Test
    public void testVisitField() throws Exception { 

        Field expectedField = AnnotatedGreeterImpl.class.getDeclaredField("foo"); 

        expectedAnnotations.add(Resource.class);
        prepareCommonExpectations(visitor);
        visitor.visitField(EasyMock.eq(expectedField), 
                           (Annotation)EasyMock.isA(Resource.class));
        visitor.visitMethod((Method)EasyMock.anyObject(), (Annotation)EasyMock.anyObject());

        runProcessor(visitor);
        
    } 

    @Test
    public void testVisitMethod() throws Exception {

        Field expectedField = AnnotatedGreeterImpl.class.getDeclaredField("foo"); 
        Method expectedMethod1 = AnnotatedGreeterImpl.class.getDeclaredMethod("sayHi"); 
        Method expectedMethod2 = AnnotatedGreeterImpl.class.getDeclaredMethod("sayHi", String.class); 
        Method expectedMethod3 = AnnotatedGreeterImpl.class.getDeclaredMethod("greetMe", String.class); 
        Method expectedMethod4 = 
            AnnotatedGreeterImpl.class.getDeclaredMethod("setContext", WebServiceContext.class); 
        Method expectedMethod5 = AnnotatedGreeterImpl.class.getDeclaredMethod("greetMeOneWay", String.class);

        expectedAnnotations.add(WebMethod.class);
        expectedAnnotations.add(Resource.class); 

        prepareCommonExpectations(visitor);
        visitor.visitField(EasyMock.eq(expectedField), 
                           (Annotation)EasyMock.isA(Resource.class));
        visitor.visitMethod(EasyMock.eq(expectedMethod1), 
                           (Annotation)EasyMock.isA(WebMethod.class));
        visitor.visitMethod(EasyMock.eq(expectedMethod2), 
                           (Annotation)EasyMock.isA(WebMethod.class));
        visitor.visitMethod(EasyMock.eq(expectedMethod3), 
                           (Annotation)EasyMock.isA(WebMethod.class));
        visitor.visitMethod(EasyMock.eq(expectedMethod4), 
                           (Annotation)EasyMock.isA(Resource.class));
        visitor.visitMethod(EasyMock.eq(expectedMethod5), 
                            (Annotation)EasyMock.isA(WebMethod.class));
        runProcessor(visitor);
    }

    @Test
    public void testProcessorInvalidConstructorArgs() { 
        
        try {
            new AnnotationProcessor(null); 
            fail("did not get expected argument");
        } catch (IllegalArgumentException e) {
            // happy
        }

    }

    @Test
    public void testProcessorInvalidAcceptArg() { 

        try {
            processor.accept(null);
            fail("did not get expected exception");
        } catch (IllegalArgumentException e) {
            // happy
        }

    } 


    private void prepareCommonExpectations(AnnotationVisitor v) {
        v.getTargetAnnotations();
        EasyMock.expectLastCall().andReturn(expectedAnnotations);
        v.setTarget(greeterImpl);
    }

    private void runProcessor(AnnotationVisitor v) { 
        EasyMock.replay(v); 
        processor.accept(v);
        EasyMock.verify(v); 
    } 
}

