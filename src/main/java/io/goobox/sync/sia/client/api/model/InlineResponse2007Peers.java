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


package io.goobox.sync.sia.client.api.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * InlineResponse2007Peers
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2017-11-21T13:36:31.527-05:00")
public class InlineResponse2007Peers {
  @SerializedName("netaddress")
  private String netaddress = null;

  @SerializedName("version")
  private String version = null;

  @SerializedName("inbound")
  private Boolean inbound = null;

  public InlineResponse2007Peers netaddress(String netaddress) {
    this.netaddress = netaddress;
    return this;
  }

   /**
   * netaddress is the address of the peer. It represents a &#x60;modules.NetAddress&#x60;.
   * @return netaddress
  **/
  @ApiModelProperty(value = "netaddress is the address of the peer. It represents a `modules.NetAddress`.")
  public String getNetaddress() {
    return netaddress;
  }

  public void setNetaddress(String netaddress) {
    this.netaddress = netaddress;
  }

  public InlineResponse2007Peers version(String version) {
    this.version = version;
    return this;
  }

   /**
   * version is the version number of the peer.
   * @return version
  **/
  @ApiModelProperty(value = "version is the version number of the peer.")
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public InlineResponse2007Peers inbound(Boolean inbound) {
    this.inbound = inbound;
    return this;
  }

   /**
   * inbound is true when the peer initiated the connection. This field is exposed as outbound peers are generally trusted more than inbound peers, as inbound peers are easily manipulated by an adversary. 
   * @return inbound
  **/
  @ApiModelProperty(value = "inbound is true when the peer initiated the connection. This field is exposed as outbound peers are generally trusted more than inbound peers, as inbound peers are easily manipulated by an adversary. ")
  public Boolean getInbound() {
    return inbound;
  }

  public void setInbound(Boolean inbound) {
    this.inbound = inbound;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2007Peers inlineResponse2007Peers = (InlineResponse2007Peers) o;
    return Objects.equals(this.netaddress, inlineResponse2007Peers.netaddress) &&
        Objects.equals(this.version, inlineResponse2007Peers.version) &&
        Objects.equals(this.inbound, inlineResponse2007Peers.inbound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(netaddress, version, inbound);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2007Peers {\n");
    
    sb.append("    netaddress: ").append(toIndentedString(netaddress)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    inbound: ").append(toIndentedString(inbound)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
  
}

