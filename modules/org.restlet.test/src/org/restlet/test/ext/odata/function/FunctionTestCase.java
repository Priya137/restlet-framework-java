package org.restlet.test.ext.odata.function;

import org.junit.Assert;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.test.RestletTestCase;

/**
 * Test case for function for Restlet.
 */
public class FunctionTestCase extends RestletTestCase {

    /** Inner component. */
    private Component component = new Component();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        component.getServers().add(Protocol.HTTP, 8111);
        component.getClients().add(Protocol.CLAP);
        component.getDefaultHost().attach("/Unit.svc",
                new UnitApplication());
        component.start();
    }

    @Override
    protected void tearDown() throws Exception {
        component.stop();
        component = null;
        super.tearDown();
    }

    /**
     * Tests the function to return correct value.
     */
    public void testFunction() {
    	FunctionService service = new FunctionService();
		Nextval_t nextval = null;
		try {
			nextval = service.nextval("RCompany");
		} catch (Exception e) {
			Context.getCurrentLogger().warning(
                    "Exception occurred while calling a function: " + e.getMessage());
			Assert.fail();
		}
		assertEquals("534", nextval.getSysGenId().toString());
	
    }

}
