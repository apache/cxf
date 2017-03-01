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

package org.apache.cxf.rt.security.claims;

import java.net.URI;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * This holds a collection of Claim Objects.
 */
public class ClaimCollection extends java.util.ArrayList<Claim> {

    private static final long serialVersionUID = -4543840943290756510L;

    private URI dialect =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity");
    private String dialectPrefix = "ic";

    public URI getDialect() {
        return dialect;
    }

    public void setDialect(URI dialect) {
        this.dialect = dialect;
    }

    public void serialize(XMLStreamWriter writer, String prefix, String namespace) throws XMLStreamException {
        writer.writeStartElement(prefix, "Claims", namespace);
        writer.writeNamespace(dialectPrefix, dialect.toString());
        writer.writeAttribute(null, "Dialect", dialect.toString());

        for (Claim claim : this) {
            claim.serialize(writer, dialectPrefix, dialect.toString());
        }

        writer.writeEndElement();
    }

    public String getDialectPrefix() {
        return dialectPrefix;
    }

    public void setDialectPrefix(String dialectPrefix) {
        this.dialectPrefix = dialectPrefix;
    }
}
