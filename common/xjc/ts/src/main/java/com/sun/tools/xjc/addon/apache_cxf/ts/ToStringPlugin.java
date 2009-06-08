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

package com.sun.tools.xjc.addon.apache_cxf.ts;

import java.io.IOException;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;

/**
 * Thin wrapper around the ToStringPlugin. This must be in the com.sun.tools.xjc.addon package
 * for it to work with Java 6. See https://issues.apache.org/jira/browse/CXF-1880.
 */
public class ToStringPlugin extends Plugin {
    
    org.apache.cxf.xjc.ts.ToStringPlugin impl = new org.apache.cxf.xjc.ts.ToStringPlugin();
    
    /* (non-Javadoc)
     * @see com.sun.tools.xjc.Plugin#getOptionName()
     */
    @Override
    public String getOptionName() {
        return impl.getOptionName();
    }

    /* (non-Javadoc)
     * @see com.sun.tools.xjc.Plugin#getUsage()
     */
    @Override
    public String getUsage() {
        return impl.getUsage();
    }

    /* (non-Javadoc)
     * @see com.sun.tools.xjc.Plugin#run(com.sun.tools.xjc.outline.Outline,
     *   com.sun.tools.xjc.Options, org.xml.sax.ErrorHandler)
     */
    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
        return impl.run(outline, opt, errorHandler);
    }

    /* (non-Javadoc)
     * @see com.sun.tools.xjc.Plugin#parseArgument(com.sun.tools.xjc.Options, java.lang.String[], int)
     */
    @Override
    public int parseArgument(Options opt, String[] args, int index) 
        throws BadCommandLineException, IOException {
        
        return impl.parseArgument(opt, args, index);
    }
}
