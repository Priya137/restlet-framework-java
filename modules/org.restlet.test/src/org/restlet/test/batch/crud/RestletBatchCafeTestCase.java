package org.restlet.test.batch.crud;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;

import junit.framework.Assert;

import org.restlet.Component;
import org.restlet.Response;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.ext.odata.Query;
import org.restlet.ext.odata.batch.request.BatchRequest;
import org.restlet.ext.odata.batch.request.impl.ChangeSetRequestImpl;
import org.restlet.ext.odata.batch.request.impl.CreateEntityRequest;
import org.restlet.ext.odata.batch.request.impl.DeleteEntityRequest;
import org.restlet.ext.odata.batch.request.impl.GetEntityRequest;
import org.restlet.ext.odata.batch.request.impl.UpdateEntityRequest;
import org.restlet.ext.odata.batch.response.BatchResponse;
import org.restlet.ext.odata.batch.response.ChangeSetResponse;
import org.restlet.test.RestletTestCase;
import org.restlet.test.ext.odata.crud.Cafe;




/**
 * Test case for RestletBatch service for CUD operation on entities.
 * 
 */
public class RestletBatchCafeTestCase extends RestletTestCase {

	private static final String cafeName = "TestName";

	private static final String cafeNameUpdated = "TestName-updated";

	private static final String cafeId = "40";

	private static final int cafeZipCode = 111111;

	private static final Logger LOGGER = Logger.getLogger(RestletBatchCafeTestCase.class.getName());

	/** The Constant failureStatus. */
	private static final int failureStatus = 500;

	/** The Constant successStatus. */
	private static final int successStatus = 200;

	/** The Constant createStatus. */
	private static final int createStatus = 201;

	private static final String cafeCity = "TestCity";

	/** Inner component. */
	private Component component = new Component();


	/** OData service used for all tests. */
	@SuppressWarnings("unused")
	private CafeService service;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		component.getServers().add(Protocol.HTTP, 8111);
		component.getClients().add(Protocol.CLAP);
		component.getDefaultHost().attach("/Cafe.svc",
				new CafeCrudApplication());
		component.start();

