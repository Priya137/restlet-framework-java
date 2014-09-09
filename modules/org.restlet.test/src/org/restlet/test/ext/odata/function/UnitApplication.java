package org.restlet.test.ext.odata.function;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.CharacterSet;
import org.restlet.data.LocalReference;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

/**
 * Sample application that simulates the "Unit" service.
 */
public class UnitApplication extends Application {

	private static class MyClapRestlet extends Restlet {
		String file;

		public MyClapRestlet(Context context, String file) {
			super(context);
			this.file = file;
		}

		@Override
		public void handle(Request request, Response response) {
			if (Method.GET.equals(request.getMethod())) {
				String uri = "/"
						+ this.getClass().getPackage().getName()
								.replace(".", "/") + "/" + file;

				Response r = getContext().getClientDispatcher().handle(
						new Request(Method.GET, LocalReference
								.createClapReference(LocalReference.CLAP_CLASS,
										uri + ".xml")));
				response.setEntity(r.getEntity());
				response.setStatus(r.getStatus());

			} else if (Method.POST.equals(request.getMethod())) {
				System.out.println();
				String uri = "/"
						+ this.getClass().getPackage().getName()
								.replace(".", "/") + "/" + file;
				Response r = getContext().getClientDispatcher().handle(
						new Request(Method.GET, LocalReference
								.createClapReference(LocalReference.CLAP_CLASS,
										uri + ".xml")));
				response.setEntity(r.getEntity());
				response.setStatus(r.getStatus());
			}

		}
	}

	@Override
	public Restlet createInboundRoot() {
		getMetadataService().setDefaultCharacterSet(CharacterSet.ISO_8859_1);
		getConnectorService().getClientProtocols().add(Protocol.CLAP);
		Router router = new Router(getContext());

		router.attach("/$metadata", new MyClapRestlet(getContext(), "metadata"));
		router.attach("/nextval", new MyClapRestlet(getContext(), "nextval"));
		router.attach("/convertDoubleArray", new MyClapRestlet(getContext(),
				"convertDoubleArray"));

		return router;
	}

}
