package com.servicecraze.acme;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servicecraze.collector.data.GsonDocument;
import com.servicecraze.collector.data.Http01AuthCore;
import com.servicecraze.collector.db.AppDB;
import com.servicecraze.collector.db.AppDBException;

@Stateless
@Path(".well-known/acme-challenge")
public class AcmeChallengeREST {
	private static final Logger log = LoggerFactory.getLogger(AcmeChallengeREST.class);
	
  @Inject
  private AppDB db;
  
	@GET
	@Path("{id}")
	public Response acmeAuthorization(@PathParam("id") String token) {
		GsonDocument filter = new GsonDocument(new Http01AuthCore(token));
		try {
			List<GsonDocument> results = db.get(filter);
			if (results.isEmpty()) {
				log.warn("No result for token: " + token);
				return Response.status(Status.NOT_FOUND).build();
			}
//			log.info("# of rows: " + results.size());
//			for (GsonDocument doc : results) {
//				Http01AuthCore core = doc.getCoreObject();
//				log.info("row: " + core.toString());
//			}
			Http01AuthCore core = results.get(0).getCoreObject();
			return Response.ok(core.getContent()).build();
		} catch (AppDBException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}
