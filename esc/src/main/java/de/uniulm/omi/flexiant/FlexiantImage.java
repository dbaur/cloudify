package de.uniulm.omi.flexiant;

import net.flexiant.extility.Image;

public class FlexiantImage {
	
	private Image image;
	
	public FlexiantImage(Image image) {
		if(image == null) {
			throw new IllegalArgumentException("The parameter image must not be null.");
		}
		this.image = image;
	}

	public String getId() {
		return image.getResourceUUID();
	}
	
	public String getDefaultUser() {
		return image.getDefaultUser();
	}
	
	public boolean isGenPassword() {
		return image.isGenPassword();
	}
}
