/*
 * Sia
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 1.3.7
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
import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * InlineResponse20010
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-12-01T00:02:50.340-05:00")
public class InlineResponse20010 {
  @SerializedName("downloads")
  private List<InlineResponse20010Downloads> downloads = null;

  public InlineResponse20010 downloads(List<InlineResponse20010Downloads> downloads) {
    this.downloads = downloads;
    return this;
  }

  public InlineResponse20010 addDownloadsItem(InlineResponse20010Downloads downloadsItem) {
    if (this.downloads == null) {
      this.downloads = new ArrayList<InlineResponse20010Downloads>();
    }
    this.downloads.add(downloadsItem);
    return this;
  }

   /**
   * Get downloads
   * @return downloads
  **/
  @ApiModelProperty(value = "")
  public List<InlineResponse20010Downloads> getDownloads() {
    return downloads;
  }

  public void setDownloads(List<InlineResponse20010Downloads> downloads) {
    this.downloads = downloads;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse20010 inlineResponse20010 = (InlineResponse20010) o;
    return Objects.equals(this.downloads, inlineResponse20010.downloads);
  }

  @Override
  public int hashCode() {
    return Objects.hash(downloads);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse20010 {\n");
    
    sb.append("    downloads: ").append(toIndentedString(downloads)).append("\n");
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

