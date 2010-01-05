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

package org.apache.cxf.tools.wsdlto.databinding.jaxb;
import com.sun.tools.xjc.api.ErrorListener;

import org.apache.cxf.tools.common.ToolException;

public class JAXBBindErrorListener implements ErrorListener {
    private boolean isVerbose;
    private String prefix = "Thrown by JAXB : ";

    public JAXBBindErrorListener(boolean verbose) {
        isVerbose = verbose;
    }

    public void error(org.xml.sax.SAXParseException exception) {
        if (exception.getLineNumber() > 0) {
            throw new ToolException(prefix + exception.getLocalizedMessage() 
                                    + " at line " + exception.getLineNumber()
                                    + " column " + exception.getColumnNumber()
                                    + " of schema " + exception.getSystemId(), exception);
           
        }
        throw new ToolException(prefix + mapMessage(exception.getLocalizedMessage()), exception);

    }

    public void fatalError(org.xml.sax.SAXParseException exception) {
        throw new ToolException(prefix + exception.getLocalizedMessage()
                                + " of schema " + exception.getSystemId(), exception);
    }

    public void info(org.xml.sax.SAXParseException exception) {
        if (this.isVerbose) {
            System.out.println("JAXB Info: " + exception.toString() 
                               + " in schema " + exception.getSystemId());
        }
    }

    public void warning(org.xml.sax.SAXParseException exception) {
        if (this.isVerbose) {
            System.err.println("JAXB parsing schema warning " + exception.toString()
                               + " in schema " + exception.getSystemId());
        }
    }
    
    private String mapMessage(String msg) {
        //this is kind of a hack to map the JAXB error message into
        //something more appropriate for CXF.  If JAXB changes their
        //error messages, this will break
        if (msg.contains("Use a class customization to resolve")
            && msg.contains("with the same name")) {
            int idx = msg.lastIndexOf("class customization") + 19;
            msg = msg.substring(0, idx) + " or the -autoNameResolution option" + msg.substring(idx);
        }
        return msg;
    }
}
