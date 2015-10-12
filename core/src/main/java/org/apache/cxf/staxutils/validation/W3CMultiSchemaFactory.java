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
/*
 * Code in this file derives from source code in Woodstox which 
 * carries a ASL 2.0 license.
 */

package org.apache.cxf.staxutils.validation;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;

import com.ctc.wstx.msv.BaseSchemaFactory;
import com.ctc.wstx.msv.W3CSchema;
import com.sun.msv.grammar.ExpressionPool;
import com.sun.msv.grammar.xmlschema.XMLSchemaGrammar;
import com.sun.msv.grammar.xmlschema.XMLSchemaSchema;
import com.sun.msv.reader.GrammarReaderController;
import com.sun.msv.reader.State;
import com.sun.msv.reader.xmlschema.MultiSchemaReader;
import com.sun.msv.reader.xmlschema.SchemaState;
import com.sun.msv.reader.xmlschema.XMLSchemaReader;

import org.codehaus.stax2.validation.XMLValidationSchema;

/**
 * 
 */
public class W3CMultiSchemaFactory extends BaseSchemaFactory {
    
    private MultiSchemaReader multiSchemaReader;  
    private SAXParserFactory parserFactory;
    private RecursiveAllowedXMLSchemaReader xmlSchemaReader;

    public W3CMultiSchemaFactory() {
        super(XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA);
    }
    
    static class RecursiveAllowedXMLSchemaReader extends XMLSchemaReader {
        Set<String> sysIds = new TreeSet<String>();
        RecursiveAllowedXMLSchemaReader(GrammarReaderController controller,
                        SAXParserFactory parserFactory) {
            super(controller, parserFactory, new StateFactory() {
                public State schemaHead(String expectedNamespace) {
                    return new SchemaState(expectedNamespace) {
                        private XMLSchemaSchema old;
                        protected void endSelf() {
                            super.endSelf();
                            RecursiveAllowedXMLSchemaReader r = (RecursiveAllowedXMLSchemaReader)reader;
                            r.currentSchema = old;
                        }
                        protected void onTargetNamespaceResolved(String targetNs, boolean ignoreContents) {
                            
                            RecursiveAllowedXMLSchemaReader r = (RecursiveAllowedXMLSchemaReader)reader;
                            // sets new XMLSchemaGrammar object.
                            old = r.currentSchema;
                            r.currentSchema = r.getOrCreateSchema(targetNs);
                            if (ignoreContents) {
                                return;
                            }
                            if (!r.isSchemaDefined(r.currentSchema)) {
                                r.markSchemaAsDefined(r.currentSchema);
                            }
                        }
                    };
                }                
            }, new ExpressionPool());
        }
        
        public void setLocator(Locator locator) {
            if (locator == null && getLocator() != null && getLocator().getSystemId() != null) {
                sysIds.add(getLocator().getSystemId());
            }
            super.setLocator(locator);
        }
        public void switchSource(Source source, State newState) {
            String url = source.getSystemId();
            if (url != null && sysIds.contains(url)) {
                return;
            }
            super.switchSource(source, newState);
        }
        
    }
    
    public XMLValidationSchema loadSchemas(String baseURI, 
                                           Map<String, EmbeddedSchema> sources) throws XMLStreamException {
        parserFactory = getSaxFactory();
        
        ResolvingGrammarReaderController ctrl = new ResolvingGrammarReaderController(baseURI, sources);
        xmlSchemaReader = new RecursiveAllowedXMLSchemaReader(ctrl, parserFactory);
        multiSchemaReader = new MultiSchemaReader(xmlSchemaReader);
        for (EmbeddedSchema source : sources.values()) {
            DOMSource domSource = new DOMSource(source.getSchemaElement());
            domSource.setSystemId(source.getSystemId());
            multiSchemaReader.parse(domSource);
        }
        
        XMLSchemaGrammar grammar = multiSchemaReader.getResult();
        if (grammar == null) {
            throw new XMLStreamException("Failed to load schemas");
        }
        return new W3CSchema(grammar); 
    }

    @Override
    protected XMLValidationSchema loadSchema(InputSource src, Object sysRef) throws XMLStreamException {
        throw new XMLStreamException("W3CMultiSchemaFactory does not support the provider API.");
    }
}
