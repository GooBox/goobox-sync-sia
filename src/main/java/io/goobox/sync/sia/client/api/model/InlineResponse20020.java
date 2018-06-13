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
 * InlineResponse20020
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-12T09:40:06.745-04:00")
public class InlineResponse20020 {
  @SerializedName("coins")
  private String coins = null;

  @SerializedName("funds")
  private String funds = null;

  public InlineResponse20020 coins(String coins) {
    this.coins = coins;
    return this;
  }

   /**
   * Number of siacoins, in hastings, transferred to the wallet as a result of the sweep.
   * @return coins
  **/
  @ApiModelProperty(example = "123456", value = "Number of siacoins, in hastings, transferred to the wallet as a result of the sweep.")
  public String getCoins() {
    return coins;
  }

  public void setCoins(String coins) {
    this.coins = coins;
  }

  public InlineResponse20020 funds(String funds) {
    this.funds = funds;
    return this;
  }

   /**
   * Number of siafunds transferred to the wallet as a result of the sweep.
   * @return funds
  **/
  @ApiModelProperty(example = "1", value = "Number of siafunds transferred to the wallet as a result of the sweep.")
  public String getFunds() {
    return funds;
  }

  public void setFunds(String funds) {
    this.funds = funds;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse20020 inlineResponse20020 = (InlineResponse20020) o;
    return Objects.equals(this.coins, inlineResponse20020.coins) &&
        Objects.equals(this.funds, inlineResponse20020.funds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coins, funds);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse20020 {\n");
    
    sb.append("    coins: ").append(toIndentedString(coins)).append("\n");
    sb.append("    funds: ").append(toIndentedString(funds)).append("\n");
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

