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

/**
 * TransactionInputs
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-12-01T00:02:50.340-05:00")
public class TransactionInputs {
  @SerializedName("fundtype")
  private String fundtype = null;

  @SerializedName("parentid")
  private String parentid = null;

  @SerializedName("relatedaddress")
  private String relatedaddress = null;

  @SerializedName("value")
  private String value = null;

  @SerializedName("walletaddress")
  private Boolean walletaddress = null;

  public TransactionInputs fundtype(String fundtype) {
    this.fundtype = fundtype;
    return this;
  }

   /**
   * Type of fund represented by the input. Possible values are &#39;siacoin input&#39; and &#39;siafund input&#39;.
   * @return fundtype
  **/
  @ApiModelProperty(example = "siacoin input", value = "Type of fund represented by the input. Possible values are 'siacoin input' and 'siafund input'.")
  public String getFundtype() {
    return fundtype;
  }

  public void setFundtype(String fundtype) {
    this.fundtype = fundtype;
  }

  public TransactionInputs parentid(String parentid) {
    this.parentid = parentid;
    return this;
  }

   /**
   * The id of the output being spent.
   * @return parentid
  **/
  @ApiModelProperty(example = "1234567890abcdef0123456789abcdef0123456789abcdef0123456789abcdef", value = "The id of the output being spent.")
  public String getParentid() {
    return parentid;
  }

  public void setParentid(String parentid) {
    this.parentid = parentid;
  }

  public TransactionInputs relatedaddress(String relatedaddress) {
    this.relatedaddress = relatedaddress;
    return this;
  }

   /**
   * Address that is affected. For inputs (outgoing money), the related address is usually not important because the wallet arbitrarily selects which addresses will fund a transaction.
   * @return relatedaddress
  **/
  @ApiModelProperty(example = "1234567890abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789ab", value = "Address that is affected. For inputs (outgoing money), the related address is usually not important because the wallet arbitrarily selects which addresses will fund a transaction.")
  public String getRelatedaddress() {
    return relatedaddress;
  }

  public void setRelatedaddress(String relatedaddress) {
    this.relatedaddress = relatedaddress;
  }

  public TransactionInputs value(String value) {
    this.value = value;
    return this;
  }

   /**
   * Amount of funds that have been moved in the input.
   * @return value
  **/
  @ApiModelProperty(example = "1234", value = "Amount of funds that have been moved in the input.")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public TransactionInputs walletaddress(Boolean walletaddress) {
    this.walletaddress = walletaddress;
    return this;
  }

   /**
   * true if the address is owned by the wallet.
   * @return walletaddress
  **/
  @ApiModelProperty(example = "false", value = "true if the address is owned by the wallet.")
  public Boolean isWalletaddress() {
    return walletaddress;
  }

  public void setWalletaddress(Boolean walletaddress) {
    this.walletaddress = walletaddress;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionInputs transactionInputs = (TransactionInputs) o;
    return Objects.equals(this.fundtype, transactionInputs.fundtype) &&
        Objects.equals(this.parentid, transactionInputs.parentid) &&
        Objects.equals(this.relatedaddress, transactionInputs.relatedaddress) &&
        Objects.equals(this.value, transactionInputs.value) &&
        Objects.equals(this.walletaddress, transactionInputs.walletaddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fundtype, parentid, relatedaddress, value, walletaddress);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TransactionInputs {\n");
    
    sb.append("    fundtype: ").append(toIndentedString(fundtype)).append("\n");
    sb.append("    parentid: ").append(toIndentedString(parentid)).append("\n");
    sb.append("    relatedaddress: ").append(toIndentedString(relatedaddress)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    walletaddress: ").append(toIndentedString(walletaddress)).append("\n");
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

