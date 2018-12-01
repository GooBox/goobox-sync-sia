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
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * InlineResponse2008
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-12-01T00:02:50.340-05:00")
public class InlineResponse2008 {
  @SerializedName("currentperiod")
  private Long currentperiod = null;

  @SerializedName("financialmetrics")
  private InlineResponse2008Financialmetrics financialmetrics = null;

  @SerializedName("settings")
  private InlineResponse2008Settings settings = null;

  public InlineResponse2008 currentperiod(Long currentperiod) {
    this.currentperiod = currentperiod;
    return this;
  }

   /**
   * Height at which the current allowance period began.
   * @return currentperiod
  **/
  @ApiModelProperty(example = "200", value = "Height at which the current allowance period began.")
  public Long getCurrentperiod() {
    return currentperiod;
  }

  public void setCurrentperiod(Long currentperiod) {
    this.currentperiod = currentperiod;
  }

  public InlineResponse2008 financialmetrics(InlineResponse2008Financialmetrics financialmetrics) {
    this.financialmetrics = financialmetrics;
    return this;
  }

   /**
   * Get financialmetrics
   * @return financialmetrics
  **/
  @ApiModelProperty(value = "")
  public InlineResponse2008Financialmetrics getFinancialmetrics() {
    return financialmetrics;
  }

  public void setFinancialmetrics(InlineResponse2008Financialmetrics financialmetrics) {
    this.financialmetrics = financialmetrics;
  }

  public InlineResponse2008 settings(InlineResponse2008Settings settings) {
    this.settings = settings;
    return this;
  }

   /**
   * Get settings
   * @return settings
  **/
  @ApiModelProperty(value = "")
  public InlineResponse2008Settings getSettings() {
    return settings;
  }

  public void setSettings(InlineResponse2008Settings settings) {
    this.settings = settings;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2008 inlineResponse2008 = (InlineResponse2008) o;
    return Objects.equals(this.currentperiod, inlineResponse2008.currentperiod) &&
        Objects.equals(this.financialmetrics, inlineResponse2008.financialmetrics) &&
        Objects.equals(this.settings, inlineResponse2008.settings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentperiod, financialmetrics, settings);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2008 {\n");
    
    sb.append("    currentperiod: ").append(toIndentedString(currentperiod)).append("\n");
    sb.append("    financialmetrics: ").append(toIndentedString(financialmetrics)).append("\n");
    sb.append("    settings: ").append(toIndentedString(settings)).append("\n");
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

