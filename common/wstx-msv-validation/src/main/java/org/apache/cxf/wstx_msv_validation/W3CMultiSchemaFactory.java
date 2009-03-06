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

package org.apache.cxf.wstx_msv_validation;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;

import com.ctc.wstx.msv.BaseSchemaFactory;
import com.ctc.wstx.msv.W3CSchema;
import com.sun.msv.grammar.xmlschema.XMLSchemaGrammar;
import com.sun.msv.reader.xmlschema.MultiSchemaReader;
import com.sun.msv.reader.xmlschema.XMLSchemaReader;

import org.codehaus.stax2.validation.XMLValidationSchema;

final class MyGrammarController extends com.sun.msv.reader.util.IgnoreController {
    private String mErrorMsg;

    public MyGrammarController() {
    }

    // public void warning(Locator[] locs, String errorMessage) { }

    public void error(Locator[] locs, String msg, Exception nestedException) {
        if (getMErrorMsg() == null) {
            setMErrorMsg(msg);
        } else {
            setMErrorMsg(getMErrorMsg() + "; " + msg);
        }
    }

    public void setMErrorMsg(String mErrorMsg) {
        this.mErrorMsg = mErrorMsg;
    }

    public String getMErrorMsg() {
        return mErrorMsg;
    }
}

/**
 * 
 */
public class W3CMultiSchemaFactory extends BaseSchemaFactory {
    
    private MultiSchemaReader multiSchemaReader;  
    private SAXParserFactory parserFactory;
    private XMLSchemaReader xmlSchemaReader;
    private MyGrammarController ctrl = new MyGrammarController();

    public W3CMultiSchemaFactory() {
        super(XMLValidationSchema.SCHEMA_ID_RELAXNG);
    }
    
    public XMLValidationSchema loadSchemas(InputSource[] sources) throws XMLStreamException {
        parserFactory = getSaxFactory();
        xmlSchemaReader = new XMLSchemaReader(ctrl, parserFactory);
        multiSchemaReader = new MultiSchemaReader(xmlSchemaReader);
        for (InputSource source : sources) {
            multiSchemaReader.parse(source);
        }
        
        XMLSchemaGrammar grammar = multiSchemaReader.getResult();
        if (grammar == null) {
            throw new XMLStreamException("Failed to load schemas: " + ctrl.getMErrorMsg());
        }
        return new W3CSchema(grammar); 
    }

    @Override
    protected XMLValidationSchema loadSchema(InputSource src, Object sysRef) throws XMLStreamException {
        throw new XMLStreamException("W3CMultiSchemaFactory does not support the provider API.");
    }
}
