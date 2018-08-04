package bnw.abm.intg.sync.wdwrapper;

public enum GlobalConcept {
	SEX;

	/**
	 * Converts the local concept to global representation 
	 * 
	 * @param attr Attribute that need to be converted
	 * @param concept The commonly used global concept - Global Concept enum
	 * @return Local representation of the attribute
	 */
	public static Object convert2GlobalFormat(Object attr, GlobalConcept concept) {
		switch (concept) {
		case SEX:
			if (attr.toString().matches("(?i)^(f|female)$")) {
				return "female";
			} else if (attr.toString().matches("(?i)^(m|male)$")) {
				return "male";
			}
			break;
		default:
			return null;
		}
		return null;

	}
	
	/**
	 * Converts the global concept to local representation
	 * 
	 * @param attr Object that need to be converted
	 * @param concept The commonly used global concept - Global Concept enum
	 * 
	 * @return Local representation of the attribute
	 */
	public static Object convert2LocalFormat(Object attr, GlobalConcept concept){
		switch (concept) {
		case SEX:
			if (attr.toString().matches("(?i)^(f|female)$")) {
				return "Female";
			} else if (attr.toString().matches("(?i)^(m|male)$")) {
				return "Male";
			}
			break;
			
		default:
			return null;
		}
		return null;

	}
}
