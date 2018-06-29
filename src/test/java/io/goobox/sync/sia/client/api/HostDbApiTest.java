/*
 * Sia
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 1.3.3
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.goobox.sync.sia.client.api;

import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.Hostdb;
import io.goobox.sync.sia.client.api.model.InlineResponse2002;
import io.goobox.sync.sia.client.api.model.StandardError;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for HostDbApi
 */
@Ignore
public class HostDbApiTest {

    private final HostDbApi api = new HostDbApi();

    
    /**
     * 
     *
     * lists all of the active hosts known to the renter, sorted by preference. 
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void hostdbActiveGetTest() throws ApiException {
        Integer numhosts = null;
        Hostdb response = api.hostdbActiveGet(numhosts);

        // TODO: test validations
    }
    
    /**
     * 
     *
     * lists all of the hosts known to the renter. Hosts are not guaranteed to be in any particular order, and the order may change in subsequent calls. 
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void hostdbAllGetTest() throws ApiException {
        Hostdb response = api.hostdbAllGet();

        // TODO: test validations
    }
    
    /**
     * 
     *
     * fetches detailed information about a particular host, including metrics regarding the score of the host within the database. It should be noted that each renter uses different metrics for selecting hosts, and that a good score on in one hostdb does not mean that the host will be successful on the network overall. 
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void hostdbHostsPubkeyGetTest() throws ApiException {
        String pubkey = null;
        InlineResponse2002 response = api.hostdbHostsPubkeyGet(pubkey);

        // TODO: test validations
    }
    
}
