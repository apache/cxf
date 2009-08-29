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

package org.apache.cxf.systest.aegis.mtom.fortest;

import javax.activation.DataHandler;
import javax.jws.WebService;

/**
 * 
 */
@WebService
@javax.xml.ws.soap.MTOM
public class MtomTestImpl implements MtomTest {
    
    public static final String STRING_DATA = "What rough beast, its hour come at last,"
        + " slouches toward Bethlehem to be born?";
            
    private DataHandlerBean lastDhBean;

    /** {@inheritDoc}*/
    public void acceptDataHandler(DataHandlerBean dhBean) {
        lastDhBean = dhBean;
    }

    public DataHandlerBean getLastDhBean() {
        return lastDhBean;
    }

    public DataHandlerBean produceDataHandlerBean() {
        DataHandlerBean dhBean = new DataHandlerBean();
        dhBean.setName("legion");
        // since we know that the code has no lower threshold for using attachments, 
        // we can just return a fairly short string.
        dhBean.setDataHandler(new DataHandler(STRING_DATA, "text/plain;charset=utf-8"));
        return dhBean;
    }

}