		service = new CafeService();
	}

	@Override
	protected void tearDown() throws Exception {
		component.stop();
		component = null;
		super.tearDown();
	}

	/**
	 * Test method for crud operation on simple entities.
	 */
	public void testCrudSimpleEntity() {
		CafeService service = new CafeService();

		// create.
		Cafe cafe = new Cafe();
		cafe.setId(cafeId);
		cafe.setName(cafeName);
		cafe.setCity(cafeCity);
		cafe.setZipCode(cafeZipCode);
		try {

			BatchRequest br = service.createBatchRequest();
			ChangeSetRequestImpl changeSetRequest = new ChangeSetRequestImpl();
			//Create request
			CreateEntityRequest createEntityRequest = new CreateEntityRequest(service,cafe);
			changeSetRequest.addRequest(createEntityRequest);
			List<BatchResponse> responses = br.addRequest(changeSetRequest).execute();
			dumpResponse(responses);
			//Assert for response.
			Query<Cafe> createquery = service.createCafeQuery("/Cafes");
			Cafe cafe1=createquery.iterator().next();			
			assertEquals(cafeName, cafe1.getName());
			assertEquals(cafeId, cafe1.getId());
			assertEquals(cafeZipCode, cafe1.getZipCode());
			Response latestResponse = createquery.getService().getLatestResponse();
			latestResponse = createquery.getService().getLatestResponse();
			assertEquals(Status.SUCCESS_OK,latestResponse.getStatus());
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail();
		}

		//Get

		Cafe cafeG = new Cafe();
		cafeG.setId(cafeId);
		cafeG.setName(cafeName);
		cafeG.setCity(cafeCity);
		cafeG.setZipCode(cafeZipCode);
		try {
			BatchRequest br = service.createBatchRequest();
			//get request
			Query<Cafe> getQuery = service.createQuery("/Cafes('40')",Cafe.class);
			GetEntityRequest getEntityRequest = new GetEntityRequest(getQuery);
			List<BatchResponse> responses = br.addRequest(getEntityRequest).execute();
			dumpResponse(responses);
				Assert.assertTrue(true);
				//Assert for response.
				Query<Cafe> getquery = service.getCafeQuery("/Cafes");
				Cafe cafe2=getquery.iterator().next();			
				assertEquals(cafeName, cafe2.getName());
				assertEquals(cafeId, cafe2.getId());
				assertEquals(cafeZipCode, cafe2.getZipCode());
				Response latestResponse = getQuery.getService().getLatestResponse();
				latestResponse = getquery.getService().getLatestResponse();
				assertTrue(latestResponse.getStatus().isSuccess());
			} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail();
		}

		//update
		Cafe cafeU = new Cafe();
		cafeU.setId(cafeId);
		cafeU.setName(cafeName);
		cafeU.setCity(cafeCity);
		cafeU.setZipCode(cafeZipCode);
		cafeU.setName(cafeNameUpdated);
		try {
			BatchRequest br = service.createBatchRequest();
			ChangeSetRequestImpl changeSetRequest = new ChangeSetRequestImpl();
			//Create request
			UpdateEntityRequest updateEntityRequest = new UpdateEntityRequest(service,cafe);
			changeSetRequest.addRequest(updateEntityRequest);
			List<BatchResponse> responses = br.addRequest(changeSetRequest).execute();
			dumpResponse(responses);
				
				Query<Cafe> updatequery = service.updateCafeQuery("/Cafes('40')");
				Cafe cafe3=updatequery.iterator().next();			
				assertEquals(cafeNameUpdated, cafe3.getName());
				assertEquals(cafeId, cafe3.getId());
				assertEquals(cafeZipCode, cafe3.getZipCode());
				Response latestResponse = updatequery.getService().getLatestResponse();
				assertEquals(Status.SUCCESS_OK, latestResponse.getStatus());
				Assert.assertTrue(true);
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail();
		}
		//Assert for response.
		

		//delete

		Cafe cafeD = new Cafe();
		cafeD.setId(cafeId);
		cafeD.setName(cafeName);
		cafeD.setCity("TestCity");
		cafeD.setZipCode(cafeZipCode);
		try {
			BatchRequest br = service.createBatchRequest();
			ChangeSetRequestImpl changeSetRequest = new ChangeSetRequestImpl();
			//Create request
			DeleteEntityRequest deleteEntityRequest = new DeleteEntityRequest(service,cafe);
			changeSetRequest.addRequest(deleteEntityRequest);
			List<BatchResponse> responses = br.addRequest(changeSetRequest).execute();
			dumpResponse(responses);
			Query<Cafe> deleteQuery = service
					.deleteCafeQuery("/Cafes('40')");
			Response latestResponse = deleteQuery.getService().getLatestResponse();
			assertTrue(latestResponse.getStatus().isSuccess());
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * Dump response.
	 *
	 * @param responses the responses
	 */
	@SuppressWarnings("unchecked")
	public static void dumpResponse(List<BatchResponse> responses) {
		for (BatchResponse batchResponse : responses) {
			Object entity = batchResponse.getEntity();
			if(batchResponse instanceof ChangeSetResponse){
				LOGGER.info("Dumping changeset");
				dumpResponse((List<BatchResponse>)entity);
				LOGGER.info("Done with changeset");
			}else{
				LOGGER.info("Status ="+ batchResponse.getStatus());
				LOGGER.info("Entity = "+ entity);
				MultivaluedMap<String, String> headers = batchResponse.getHeaders();
				if(headers!=null){
					Set<String> keySet = headers.keySet();
					LOGGER.info("Headers : ");
					for (String key : keySet) {
						List<String> value = headers.get(key);
						LOGGER.info("Key ="+ key + "/t"+"value = "+ value);
					}
				}
			}
		}	
	}


}