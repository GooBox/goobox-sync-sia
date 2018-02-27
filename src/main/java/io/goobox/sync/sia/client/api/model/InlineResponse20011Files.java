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
import java.math.BigDecimal;

/**
 * InlineResponse20011Files
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-02-24T04:09:14.967-05:00")
public class InlineResponse20011Files {
  @SerializedName("siapath")
  private String siapath = null;

  @SerializedName("localpath")
  private String localpath = null;

  @SerializedName("filesize")
  private Long filesize = null;

  @SerializedName("available")
  private Boolean available = null;

  @SerializedName("renewing")
  private Boolean renewing = null;

  @SerializedName("redundancy")
  private BigDecimal redundancy = null;

  @SerializedName("uploadprogress")
  private BigDecimal uploadprogress = null;

  @SerializedName("expiration")
  private Long expiration = null;

  public InlineResponse20011Files siapath(String siapath) {
    this.siapath = siapath;
    return this;
  }

   /**
   * Path to the file in the renter on the network.
   * @return siapath
  **/
  @ApiModelProperty(example = "foo/bar.txt", value = "Path to the file in the renter on the network.")
  public String getSiapath() {
    return siapath;
  }

  public void setSiapath(String siapath) {
    this.siapath = siapath;
  }

  public InlineResponse20011Files localpath(String localpath) {
    this.localpath = localpath;
    return this;
  }

   /**
   * Path to the local file on disk.
   * @return localpath
  **/
  @ApiModelProperty(example = "/home/foo/bar.txt", value = "Path to the local file on disk.")
  public String getLocalpath() {
    return localpath;
  }

  public void setLocalpath(String localpath) {
    this.localpath = localpath;
  }

  public InlineResponse20011Files filesize(Long filesize) {
    this.filesize = filesize;
    return this;
  }

   /**
   * Size of the file in bytes.
   * @return filesize
  **/
  @ApiModelProperty(example = "8192", value = "Size of the file in bytes.")
  public Long getFilesize() {
    return filesize;
  }

  public void setFilesize(Long filesize) {
    this.filesize = filesize;
  }

  public InlineResponse20011Files available(Boolean available) {
    this.available = available;
    return this;
  }

   /**
   * true if the file is available for download. Files may be available before they are completely uploaded.
   * @return available
  **/
  @ApiModelProperty(example = "true", value = "true if the file is available for download. Files may be available before they are completely uploaded.")
  public Boolean isAvailable() {
    return available;
  }

  public void setAvailable(Boolean available) {
    this.available = available;
  }

  public InlineResponse20011Files renewing(Boolean renewing) {
    this.renewing = renewing;
    return this;
  }

   /**
   * true if the file&#39;s contracts will be automatically renewed by the renter.
   * @return renewing
  **/
  @ApiModelProperty(example = "true", value = "true if the file's contracts will be automatically renewed by the renter.")
  public Boolean isRenewing() {
    return renewing;
  }

  public void setRenewing(Boolean renewing) {
    this.renewing = renewing;
  }

  public InlineResponse20011Files redundancy(BigDecimal redundancy) {
    this.redundancy = redundancy;
    return this;
  }

   /**
   * Average redundancy of the file on the network. Redundancy is calculated by dividing the amount of data uploaded in the file&#39;s open contracts by the size of the file. Redundancy does not necessarily correspond to availability. Specifically, a redundancy &gt;&#x3D; 1 does not indicate the file is available as there could be a chunk of the file with 0 redundancy.
   * @return redundancy
  **/
  @ApiModelProperty(example = "5.0", value = "Average redundancy of the file on the network. Redundancy is calculated by dividing the amount of data uploaded in the file's open contracts by the size of the file. Redundancy does not necessarily correspond to availability. Specifically, a redundancy >= 1 does not indicate the file is available as there could be a chunk of the file with 0 redundancy.")
  public BigDecimal getRedundancy() {
    return redundancy;
  }

  public void setRedundancy(BigDecimal redundancy) {
    this.redundancy = redundancy;
  }

  public InlineResponse20011Files uploadprogress(BigDecimal uploadprogress) {
    this.uploadprogress = uploadprogress;
    return this;
  }

   /**
   * Percentage of the file uploaded, including redundancy. Uploading has completed when uploadprogress is 100. Files may be available for download before upload progress is 100.                      
   * @return uploadprogress
  **/
  @ApiModelProperty(example = "100.0", value = "Percentage of the file uploaded, including redundancy. Uploading has completed when uploadprogress is 100. Files may be available for download before upload progress is 100.                      ")
  public BigDecimal getUploadprogress() {
    return uploadprogress;
  }

  public void setUploadprogress(BigDecimal uploadprogress) {
    this.uploadprogress = uploadprogress;
  }

  public InlineResponse20011Files expiration(Long expiration) {
    this.expiration = expiration;
    return this;
  }

   /**
   * Block height at which the file ceases availability.
   * @return expiration
  **/
  @ApiModelProperty(example = "60000", value = "Block height at which the file ceases availability.")
  public Long getExpiration() {
    return expiration;
  }

  public void setExpiration(Long expiration) {
    this.expiration = expiration;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse20011Files inlineResponse20011Files = (InlineResponse20011Files) o;
    return Objects.equals(this.siapath, inlineResponse20011Files.siapath) &&
        Objects.equals(this.localpath, inlineResponse20011Files.localpath) &&
        Objects.equals(this.filesize, inlineResponse20011Files.filesize) &&
        Objects.equals(this.available, inlineResponse20011Files.available) &&
        Objects.equals(this.renewing, inlineResponse20011Files.renewing) &&
        Objects.equals(this.redundancy, inlineResponse20011Files.redundancy) &&
        Objects.equals(this.uploadprogress, inlineResponse20011Files.uploadprogress) &&
        Objects.equals(this.expiration, inlineResponse20011Files.expiration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(siapath, localpath, filesize, available, renewing, redundancy, uploadprogress, expiration);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse20011Files {\n");
    
    sb.append("    siapath: ").append(toIndentedString(siapath)).append("\n");
    sb.append("    localpath: ").append(toIndentedString(localpath)).append("\n");
    sb.append("    filesize: ").append(toIndentedString(filesize)).append("\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
    sb.append("    renewing: ").append(toIndentedString(renewing)).append("\n");
    sb.append("    redundancy: ").append(toIndentedString(redundancy)).append("\n");
    sb.append("    uploadprogress: ").append(toIndentedString(uploadprogress)).append("\n");
    sb.append("    expiration: ").append(toIndentedString(expiration)).append("\n");
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

