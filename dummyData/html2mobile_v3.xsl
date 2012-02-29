<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- Edited by XMLSpy® -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
  <head>
	<link rel="stylesheet" href="http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.css" />
	<script src="http://code.jquery.com/jquery-1.6.4.min.js"></script>
	<script src="http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.js"></script>
  </head>
  <body>
<div data-role="page" data-theme="b" id="jqm-home">
    <div data-role="content"> 
  
<!-- Main List -->  
<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 

	<!-- Links List -->
	<li>Links
		<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 

			<!-- Navigation List -->
			<xsl:for-each select="html2mobile/document/links/linkgroup">
				<li>Link Group Title
					<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
						
				<!-- End Navigation List -->
					</ul></li>
			</xsl:for-each>

		<!-- End Links List -->
		</ul>
	</li>

	<!-- Content List -->
	<li>Content
		<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 

			<!-- Main Content -->
			<li>Main Content
				<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e">
					<li><xsl:value-of select="html2mobile/document/content/main/summary"/><a href="#">more...</a></li>
				<!-- End Main Content -->
				</ul>
			</li>

			<!-- Notes Content List -->
			<li>Notes Content
				<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
					<xsl:apply-templates select="linkInst"/>
				<!-- End Notes Content List -->
				</ul>
			</li>

		<!-- End Content List -->
		</ul>
	</li>

	<!-- Form -->
	<li>Form
		<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
			<xsl:for-each select="html2mobile/document/form">
				<li><xsl:value-of select="code"/></li>
			</xsl:for-each>
		<!-- End Form Content -->
		</ul>
	</li>

	<!-- Media List -->
	<li>Media
		<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 

			<!-- Picture List -->
			<li>Pictures
				<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
					<xsl:for-each select="html2mobile/document/media/pictures">
						<li><xsl:value-of select="src"/></li>
					</xsl:for-each>
				<!-- End Picture List -->
				</ul>
			</li>

			<!-- Video List -->
			<li>Videos
				<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
					<xsl:for-each select="html2mobile/document/media/videos">
						<li><xsl:value-of select="src"/></li>
					</xsl:for-each>
				<!-- End Video List -->
				</ul>
			</li>

			<!-- Audio List -->
			<li>Audio
				<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
					<xsl:for-each select="html2mobile/document/media/audio">
						<li><xsl:value-of select="src"/></li>
					</xsl:for-each>
				<!-- End Audio List -->
				</ul>
			</li>

		<!-- End Media List -->
		</ul>
	</li>
<!-- End Main List -->
</ul>
	</div>
</div>
</body>
</html>
</xsl:template>

<xsl:template match="linkInst">
	<xsl:for-each select="html2mobile/document/links/linkgroup/link">
		<xsl:if test="text != ''">
			<li>
				<a href="#">
					<xsl:value-of select="html2mobile/document/links/linkgroup/link/text"/>
				</a>
			</li>
		</xsl:if>
	</xsl:for-each>
</xsl:template>

</xsl:stylesheet>


