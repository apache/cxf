<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:books="http://books">

 <xsl:param name="id" select="''"/>

 <xsl:template match="@*|node()">
   <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
   </xsl:copy>
 </xsl:template>
 
 <xsl:template match="id">
     <xsl:element name="id">
	   <xsl:value-of select="$id"/>
	</xsl:element>
 </xsl:template>

 </xsl:stylesheet> 
