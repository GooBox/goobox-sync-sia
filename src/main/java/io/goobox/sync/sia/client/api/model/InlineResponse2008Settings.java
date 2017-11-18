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
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * Settings that control the behavior of the renter.
 */
@ApiModel(description = "Settings that control the behavior of the renter.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2017-11-17T14:17:27.462-05:00")
public class InlineResponse2008Settings {
  @SerializedName("allowance")
  private InlineResponse2008SettingsAllowance allowance = null;

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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2008Settings inlineResponse2008Settings = (InlineResponse2008Settings) o;
    return Objects.equals(this.allowance, inlineResponse2008Settings.allowance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowance);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2008Settings {\n");
    
    sb.append("    allowance: ").append(toIndentedString(allowance)).append("\n");
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

