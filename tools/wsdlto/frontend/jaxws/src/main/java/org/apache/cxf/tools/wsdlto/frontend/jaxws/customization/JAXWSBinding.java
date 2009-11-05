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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.customization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

public class JAXWSBinding implements ExtensibilityElement, Serializable {

    private Boolean enableAsyncMapping;
    private Boolean enableWrapperStyle;
    private Boolean enableMime;

    private Element element;
    private boolean required;
    private QName elementType;


    private String documentBaseURI;

    private String packageName;

    private String methodName;
    
    private String methodJavaDoc;
    
    private String packageJavaDoc;
    
    private String classJavaDoc;

    private List<JAXWSParameter> jaxwsPara;

    private JAXWSClass jaxwsClass;

    public void setDocumentBaseURI(String baseURI) {
        this.documentBaseURI = baseURI;
    }

    public String getDocumentBaseURI() {
        return this.documentBaseURI;
    }

    public void setElement(Element elem) {
        this.element = elem;
    }

    public Element getElement() {
        return element;
    }

    public void setRequired(Boolean r) {
        this.required = r;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setElementType(QName elemType) {
        this.elementType = elemType;
    }

    public QName getElementType() {
        return elementType;
    }

    public boolean isEnableMime() {
        return enableMime == null ? false : enableMime;
    }
    public boolean isSetEnableMime() {
        return enableMime != null;
    }
    public void setEnableMime(boolean enableMime) {
        this.enableMime = enableMime;
    }

    public boolean isEnableAsyncMapping() {
        return enableAsyncMapping == null ? false : enableAsyncMapping;
    }
    public boolean isSetEnableAsyncMapping() {
        return enableAsyncMapping != null;
    }
    public void setEnableAsyncMapping(boolean enableAsync) {
        this.enableAsyncMapping = enableAsync;
    }

    public boolean isEnableWrapperStyle() {
        return enableWrapperStyle == null ? true : enableWrapperStyle;
    }
    public boolean isSetEnableWrapperStyle() {
        return enableWrapperStyle != null;
    }
    public void setEnableWrapperStyle(boolean pEnableWrapperStyle) {
        this.enableWrapperStyle = pEnableWrapperStyle;
    }

    public void setPackage(String pkg) {
        this.packageName = pkg;
    }

    public String getPackage() {
        return this.packageName;
    }

    public void addJaxwsPara(JAXWSParameter para) {
        if (jaxwsPara == null) {
            jaxwsPara = new ArrayList<JAXWSParameter>();
        }
        jaxwsPara.add(para);
    }

    public void setJaxwsParas(List<JAXWSParameter> p) {
        jaxwsPara = new ArrayList<JAXWSParameter>(p);
    }
    public List<JAXWSParameter> getJaxwsParas() {
        return jaxwsPara;
    }

    public void setJaxwsClass(JAXWSClass clz) {
        this.jaxwsClass = clz;
    }

    public JAXWSClass getJaxwsClass() {
        return this.jaxwsClass;
    }


    public void setMethodName(String name) {
        methodName = name;
    }

    public String getMethodName() {
        return this.methodName;
    }
    
    public void setMethodJavaDoc(String doc) {
        methodJavaDoc = doc;
    }
    
    public String getMethodJavaDoc() {   
        return (methodJavaDoc != null) ? methodJavaDoc : "";
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
}
