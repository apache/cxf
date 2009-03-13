<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/"
    xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" >
    <xsl:output method="xml"/>

    <xsl:template match="xs:import">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="@schemaLocation='http://localhost/trust?xsd=xsd0'">
                    <xsl:attribute name="schemaLocation">trust0.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://localhost/trust?xsd=xsd1'">
                    <xsl:attribute name="schemaLocation">trust1.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://localhost/trust?xsd=xsd2'">
                    <xsl:attribute name="schemaLocation">trust2.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://131.107.153.205/trust?xsd=xsd0'">
                    <xsl:attribute name="schemaLocation">trust0.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://131.107.153.205/trust?xsd=xsd1'">
                    <xsl:attribute name="schemaLocation">trust1.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://131.107.153.205/trust?xsd=xsd2'">
                    <xsl:attribute name="schemaLocation">trust2.xsd</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="@schemaLocation"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="@namespace"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="soap12:address|soap:address">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="starts-with(@location,'http://localhost/')">
                    <xsl:attribute name="location">http://131.107.153.205/<xsl:value-of select="substring(@location,18)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'https://localhost/')">
                    <xsl:attribute name="location">https://131.107.153.205/<xsl:value-of select="substring(@location,19)"/></xsl:attribute>
                </xsl:when>
                <!--xsl:when test="starts-with(@location,'http://131.107.153.205/')">
                    <xsl:attribute name="location">http://131.107.72.15/<xsl:value-of select="substring(@location,24)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'https://131.107.153.205/')">
                    <xsl:attribute name="location">https://131.107.72.15/<xsl:value-of select="substring(@location,25)"/></xsl:attribute>
                </xsl:when-->
                <xsl:otherwise>
                    <xsl:apply-templates select="@location"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="wsdl:import">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="@location='http://localhost/trust?wsdl=wsdl0'">
                    <xsl:attribute name="location">trust2.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:when test="@location='http://localhost/trust?wsdl'">
                    <xsl:attribute name="location">trust.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:when test="@location='http://131.107.153.205/trust?wsdl=wsdl0'">
                    <xsl:attribute name="location">trust2.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:when test="@location='http://131.107.153.205/trust?wsdl'">
                    <xsl:attribute name="location">trust.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="@location"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="@namespace"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="xs:element[@ref='xs:schema']">
    </xsl:template>
    <xsl:template match="@*">
        <xsl:copy/>
    </xsl:template>
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
