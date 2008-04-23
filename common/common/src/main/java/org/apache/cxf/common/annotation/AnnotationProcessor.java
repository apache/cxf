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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;


/** Process instance of an annotated class.  This is a visitable
 * object that allows an caller to visit that annotated elements in
 * this class definition.  If a class level annotation is overridden
 * by a member level annotation, only the visit method for the member
 * level annotation
 */
public  class AnnotationProcessor {
    
    private static final Logger LOG = LogUtils.getL7dLogger(AnnotationProcessor.class); 
    
    private static Method visitClassMethod; 
    private static Method visitFieldMethod; 
    private static Method visitMethodMethod; 
    
    static { 
        try {
            visitClassMethod = AnnotationVisitor.class.getMethod("visitClass", Class.class, Annotation.class);
            visitFieldMethod = AnnotationVisitor.class.getMethod("visitField", Field.class, Annotation.class);
            visitMethodMethod = AnnotationVisitor.class.getMethod("visitMethod", 
                                                                  Method.class, Annotation.class);
            
        } catch (NoSuchMethodException e) {
            // ignore
        }
        
    } 
    
    private final Object target; 
    private List<Class<? extends Annotation>> annotationTypes; 
    
    
    public AnnotationProcessor(Object o) {
        if (o == null) {
            throw new IllegalArgumentException(new Message("INVALID_CTOR_ARGS", LOG).toString()); 
        }
        target = o; 
    }
    
    /** 
     * Visits each of the annotated elements of the object.
     * 
     * @param visitor a visitor 
     * @param claz the Class of the targe object
     */
    public void accept(AnnotationVisitor visitor, Class<?> claz) { 
        
        if (visitor == null) {
            throw new IllegalArgumentException();
        }
        
        annotationTypes = visitor.getTargetAnnotations();
        visitor.setTarget(target);
        //recursively check annotation in super class
        processClass(visitor, claz);
        processFields(visitor, claz); 
        processMethods(visitor, claz);
    } 
    
    public void accept(AnnotationVisitor visitor) {
        accept(visitor, target.getClass());
    }
    
    
    private void processMethods(AnnotationVisitor visitor, Class<? extends Object> targetClass) {

        if (targetClass.getSuperclass() != null) {
            processMethods(visitor, targetClass.getSuperclass());
        }
        visitAnnotatedElement(targetClass.getDeclaredMethods(), visitor, visitMethodMethod); 
    }
    
    private void processFields(AnnotationVisitor visitor, Class<? extends Object> targetClass) { 
        if (targetClass.getSuperclass() != null) {
            processFields(visitor, targetClass.getSuperclass());
        }
        visitAnnotatedElement(targetClass.getDeclaredFields(), visitor, visitFieldMethod); 
    } 
    
    
    private void processClass(AnnotationVisitor visitor, Class<? extends Object> targetClass) {
        if (targetClass.getSuperclass() != null) {
            processClass(visitor, targetClass.getSuperclass());
        }
        Class<?>[] classes = {targetClass}; 
        visitAnnotatedElement(classes, visitor, visitClassMethod);
    }
    
    private <U extends AnnotatedElement> void visitAnnotatedElement(U[] elements, 
                                                                    AnnotationVisitor visitor,
                                                                    Method visitorMethod) {
        
        for (U element : elements) {
            for (Class<? extends Annotation> clz : annotationTypes) {
                Annotation ann = element.getAnnotation(clz); 
                if (ann != null) {
                    try {                        
                        visitorMethod.invoke(visitor, element, ann);
                    } catch (IllegalAccessException e) {
                        // ignore, we're invoking methods of a public interface
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause() == null ? e : e.getCause();
                        LogUtils.log(LOG, Level.SEVERE, "VISITOR_RAISED_EXCEPTION", cause, visitor, element);
                    }
                }
            }
        }
    }
}
