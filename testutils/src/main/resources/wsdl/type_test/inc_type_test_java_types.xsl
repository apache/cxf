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
<!--
     Stylesheet to convert schema into java file for test implementation.
-->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:xalan="http://xml.apache.org/xslt"
    xmlns:itst="http://tests.iona.com/ittests">

    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="itst:builtIn" mode="javaHolderType">
        <xsl:text>&#10;            Holder&#x3c;</xsl:text>
        <xsl:value-of select="@javaHolder"/>
        <xsl:text>&#x3e;</xsl:text>
    </xsl:template>
    
    <xsl:template match="itst:builtIn" mode="javaType">
        <xsl:value-of select="@javaType"/>
    </xsl:template>

    <xsl:template match="xsd:simpleType|xsd:complexType|xsd:element" mode="javaHolderType">
        <xsl:text>&#10;            Holder&#x3c;</xsl:text>
        <xsl:apply-templates select="." mode="javaType"/>
        <xsl:text>&#x3e;</xsl:text>
    </xsl:template>
    
    <xsl:template match="xsd:simpleType" mode="javaType">
        <xsl:choose>
            <xsl:when test="contains(@name, 'Union') and (contains(@name, 'List')
                    or @name='UnionWithUnion'
                    or @name='UnionWithAnonUnion')">
                <xsl:value-of select="'List&#x3c;String&#x3e;'"/>
            </xsl:when>
            <xsl:when test="contains(@name, 'Union') and not(contains(@name, 'List') 
                    or @name='UnionWithUnion' 
                    or @name='UnionWithAnonUnion')">
                <xsl:value-of select="'String'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="@name"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="xsd:complexType|xsd:element" mode="javaType">
        <xsl:value-of select="@name"/>
    </xsl:template>

</xsl:stylesheet>
