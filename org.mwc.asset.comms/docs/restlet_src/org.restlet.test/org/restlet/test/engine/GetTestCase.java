/**
 * Copyright 2005-2010 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.test.engine;

import org.restlet.Application;
import org.restlet.Client;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;

/**
 * Test that a simple get works for all the connectors.
 * 
 * @author Kevin Conaway
 */
public class GetTestCase extends BaseConnectorsTestCase {

    public static class GetTestResource extends ServerResource {
        @Get
        public String toString() {
            return "Hello world";
        }
    }

    @Override
    protected void call(String uri) throws Exception {
        final Request request = new Request(Method.GET, uri);
        Client c = new Client(Protocol.HTTP);
        final Response r = c.handle(request);

        assertEquals(r.getStatus().getDescription(), Status.SUCCESS_OK, r
                .getStatus());
        assertEquals("Hello world", r.getEntity().getText());
        c.stop();
    }

    @Override
    protected Application createApplication(Component component) {
        final Application application = new Application() {
            @Override
            public Restlet createInboundRoot() {
                final Router router = new Router(getContext());
                router.attach("/test", GetTestResource.class);
                return router;
            }
        };

        return application;
    }
}
