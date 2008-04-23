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

import javax.xml.bind.annotation.XmlList;

import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;

public class XmlListAnotator implements Annotator {
    private JavaInterface jf;

    public XmlListAnotator(JavaInterface intf) {
        jf = intf;
    }

    public void annotate(JavaAnnotatable jn) {
        
        JAnnotation jaxbAnnotation = new JAnnotation(XmlList.class);
        if (jn instanceof JavaParameter) {
            JavaParameter jp = (JavaParameter)jn;
            jp.addAnnotation("XmlList", jaxbAnnotation);
        } else if (jn instanceof JavaMethod) {
            JavaMethod jm = (JavaMethod)jn;
            jm.addAnnotation("XmlList", jaxbAnnotation);
        } else {
            throw new RuntimeException("XmlList can only annotate to JavaParameter or JavaMethod");
        }
        
        jf.addImport(XmlList.class.getName());

    }

}
