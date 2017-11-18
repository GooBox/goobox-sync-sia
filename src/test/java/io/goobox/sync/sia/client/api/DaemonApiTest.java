/*
 * Sia
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.goobox.sync.sia.client.api;

import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.InlineResponse2004;
import io.goobox.sync.sia.client.api.model.InlineResponse2005;
import io.goobox.sync.sia.client.api.model.StandardError;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for DaemonApi
 */
@Ignore
public class DaemonApiTest {

    private final DaemonApi api = new DaemonApi();

    
    /**
     * 
     *
     * Returns the set of constants in use.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void daemonConstantsGetTest() throws ApiException {
        InlineResponse2004 response = api.daemonConstantsGet();

        // TODO: test validations
    }
    
    /**
     * 
     *
     * cleanly shuts down the daemon. May take a few seconds.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void daemonStopGetTest() throws ApiException {
        api.daemonStopGet();

        // TODO: test validations
    }
    
    /**
     * 
     *
     * returns the version of the Sia daemon currently running.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void daemonVersionGetTest() throws ApiException {
        InlineResponse2005 response = api.daemonVersionGet();

        // TODO: test validations
    }
    
}
