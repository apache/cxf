<?xml version="1.0"?>
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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns="http://www.w3.org/2001/XMLSchema"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:x1="http://apache.org/type_test/types1"
    >
    
    <xsl:output method="xml" indent="yes" />
    <xsl:strip-space elements="*"/>


    <!-- These types fail validation so we just map them to a generic schema -->
    <xsl:template match="xsd:complexType[@name='DerivedChoiceBaseAll']">
        <complexType name="DerivedChoiceBaseAll">
            <sequence>
                <any/>
            </sequence>
            <anyAttribute/>
        </complexType>
    </xsl:template>
    <xsl:template match="xsd:complexType[@name='DerivedAllBaseStruct']">
        <complexType name="DerivedAllBaseStruct">
            <sequence>
                <any/>
            </sequence>
            <anyAttribute/>
        </complexType>
    </xsl:template>
    <xsl:template match="xsd:complexType[@name='DerivedAllBaseChoice']">
        <complexType name="DerivedAllBaseChoice">
            <sequence>
                <any/>
            </sequence>
            <anyAttribute/>
        </complexType>
    </xsl:template>
    <xsl:template match="xsd:complexType[@name='DerivedAllBaseAll']">
        <complexType name="DerivedAllBaseAll">
            <sequence>
                <any/>
            </sequence>
            <anyAttribute/>
        </complexType>
    </xsl:template>
    <xsl:template match="xsd:complexType[@name='DerivedChoiceBaseComplex']">
        <complexType name="DerivedChoiceBaseComplex">
            <sequence>
                <any/>
            </sequence>
            <anyAttribute/>
        </complexType>
    </xsl:template>


    <!-- Idiomatic Copy Transformation -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
