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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import org.w3c.dom.Element;

import jakarta.jws.soap.SOAPBinding;
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

    private final List<JavaMethod> methods = new ArrayList<>();
    private final List<JAnnotation> annotations = new ArrayList<>();
    private final Set<String> imports = new TreeSet<>();
    private final List<String> supers = new ArrayList<>();

    private String webserviceName;
    private Element handlerChains;

    public JavaInterface() {
    }
    public JavaInterface(JavaModel m) {
        this.model = m;
    }


    static String formatJavaDoc(String d, String spaces) {
        if (d != null) {
            StringBuilder d2 = new StringBuilder(d.length());
            StringReader r = new StringReader(d);
            BufferedReader r2 = new BufferedReader(r);
            try {
                String s2 = r2.readLine();
                String pfx = null;
                while (s2 != null) {
                    if (pfx == null && s2.length() > 0) {
                        pfx = "";
                        while (s2.length() > 0 && Character.isWhitespace(s2.charAt(0))) {
                            pfx += " ";
                            s2 = s2.substring(1);
                        }
                    }
                    if (pfx != null) {
                        if (d2.length() > 0) {
                            d2.append('\n');
                        }
                        d2.append(spaces).append("* ");
                        if (s2.startsWith(pfx)) {
                            d2.append(s2.substring(pfx.length()));
                        } else {
                            d2.append(s2);
                        }
                    }
                    s2 = r2.readLine();
                }
                d = d2.toString();
            } catch (IOException ex) {
                //ignore, use the raw value
            }
        }
        return d;
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

    public List<String> getSuperInterfaces() {
        return supers;
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

    public void addSuperInterface(String s) {
        if (s.contains(".")) {
            if (!s.startsWith("java.lang.")) {
                addImport(s);
            }
            s = s.substring(s.lastIndexOf('.') + 1);
        }
        supers.add(s);
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
        packageJavaDoc = formatJavaDoc(doc, " ");
    }

    public String getPackageJavaDoc() {
        return (packageJavaDoc != null) ? packageJavaDoc : "";
    }

    public void setClassJavaDoc(String doc) {
        classJavaDoc = formatJavaDoc(doc, " ");
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
        if (i != null && i.lastIndexOf('.') != -1 && getPackageName() != null
            && getPackageName().equals(i.substring(0, i.lastIndexOf('.')))) {
            return;
        }
        // replace "$" with "." to correctly deal with member classes
        if (i != null) {
            imports.add(i.replaceAll("\\$", "\\."));
        }
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
        int index = fullName.lastIndexOf('.');
        setPackageName(fullName.substring(0, index));
        setName(fullName.substring(index + 1, fullName.length()));
    }

    public String getFullClassName() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPackageName());
        sb.append('.');
        sb.append(getName());
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (JAnnotation anno : annotations) {
            sb.append(anno);
            sb.append('\n');
        }
        sb.append(getFullClassName());
        return sb.toString();
    }
}
