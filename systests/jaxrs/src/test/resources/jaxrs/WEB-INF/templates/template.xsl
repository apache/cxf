<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:books="http://www.w3.org/books">
 
 <xsl:import href="template2.xsl"/>

 <xsl:variable name="root" select="/"/>
 <xsl:variable name="htmlDoc" select="document('book.xhtml')"/>

 <xsl:template match="/">
    <xsl:apply-templates select="$htmlDoc/*"/>
 </xsl:template>
 
 <xsl:template match="books:bookTag">
    <xsl:apply-templates select="$root/*"/>
 </xsl:template>
 
</xsl:stylesheet> 
