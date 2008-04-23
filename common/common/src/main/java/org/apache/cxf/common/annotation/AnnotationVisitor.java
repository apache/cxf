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
import java.util.List;

/** Visits the annotated elements of an object
 *
 */
public interface AnnotationVisitor {

    /** set the target object being visited.  Invoked before any of
     * the visit methods.   
     *
     * @see AnnotationProcessor
     *
     * @param target the target object 
     */ 
    void setTarget(Object target);


    /** return the list of annotations this visitor wants to be
     * informed about.
     *
     * @return list of annotation types to be informed about
     *
     */
    List<Class<? extends Annotation>> getTargetAnnotations(); 
    
    /** visit an annotated class. Invoked when the class of an object
     * is annotated by one of the specified annotations.
     * <code>visitClass</code> is called for each of the annotations
     * that matches and for each class. 
     *
     * @param clz the class with the annotation
     * @param annotation the annotation 
     *
     */
    void visitClass(Class<?> clz, Annotation annotation); 

    
    /** visit an annotated field. Invoked when the field of an object
     * is annotated by one of the specified annotations.
     * <code>visitField</code> is called for each of the annotations
     * that matches and for each field. 
     *
     * @param field the annotated field
     * @param annotation the annotation 
     *
     */
    void visitField(Field field, Annotation annotation); 

    /** visit an annotated method. Invoked when the method of an object
     * is annotated by one of the specified annotations.
     * <code>visitMethod</code> is called for each of the annotations
     * that matches and for each method. 
     *
     * @param method the annotated fieldx
     * @param annotation the annotation 
     *
     */
    void visitMethod(Method method, Annotation annotation); 
}
