package de.uniulm.omi.flexiant;

import de.uniulm.omi.flexiant.extility.Vdc;

public class FlexiantLocation {

	private Vdc vdc;

	public FlexiantLocation(Vdc vdc) {
		if(vdc == null) {
			throw new IllegalArgumentException(
				"The parameter vdc must not be null."
			);
		}
		this.vdc = vdc;
	}
	
	public String getId() {
		return vdc.getResourceUUID();
	}

}
