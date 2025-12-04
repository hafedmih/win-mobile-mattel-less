// The present software is not subject to the US Export Administration Regulations (no exportation license required), May 2012
package com.famoco.kyctelcomrtlib.biometrics;

import com.morpho.morphosmart.sdk.CompressionAlgorithm;
import com.morpho.morphosmart.sdk.TemplateFVPType;
import com.morpho.morphosmart.sdk.TemplateType;

public class EnrollInfo extends MorphoInfo
{
	private static EnrollInfo	mInstance	= null;

	public static EnrollInfo getInstance()
	{
		if (mInstance == null)
		{
			mInstance = new EnrollInfo();
			mInstance.reset();
		}
		return mInstance;
	}

	private EnrollInfo()
	{
	}

	public String toString()
	{
		return "idNumber" + IDNumber + "\r\n" + "firstname:\t" + firstName + "\r\n" + "lastname:\t" + lastName + "\r\n" + "fingernumber:\t" + fingerNumber + "\r\n" + "savePKinDatabase:\t"
				+ savePKinDatabase + "\r\n" + "exportImage:\t" + compressionAlgorithm.getLabel() + "\r\n" + "fpTemplateType:\t" + templateType;

	}

	public void reset()
	{
		IDNumber = "";
		firstName = "";
		lastName = "";
		fingerNumber = 1;
		savePKinDatabase = true;
		compressionAlgorithm = CompressionAlgorithm.NO_IMAGE;
		templateType = TemplateType.MORPHO_NO_PK_FP;
		updateTemplate = false;
		setFingerIndex(0);
	}

	private String IDNumber				= "";
	private String firstName				= "";
	private String lastName				= "";
	private int						fingerNumber			= 0;
	private boolean					savePKinDatabase		= true;
	private CompressionAlgorithm compressionAlgorithm	= CompressionAlgorithm.NO_IMAGE;
	private TemplateType templateType			= TemplateType.MORPHO_NO_PK_FP;
	private TemplateFVPType fvptemplateType			= TemplateFVPType.MORPHO_NO_PK_FVP;
	private boolean                 updateTemplate          = false;
	private int						fingerIndex				= 0;

	public void setFingerIndex(int fingerIndex) {
		this.fingerIndex = fingerIndex;
	}
}
