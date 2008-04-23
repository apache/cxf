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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaType;
import org.apache.cxf.tools.util.ClassCollector;

public final class XmlSeeAlsoAnnotator implements Annotator {
    private ClassCollector collector;

    public XmlSeeAlsoAnnotator(ClassCollector c) {
        this.collector = c;
    }
    
    public void annotate(JavaAnnotatable  ja) {
        if (collector == null || collector.getTypesPackages().isEmpty()) {
            return;
        }

        JavaInterface intf = null;
        if (ja instanceof JavaInterface) {
            intf = (JavaInterface) ja;
        } else {
            throw new RuntimeException("XmlSeeAlso can only annotate JavaInterface");
        }

        JAnnotation jaxbAnnotation = new JAnnotation(XmlSeeAlso.class);
        intf.addImports(jaxbAnnotation.getImports());
        
        List<JavaType> types = new ArrayList<JavaType>();
        for (String pkg : collector.getTypesPackages()) {
            if (pkg.equals(intf.getPackageName())) {
                types.add(new JavaType(null, "ObjectFactory", null));
            } else {
                types.add(new JavaType(null, pkg + ".ObjectFactory", null));
            }
        }
        jaxbAnnotation.addElement(new JAnnotationElement(null, types));
        intf.addAnnotation(jaxbAnnotation);
    }
}

