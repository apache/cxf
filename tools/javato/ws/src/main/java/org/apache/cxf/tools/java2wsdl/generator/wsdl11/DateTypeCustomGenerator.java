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
package org.apache.cxf.tools.java2wsdl.generator.wsdl11;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.tools.common.VelocityGenerator;
import org.apache.cxf.tools.java2wsdl.generator.AbstractGenerator;
import org.apache.cxf.tools.util.FileWriterUtil;

public final class DateTypeCustomGenerator extends AbstractGenerator<File> {

    private static final String TEMPLATE_BASE = "org/apache/cxf/tools/java2wsdl/generator/wsdl11/";
    private static final String TEMPLATE_EXT = TEMPLATE_BASE + "date_type_cust.vm";
    private static final String TEMPLATE_EMB = TEMPLATE_BASE + "date_type_cust_embed.vm";

    private static final String DATE_ADAPTER = "org.apache.cxf.tools.common.DataTypeAdapter";
    private static final String CALENDAR_ADAPTER = "javax.xml.bind.DatatypeConverter";

    private String wsdlName;
    private List<String> schemaFiles = new ArrayList<String>();

    public void addSchemaFiles(final Collection<String> s) {
        this.schemaFiles.addAll(s);
    }

    public void setWSDLName(final String ws) {
        this.wsdlName = ws;
    }

    private String getTemplate() {
        if (allowImports()) {
            return TEMPLATE_EXT;
        }
        return TEMPLATE_EMB;
    }

    public List<String> getSchemaNamespaces() {
        List<String> ns = new ArrayList<String>();
        for (SchemaInfo schema : getServiceModel().getSchemas()) {
            ns.add(schema.getNamespaceURI());
        }
        return ns;
    }

    public File generate(File outputdir) {
        Class dateType = getDateType();
        File xjb = getJAXBCustFile(outputdir);

        if (dateType != null) {
            VelocityGenerator generator = new VelocityGenerator(false);

            generator.setCommonAttributes();
            generator.setAttributes("parseMethod", getAdapterMethod(dateType, ".parseDateTime"));
            generator.setAttributes("printMethod", getAdapterMethod(dateType, ".printDateTime"));
            generator.setAttributes("datetype", dateType.getName());

            if (allowImports()) {
                if (schemaFiles.size() == 0) {
                    return null;
                }
                generator.setAttributes("schemaFiles", schemaFiles);
            } else {
                generator.setAttributes("wsdlName", wsdlName);
                List<String> ns = getSchemaNamespaces();
                if (ns.size() == 0) {
                    return null;
                }
                generator.setAttributes("targetNamespaces", ns);
            }

            try {
                generator.doWrite(getTemplate(), FileWriterUtil.getWriter(xjb));
            } catch (Exception e) {
                e.printStackTrace();
            }

            generator.clearAttributes();
        }
        return xjb;
    }

    protected File getJAXBCustFile(File outputdir) {
        return new File(outputdir, wsdlName + ".xjb");
    }

    protected String getAdapterMethod(final Class clz, final String methodName) {
        if (clz == Date.class) {
            return DATE_ADAPTER + methodName;
        }
        return CALENDAR_ADAPTER + methodName;
    }

    protected Class getDateType() {
        if (getServiceModel() == null) {
            return null;
        }

        for (OperationInfo op : getServiceModel().getInterface().getOperations()) {
            Method m = (Method) op.getProperty("operation.method");
            for (Class clz : m.getParameterTypes()) {
                if (clz == Date.class || clz == Calendar.class) {
                    return clz;
                }
            }
            if (m.getReturnType() == Date.class
                || m.getReturnType() == Calendar.class) {
                return m.getReturnType();
            }
        }
        return null;
    }
}
