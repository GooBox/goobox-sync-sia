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
 * InlineResponse2001Folders
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-12T09:40:06.745-04:00")
public class InlineResponse2001Folders {
  @SerializedName("path")
  private String path = null;

  @SerializedName("capacity")
  private Integer capacity = null;

  @SerializedName("capacityremaining")
  private Integer capacityremaining = null;

  @SerializedName("failedreads")
  private Integer failedreads = null;

  @SerializedName("failedwrites")
  private Integer failedwrites = null;

  @SerializedName("successfulreads")
  private Integer successfulreads = null;

  @SerializedName("successfulwrites")
  private Integer successfulwrites = null;

  public InlineResponse2001Folders path(String path) {
    this.path = path;
    return this;
  }

   /**
   * Absolute path to the storage folder on the local filesystem.
   * @return path
  **/
  @ApiModelProperty(example = "/home/foo/bar", value = "Absolute path to the storage folder on the local filesystem.")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public InlineResponse2001Folders capacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

   /**
   * Maximum capacity of the storage folder. The host will not store more than this many bytes in the folder. This capacity is not checked against the drive&#39;s remaining capacity. Therefore, you must manually ensure the disk has sufficient capacity for the folder at all times. Otherwise you risk losing renter&#39;s data and failing storage proofs. 
   * @return capacity
  **/
  @ApiModelProperty(example = "50000000000", value = "Maximum capacity of the storage folder. The host will not store more than this many bytes in the folder. This capacity is not checked against the drive's remaining capacity. Therefore, you must manually ensure the disk has sufficient capacity for the folder at all times. Otherwise you risk losing renter's data and failing storage proofs. ")
  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public InlineResponse2001Folders capacityremaining(Integer capacityremaining) {
    this.capacityremaining = capacityremaining;
    return this;
  }

   /**
   * Unused capacity of the storage folder.
   * @return capacityremaining
  **/
  @ApiModelProperty(example = "100000", value = "Unused capacity of the storage folder.")
  public Integer getCapacityremaining() {
    return capacityremaining;
  }

  public void setCapacityremaining(Integer capacityremaining) {
    this.capacityremaining = capacityremaining;
  }

  public InlineResponse2001Folders failedreads(Integer failedreads) {
    this.failedreads = failedreads;
    return this;
  }

   /**
   * Number of failed disk read &amp; write operations. A large number of failed reads or writes indicates a problem with the filesystem or drive&#39;s hardware. 
   * @return failedreads
  **/
  @ApiModelProperty(example = "1", value = "Number of failed disk read & write operations. A large number of failed reads or writes indicates a problem with the filesystem or drive's hardware. ")
  public Integer getFailedreads() {
    return failedreads;
  }

  public void setFailedreads(Integer failedreads) {
    this.failedreads = failedreads;
  }

  public InlineResponse2001Folders failedwrites(Integer failedwrites) {
    this.failedwrites = failedwrites;
    return this;
  }

   /**
   * Get failedwrites
   * @return failedwrites
  **/
  @ApiModelProperty(example = "0", value = "")
  public Integer getFailedwrites() {
    return failedwrites;
  }

  public void setFailedwrites(Integer failedwrites) {
    this.failedwrites = failedwrites;
  }

  public InlineResponse2001Folders successfulreads(Integer successfulreads) {
    this.successfulreads = successfulreads;
    return this;
  }

   /**
   * Number of successful read &amp; write operations.
   * @return successfulreads
  **/
  @ApiModelProperty(example = "2", value = "Number of successful read & write operations.")
  public Integer getSuccessfulreads() {
    return successfulreads;
  }

  public void setSuccessfulreads(Integer successfulreads) {
    this.successfulreads = successfulreads;
  }

  public InlineResponse2001Folders successfulwrites(Integer successfulwrites) {
    this.successfulwrites = successfulwrites;
    return this;
  }

   /**
   * Get successfulwrites
   * @return successfulwrites
  **/
  @ApiModelProperty(example = "3", value = "")
  public Integer getSuccessfulwrites() {
    return successfulwrites;
  }

  public void setSuccessfulwrites(Integer successfulwrites) {
    this.successfulwrites = successfulwrites;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2001Folders inlineResponse2001Folders = (InlineResponse2001Folders) o;
    return Objects.equals(this.path, inlineResponse2001Folders.path) &&
        Objects.equals(this.capacity, inlineResponse2001Folders.capacity) &&
        Objects.equals(this.capacityremaining, inlineResponse2001Folders.capacityremaining) &&
        Objects.equals(this.failedreads, inlineResponse2001Folders.failedreads) &&
        Objects.equals(this.failedwrites, inlineResponse2001Folders.failedwrites) &&
        Objects.equals(this.successfulreads, inlineResponse2001Folders.successfulreads) &&
        Objects.equals(this.successfulwrites, inlineResponse2001Folders.successfulwrites);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, capacity, capacityremaining, failedreads, failedwrites, successfulreads, successfulwrites);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2001Folders {\n");
    
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
    sb.append("    capacityremaining: ").append(toIndentedString(capacityremaining)).append("\n");
    sb.append("    failedreads: ").append(toIndentedString(failedreads)).append("\n");
    sb.append("    failedwrites: ").append(toIndentedString(failedwrites)).append("\n");
    sb.append("    successfulreads: ").append(toIndentedString(successfulreads)).append("\n");
    sb.append("    successfulwrites: ").append(toIndentedString(successfulwrites)).append("\n");
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

