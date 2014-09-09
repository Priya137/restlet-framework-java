package org.restlet.ext.xml.format;

import org.restlet.data.MediaType;

/**
 * The Enum FormatType which corresponds to different MediaType like atom/json/jsonverbose. 
 * * @author <a href="mailto:onkar.dhuri@synerzip.com">Onkar Dhuri</a>
 */
public enum FormatType {

	ATOM(MediaType.APPLICATION_XML.getName(), MediaType.APPLICATION_ATOM .getName()),
	JSONVERBOSE(MediaType.APPLICATION_JSONVERBOSE.getName()),
	JSON( MediaType.APPLICATION_JSON.getName(), MediaType.APPLICATION_JSONLITE_MINIMALMETADATA.getName()),
	JSONLITEFULLMETADATA(MediaType.APPLICATION_JSONLITE_FULLMETADATA.getName()),
	JSONLITENOMETADATA(MediaType.APPLICATION_JSONLITE_NOMETADATA.getName());

	/**
	 * Private constructor to avoid instantiation outside this class.
	 *
	 * @param mediaTypes the media types
	 */
	private FormatType(String... mediaTypes) {
		this.mediaTypes = mediaTypes;
	}

	/** The media types. */
	private final String[] mediaTypes;

	/**
	 * Gets the acceptable media types.
	 *
	 * @return the acceptable media types
	 */
	public String[] getAcceptableMediaTypes() {
		return mediaTypes;
	}

	//TODO:Onkar do we need to compare for hardcoded value strings belows. also modify javadocs
	/**
	 * Return the format type corresponding to argument.
	 *
	 * @param format the format
	 * @return the format type
	 */
	public static FormatType parse(String format) {
		if ("verbosejson".equalsIgnoreCase(format)
				|| "jsonverbose".equalsIgnoreCase(format)
				|| MediaType.APPLICATION_JSONVERBOSE.getName()
						.equalsIgnoreCase(format))
			return JSONVERBOSE;
		else if ("atom".equalsIgnoreCase(format)
				|| "xml".equalsIgnoreCase(format)
				|| MediaType.APPLICATION_XML.getName().equalsIgnoreCase(format)
				|| MediaType.APPLICATION_ATOM.getName()
						.equalsIgnoreCase(format))
			return ATOM;
		else if ("json".equalsIgnoreCase(format)
				|| "json;odata=minimalmetadata".equalsIgnoreCase(format)
				|| MediaType.APPLICATION_JSON.getName()
				.equalsIgnoreCase(format)
				|| MediaType.APPLICATION_JSONLITE_MINIMALMETADATA.getName()
				.equalsIgnoreCase(format))
			return JSON;
		else if ("json;odata=nometadata".equalsIgnoreCase(format)
				|| "jsonlitenometadata".equalsIgnoreCase(format)
				|| MediaType.APPLICATION_JSONLITE_NOMETADATA.getName()
				.equalsIgnoreCase(format))
			return JSONLITENOMETADATA;
		else if ("json;odata=fullmetadata".equalsIgnoreCase(format)
				|| "jsonlitefullmetadata".equalsIgnoreCase(format)
				|| MediaType.APPLICATION_JSONLITE_FULLMETADATA.getName()
				.equalsIgnoreCase(format))
			return JSONLITEFULLMETADATA;
		throw new UnsupportedOperationException("Unsupported format " + format);
	}
	
	/**
	 * Gets the media type corresponding to request format type.
	 *
	 * @param formatType the format type
	 * @return the media type
	 */
	public static MediaType getMediaType(FormatType formatType) {
		switch (formatType) {
			case ATOM:
				return MediaType.APPLICATION_ATOM;
				
			case JSONVERBOSE:
				return MediaType.APPLICATION_JSONVERBOSE;
				
			case JSON:
				return MediaType.APPLICATION_JSON;
				
			case JSONLITEFULLMETADATA:
				return MediaType.APPLICATION_JSONLITE_FULLMETADATA;
				
			case JSONLITENOMETADATA:
				return MediaType.APPLICATION_JSONLITE_NOMETADATA;
				
			default:
				return MediaType.APPLICATION_ATOM;
		}
	}
}
