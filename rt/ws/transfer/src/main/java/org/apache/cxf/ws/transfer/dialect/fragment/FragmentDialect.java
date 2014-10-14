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

package org.apache.cxf.ws.transfer.dialect.fragment;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.Dialect;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.UnsupportedLanguage;
import org.apache.cxf.ws.transfer.dialect.fragment.language.FragmentDialectLanguage;
import org.apache.cxf.ws.transfer.dialect.fragment.language.FragmentDialectLanguageQName;
import org.apache.cxf.ws.transfer.dialect.fragment.language.FragmentDialectLanguageXPath10;
import org.apache.cxf.ws.transfer.shared.TransferTools;
import org.apache.cxf.ws.transfer.shared.faults.UnknownDialect;

/**
 * Implementation of the WS-Fragment dialect.
 * 
 * @author Erich Duda
 */
public class FragmentDialect implements Dialect {
    
    private final Map<String, FragmentDialectLanguage> languages;
    
    public FragmentDialect() {
        languages = new HashMap<String, FragmentDialectLanguage>();
        languages.put(FragmentDialectConstants.QNAME_LANGUAGE_IRI, new FragmentDialectLanguageQName());
        languages.put(FragmentDialectConstants.XPATH10_LANGUAGE_IRI, new FragmentDialectLanguageXPath10());
    }

    @Override
    public JAXBElement<ValueType> processGet(Get body, Representation representation) {
        for (Object o : body.getAny()) {
            if (o instanceof JAXBElement && ((JAXBElement)o).getValue() instanceof ExpressionType) {
                ExpressionType expression = (ExpressionType) ((JAXBElement)o).getValue();
                String languageIRI = expression.getLanguage();
                languageIRI = languageIRI == null ? FragmentDialectConstants.XPATH10_LANGUAGE_IRI : languageIRI;
                if (languages.containsKey(languageIRI)) {
                    FragmentDialectLanguage language = languages.get(languageIRI);
                    return generateGetResponse(language.getResourceFragment(representation, expression));
                } else {
                    throw new UnsupportedLanguage();
                }
            }
        }
        // TODO: throw SOAP fault
        return null;
    }

    @Override
    public Representation processPut(Put body, Representation representation) {
        throw new UnknownDialect();
    }

    @Override
    public Representation processDelete(Delete body, Representation representation) {
        throw new UnknownDialect();
    }

    @Override
    public Representation processCreate(Create body) {
        throw new UnknownDialect();
    }
    
    /**
     * Register FragmentDialectLanguage object for IRI.
     * @param iri
     * @param language 
     */
    public void registerLanguage(String iri, FragmentDialectLanguage language) {
        if (languages.containsKey(iri)) {
            throw new IllegalArgumentException(String.format("IRI \"%s\" is already registered", iri));
        }
        languages.put(iri, language);
    }
    
    /**
     * Unregister FragmentDialectLanguage object for IRI.
     * @param iri 
     */
    public void unregisterLanguage(String iri) {
        if (!languages.containsKey(iri)) {
            throw new IllegalArgumentException(String.format("IRI \"%s\" is not registered", iri));
        }
        languages.remove(iri);
    }
    
    private JAXBElement<ValueType> generateGetResponse(Object value) {
        if (value instanceof Node) {
            return generateGetResponseNode((Node) value);
        } else if (value instanceof NodeList) {
            return generateGetResponseNodeList((NodeList) value);
        } else if (value instanceof String) {
            return generateGetResponseString((String) value);
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(new ValueType());
    }
    
    private JAXBElement<ValueType> generateGetResponseNodeList(NodeList nodeList) {
        ValueType resultValue = new ValueType();
        for (int i = 0; i < nodeList.getLength(); i++) {
            resultValue.getContent().add(nodeList.item(i));
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(resultValue);
    }
    
    private JAXBElement<ValueType> generateGetResponseNode(Node node) {
        ValueType resultValue = new ValueType();
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            Element attrNodeEl = TransferTools.createElementNS(
                FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME
            );
            attrNodeEl.setAttribute(
                FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME_ATTR,
                node.getNodeName()
            );
            attrNodeEl.setTextContent(node.getNodeValue());
            resultValue.getContent().add(attrNodeEl);
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            Element textNodeEl = TransferTools.createElementNS(
                FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                FragmentDialectConstants.FRAGMENT_TEXT_NODE_NAME
            );
            textNodeEl.setNodeValue(node.getNodeValue());
            resultValue.getContent().add(textNodeEl);
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            resultValue.getContent().add(node);
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(resultValue);
    }
    
    private JAXBElement<ValueType> generateGetResponseString(String value) {
        ValueType resultValue = new ValueType();
        resultValue.getContent().add(value);
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(resultValue);
    }

}
