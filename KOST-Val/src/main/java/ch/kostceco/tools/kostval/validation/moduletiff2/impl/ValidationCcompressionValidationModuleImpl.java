/* == KOST-Val ==================================================================================
 * The KOST-Val application is used for validate TIFF, SIARD, PDF/A, JP2, JPEG-Files and Submission
 * Information Package (SIP). Copyright (C) 2012-2016 Claire Röthlisberger (KOST-CECO), Christian
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

package ch.kostceco.tools.kostval.validation.moduletiff2.impl;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import ch.kostceco.tools.kostval.exception.moduletiff2.ValidationCcompressionValidationException;
import ch.kostceco.tools.kostval.service.ConfigurationService;
import ch.kostceco.tools.kostval.util.StreamGobbler;
import ch.kostceco.tools.kostval.util.Util;
import ch.kostceco.tools.kostval.validation.ValidationModuleImpl;
import ch.kostceco.tools.kostval.validation.moduletiff2.ValidationCcompressionValidationModule;

/** Validierungsschritt C (Komprimierung-Validierung) Ist die TIFF-Datei gemäss Konfigurationsdatei
 * valid?
 * 
 * @author Rc Claire Röthlisberger, KOST-CECO */

public class ValidationCcompressionValidationModuleImpl extends ValidationModuleImpl implements
		ValidationCcompressionValidationModule
{

	private ConfigurationService	configurationService;

	public static String					NEWLINE	= System.getProperty( "line.separator" );

	public ConfigurationService getConfigurationService()
	{
		return configurationService;
	}

	public void setConfigurationService( ConfigurationService configurationService )
	{
		this.configurationService = configurationService;
	}

	@Override
	public boolean validate( File valDatei, File directoryOfLogfile )
			throws ValidationCcompressionValidationException
	{

		boolean isValid = true;

		// Informationen zum Logverzeichnis holen
		String pathToExiftoolOutput = directoryOfLogfile.getAbsolutePath();
		File exiftoolReport = new File( pathToExiftoolOutput, valDatei.getName() + ".exiftool-log.txt" );
		pathToExiftoolOutput = exiftoolReport.getAbsolutePath();

		/* Nicht vergessen in "src/main/resources/config/applicationContext-services.xml" beim
		 * entsprechenden Modul die property anzugeben: <property name="configurationService"
		 * ref="configurationService" /> */

		String com1 = getConfigurationService().getAllowedCompression1();
		String com2 = getConfigurationService().getAllowedCompression2();
		String com3 = getConfigurationService().getAllowedCompression3();
		String com4 = getConfigurationService().getAllowedCompression4();
		String com5 = getConfigurationService().getAllowedCompression5();
		String com7 = getConfigurationService().getAllowedCompression7();
		String com8 = getConfigurationService().getAllowedCompression8();
		String com32773 = getConfigurationService().getAllowedCompression32773();

		Integer exiftoolio = 0;
		String oldErrorLine = "";

		/* TODO: Exiftool starten. Anschliessend auswerten! Auf jhove wird verzichtet */

		File fIdentifyExe = new File( "resources" + File.separator + "exiftool-9.32" + File.separator
				+ "exiftool.exe" );
		if ( !fIdentifyExe.exists() ) {
			// exiftool.exe existiert nicht --> Abbruch
			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_C_TIFF )
							+ getTextResourceService().getText( MESSAGE_XML_CG_ET_MISSING ) );
			return false;
		} else {
			String pathToIdentifyExe = fIdentifyExe.getAbsolutePath();

			try {

				String command = "cmd /c \"\""
						+ pathToIdentifyExe
						+ "\" -ver -a -s2 -FileName -Directory -Compression -FillOrder -PhotometricInterpretation"
						+ " -PlanarConfiguration -BitsPerSample -StripByteCounts -RowsPerStrip -FileSize"
						+ " -Orientation -TileWidth -TileLength -TileDepth \"" + valDatei.getAbsolutePath()
						+ "\" >>\"" + pathToExiftoolOutput + "\"";
				/* Das redirect Zeichen verunmöglicht eine direkte eingabe. mit dem geschachtellten Befehl
				 * gehts: cmd /c\"urspruenlicher Befehl\" */

				Process proc = null;
				Runtime rt = null;

				try {
					/* falls das File exiftoolReport bereits existiert, z.B. von einem vorhergehenden
					 * Durchlauf, löschen wir es */
					if ( exiftoolReport.exists() ) {
						exiftoolReport.delete();
					}
					Util.switchOffConsole();
					rt = Runtime.getRuntime();
					proc = rt.exec( command.toString().split( " " ) );
					// .split(" ") ist notwendig wenn in einem Pfad ein Doppelleerschlag vorhanden ist!

					// Fehleroutput holen
					StreamGobbler errorGobbler = new StreamGobbler( proc.getErrorStream(), "ERROR" );

					// Output holen
					StreamGobbler outputGobbler = new StreamGobbler( proc.getInputStream(), "OUTPUT" );

					// Threads starten
					errorGobbler.start();
					outputGobbler.start();

					// Warte, bis wget fertig ist
					proc.waitFor();

					Util.switchOnConsole();
					// Kontrolle ob der Report existiert
					if ( !exiftoolReport.exists() ) {
						getMessageService().logError(
								getTextResourceService().getText( MESSAGE_XML_MODUL_C_TIFF )
										+ getTextResourceService().getText( MESSAGE_XML_CG_ET_MISSING ) );
						return false;
					}
				} catch ( Exception e ) {
					getMessageService().logError(
							getTextResourceService().getText( MESSAGE_XML_MODUL_C_TIFF )
									+ getTextResourceService().getText( MESSAGE_XML_CG_ET_SERVICEFAILED,
											e.getMessage() ) );
					return false;
				} finally {
					if ( proc != null ) {
						closeQuietly( proc.getOutputStream() );
						closeQuietly( proc.getInputStream() );
						closeQuietly( proc.getErrorStream() );
					}
				}

				// Ende Exiftool direkt auszulösen

			} catch ( Exception e ) {
				getMessageService().logError(
						getTextResourceService().getText( MESSAGE_XML_MODUL_C_TIFF )
								+ getTextResourceService().getText( ERROR_XML_UNKNOWN, e.getMessage() ) );
				return false;
			}

		}

		try {
			BufferedReader in = new BufferedReader( new FileReader( exiftoolReport ) );
			String line;
			while ( (line = in.readLine()) != null ) {
				/* zu analysierende TIFF-IFD-Zeile die CompressionScheme-Zeile enthält einer dieser
				 * Freitexte der Komprimierungsart */
				if ( line.contains( "Compression: " ) ) {
					exiftoolio = 1;
					if ( line.equalsIgnoreCase( "Compression: " + com1 )
							|| line.equalsIgnoreCase( "Compression: " + com2 )
							|| line.equalsIgnoreCase( "Compression: " + com3 )
							|| line.equalsIgnoreCase( "Compression: " + com4 )
							|| line.equalsIgnoreCase( "Compression: " + com5 )
							|| line.equalsIgnoreCase( "Compression: " + com7 )
							|| line.equalsIgnoreCase( "Compression: " + com8 )
							|| line.equalsIgnoreCase( "Compression: " + com32773 ) ) {
						// Valider Status
					} else {
						// Invalider Status
						isValid = false;
						if ( !line.equals( oldErrorLine ) ) {
							// neuer Fehler
							oldErrorLine = line;
							getMessageService().logError(
									getTextResourceService().getText( MESSAGE_XML_MODUL_C_TIFF )
											+ getTextResourceService().getText( MESSAGE_XML_CG_INVALID, line ) );
						}
					}
				}
			}
			if ( exiftoolio == 0 ) {
				// Invalider Status
				isValid = false;
				getMessageService().logError(
						getTextResourceService().getText( MESSAGE_XML_MODUL_C_TIFF )
								+ getTextResourceService().getText( MESSAGE_XML_CG_ETNIO, "C" ) );
			}
			in.close();
		} catch ( Exception e ) {
			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_C_TIFF )
							+ getTextResourceService().getText( MESSAGE_XML_CG_CANNOTFINDETREPORT ) );
			return false;
		}
		return isValid;
	}
}