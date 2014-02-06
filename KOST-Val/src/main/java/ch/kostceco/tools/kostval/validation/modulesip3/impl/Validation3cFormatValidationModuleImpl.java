/*== KOST-Val ==================================================================================
The KOST-Val application is used for validate TIFF, SIARD, PDF/A-Files and Submission 
Information Package (SIP). 
Copyright (C) 2012-2014 Claire Röthlisberger (KOST-CECO), Christian Eugster, Olivier Debenath, 
Peter Schneider (Staatsarchiv Aargau), Daniel Ludin (BEDAG AG)
-----------------------------------------------------------------------------------------------
KOST-Val is a development of the KOST-CECO. All rights rest with the KOST-CECO. 
This application is free software: you can redistribute it and/or modify it under the 
terms of the GNU General Public License as published by the Free Software Foundation, 
either version 3 of the License, or (at your option) any later version. 
BEDAG AG and Daniel Ludin hereby disclaims all copyright interest in the program 
SIP-Val v0.2.0 written by Daniel Ludin (BEDAG AG). Switzerland, 1 March 2011.
This application is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the follow GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program; 
if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
Boston, MA 02110-1301 USA or see <http://www.gnu.org/licenses/>.
==============================================================================================*/

package ch.kostceco.tools.kostval.validation.modulesip3.impl;

import java.io.File;

import ch.kostceco.tools.kostval.exception.modulesip3.Validation3cFormatValidationException;
import ch.kostceco.tools.kostval.service.ConfigurationService;
import ch.kostceco.tools.kostval.util.Util;
import ch.kostceco.tools.kostval.validation.ValidationModuleImpl;
import ch.kostceco.tools.kostval.validation.modulesip3.Validation3cFormatValidationModule;

public class Validation3cFormatValidationModuleImpl extends
		ValidationModuleImpl implements Validation3cFormatValidationModule
{

	private ConfigurationService	configurationService;

	public static String			NEWLINE	= System.getProperty( "line.separator" );

	public ConfigurationService getConfigurationService()
	{
		return configurationService;
	}

	public void setConfigurationService(
			ConfigurationService configurationService )
	{
		this.configurationService = configurationService;
	}

	@Override
	public boolean validate( File valDatei, File directoryOfLogfile )
			throws Validation3cFormatValidationException
	{

		boolean isValid = true;
		
		// Die Formatvalidierung des Contents erfolgte vor 1a

		String fileName3cIo = "3c_Valide.txt";
		String fileName3cNio = "3c_Invalide.txt";

		File outputFile3cIo = new File( directoryOfLogfile + fileName3cIo );
		File outputFile3cNio = new File( directoryOfLogfile + fileName3cNio );
		if ( outputFile3cIo.exists() ) {
			// 3c valid
			Util.deleteDir( outputFile3cIo );
			isValid = true;
		} else {
			Util.deleteDir( outputFile3cNio );
			isValid = false;
			getMessageService().logError(
					getTextResourceService()
							.getText( MESSAGE_MODULE_Cc )
							+ getTextResourceService().getText(
									MESSAGE_DASHES )
							+ getTextResourceService().getText(
									MESSAGE_MODULE_CC_INVALID ) );

		}
		return isValid;

	}
}