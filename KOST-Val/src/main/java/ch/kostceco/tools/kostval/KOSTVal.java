/* == KOST-Val ==================================================================================
 * The KOST-Val v1.9.8 application is used for validate TIFF, SIARD, PDF/A, JP2, JPEG-Files and
 * Submission Information Package (SIP). Copyright (C) 2012-2020 Claire Roethlisberger (KOST-CECO),
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

package ch.kostceco.tools.kostval;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Element;

import ch.kostceco.tools.kostval.controller.Controllervalfile;
import ch.kostceco.tools.kostval.controller.Controllervalfolder;
import ch.kostceco.tools.kostval.controller.Controllervalinit;
import ch.kostceco.tools.kostval.controller.Controllervalinitlog;
import ch.kostceco.tools.kostval.controller.Controllervalsip;
import ch.kostceco.tools.kostval.logging.LogConfigurator;
import ch.kostceco.tools.kostval.logging.Logger;
import ch.kostceco.tools.kostval.logging.MessageConstants;
import ch.kostceco.tools.kostval.service.ConfigurationService;
import ch.kostceco.tools.kostval.service.TextResourceService;
import ch.kostceco.tools.kostval.util.Util;

/** Dies ist die Starter-Klasse, verantwortlich fuer das Initialisieren des Controllers, des
 * Loggings und das Parsen der Start-Parameter.
 * 
 * @author Rc Claire Roethlisberger, KOST-CECO */

public class KOSTVal implements MessageConstants
{

	private static final Logger		LOGGER	= new Logger( KOSTVal.class );

	private TextResourceService		textResourceService;
	private ConfigurationService	configurationService;

	public TextResourceService getTextResourceService()
	{
		return textResourceService;
	}

	public void setTextResourceService( TextResourceService textResourceService )
	{
		this.textResourceService = textResourceService;
	}

	public ConfigurationService getConfigurationService()
	{
		return configurationService;
	}

	public void setConfigurationService( ConfigurationService configurationService )
	{
		this.configurationService = configurationService;
	}

	/** Die Eingabe besteht aus 2 oder 3 Parameter: [0] Validierungstyp [1] Pfad zur Val-File [2]
	 * option: Verbose
	 * 
	 * @param args
	 * @throws IOException */

