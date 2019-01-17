/* == KOST-Val ==================================================================================
 * The KOST-Val application is used for validate TIFF, SIARD, PDF/A, JP2, JPEG-Files and Submission
 * Information Package (SIP). Copyright (C) 2012-2019 Claire Roethlisberger (KOST-CECO), Christian
 * Eugster, Olivier Debenath, Peter Schneider (Staatsarchiv Aargau), Markus Hahn (coderslagoon),
 * Daniel Ludin (BEDAG AG)
 * -----------------------------------------------------------------------------------------------
 * KOST-Val is a development of the KOST-CECO. All rights rest with the KOST-CECO. This application
 * is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. BEDAG AG and Daniel Ludin hereby disclaims all copyright
 * interest in the program SIP-Val v0.2.0 written by Daniel Ludin (BEDAG AG). Switzerland, 1 March
 * 2011. This application is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the follow GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA or see
 * <http://www.gnu.org/licenses/>.
 * ============================================================================================== */

package ch.kostceco.tools.kostval.validation.modulesiard.impl;

import java.io.BufferedReader;
import java.io.File;
import java.util.Map;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import ch.kostceco.tools.kostval.exception.modulesiard.ValidationCheaderException;
import ch.kostceco.tools.kostval.util.Util;
import ch.kostceco.tools.kostval.validation.ValidationModuleImpl;
import ch.kostceco.tools.kostval.validation.modulesiard.ValidationCheaderModule;

/** Validierungsschritt C (Header-Validierung) Ist der header-Ordner valid? valid --> metadata.xml
 * valid zu metadata.xsd und beides vorhanden Bemerkung --> zusätzliche Ordner oder Dateien wie z.B.
 * metadata.xls sind im header-Ordner erlaubt ==> Bei den Module A, B, C und D wird die Validierung
 * abgebrochen, sollte das Resulat invalid sein!
 * 
 * @author Rc Claire Roethlisberger, KOST-CECO */

