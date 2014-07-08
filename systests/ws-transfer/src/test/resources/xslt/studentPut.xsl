<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns="http://university.edu/student"
                xmlns:ns="http://university.edu/student"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">
    <xsl:output method="xml"/>

    <xsl:template match="/">
        <xsl:element name="student">
            <xsl:call-template name="name"/>
            <xsl:call-template name="surname"/>
            <xsl:call-template name="birthdate"/>
            <xsl:call-template name="address"/>
            <xsl:call-template name="uid"/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template name="name">
        <xsl:choose>
            <xsl:when test="/ns:student/ns:name">
                <xsl:element name="name">
                    <xsl:value-of select="/ns:student/ns:name" />
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="name">Unspecified</xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="surname">
        <xsl:choose>
            <xsl:when test="/ns:student/ns:surname">
                <xsl:element name="surname">
                    <xsl:value-of select="/ns:student/ns:surname" />
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="surname">Unspecified</xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="birthdate">
        <xsl:choose>
            <xsl:when test="/ns:student/ns:birthdate">
                <xsl:element name="date">
                    <xsl:element name="day">
                        <xsl:value-of select="/ns:student/ns:birthdate/ns:day" />
                    </xsl:element>
                    <xsl:element name="month">
                        <xsl:value-of select="/ns:student/ns:birthdate/ns:month" />
                    </xsl:element>
                    <xsl:element name="year">
                        <xsl:value-of select="/ns:student/ns:birthdate/ns:year" />
                    </xsl:element>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="date">
                    <xsl:element name="day">Unspecified</xsl:element>
                    <xsl:element name="month">Unspecified</xsl:element>
                    <xsl:element name="year">Unspecified</xsl:element>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="address">
        <xsl:choose>
            <xsl:when test="/ns:student/ns:address">
                <xsl:element name="address">
                    <xsl:value-of select="/ns:student/ns:address" />
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="address">Unspecified</xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="uid">
        <xsl:element name="uid">
            <xsl:value-of select="ns:student/ns:uid" />
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
