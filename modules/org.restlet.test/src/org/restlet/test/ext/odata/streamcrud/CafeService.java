package org.restlet.test.ext.odata.streamcrud;

import org.restlet.ext.odata.Query;
import org.restlet.ext.odata.Service;

/**
* Generated by the generator tool for the WCF Data Services extension for the Restlet framework.<br>
*
* @see <a href="http://localhost:8111/Cafe.svc/$metadata">Metadata of the target WCF Data Services</a>
*
*/
public class CafeService extends Service {

    /**
     * Constructor.
     * 
     */
    public CafeService() {
        super("http://localhost:8111/Cafe.svc");
    }

    /**
     * Adds a new entity to the service.
     * 
     * @param entity
     *            The entity to add to the service.
     * @throws Exception 
     */
    public void addEntity(Cafe entity) throws Exception {
        addEntity("/Cafes", entity);
    }

    /**
     * Creates a query for cafe entities hosted by this service.
     * 
     * @param subpath
     *            The path to this entity relatively to the service URI.
     * @return A query object.
     */
    public Query<Cafe> createCafeQuery(String subpath) {
        return createQuery(subpath, Cafe.class);
    }

}
