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

package org.apache.cxf.tools.common.model;

import java.util.*;
import javax.jws.soap.SOAPBinding;

import org.w3c.dom.Element;

import org.apache.cxf.tools.common.ToolException;

public class JavaInterface implements JavaAnnotatable {

    private String name;
    private String packageName;
    private String namespace;
    private String location;
    private String packageJavaDoc;
    private String classJavaDoc;
    private JavaModel model;
    private SOAPBinding.Style soapStyle;
    private SOAPBinding.Use soapUse;
    private SOAPBinding.ParameterStyle soapParameterStyle;
    
    private final List<JavaMethod> methods = new ArrayList<JavaMethod>();
    private final List<JAnnotation> annotations = new ArrayList<JAnnotation>();
    private final Set<String> imports = new TreeSet<String>();

    private String webserviceName;
    private Element handlerChains;
      
    public JavaInterface() {
    }
    
    public JavaInterface(JavaModel m) {
        this.model = m;
    }

    public void setWebServiceName(String wsn) {
        this.webserviceName = wsn;
    }

    public String getWebServiceName() {
        return this.webserviceName;
    }

    public void setSOAPStyle(SOAPBinding.Style s) {
        this.soapStyle = s;
    }

    public SOAPBinding.Style getSOAPStyle() {
        return this.soapStyle;
    }

    public void setSOAPUse(SOAPBinding.Use u) {
        this.soapUse = u;
    }

    public SOAPBinding.Use getSOAPUse() {
        return this.soapUse;
    }

    public void setSOAPParameterStyle(SOAPBinding.ParameterStyle p) {
        this.soapParameterStyle = p;
    }    
    
    public SOAPBinding.ParameterStyle getSOAPParameterStyle() {
        return this.soapParameterStyle;
    }
    
    public JavaModel getJavaModel() {
        return this.model;
    }
    
    public void setName(String n) {
        this.name = n;
    }
    
    public String getName() {
        return name;
    }

    public void setLocation(String l) {
        this.location = l;
    }

    public String getLocation() {
        return this.location;
    }

    public List<JavaMethod> getMethods() {
        return methods;
    }

    public boolean hasMethod(JavaMethod method) {
        if (method != null) {
            String signature = method.getSignature();
            for (int i = 0; i < methods.size(); i++) {
                if (signature.equals(methods.get(i).getSignature())) {
                    return true;
                }
            }
        }
        return false;
    }

    public int indexOf(JavaMethod method) {
        if (method != null) {
            String signature = method.getSignature();
            for (int i = 0; i < methods.size(); i++) {
                if (signature.equals(methods.get(i).getSignature())) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int removeMethod(JavaMethod method) {
        int index = indexOf(method);
        if (index > -1) {
            methods.remove(index);
        }
        return index;
    }

    public void replaceMethod(JavaMethod method) {
        int index = removeMethod(method);
        methods.add(index, method);
    }

    public void addMethod(JavaMethod method) throws ToolException {
        if (hasMethod(method)) {
            replaceMethod(method);
        } else {
            methods.add(method);
        }
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String pn) {
        this.packageName = pn;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String ns) {
        this.namespace = ns;
    }
    
    public void setPackageJavaDoc(String doc) {
        packageJavaDoc = doc;
    }
    
    public String getPackageJavaDoc() {   
        return (packageJavaDoc != null) ? packageJavaDoc : "";
    }
    
    public void setClassJavaDoc(String doc) {
        classJavaDoc = doc;
    }
    
    public String getClassJavaDoc() {   
        return (classJavaDoc != null) ? classJavaDoc : "";
    }

    public void addAnnotation(JAnnotation annotation) {
        this.annotations.add(annotation);
        for (String importClz : annotation.getImports()) {
            addImport(importClz);
        }        
    }

    public List<JAnnotation> getAnnotations() {
        return this.annotations;
    }

    public void addImport(String i) {
        if (i != null && i.lastIndexOf(".") != -1 && getPackageName() != null
            && getPackageName().equals(i.substring(0, i.lastIndexOf(".")))) {
            return;
        }
        // replace "$" with "." to correctly deal with member classes
        imports.add(i.replaceAll("\\$", "\\."));
    }
    
    public void addImports(Collection<String> ii) {
        for (String i : ii) {
            // replace "$" with "." to correctly deal with member classes
            imports.add(i.replaceAll("\\$", "\\."));
        }
    }

    public Iterator<String> getImports() {
        return imports.iterator();
    }

    public void setJavaModel(JavaModel jm) {
        this.model = jm;
    }

    public void annotate(Annotator annotator) {
        annotator.annotate(this);
    }
    
    public Element getHandlerChains() {
        return this.handlerChains;
    }

    public void setHandlerChains(Element elem) {
        this.handlerChains = elem;
    }

    public void setFullClassName(String fullName) {
        int index = fullName.lastIndexOf(".");
        setPackageName(fullName.substring(0, index));
        setName(fullName.substring(index + 1, fullName.length()));
    }

    public String getFullClassName() {
        StringBuffer sb = new StringBuffer();
        sb.append(getPackageName());
        sb.append(".");
        sb.append(getName());
        return sb.toString();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (JAnnotation anno : annotations) {
            sb.append(anno);
            sb.append("\n");
        }
        sb.append(getFullClassName());
        return sb.toString();
    }
}
