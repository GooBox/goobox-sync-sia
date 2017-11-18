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
import io.goobox.sync.sia.client.api.model.HostdbHosts;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hostdb
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2017-11-17T14:17:27.462-05:00")
public class Hostdb {
  @SerializedName("hosts")
  private List<HostdbHosts> hosts = null;

  public Hostdb hosts(List<HostdbHosts> hosts) {
    this.hosts = hosts;
    return this;
  }

  public Hostdb addHostsItem(HostdbHosts hostsItem) {
    if (this.hosts == null) {
      this.hosts = new ArrayList<HostdbHosts>();
    }
    this.hosts.add(hostsItem);
    return this;
  }

   /**
   * Get hosts
   * @return hosts
  **/
  @ApiModelProperty(value = "")
  public List<HostdbHosts> getHosts() {
    return hosts;
  }

  public void setHosts(List<HostdbHosts> hosts) {
    this.hosts = hosts;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Hostdb hostdb = (Hostdb) o;
    return Objects.equals(this.hosts, hostdb.hosts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hosts);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Hostdb {\n");
    
    sb.append("    hosts: ").append(toIndentedString(hosts)).append("\n");
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

