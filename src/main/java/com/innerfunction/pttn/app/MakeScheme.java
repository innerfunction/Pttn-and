package com.innerfunction.pttn.app;

import com.innerfunction.pttn.Configuration;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.URIScheme;

import java.util.Map;

/**
 * An internal URI handler for the make: scheme.
 * The make: scheme allows new components to be instantiated from a pre-defined configuration.
 * The set of pre-defined configurations must be declared in a top-level property of the app
 * container named 'makes'. The 'name' part of the make: URI then refers to a key within
 * the makes map. Make configurations can be parameterized, with parameter values provided
 * via the make: URI's parameters.
 *
 * Attached by juliangoacher on 30/03/16.
 */
public class MakeScheme implements URIScheme {

    private AppContainer container;

    public MakeScheme(AppContainer container) {
        this.container = container;
    }

    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        Object result = null;
        Configuration makes = container.getMakes();
        Configuration config = makes.getValueAsConfiguration( uri.getName() );
        if( config != null ) {
            config = config.extendWithParameters( params );
            result = container.buildObject( config, uri.toString(), false );
        }
        return result;
    }

}