	@SuppressWarnings("resource")
	public static boolean main( String[] args ) throws IOException
	{
		boolean mainBoolean = true;
		// System.out.println( new Timestamp( System.currentTimeMillis() ) + " 107 Start " );
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:config/applicationContext.xml" );
		// System.out.println( new Timestamp( System.currentTimeMillis() ) +
		// " 110 Ende ApplicationContext " );

		System.out.println( "KOST-Val" );

		// Zeitstempel Start
		java.util.Date nowStart = new java.util.Date();
		java.text.SimpleDateFormat sdfStart = new java.text.SimpleDateFormat( "dd.MM.yyyy HH:mm:ss" );
		String ausgabeStart = sdfStart.format( nowStart );

		/* TODO: siehe Bemerkung im applicationContext-services.xml bezueglich Injection in der
		 * Superklasse aller Impl-Klassen ValidationModuleImpl validationModuleImpl =
		 * (ValidationModuleImpl) context.getBean("validationmoduleimpl"); */

		KOSTVal kostval = (KOSTVal) context.getBean( "kostval" );

		/* dirOfJarPath damit auch absolute Pfade kein Problem sind Dies ist eine generelle Aufgabe in
		 * allen Modulen. Zuerst immer dirOfJarPath ermitteln und dann alle Pfade mit dirOfJarPath +
		 * File.separator + erweitern. */
		String path = new java.io.File(
				KOSTVal.class.getProtectionDomain().getCodeSource().getLocation().getPath() )
						.getAbsolutePath();
		String locationOfJarPath = path;
		String dirOfJarPath = locationOfJarPath;
		if ( locationOfJarPath.endsWith( ".jar" ) || locationOfJarPath.endsWith( ".exe" ) ) {
			File file = new File( locationOfJarPath );
			dirOfJarPath = file.getParent();
		}

		// Vorbereitung Konfiguration
		File directoryOfConfigfile = new File( System.getenv( "USERPROFILE" ) + File.separator
				+ ".kost-val" + File.separator + "configuration" );
		File kadZip = new File( dirOfJarPath + File.separator + "configuration" + File.separator
				+ "KaD_SignatureFile_V72.xml" );
		File kadFile = new File( directoryOfConfigfile + File.separator + "KaD_SignatureFile_V72.xml" );
		if ( !kadFile.exists() ) {
			directoryOfConfigfile.mkdirs();
			Util.copyFile( kadZip, kadFile );
		}
		File configFileZip = new File(
				dirOfJarPath + File.separator + "configuration" + File.separator + "kostval.conf.xml" );
		File configFile = new File( directoryOfConfigfile + File.separator + "kostval.conf.xml" );
		if ( !configFile.exists() ) {
			directoryOfConfigfile.mkdirs();
			Util.copyFile( configFileZip, configFile );
		}
		Locale locale = Locale.getDefault();

		if ( args[2].equalsIgnoreCase( "--de" ) ) {
			locale = new Locale( "de" );
		} else if ( args[2].equalsIgnoreCase( "--fr" ) ) {
			locale = new Locale( "fr" );
		} else if ( args[2].equalsIgnoreCase( "--en" ) ) {
			locale = new Locale( "en" );
		} else {
			locale = new Locale( "de" );
		}

		// Konfigurations Map erstellen (Zeitgewinn)
		Map<String, String> configMap = kostval.getConfigurationService().configMap( locale );

		Controllervalinit controller0 = (Controllervalinit) context.getBean( "controllervalinit" );
		boolean valInit = controller0.valInit( args, configMap );
		if ( valInit ) {
			// folgendes wurde erfolgreich ueberprueft:

			// Ueberpruefung args (Anzahl und Werte)
			// Ueberpruefung des Parameters (Log-Verzeichnis)
		} else {
			// Fehler: es wird abgebrochen
			if ( args[3].equalsIgnoreCase( "--gui" ) || args[3].equalsIgnoreCase( "--hot" ) ) {
				mainBoolean = false;
				return mainBoolean;
			} else {
				System.exit( 1 );
			}
		}
		String pathToLogfile = configMap.get( "PathToLogfile" );
		File directoryOfLogfile = new File( pathToLogfile );

		if ( args[2].equalsIgnoreCase( "--de" ) ) {
			locale = new Locale( "de" );
		} else if ( args[2].equalsIgnoreCase( "--fr" ) ) {
			locale = new Locale( "fr" );
		} else if ( args[2].equalsIgnoreCase( "--en" ) ) {
			locale = new Locale( "en" );
		}

		File valDatei = new File( args[4] );
		File logDatei = null;
		logDatei = valDatei;

		// Konfiguration des Loggings, ein File Logger wird zusaetzlich erstellt
		LogConfigurator logConfigurator = (LogConfigurator) context.getBean( "logconfigurator" );
		String logFileName = logConfigurator.configure( directoryOfLogfile.getAbsolutePath(),
				logDatei.getName() );
		File logFile = new File( logFileName );
		// Ab hier kann ins log geschrieben werden...

		Controllervalinitlog controller0log = (Controllervalinitlog) context
				.getBean( "controllervalinitlog" );
		boolean valInitlog = controller0log.valInitlog( args, configMap, directoryOfLogfile, locale,
				ausgabeStart, logFile, dirOfJarPath );
		if ( valInitlog ) {
			// ggf alte SIP-Validierung-Versions-Notiz loeschen
			// ermitteln welche Formate validiert werden koennen respektive eingeschaltet sind
			// Informationen zum Arbeitsverzeichnis holen
			// bestehendes Workverzeichnis Abbruch wenn nicht leer, da am Schluss das Workverzeichnis
			// Im Pfad keine Sonderzeichen xml-Validierung SIP 1d und SIARD C stuerzen ab
			// die Anwendung muss mindestens unter Java 8 laufen
			// Im workverzeichnis besteht kein Schreibrecht
			// existiert configuration\jhove.conf
			// Im Pfad keine Sonderzeichen xml-Validierung SIP 1d und SIARD C stuerzen ab
		} else {
			// Fehler: es wird abgebrochen
			if ( args[3].equalsIgnoreCase( "--gui" ) || args[3].equalsIgnoreCase( "--hot" ) ) {
				mainBoolean = false;
				return mainBoolean;
			} else {
				System.exit( 1 );
			}
		}

		// Informationen zum Arbeitsverzeichnis holen
		String pathToWorkDir = configMap.get( "PathToWorkDir" );
		File tmpDir = new File( pathToWorkDir );

		/* Ueberprüfung des optionalen Parameters (4 --max = xml + verbose --> im Verbose-mode werden
		 * die originalen Logs nicht geloescht (Jhove & Co.) */
		boolean verbose = false;
		if ( (args[1].equals( "--max" )) ) {
			verbose = true;
		}

		if ( args[0].equals( "--format" ) ) {
			LOGGER.logError( kostval.getTextResourceService().getText( locale, MESSAGE_XML_FORMAT1 ) );

			// TODO: Formatvalidierung an einer Datei --> erledigt --> nur Marker
			if ( !valDatei.isDirectory() ) {
				System.out.print( valDatei.getAbsolutePath() + " " );
				/* boolean valFile = valFile( valDatei, logFileName, directoryOfLogfile, verbose,
				 * dirOfJarPath, configMap, context ); */
				Controllervalfile controller1 = (Controllervalfile) context.getBean( "controllervalfile" );
				boolean valFile = controller1.valFile( valDatei, logFileName, directoryOfLogfile, verbose,
						dirOfJarPath, configMap, context, locale );

				LOGGER.logError( kostval.getTextResourceService().getText( locale, MESSAGE_XML_FORMAT2 ) );

				// Loeschen des Arbeitsverzeichnisses, falls eines angelegt wurde
				if ( tmpDir.exists() ) {
					Util.deleteDir( tmpDir );
				}

				LOGGER.logError( kostval.getTextResourceService().getText( locale, MESSAGE_XML_LOGEND ) );
				// logFile bereinigung (& End und ggf 3c)
				Util.valEnd3cAmp( "", logFile );

				if ( valFile ) {
					// Loeschen des Arbeitsverzeichnisses, falls eines angelegt wurde
					if ( tmpDir.exists() ) {
						Util.deleteDir( tmpDir );
					}
					if ( tmpDir.exists() ) {
						tmpDir.deleteOnExit();
					}

					File pathTemp = new File( directoryOfLogfile, "path.tmp" );
					if ( pathTemp.exists() ) {
						pathTemp.delete();
					}
					if ( pathTemp.exists() ) {
						pathTemp.deleteOnExit();
					}

					// Validierte Datei valide
					if ( args[3].equalsIgnoreCase( "--gui" ) || args[3].equalsIgnoreCase( "--hot" ) ) {
						mainBoolean = true;
						return mainBoolean;
					} else {
						System.exit( 0 );
					}
				} else {
					// Loeschen des Arbeitsverzeichnisses, falls eines angelegt wurde
					if ( tmpDir.exists() ) {
						Util.deleteDir( tmpDir );
					}
					// Fehler in Validierte Datei --> invalide
					if ( args[3].equalsIgnoreCase( "--gui" ) || args[3].equalsIgnoreCase( "--hot" ) ) {
						mainBoolean = false;
						return mainBoolean;
					} else {
						System.exit( 2 );
					}

				}
			} else {
				// TODO: Formatvalidierung ueber ein Ordner --> erledigt --> nur Marker
				Controllervalfolder controller2 = (Controllervalfolder) context
						.getBean( "controllervalfolder" );
				boolean valFolder = controller2.valFolder( valDatei, logFileName, directoryOfLogfile,
						verbose, dirOfJarPath, configMap, context, locale );
				if ( valFolder ) {
					// Loeschen des Arbeitsverzeichnisses, falls eines angelegt wurde
					if ( tmpDir.exists() ) {
						Util.deleteDir( tmpDir );
					}
					if ( tmpDir.exists() ) {
						tmpDir.deleteOnExit();
					}

					File pathTemp = new File( directoryOfLogfile, "path.tmp" );
					if ( pathTemp.exists() ) {
						pathTemp.delete();
					}
					if ( pathTemp.exists() ) {
						pathTemp.deleteOnExit();
					}

					// Validierte Dateien valide
					if ( args[3].equalsIgnoreCase( "--gui" ) || args[3].equalsIgnoreCase( "--hot" ) ) {
						mainBoolean = true;
						return mainBoolean;
					} else {
						System.exit( 0 );
					}
				} else {
					// Loeschen des Arbeitsverzeichnisses, falls eines angelegt wurde
					if ( tmpDir.exists() ) {
						Util.deleteDir( tmpDir );
					}
					// Fehler in Validierte Dateien --> invalide
					if ( args[3].equalsIgnoreCase( "--gui" ) || args[3].equalsIgnoreCase( "--hot" ) ) {
						mainBoolean = false;
						return mainBoolean;
					} else {
						System.exit(2 );
					}
				}
			}

		} else if ( args[0].equals( "--sip" ) ) {
			// TODO: Sipvalidierung --> erledigt --> nur Marker
			Controllervalsip controller3 = (Controllervalsip) context.getBean( "controllervalsip" );
			boolean valSip = controller3.valSip( valDatei, logFileName, directoryOfLogfile, verbose,
					dirOfJarPath, configMap, context, locale );
			if ( valSip ) {
				// Loeschen des Arbeitsverzeichnisses, falls eines angelegt wurde
				if ( tmpDir.exists() ) {
					Util.deleteDir( tmpDir );
				}
				if ( tmpDir.exists() ) {
					tmpDir.deleteOnExit();
				}

				File pathTemp = new File( directoryOfLogfile, "path.tmp" );
				if ( pathTemp.exists() ) {
					pathTemp.delete();
				}
				if ( pathTemp.exists() ) {
					pathTemp.deleteOnExit();
				}

				// Validierte Dateien valide
				if ( args[3].equalsIgnoreCase( "--gui" ) || args[3].equalsIgnoreCase( "--hot" ) ) {
					mainBoolean = true;
					return mainBoolean;
				} else {
					System.exit( 0 );
				}
			} else {
				// Loeschen des Arbeitsverzeichnisses, falls eines angelegt wurde
				if ( tmpDir.exists() ) {
					Util.deleteDir( tmpDir );
				}
				// Fehler in Validierte Dateien --> invalide
				if ( args[3].equalsIgnoreCase( "--gui" ) || args[3].equalsIgnoreCase( "--hot" ) ) {
					mainBoolean = false;
					return mainBoolean;
				} else {
					System.exit( 2 );
				}
			}

		} else {
			/* Ueberpruefung des Parameters (Val-Typ): format / sip args[0] ist nicht "--format" oder
			 * "--sip" --> INVALIDE
			 * 
			 * in Controllerinit ueberprueft */
		}
		return mainBoolean;
	}

	// TODO: ElementToStream --> erledigt --> nur Marker
	public static void ElementToStream( Element element, OutputStream out )
	{
		try {
			DOMSource source = new DOMSource( element );
			StreamResult result = new StreamResult( out );
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			transformer.transform( source, result );
		} catch ( Exception ex ) {
		}
	}

}
