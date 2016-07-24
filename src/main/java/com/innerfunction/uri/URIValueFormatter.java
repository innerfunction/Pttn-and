package com.innerfunction.uri;

/**
 * Attached by juliangoacher on 19/04/16.
 */
public interface URIValueFormatter {

    /** Format a value deferenced from a URI. */
    Object formatValue(Object value, CompoundURI uri);

}
