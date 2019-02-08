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

package org.apache.cxf.tools.wsdlto.databinding.source;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.DefaultValueWriter;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;

/**
 *
 */
public class SourceDatabindingProfile implements DataBindingProfile {
    Class<? extends Source> cls;


    /** {@inheritDoc}*/
    public void generate(ToolContext context) throws ToolException {
        //nothing to generate
    }

    /** {@inheritDoc}*/
    public void initialize(ToolContext c) throws ToolException {
        //nothing really to do
        c.put(ToolConstants.RUNTIME_DATABINDING_CLASS, SourceDataBinding.class.getName() + ".class");

        String s = (String)c.get(ToolConstants.CFG_DATABINDING);
        if ("source".equalsIgnoreCase(s)) {
            cls = Source.class;
        } else if ("domsource".equalsIgnoreCase(s)) {
            cls = DOMSource.class;
        } else if ("staxsource".equalsIgnoreCase(s)) {
            cls = StaxSource.class;
        } else if ("saxsource".equalsIgnoreCase(s)) {
            cls = SAXSource.class;
        } else {
            cls = Source.class;
        }
    }

    /** {@inheritDoc}*/
    public String getType(QName qn, boolean element) {
        return cls.getName();
    }

    /** {@inheritDoc}*/
    public String getWrappedElementType(QName wrapperElement, QName item) {
        //return null, cannot unwrap
        return null;
    }

    /** {@inheritDoc}*/
    public DefaultValueWriter createDefaultValueWriter(QName qn, boolean element) {
        //won't support this for now
        return null;
    }

    /** {@inheritDoc}*/
    public DefaultValueWriter createDefaultValueWriterForWrappedElement(QName wrapperElement, QName qn) {
        //return null, cannot unwrap
        return null;
    }

}
