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

package demo.sts.provider.token;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.DOMUtils;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;

public final class SamlUtils {

    private SamlUtils() {
        
    }
    
    public static Document toDom(XMLObject object) throws MarshallingException,
            ParserConfigurationException, ConfigurationException {
        Document document = DOMUtils.createDocument();

        DefaultBootstrap.bootstrap();

        Marshaller out = Configuration.getMarshallerFactory().getMarshaller(
                object);
        out.marshall(object, document);
        return document;
    }

}
