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
import io.goobox.sync.sia.client.api.model.Transaction;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * InlineResponse20022
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-12-01T00:02:50.340-05:00")
public class InlineResponse20022 {
  @SerializedName("confirmedtransactions")
  private List<Transaction> confirmedtransactions = null;

  @SerializedName("unconfirmedtransactions")
  private List<Transaction> unconfirmedtransactions = null;

  public InlineResponse20022 confirmedtransactions(List<Transaction> confirmedtransactions) {
    this.confirmedtransactions = confirmedtransactions;
    return this;
  }

  public InlineResponse20022 addConfirmedtransactionsItem(Transaction confirmedtransactionsItem) {
    if (this.confirmedtransactions == null) {
      this.confirmedtransactions = new ArrayList<Transaction>();
    }
    this.confirmedtransactions.add(confirmedtransactionsItem);
    return this;
  }

   /**
   * All of the confirmed transactions appearing between height &#39;startheight&#39; and height &#39;endheight&#39; (inclusive).
   * @return confirmedtransactions
  **/
  @ApiModelProperty(value = "All of the confirmed transactions appearing between height 'startheight' and height 'endheight' (inclusive).")
  public List<Transaction> getConfirmedtransactions() {
    return confirmedtransactions;
  }

  public void setConfirmedtransactions(List<Transaction> confirmedtransactions) {
    this.confirmedtransactions = confirmedtransactions;
  }

  public InlineResponse20022 unconfirmedtransactions(List<Transaction> unconfirmedtransactions) {
    this.unconfirmedtransactions = unconfirmedtransactions;
    return this;
  }

  public InlineResponse20022 addUnconfirmedtransactionsItem(Transaction unconfirmedtransactionsItem) {
    if (this.unconfirmedtransactions == null) {
      this.unconfirmedtransactions = new ArrayList<Transaction>();
    }
    this.unconfirmedtransactions.add(unconfirmedtransactionsItem);
    return this;
  }

   /**
   * All of the unconfirmed transactions.
   * @return unconfirmedtransactions
  **/
  @ApiModelProperty(value = "All of the unconfirmed transactions.")
  public List<Transaction> getUnconfirmedtransactions() {
    return unconfirmedtransactions;
  }

  public void setUnconfirmedtransactions(List<Transaction> unconfirmedtransactions) {
    this.unconfirmedtransactions = unconfirmedtransactions;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse20022 inlineResponse20022 = (InlineResponse20022) o;
    return Objects.equals(this.confirmedtransactions, inlineResponse20022.confirmedtransactions) &&
        Objects.equals(this.unconfirmedtransactions, inlineResponse20022.unconfirmedtransactions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(confirmedtransactions, unconfirmedtransactions);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse20022 {\n");
    
    sb.append("    confirmedtransactions: ").append(toIndentedString(confirmedtransactions)).append("\n");
    sb.append("    unconfirmedtransactions: ").append(toIndentedString(unconfirmedtransactions)).append("\n");
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

