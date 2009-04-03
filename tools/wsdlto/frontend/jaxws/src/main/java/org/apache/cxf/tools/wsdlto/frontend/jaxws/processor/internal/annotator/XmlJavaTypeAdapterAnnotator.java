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
package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;

public class XmlJavaTypeAdapterAnnotator implements Annotator {
    private JavaInterface jf;
    private Class<? extends XmlAdapter> adapter;

    public XmlJavaTypeAdapterAnnotator(JavaInterface intf, Class<? extends XmlAdapter> adapter) {
        this.jf = intf;
        this.adapter = adapter;
    }

    public void annotate(JavaAnnotatable jn) {

        JAnnotation jaxbAnnotation = new JAnnotation(XmlJavaTypeAdapter.class);
        jaxbAnnotation.addElement(new JAnnotationElement("value", adapter));
        if (jn instanceof JavaParameter) {
            JavaParameter jp = (JavaParameter)jn;
            jp.addAnnotation("XmlJavaTypeAdapter", jaxbAnnotation);
        } else if (jn instanceof JavaMethod) {
            JavaMethod jm = (JavaMethod)jn;
            jm.addAnnotation("XmlJavaTypeAdapter", jaxbAnnotation);
        } else {
            throw new RuntimeException("Annotation of " + jn.getClass() + " not supported.");
        }
        jf.addImport(XmlJavaTypeAdapter.class.getName());
        jf.addImport(adapter.getName());
    }

}
