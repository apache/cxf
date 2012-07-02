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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.tools.xjc.api.ErrorListener;

import org.apache.cxf.tools.common.ToolErrorListener;

public class JAXBBindErrorListener implements ErrorListener {
    private boolean isVerbose;
    private ToolErrorListener listener;
    
    public JAXBBindErrorListener(boolean verbose, ToolErrorListener l) {
        isVerbose = verbose;
        listener = l;
    }

    public boolean hasErrors() {
        return listener.getErrorCount() > 0;
    }

    public void error(org.xml.sax.SAXParseException exception) {
        String s = exception.getSystemId();
        File file = null;
        if (s != null && s.startsWith("file:")) {
            if (s.contains("#")) {
                s = s.substring(0, s.indexOf('#'));
            }
            try {
                URI uri = new URI(s);
                file = new File(uri);
            } catch (URISyntaxException e) {
                //ignore
            }
        }
        listener.addError(file,
                          exception.getLineNumber(),
                          exception.getColumnNumber(),
                          mapMessage(exception.getLocalizedMessage()),
                          exception);
    }

    public void fatalError(org.xml.sax.SAXParseException exception) {
        error(exception);
    }

    public void info(org.xml.sax.SAXParseException exception) {
        if (this.isVerbose) {
            System.out.println("JAXB Info: " + exception.toString() 
                               + " in schema " + exception.getSystemId());
        }
    }

    public void warning(org.xml.sax.SAXParseException exception) {
        if (this.isVerbose) {
            System.out.println("JAXB parsing schema warning " + exception.toString()
                               + " in schema " + exception.getSystemId());
        }
        String s = exception.getSystemId();
        File file = null;
        if (s != null && s.startsWith("file:")) {
            if (s.contains("#")) {
                s = s.substring(0, s.indexOf('#'));
            }
            try {
                URI uri = new URI(s);
                file = new File(uri);
            } catch (URISyntaxException e) {
                //ignore
            }
        }
        listener.addWarning(file,
                          exception.getLineNumber(),
                          exception.getColumnNumber(),
                          mapMessage(exception.getLocalizedMessage()),
                          exception);
        
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
