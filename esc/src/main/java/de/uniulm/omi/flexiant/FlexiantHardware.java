package de.uniulm.omi.flexiant;

import net.flexiant.extility.ProductOffer;

public class FlexiantHardware {

	private ProductOffer productOffer;

	public FlexiantHardware(ProductOffer productOffer) {
		if(productOffer == null) {
			throw new IllegalArgumentException("The parameter productOffer must not be null.");
		}
		this.productOffer = productOffer;
	}
	
	public String getId() {
		return productOffer.getResourceUUID();
	}

}
