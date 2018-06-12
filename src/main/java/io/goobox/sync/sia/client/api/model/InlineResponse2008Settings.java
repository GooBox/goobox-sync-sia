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
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * Settings that control the behavior of the renter.
 */
@ApiModel(description = "Settings that control the behavior of the renter.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-12T09:40:06.745-04:00")
public class InlineResponse2008Settings {
  @SerializedName("allowance")
  private InlineResponse2008SettingsAllowance allowance = null;

  @SerializedName("maxuploadspeed")
  private Long maxuploadspeed = null;

  @SerializedName("maxdownloadspeed")
  private Long maxdownloadspeed = null;

  @SerializedName("streamcachesize")
  private Long streamcachesize = null;

  public InlineResponse2008Settings allowance(InlineResponse2008SettingsAllowance allowance) {
    this.allowance = allowance;
    return this;
  }

   /**
   * Get allowance
   * @return allowance
  **/
  @ApiModelProperty(value = "")
  public InlineResponse2008SettingsAllowance getAllowance() {
    return allowance;
  }

  public void setAllowance(InlineResponse2008SettingsAllowance allowance) {
    this.allowance = allowance;
  }

  public InlineResponse2008Settings maxuploadspeed(Long maxuploadspeed) {
    this.maxuploadspeed = maxuploadspeed;
    return this;
  }

   /**
   * bytes per second.  MaxUploadSpeed by defaul is unlimited but can be set by the user to  manage bandwidth
   * @return maxuploadspeed
  **/
  @ApiModelProperty(example = "1234", value = "bytes per second.  MaxUploadSpeed by defaul is unlimited but can be set by the user to  manage bandwidth")
  public Long getMaxuploadspeed() {
    return maxuploadspeed;
  }

  public void setMaxuploadspeed(Long maxuploadspeed) {
    this.maxuploadspeed = maxuploadspeed;
  }

  public InlineResponse2008Settings maxdownloadspeed(Long maxdownloadspeed) {
    this.maxdownloadspeed = maxdownloadspeed;
    return this;
  }

   /**
   * bytes per second.                     MaxDownloadSpeed by defaul is unlimited but can be set by the user to  manage bandwidth
   * @return maxdownloadspeed
  **/
  @ApiModelProperty(example = "1234", value = "bytes per second.                     MaxDownloadSpeed by defaul is unlimited but can be set by the user to  manage bandwidth")
  public Long getMaxdownloadspeed() {
    return maxdownloadspeed;
  }

  public void setMaxdownloadspeed(Long maxdownloadspeed) {
    this.maxdownloadspeed = maxdownloadspeed;
  }

  public InlineResponse2008Settings streamcachesize(Long streamcachesize) {
    this.streamcachesize = streamcachesize;
    return this;
  }

   /**
   * The StreamCacheSize is the number of data chunks that will be cached during streaming
   * @return streamcachesize
  **/
  @ApiModelProperty(example = "4", value = "The StreamCacheSize is the number of data chunks that will be cached during streaming")
  public Long getStreamcachesize() {
    return streamcachesize;
  }

  public void setStreamcachesize(Long streamcachesize) {
    this.streamcachesize = streamcachesize;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2008Settings inlineResponse2008Settings = (InlineResponse2008Settings) o;
    return Objects.equals(this.allowance, inlineResponse2008Settings.allowance) &&
        Objects.equals(this.maxuploadspeed, inlineResponse2008Settings.maxuploadspeed) &&
        Objects.equals(this.maxdownloadspeed, inlineResponse2008Settings.maxdownloadspeed) &&
        Objects.equals(this.streamcachesize, inlineResponse2008Settings.streamcachesize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowance, maxuploadspeed, maxdownloadspeed, streamcachesize);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2008Settings {\n");
    
    sb.append("    allowance: ").append(toIndentedString(allowance)).append("\n");
    sb.append("    maxuploadspeed: ").append(toIndentedString(maxuploadspeed)).append("\n");
    sb.append("    maxdownloadspeed: ").append(toIndentedString(maxdownloadspeed)).append("\n");
    sb.append("    streamcachesize: ").append(toIndentedString(streamcachesize)).append("\n");
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

