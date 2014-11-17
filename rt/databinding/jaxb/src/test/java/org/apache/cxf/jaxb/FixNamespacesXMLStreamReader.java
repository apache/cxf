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

package org.apache.cxf.jaxb;

import java.util.Collection;

import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallerImpl;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

/**
 * A sample of an unmarshaller aware xml stream reader that can adjust the namespace of elements read in, as long
 * as the local name matches.  Based on the same kind of thing for SAX encountered at 
 * 
 * http://stackoverflow.com/questions/277502/jaxb-how-to-ignore-namespace-during-unmarshalling-xml-document
 */
public class FixNamespacesXMLStreamReader extends StreamReaderDelegate implements UnmarshallerAwareXMLStreamReader {
    private UnmarshallingContext ctx;

    public FixNamespacesXMLStreamReader(final XMLStreamReader delegate) {
        super(delegate);
    }

    public String getNamespaceURI() {
        String localName = getLocalName();
        String namespace = super.getNamespaceURI();
        namespace = getMappedNamespace(localName, namespace);
        return namespace;
    }

    private String getMappedNamespace(String localName, String namespace) {
        if (ctx != null) {
            Collection<QName> expected = ctx.getCurrentExpectedElements();
            for (QName expectedQname : expected) {
                if (localName.equals(expectedQname.getLocalPart())) {
                    return expectedQname.getNamespaceURI();
                }
            }
        }
        return namespace;
    }

    @Override
    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.ctx = ((UnmarshallerImpl) unmarshaller).getContext();
    }
}
