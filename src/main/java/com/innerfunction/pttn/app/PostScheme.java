package com.innerfunction.pttn.app;

import com.innerfunction.pttn.Message;
import com.innerfunction.uri.CompoundURI;
import com.innerfunction.uri.URIScheme;

import java.util.Map;

/**
 * An internal URI scheme handler for the post: scheme.
 * The post: scheme allows messages to be posted using a URI string description. For example,
 * in the URI post:open+view@make:WebView, the message to be posted is named open and has a
 * single parameter named view.
 *
 * Attached by juliangoacher on 30/03/16.
 */
public class PostScheme implements URIScheme {

    public Object dereference(CompoundURI uri, Map<String,Object> params) {
        Message message = new Message( uri.getFragment(), uri.getName(), params );
        return message;
    }

}
