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

import jakarta.annotation.Resource;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;

import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AnnotationProcessorTest {

    AnnotatedGreeterImpl greeterImpl = new AnnotatedGreeterImpl();
    AnnotationProcessor processor = new AnnotationProcessor(greeterImpl);
    List<Class<? extends Annotation>> expectedAnnotations = new ArrayList<>();

    AnnotationVisitor visitor = mock(AnnotationVisitor.class);

    @Test
    public void testVisitClass() {

        expectedAnnotations.add(WebService.class);

        prepareCommonExpectations(visitor);
        visitor.visitClass(eq(AnnotatedGreeterImpl.class),
                           isA(WebService.class));

        runProcessor(visitor);
    }

    @Test
    public void testVisitField() throws Exception {

        Field expectedField = AnnotatedGreeterImpl.class.getDeclaredField("foo");

        expectedAnnotations.add(Resource.class);
        prepareCommonExpectations(visitor);
        visitor.visitField(eq(expectedField),
                           isA(Resource.class));
        visitor.visitMethod(any(Method.class), any(Annotation.class));

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
        visitor.visitField(eq(expectedField),
                           isA(Resource.class));
        visitor.visitMethod(eq(expectedMethod1),
                            isA(WebMethod.class));
        visitor.visitMethod(eq(expectedMethod2),
                            isA(WebMethod.class));
        visitor.visitMethod(eq(expectedMethod3),
                            isA(WebMethod.class));
        visitor.visitMethod(eq(expectedMethod4),
                            isA(Resource.class));
        visitor.visitMethod(eq(expectedMethod5),
                            isA(WebMethod.class));
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
        when(v.getTargetAnnotations()).thenReturn(expectedAnnotations);
        v.setTarget(greeterImpl);
    }

    private void runProcessor(AnnotationVisitor v) {
        processor.accept(v);
        verify(v, times(1)).getTargetAnnotations();
    }
}
