<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="https://jakarta.ee/xml/ns/jaxb" xmlns:xjc="http://java.sun.com/xml/ns/jaxb/xjc" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:jxb="https://jakarta.ee/xml/ns/jaxb" xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
	xmlns:jaxws="https://jakarta.ee/xml/ns/jaxws">

	<xsl:param name="fileName" />
	<xsl:param name="namespace" />
	<xsl:param name="pkgName" />

	<xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes' />
	<xsl:strip-space elements="*" />





	<xsl:template match="/">
		<jaxws:bindings jxb:extensionBindingPrefixes="jaxws jxb xjc" version="3.0">
			<xsl:attribute name="wsdlLocation">
						<xsl:text>src/main/resources/META-INF/wsdl/</xsl:text>
					<xsl:value-of select="$fileName" />
					 <xsl:text>.wsdl</xsl:text>
					 </xsl:attribute>



			<jaxws:enableWrapperStyle>false</jaxws:enableWrapperStyle>


			<jaxws:bindings node="wsdl:definitions">
				<jaxws:package>
					<xsl:attribute name="name">
						<xsl:value-of select="$pkgName" />
					 	<xsl:text>.ws</xsl:text>
					 </xsl:attribute>
				</jaxws:package>
			</jaxws:bindings>

			<jxb:bindings>
				<xsl:attribute name="node">
						<xsl:text>wsdl:definitions/wsdl:types/xsd:schema[@targetNamespace='</xsl:text>
						<xsl:value-of select="$namespace" />
					 	<xsl:text>']</xsl:text>
					 </xsl:attribute>
				<jxb:schemaBindings>
					<jxb:package>
						<xsl:attribute name="name">
						<xsl:value-of select="$pkgName" />
					 	<xsl:text>.xml</xsl:text>
					 </xsl:attribute>
					</jxb:package>
				</jxb:schemaBindings>
				<jxb:globalBindings generateElementProperty="false">
					<xjc:serializable uid="1" />
					<xjc:javaType name="java.time.OffsetDateTime" xmlType="xsd:dateTime">
						<xsl:attribute name="adapter">
							<xsl:value-of select="concat($pkgName, '.XmlAdapters.DateTimeAdapter')" />
						</xsl:attribute>
					</xjc:javaType>
					<xjc:javaType name="java.time.LocalDate" xmlType="xsd:date">
						<xsl:attribute name="adapter">
							<xsl:value-of select="concat($pkgName, '.XmlAdapters.DateAdapter')" />
					</xsl:attribute>
					</xjc:javaType>
					<xjc:javaType name="java.time.OffsetTime" xmlType="xsd:time">
						<xsl:attribute name="adapter">
							<xsl:value-of select="concat($pkgName, '.XmlAdapters.TimeAdapter')" />
						</xsl:attribute>
					</xjc:javaType>
				</jxb:globalBindings>

				<xsl:apply-templates />

			</jxb:bindings>

		</jaxws:bindings>

	</xsl:template>


	<xsl:template match="//xsd:annotation" />
	<xsl:template match="//wsdl:documentation" />


	<xsl:template match="//xsd:complexType[substring(@name, string-length(@name) -3)= 'Type']">
		<jxb:bindings>
			<xsl:attribute name="node">
				<xsl:call-template name="xpath" />
			</xsl:attribute>
			<jxb:class>
				<xsl:attribute name="name">
                    <xsl:value-of select="substring(@name, 0, string-length(@name) -3)" />									
				</xsl:attribute>
			</jxb:class>
		</jxb:bindings>

		<xsl:apply-templates />
	</xsl:template>


	<xsl:template name="xpath">
		<xsl:for-each select="ancestor-or-self::*">
			<xsl:if test="count(ancestor::*) > 2">
				<xsl:if test="count(ancestor::*) != 3">
					<xsl:text>/</xsl:text>
				</xsl:if>
				<xsl:text>xsd:</xsl:text>
				<xsl:value-of select="local-name()" />
				<xsl:text>[</xsl:text>
				<xsl:number />
				<xsl:text>]</xsl:text>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>


</xsl:stylesheet>
