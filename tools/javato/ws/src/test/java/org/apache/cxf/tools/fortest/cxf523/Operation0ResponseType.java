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

package org.apache.cxf.tools.fortest.cxf523;


import java.util.Calendar;
import javax.xml.namespace.QName;

public class Operation0ResponseType {

    public static final String TARGET_NAMESPACE = "http://www.iona.com/db_service";

    public static final QName QNAME = new QName("http://www.iona.com/db_service", "operation0ResponseType");


    private String name;
    private String owner;
    private String species;
    private String sex;
    private Calendar birth;
    private Calendar death;


    public String getName() {
        return name;
    }

    public void setName(String val) {
        this.name = val;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String val) {
        this.owner = val;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String val) {
        this.species = val;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String val) {
        this.sex = val;
    }

    public Calendar getBirth() {
        return birth;
    }

    public void setBirth(Calendar val) {
        this.birth = val;
    }

    public Calendar getDeath() {
        return death;
    }

    public void setDeath(Calendar val) {
        this.death = val;
    }

    public QName getQName() {
        return QNAME;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (name != null) {
            buffer.append("name : " + name + "\n");
        }
        if (owner != null) {
            buffer.append("owner : " + owner + "\n");
        }
        if (species != null) {
            buffer.append("species : " + species + "\n");
        }
        if (sex != null) {
            buffer.append("sex : " + sex + "\n");
        }
        if (birth != null) {
            buffer.append("birth : " + birth + "\n");
        }
        if (death != null) {
            buffer.append("death : " + death + "\n");
        }
        return buffer.toString();
    }
}
