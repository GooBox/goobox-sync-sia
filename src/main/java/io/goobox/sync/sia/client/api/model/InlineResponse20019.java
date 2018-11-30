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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * InlineResponse20019
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-12-01T00:02:50.340-05:00")
public class InlineResponse20019 {
  @SerializedName("transactionids")
  private List<String> transactionids = null;

  public InlineResponse20019 transactionids(List<String> transactionids) {
    this.transactionids = transactionids;
    return this;
  }

  public InlineResponse20019 addTransactionidsItem(String transactionidsItem) {
    if (this.transactionids == null) {
      this.transactionids = new ArrayList<String>();
    }
    this.transactionids.add(transactionidsItem);
    return this;
  }

   /**
   * Array of IDs of the transactions that were created when sending the coins. The last transaction contains the output headed to the &#39;destination&#39;. Transaction IDs are 64 character long hex strings.
   * @return transactionids
  **/
  @ApiModelProperty(example = "[\"1234567890abcdef0123456789abcdef0123456789abcdef0123456789abcdef\",\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"]", value = "Array of IDs of the transactions that were created when sending the coins. The last transaction contains the output headed to the 'destination'. Transaction IDs are 64 character long hex strings.")
  public List<String> getTransactionids() {
    return transactionids;
  }

  public void setTransactionids(List<String> transactionids) {
    this.transactionids = transactionids;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse20019 inlineResponse20019 = (InlineResponse20019) o;
    return Objects.equals(this.transactionids, inlineResponse20019.transactionids);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactionids);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse20019 {\n");
    
    sb.append("    transactionids: ").append(toIndentedString(transactionids)).append("\n");
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

