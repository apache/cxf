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

public abstract class AbstractAnnotationVisitor implements AnnotationVisitor {
    protected Object target;
    protected Class<?> targetClass;
    

    private final List<Class<? extends Annotation>> targetAnnotations = 
                                 new ArrayList<Class<? extends Annotation>>(); 
    
    
    protected AbstractAnnotationVisitor(Class<? extends Annotation> ann) {
        addTargetAnnotation(ann);
    }
    
    protected AbstractAnnotationVisitor(List<Class<? extends Annotation>> ann) {
        targetAnnotations.addAll(ann);
    }

    protected final void addTargetAnnotation(Class<? extends Annotation> ann) { 
        targetAnnotations.add(ann); 
    } 

    public void visitClass(Class<?> clz, Annotation annotation) {
        // complete
    }

    public List<Class<? extends Annotation>> getTargetAnnotations() {
        return targetAnnotations;
    }

    public void visitField(Field field, Annotation annotation) {
        // complete
    }

    public void visitMethod(Method method, Annotation annotation) {
        // complete
    }

    public void setTarget(Object object) {
        target = object;
        targetClass = object.getClass();
    }
    public void setTarget(Object object, Class<?> cls) {
        target = object;
        targetClass = cls;
    }
    
    public Object getTarget() { 
        return target;
    } 
    public Class<?> getTargetClass() { 
        return targetClass;
    } 

}
