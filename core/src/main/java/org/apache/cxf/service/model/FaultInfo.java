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

package org.apache.cxf.service.model;

import javax.xml.namespace.QName;

public class FaultInfo extends AbstractMessageContainer {
    private QName faultName;

    public FaultInfo(QName fname, QName mname, OperationInfo info) {
        super(info, mname);
        faultName = fname;
    }

    public QName getFaultName() {
        return faultName;
    }
    public void setFaultName(QName fname) {
        faultName = fname;
    }



    public int hashCode() {
        return faultName == null ? -1 : faultName.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof FaultInfo)) {
            return false;
        }
        FaultInfo oi = (FaultInfo)o;
        return equals(faultName, oi.faultName)
            && super.equals(o);
    }


}
