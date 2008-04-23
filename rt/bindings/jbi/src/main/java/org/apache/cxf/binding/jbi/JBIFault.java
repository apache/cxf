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

package org.apache.cxf.binding.jbi;

import java.util.ResourceBundle;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.interceptor.Fault;

public class JBIFault extends Fault {
    public static final String JBI_FAULT_PREFIX = "jfns";

    public static final String JBI_FAULT_ROOT = "JBIFault";
    public static final String JBI_FAULT_STRING = "faultstring";

    public static final String JBI_FAULT_DETAIL = "detail";

    public static final String JBI_FAULT_CODE_SERVER = "SERVER";

    public static final String JBI_FAULT_CODE_CLIENT = "CLIENT";

    
    static final long serialVersionUID = 100000;

    public JBIFault(Message message, Throwable throwable) {
        super(message, throwable);        
    }

    public JBIFault(Message message) {
        super(message);        
    }

    public JBIFault(String message) {
        super(new Message(message, (ResourceBundle) null));        
    }

    public static JBIFault createFault(Fault f) {
        if (f instanceof JBIFault) {
            return (JBIFault) f;
        }
        Throwable th = f.getCause();
        JBIFault jbiFault = new JBIFault(new Message(f.getMessage(), (ResourceBundle) null), th);
        jbiFault.setDetail(f.getDetail());
        return jbiFault;
    }

}
