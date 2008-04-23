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

import java.util.Iterator;

import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.types.ObjectFactory;
import org.junit.Assert;
import org.junit.Test;

public class XmlSeeAlsoAnnotatorTest extends Assert {

    @Test
    public void testAddXmlSeeAlsoAnnotation() throws Exception {
        JavaInterface intf = new JavaInterface();
        assertFalse(intf.getImports().hasNext());

        ClassCollector collector = new ClassCollector();
        collector.getTypesPackages().add(ObjectFactory.class.getPackage().getName());
        intf.annotate(new XmlSeeAlsoAnnotator(collector));

        Iterator iter = intf.getImports();
        assertEquals("javax.xml.bind.annotation.XmlSeeAlso", iter.next());
     
        assertEquals("@XmlSeeAlso({" + ObjectFactory.class.getName() + ".class})", 
                     intf.getAnnotations().iterator().next().toString());
    }
}