public class ValidationCheaderModuleImpl extends ValidationModuleImpl implements
		ValidationCheaderModule
{

	public static String	NEWLINE	= System.getProperty( "line.separator" );

	@Override
	public boolean validate( File valDatei, File directoryOfLogfile, Map<String, String> configMap )
			throws ValidationCheaderException
	{
		boolean showOnWork = true;
		int onWork = 410;
		// Informationen zur Darstellung "onWork" holen
		String onWorkConfig = configMap.get( "ShowProgressOnWork" );
		if ( onWorkConfig.equals( "no" ) ) {
			// keine Ausgabe
			showOnWork = false;
		} else {
			// Ausgabe SIP-Modul Ersichtlich das KOST-Val arbeitet
			System.out.print( "C    " );
			System.out.print( "\b\b\b\b\b" );
		}

		boolean result = true;
		// Sind im Header-Ordner metadata.xml und metadata.xsd vorhanden?
		ZipEntry metadataxml = null;
		ZipEntry metadataxsd = null;

		try {
			ZipInputStream zipfile = null;
			ZipEntry zEntry = null;
			FileInputStream fis = null;
			fis = new FileInputStream( valDatei );
			zipfile = new ZipInputStream( new BufferedInputStream( fis ) );
			while ( (zEntry = zipfile.getNextEntry()) != null ) {
				if ( zEntry.getName().equals( "header/" + METADATA ) ) {
					metadataxml = zEntry;
				}
				if ( zEntry.getName().equals( "header/" + XSD_METADATA ) ) {
					metadataxsd = zEntry;
				}
				if ( showOnWork ) {
					if ( onWork == 410 ) {
						onWork = 2;
						System.out.print( "C-   " );
						System.out.print( "\b\b\b\b\b" );
					} else if ( onWork == 110 ) {
						onWork = onWork + 1;
						System.out.print( "C\\   " );
						System.out.print( "\b\b\b\b\b" );
					} else if ( onWork == 210 ) {
						onWork = onWork + 1;
						System.out.print( "C|   " );
						System.out.print( "\b\b\b\b\b" );
					} else if ( onWork == 310 ) {
						onWork = onWork + 1;
						System.out.print( "C/   " );
						System.out.print( "\b\b\b\b\b" );
					} else {
						onWork = onWork + 1;
					}
				}
			}
			if ( metadataxml == null ) {
				// keine metadata.xml = METADATA in der SIARD-Datei gefunden
				getMessageService().logError(
						getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
								+ getTextResourceService().getText( MESSAGE_XML_C_NOMETADATAFOUND ) );
				zipfile.close();
				return false;
			}
			if ( metadataxsd == null ) {
				// keine metadata.xsd = XSD_METADATA in der SIARD-Datei gefunden
				getMessageService().logError(
						getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
								+ getTextResourceService().getText( MESSAGE_XML_C_NOMETADATAXSD ) );
				zipfile.close();
				return false;
			}
			zipfile.close();
		} catch ( Exception e ) {
			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
							+ getTextResourceService().getText( ERROR_XML_UNKNOWN,
									e.getMessage() + " xml und xsd" ) );
			return false;
		}

		// Validierung metadata.xml mit metadata.xsd
		File xmlToValidate = null;
		File xsdToValidate = null;
		String toplevelDir = valDatei.getName();
		int lastDotIdx = toplevelDir.lastIndexOf( "." );
		toplevelDir = toplevelDir.substring( 0, lastDotIdx );

		try {
			/* Nicht vergessen in "src/main/resources/config/applicationContext-services.xml" beim
			 * entsprechenden Modul die property anzugeben: <property name="configurationService"
			 * ref="configurationService" /> */
			// Arbeitsverzeichnis zum Entpacken des Archivs erstellen
			String pathToWorkDir = configMap.get( "PathToWorkDir" );
			File tmpDir = new File( pathToWorkDir + File.separator + "SIARD" );
			if ( tmpDir.exists() ) {
				Util.deleteDir( tmpDir );
			}
			if ( !tmpDir.exists() ) {
				tmpDir.mkdir();
			}

			/* Das metadata.xml und sein xsd müssen in das Filesystem extrahiert werden, weil bei bei
			 * Verwendung eines Inputstreams bei der Validierung ein Problem mit den xs:include Statements
			 * besteht, die includes können so nicht aufgelöst werden. Es werden hier jedoch nicht nur
			 * diese Files extrahiert, sondern gleich die ganze Zip-Datei, weil auch spätere Validierungen
			 * nur mit den extrahierten Files arbeiten können. */
			FileInputStream fis = null;
			ZipInputStream zipfile = null;
			ZipEntry zEntry = null;
			fis = new FileInputStream( valDatei );
			zipfile = new ZipInputStream( new BufferedInputStream( fis ) );

			while ( (zEntry = zipfile.getNextEntry()) != null ) {
				try {
					if ( !zEntry.isDirectory() ) {
						byte[] tmp = new byte[4 * 1024];
						FileOutputStream fos = null;
						String opFilePath = tmpDir + File.separator + zEntry.getName();
						File newFile = new File( opFilePath );
						File parent = newFile.getParentFile();
						if ( !parent.exists() ) {
							parent.mkdirs();
						}
						// System.out.println( "Extracting file to " + newFile.getAbsolutePath() );
						fos = new FileOutputStream( opFilePath );
						int size = 0;
						while ( (size = zipfile.read( tmp )) != -1 ) {
							fos.write( tmp, 0, size );
						}
						fos.flush();
						fos.close();
						// Festhalten von metadata.xml und metadata.xsd
						if ( newFile.getName().endsWith( METADATA ) ) {
							xmlToValidate = newFile;
						}
						if ( newFile.getName().endsWith( XSD_METADATA ) ) {
							xsdToValidate = newFile;
						}
					} else {
						/* Scheibe den Ordner wenn noch nicht vorhanden an den richtigen Ort respektive in den
						 * richtigen Ordner der ggf angelegt werden muss. Dies muss gemacht werden, damit auch
						 * leere Ordner ins Work geschrieben werden. Diese werden danach im J als Fehler
						 * angegeben */
						File newFolder = new File( tmpDir, zEntry.getName() );
						if ( !newFolder.exists() ) {
							File parent = newFolder.getParentFile();
							if ( !parent.exists() ) {
								parent.mkdirs();
							}
							newFolder.mkdirs();
						}
					}
				} catch ( IOException e ) {
					System.out.println( e.getMessage() );
				}
				if ( showOnWork ) {
					if ( onWork == 41 ) {
						onWork = 2;
						System.out.print( "C-   " );
						System.out.print( "\b\b\b\b\b" );
					} else if ( onWork == 11 ) {
						onWork = 12;
						System.out.print( "C\\   " );
						System.out.print( "\b\b\b\b\b" );
					} else if ( onWork == 21 ) {
						onWork = 22;
						System.out.print( "C|   " );
						System.out.print( "\b\b\b\b\b" );
					} else if ( onWork == 31 ) {
						onWork = 32;
						System.out.print( "C/   " );
						System.out.print( "\b\b\b\b\b" );
					} else {
						onWork = onWork + 1;
					}
				}
			}
			// Ausgabe der SIARD-Version
			String pathToWorkDir2 = pathToWorkDir + File.separator + "SIARD";
			File metadataXml = new File( new StringBuilder( pathToWorkDir2 ).append( File.separator )
					.append( "header" ).append( File.separator ).append( "metadata.xml" ).toString() );
			Boolean version1 = FileUtils.readFileToString( metadataXml, "ISO-8859-1" ).contains(
					"http://www.bar.admin.ch/xmlns/siard/1.0/metadata.xsd" );
			Boolean version2 = FileUtils.readFileToString( metadataXml, "ISO-8859-1" ).contains(
					"http://www.bar.admin.ch/xmlns/siard/2/metadata.xsd" );
			if ( version1 ) {
				getMessageService().logError(
						getTextResourceService().getText( MESSAGE_FORMATVALIDATION_VL, "v1.0" ) );
			} else if ( version2 ) {
				File versionDir = new File( pathToWorkDir2 + File.separator + "header" + File.separator
						+ "siardversion" + File.separator + "2.1" );
				if ( versionDir.exists() ) {
					getMessageService().logError(
							getTextResourceService().getText( MESSAGE_FORMATVALIDATION_VL, "v2.1" ) );
				}
			}

			if ( xmlToValidate != null && xsdToValidate != null ) {
				// der andere Fall wurde bereits oben abgefangen
				try {

					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					// dbf.setValidating(false);
					DocumentBuilder db = dbf.newDocumentBuilder();
					Document doc = db.parse( new FileInputStream( xmlToValidate ) );
					doc.getDocumentElement().normalize();

					BufferedReader in = new BufferedReader( new FileReader( xmlToValidate ) );
					StringBuffer concatenatedOutputs = new StringBuffer();
					String line;
					while ( (line = in.readLine()) != null ) {

						concatenatedOutputs.append( line );
						concatenatedOutputs.append( NEWLINE );
						/* Kontrollieren, dass kein Namespace verwendet wurde wie z.B. v4:
						 * 
						 * <dbname> */
						if ( line.contains( "dbname>" ) ) {
							if ( !line.contains( "<dbname>" ) ) {
								// Invalider Status
								int start = line.indexOf( "<" ) + 1;
								int ns = line.indexOf( ":" ) + 1;
								int end = line.indexOf( ">" );
								String lineNode = line.substring( ns, end );
								String lineNodeNS = line.substring( start, end );
								// System.out.println( lineNode + " " + lineNodeNS );
								getMessageService().logError(
										getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
												+ getTextResourceService().getText( MESSAGE_XML_C_METADATA_NSFOUND,
														lineNode, lineNodeNS ) );
								in.close();
								// set to null
								in = null;
								return false;
							} else {
								// valider Status
								line = null;
							}
						}
					}
					in.close();
					// set to null
					in = null;

					// Validierung von metadata.xml und metadata.xsd mit dem (private class) Validator
					System.setProperty( "javax.xml.parsers.DocumentBuilderFactory",
							"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl" );
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					factory.setNamespaceAware( true );
					factory.setValidating( true );
					factory.setAttribute( "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
							"http://www.w3.org/2001/XMLSchema" );
					factory.setAttribute( "http://java.sun.com/xml/jaxp/properties/schemaSource",
							xsdToValidate.getAbsolutePath() );
					DocumentBuilder builder = factory.newDocumentBuilder();
					Validator handler = new Validator();
					builder.setErrorHandler( handler );
					builder.parse( xmlToValidate.getAbsolutePath() );
					if ( handler.validationError == true ) {
						return false;
					}
				} catch ( java.io.IOException ioe ) {
					getMessageService().logError(
							getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
									+ getTextResourceService().getText( ERROR_XML_UNKNOWN,
											ioe.getMessage() + " (IOException)" ) );
					result = false;
				} catch ( SAXException e ) {
					getMessageService().logError(
							getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
									+ getTextResourceService().getText( ERROR_XML_UNKNOWN,
											e.getMessage() + " (SAXException)" ) );
					result = false;
				} catch ( ParserConfigurationException e ) {
					getMessageService().logError(
							getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
									+ getTextResourceService().getText( ERROR_XML_UNKNOWN,
											e.getMessage() + " (ParserConfigurationException)" ) );
					result = false;
				}
			}
			zipfile.close();
			// set to null
			zipfile = null;
		} catch ( Exception e ) {
			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
							+ getTextResourceService().getText( ERROR_XML_UNKNOWN, e.getMessage() ) );
			return false;
		}
		return result;
	}

	private class Validator extends DefaultHandler
	{
		public boolean						validationError		= false;
		public SAXParseException	saxParseException	= null;

		public void error( SAXParseException exception ) throws SAXException
		{
			validationError = true;
			saxParseException = exception;
			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
							+ getTextResourceService().getText( MESSAGE_XML_C_METADATA_ERRORS,
									saxParseException.getLineNumber(), saxParseException.getColumnNumber(),
									saxParseException.getMessage() ) );
		}

		public void fatalError( SAXParseException exception ) throws SAXException
		{
			validationError = true;
			saxParseException = exception;
			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_C_SIARD )
							+ getTextResourceService().getText( MESSAGE_XML_C_METADATA_ERRORS,
									saxParseException.getLineNumber(), saxParseException.getColumnNumber(),
									saxParseException.getMessage() ) );
		}

		public void warning( SAXParseException exception ) throws SAXException
		{
			// Warnungen werden nicht ausgegeben
		}
	}
}
