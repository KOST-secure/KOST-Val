/* == KOST-Val ==================================================================================
 * The KOST-Val application is used for validate TIFF, SIARD, PDF/A, JP2, JPEG, PNG-Files and
 * Submission Information Package (SIP). Copyright (C) 2012-2021 Claire Roethlisberger (KOST-CECO),
 * Christian Eugster, Olivier Debenath, Peter Schneider (Staatsarchiv Aargau), Markus Hahn
 * (coderslagoon), Daniel Ludin (BEDAG AG)
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
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
 * valid zu metadata.xsd und beides vorhanden Bemerkung --> zusaetzliche Ordner oder Dateien wie
 * z.B. metadata.xls sind im header-Ordner erlaubt ==> Bei den Module A, B, C und D wird die
 * Validierung abgebrochen, sollte das Resulat invalid sein!
 * 
 * @author Rc Claire Roethlisberger, KOST-CECO */

public class ValidationCheaderModuleImpl extends ValidationModuleImpl
		implements ValidationCheaderModule
{

	public static String	NEWLINE	= System.getProperty( "line.separator" );

	private boolean				min			= false;

	@SuppressWarnings("resource")
	@Override
	public boolean validate( File valDatei, File directoryOfLogfile, Map<String, String> configMap,
			Locale locale ) throws ValidationCheaderException
	{
		boolean showOnWork = false;
		int onWork = 410;
		// Informationen zur Darstellung "onWork" holen
		String onWorkConfig = configMap.get( "ShowProgressOnWork" );
		if ( onWorkConfig.equals( "yes" ) ) {
			// Ausgabe Modul Ersichtlich das KOST-Val arbeitet
			showOnWork = true;
			System.out.print( "C    " );
			System.out.print( "\b\b\b\b\b" );
		} else if ( onWorkConfig.equals( "nomin" ) ) {
			min = true;
		}

		boolean siard10 = false;
		boolean siard21 = false;
		String siard10St = configMap.get( "siard10" );
		if ( siard10St.equals( "1.0" ) ) {
			siard10 = true;
		}
		String siard21St = configMap.get( "siard21" );
		if ( siard21St.equals( "2.1" ) ) {
			siard21 = true;
		}

		boolean result = true;
		// Sind im Header-Ordner metadata.xml und metadata.xsd vorhanden?
		ZipEntry metadataxml = null;
		ZipEntry metadataxsd = null;

		try {
			ZipFile zipfile = new ZipFile( valDatei.getAbsolutePath() );
			Enumeration<? extends ZipEntry> entries = zipfile.entries();
			while ( entries.hasMoreElements() ) {
				ZipEntry zEntry = entries.nextElement();
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
				zipfile.close();
				if ( min ) {
					return false;
				} else {
					getMessageService()
							.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
									+ getTextResourceService().getText( locale, MESSAGE_XML_C_NOMETADATAFOUND ) );
					return false;
				}
			}
			if ( metadataxsd == null ) {
				// keine metadata.xsd = XSD_METADATA in der SIARD-Datei gefunden
				zipfile.close();
				if ( min ) {
					return false;
				} else {
					getMessageService()
							.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
									+ getTextResourceService().getText( locale, MESSAGE_XML_C_NOMETADATAXSD ) );
					return false;
				}
			}
			zipfile.close();
		} catch ( Exception e ) {
			if ( min ) {
				return false;
			} else {
				getMessageService()
						.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
								+ getTextResourceService().getText( locale, ERROR_XML_UNKNOWN,
										e.getMessage() + " xml und xsd" ) );
				return false;
			}
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

			/* Das metadata.xml und sein xsd muessen in das Filesystem extrahiert werden, weil bei bei
			 * Verwendung eines Inputstreams bei der Validierung ein Problem mit den xs:include Statements
			 * besteht, die includes koennen so nicht aufgeloest werden. Es werden hier jedoch nicht nur
			 * diese Files extrahiert, sondern gleich die ganze Zip-Datei, weil auch spaetere
			 * Validierungen nur mit den extrahierten Files arbeiten koennen. */
			int BUFFER = 2048;
			ZipFile zipfile = new ZipFile( valDatei.getAbsolutePath() );
			Enumeration<? extends ZipEntry> entries = zipfile.entries();

			// jeden entry durchgechen
			while ( entries.hasMoreElements() ) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				String entryName = entry.getName();
				File destFile = new File( tmpDir, entryName );
				// System.out.println (entryName);

				// erstelle den Ueberordner
				File destinationParent = destFile.getParentFile();
				destinationParent.mkdirs();
				if ( !entry.isDirectory() ) {
					// Festhalten von metadata.xml und metadata.xsd
					if ( destFile.getName().endsWith( METADATA ) ) {
						xmlToValidate = destFile;
					}
					if ( destFile.getName().endsWith( XSD_METADATA ) ) {
						xsdToValidate = destFile;
					}
					InputStream stream = zipfile.getInputStream( entry );
					BufferedInputStream is = new BufferedInputStream( stream );
					int currentByte;

					// erstellung Buffer zum schreiben der Dateien
					byte data[] = new byte[BUFFER];

					// schreibe die aktuelle Datei an den geuenschten Ort
					FileOutputStream fos = new FileOutputStream( destFile );
					BufferedOutputStream dest = new BufferedOutputStream( fos, BUFFER );
					while ( (currentByte = is.read( data, 0, BUFFER )) != -1 ) {
						dest.write( data, 0, currentByte );
					}
					dest.flush();
					dest.close();
					is.close();
					stream.close();
				} else {
					destFile.mkdirs();
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
			// Thread.sleep( 100 );
			// Ausgabe der SIARD-Version
			String pathToWorkDir2 = pathToWorkDir + File.separator + "SIARD";
			File metadataXml = new File( new StringBuilder( pathToWorkDir2 ).append( File.separator )
					.append( "header" ).append( File.separator ).append( "metadata.xml" ).toString() );
			Boolean version1 = FileUtils.readFileToString( metadataXml, "ISO-8859-1" )
					.contains( "http://www.bar.admin.ch/xmlns/siard/1.0/metadata.xsd" );
			Boolean version2 = FileUtils.readFileToString( metadataXml, "ISO-8859-1" )
					.contains( "http://www.bar.admin.ch/xmlns/siard/2/metadata.xsd" );
			if ( version1 ) {
				if ( siard10 ) {
					getMessageService().logError(
							getTextResourceService().getText( locale, MESSAGE_FORMATVALIDATION_VL, "v1.0" ) );
				} else {
					if ( min ) {
						return false;
					} else {
						getMessageService()
								.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
										+ getTextResourceService().getText( locale, MESSAGE_XML_C_INVALID_VERSION,
												"1.0" ) );
						return false;
					}
				}
			} else if ( version2 ) {
				if ( siard21 ) {
					File versionDir = new File( pathToWorkDir2 + File.separator + "header" + File.separator
							+ "siardversion" + File.separator + "2.1" );
					if ( versionDir.exists() ) {
						getMessageService().logError(
								getTextResourceService().getText( locale, MESSAGE_FORMATVALIDATION_VL, "v2.1" ) );
					}
				} else {
					if ( min ) {
						return false;
					} else {
						getMessageService()
								.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
										+ getTextResourceService().getText( locale, MESSAGE_XML_C_INVALID_VERSION,
												"2.1" ) );
						return false;
					}
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
								if ( min ) {
									return false;
								} else {
									int start = line.indexOf( "<" ) + 1;
									int ns = line.indexOf( ":" ) + 1;
									int end = line.indexOf( ">" );
									String lineNode = line.substring( ns, end );
									String lineNodeNS = line.substring( start, end );
									// System.out.println( lineNode + " " + lineNodeNS );
									getMessageService().logError(
											getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
													+ getTextResourceService().getText( locale,
															MESSAGE_XML_C_METADATA_NSFOUND, lineNode, lineNodeNS ) );
									in.close();
									// set to null
									in = null;
									return false;
								}
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
					if ( min ) {
						return false;
					} else {
						getMessageService()
								.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
										+ getTextResourceService().getText( locale, ERROR_XML_UNKNOWN,
												ioe.getMessage() + " (IOException)" ) );
						result = false;
					}
				} catch ( SAXException e ) {
					if ( min ) {
						return false;
					} else {
						getMessageService()
								.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
										+ getTextResourceService().getText( locale, ERROR_XML_UNKNOWN,
												e.getMessage() + " (SAXException)" ) );
						result = false;
					}
				} catch ( ParserConfigurationException e ) {
					if ( min ) {
						return false;
					} else {
						getMessageService()
								.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
										+ getTextResourceService().getText( locale, ERROR_XML_UNKNOWN,
												e.getMessage() + " (ParserConfigurationException)" ) );
						result = false;
					}
				}
			}
			zipfile.close();
			// set to null
			zipfile = null;
		} catch ( Exception e ) {
			if ( min ) {
				return false;
			} else {
				getMessageService()
						.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
								+ getTextResourceService().getText( locale, ERROR_XML_UNKNOWN, e.getMessage() ) );
				return false;
			}
		}
		return result;
	}

	private class Validator extends DefaultHandler
	{
		public boolean						validationError		= false;
		public SAXParseException	saxParseException	= null;

		@SuppressWarnings("unused")
		public void error( SAXParseException exception, Locale locale ) throws SAXException
		{
			validationError = true;
			saxParseException = exception;
			getMessageService()
					.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
							+ getTextResourceService().getText( locale, MESSAGE_XML_C_METADATA_ERRORS,
									saxParseException.getLineNumber(), saxParseException.getColumnNumber(),
									saxParseException.getMessage() ) );
		}

		@SuppressWarnings("unused")
		public void fatalError( SAXParseException exception, Locale locale ) throws SAXException
		{
			validationError = true;
			saxParseException = exception;
			getMessageService()
					.logError( getTextResourceService().getText( locale, MESSAGE_XML_MODUL_C_SIARD )
							+ getTextResourceService().getText( locale, MESSAGE_XML_C_METADATA_ERRORS,
									saxParseException.getLineNumber(), saxParseException.getColumnNumber(),
									saxParseException.getMessage() ) );
		}

		public void warning( SAXParseException exception ) throws SAXException
		{
			// Warnungen werden nicht ausgegeben
		}
	}
}
