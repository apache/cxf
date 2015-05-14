<?xml version="1.0" encoding="UTF-8"?>

<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements. See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership. The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied. See the License for the
  specific language governing permissions and limitations
  under the License.
-->

<xsl:stylesheet xmlns="http://university.edu/teacher"
                xmlns:ns="http://university.edu/teacher"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:uid="org.apache.cxf.systest.ws.transfer.UIDManager"
                exclude-result-prefixes="uid"
                version="1.0">
    <xsl:output method="xml"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="/ns:teacher">
        <xsl:element name="teacher">
            <xsl:apply-templates select="@*|node()" />
            <xsl:element name="uid">
                <xsl:value-of select="uid:getUID()" />
            </xsl:element>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
