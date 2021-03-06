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
import io.goobox.sync.sia.client.api.model.InlineResponse2003;
import org.junit.Ignore;
import org.junit.Test;

/**
 * API tests for GatewayApi
 */
@Ignore
public class GatewayApiTest {

    private final GatewayApi api = new GatewayApi();


    /**
     * connects the gateway to a peer. The peer is added to the node list if it is not already present. The node list is the list of all nodes the gateway knows about, but is not necessarily connected to.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void gatewayConnectNetaddressPostTest() throws ApiException {
        String netaddress = null;
        api.gatewayConnectNetaddressPost(netaddress);

        // TODO: test validations
    }

    /**
     * disconnects the gateway from a peer. The peer remains in the node list. Disconnecting from a peer does not prevent the gateway from automatically connecting to the peer in the future.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void gatewayDisconnectNetaddressPostTest() throws ApiException {
        String netaddress = null;
        api.gatewayDisconnectNetaddressPost(netaddress);

        // TODO: test validations
    }

    /**
     * returns information about the gateway, including the list of connected peers.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void gatewayGetTest() throws ApiException {
        InlineResponse2003 response = api.gatewayGet();

        // TODO: test validations
    }

}
