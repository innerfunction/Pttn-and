package com.innerfunction.pttn.app;

import com.innerfunction.pttn.Configuration;
import com.innerfunction.pttn.Container;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.URIScheme;

import java.util.Map;

/**
 * An internal URI handler for the _new:_ scheme.
 * The new: scheme allows new components to be instantiated using a URI. The URI's 'name'
 * part specified the type or class name of the object to be instantiated. Dependency injection
 * is then performed using the URI's parameters as configuration values.
 *
 * Created by juliangoacher on 30/03/16.
 */
public class NewScheme implements URIScheme {

    private Container container;

    public NewScheme(Container container) {
        this.container = container;
    }

    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        String typeName = uri.getName();
        Configuration config = container.makeConfiguration( params ).normalize();
        Object result = container.newInstanceForTypeNameAndConfiguration( typeName, config );
        if( result == null ) {
            // If instantiation fails (i.e. because the type name isn't recognized) then try
            // instantiating from class name.
            result = container.newInstanceForClassNameAndConfiguration( typeName, config );
        }
        if( result != null ) {
            container.configureObject( result, config, uri.toString() );
        }
        return result;
    }

}
