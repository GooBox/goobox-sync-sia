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
 * The settings of the host. Most interactions between the user and the host occur by changing the internal settings.
 */
@ApiModel(description = "The settings of the host. Most interactions between the user and the host occur by changing the internal settings.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-12-01T00:02:50.340-05:00")
public class InlineResponse2004Internalsettings {
  @SerializedName("acceptingcontracts")
  private Boolean acceptingcontracts = null;

  @SerializedName("collateral")
  private String collateral = null;

  @SerializedName("collateralbudget")
  private String collateralbudget = null;

  @SerializedName("maxcollateral")
  private String maxcollateral = null;

  @SerializedName("maxdownloadbatchsize")
  private Integer maxdownloadbatchsize = null;

  @SerializedName("maxduration")
  private Integer maxduration = null;

  @SerializedName("maxrevisebatchsize")
  private Integer maxrevisebatchsize = null;

  @SerializedName("mincontractprice")
  private String mincontractprice = null;

  @SerializedName("mindownloadbandwidthprice")
  private String mindownloadbandwidthprice = null;

  @SerializedName("minstorageprice")
  private String minstorageprice = null;

  @SerializedName("minuploadbandwidthprice")
  private String minuploadbandwidthprice = null;

  @SerializedName("netaddress")
  private String netaddress = null;

  @SerializedName("windowsize")
  private Integer windowsize = null;

  public InlineResponse2004Internalsettings acceptingcontracts(Boolean acceptingcontracts) {
    this.acceptingcontracts = acceptingcontracts;
    return this;
  }

   /**
   * When set to true, the host will accept new file contracts if the terms are reasonable. When set to false, the host will not accept new file contracts at all. 
   * @return acceptingcontracts
  **/
  @ApiModelProperty(example = "true", value = "When set to true, the host will accept new file contracts if the terms are reasonable. When set to false, the host will not accept new file contracts at all. ")
  public Boolean isAcceptingcontracts() {
    return acceptingcontracts;
  }

  public void setAcceptingcontracts(Boolean acceptingcontracts) {
    this.acceptingcontracts = acceptingcontracts;
  }

  public InlineResponse2004Internalsettings collateral(String collateral) {
    this.collateral = collateral;
    return this;
  }

   /**
   * The maximum amount of money that the host will put up as collateral per byte per block of storage that is contracted by the renter.
   * @return collateral
  **/
  @ApiModelProperty(example = "57870370370", value = "The maximum amount of money that the host will put up as collateral per byte per block of storage that is contracted by the renter.")
  public String getCollateral() {
    return collateral;
  }

  public void setCollateral(String collateral) {
    this.collateral = collateral;
  }

  public InlineResponse2004Internalsettings collateralbudget(String collateralbudget) {
    this.collateralbudget = collateralbudget;
    return this;
  }

   /**
   * The total amount of money that the host will allocate to collateral across all file contracts.
   * @return collateralbudget
  **/
  @ApiModelProperty(example = "2000000000000000000000000000000", value = "The total amount of money that the host will allocate to collateral across all file contracts.")
  public String getCollateralbudget() {
    return collateralbudget;
  }

  public void setCollateralbudget(String collateralbudget) {
    this.collateralbudget = collateralbudget;
  }

  public InlineResponse2004Internalsettings maxcollateral(String maxcollateral) {
    this.maxcollateral = maxcollateral;
    return this;
  }

   /**
   * The maximum amount of collateral that the host will put into a single file contract.
   * @return maxcollateral
  **/
  @ApiModelProperty(example = "100000000000000000000000000000", value = "The maximum amount of collateral that the host will put into a single file contract.")
  public String getMaxcollateral() {
    return maxcollateral;
  }

  public void setMaxcollateral(String maxcollateral) {
    this.maxcollateral = maxcollateral;
  }

  public InlineResponse2004Internalsettings maxdownloadbatchsize(Integer maxdownloadbatchsize) {
    this.maxdownloadbatchsize = maxdownloadbatchsize;
    return this;
  }

   /**
   * The maximum size of a single download request from a renter. Each download request has multiple round trips of communication that exchange money. Larger batch sizes mean fewer round trips, but more financial risk for the host - the renter can get a free batch when downloading by refusing to provide a signature. 
   * @return maxdownloadbatchsize
  **/
  @ApiModelProperty(example = "17825792", value = "The maximum size of a single download request from a renter. Each download request has multiple round trips of communication that exchange money. Larger batch sizes mean fewer round trips, but more financial risk for the host - the renter can get a free batch when downloading by refusing to provide a signature. ")
  public Integer getMaxdownloadbatchsize() {
    return maxdownloadbatchsize;
  }

  public void setMaxdownloadbatchsize(Integer maxdownloadbatchsize) {
    this.maxdownloadbatchsize = maxdownloadbatchsize;
  }

  public InlineResponse2004Internalsettings maxduration(Integer maxduration) {
    this.maxduration = maxduration;
    return this;
  }

   /**
   * The maximum duration of a file contract that the host will accept. The storage proof window must end before the current height + maxduration. 
   * @return maxduration
  **/
  @ApiModelProperty(example = "25920", value = "The maximum duration of a file contract that the host will accept. The storage proof window must end before the current height + maxduration. ")
  public Integer getMaxduration() {
    return maxduration;
  }

  public void setMaxduration(Integer maxduration) {
    this.maxduration = maxduration;
  }

  public InlineResponse2004Internalsettings maxrevisebatchsize(Integer maxrevisebatchsize) {
    this.maxrevisebatchsize = maxrevisebatchsize;
    return this;
  }

   /**
   * The maximum size of a single batch of file contract revisions. The renter can perform DoS attacks on the host by uploading a batch of data then refusing to provide a signature to pay for the data. The host can reduce this exposure by limiting the batch size. Larger batch sizes allow for higher throughput as there is significant communication overhead associated with performing a batch upload. 
   * @return maxrevisebatchsize
  **/
  @ApiModelProperty(example = "17825792", value = "The maximum size of a single batch of file contract revisions. The renter can perform DoS attacks on the host by uploading a batch of data then refusing to provide a signature to pay for the data. The host can reduce this exposure by limiting the batch size. Larger batch sizes allow for higher throughput as there is significant communication overhead associated with performing a batch upload. ")
  public Integer getMaxrevisebatchsize() {
    return maxrevisebatchsize;
  }

  public void setMaxrevisebatchsize(Integer maxrevisebatchsize) {
    this.maxrevisebatchsize = maxrevisebatchsize;
  }

  public InlineResponse2004Internalsettings mincontractprice(String mincontractprice) {
    this.mincontractprice = mincontractprice;
    return this;
  }

   /**
   * The minimum price that the host will demand from a renter when forming a contract. Typically this price is to cover transaction fees on the file contract revision and storage proof, but can also be used if the host has a low amount of collateral. The price is a minimum because the host may automatically adjust the price upwards in times of high demand. 
   * @return mincontractprice
  **/
  @ApiModelProperty(example = "30000000000000000000000000", value = "The minimum price that the host will demand from a renter when forming a contract. Typically this price is to cover transaction fees on the file contract revision and storage proof, but can also be used if the host has a low amount of collateral. The price is a minimum because the host may automatically adjust the price upwards in times of high demand. ")
  public String getMincontractprice() {
    return mincontractprice;
  }

  public void setMincontractprice(String mincontractprice) {
    this.mincontractprice = mincontractprice;
  }

  public InlineResponse2004Internalsettings mindownloadbandwidthprice(String mindownloadbandwidthprice) {
    this.mindownloadbandwidthprice = mindownloadbandwidthprice;
    return this;
  }

   /**
   * The minimum price that the host will demand from a renter when the renter is downloading data. If the host is saturated, the host may increase the price from the minimum. 
   * @return mindownloadbandwidthprice
  **/
  @ApiModelProperty(example = "250000000000000", value = "The minimum price that the host will demand from a renter when the renter is downloading data. If the host is saturated, the host may increase the price from the minimum. ")
  public String getMindownloadbandwidthprice() {
    return mindownloadbandwidthprice;
  }

  public void setMindownloadbandwidthprice(String mindownloadbandwidthprice) {
    this.mindownloadbandwidthprice = mindownloadbandwidthprice;
  }

  public InlineResponse2004Internalsettings minstorageprice(String minstorageprice) {
    this.minstorageprice = minstorageprice;
    return this;
  }

   /**
   * The minimum price that the host will demand when storing data for extended periods of time. If the host is low on space, the price of storage may be set higher than the minimum. 
   * @return minstorageprice
  **/
  @ApiModelProperty(example = "231481481481", value = "The minimum price that the host will demand when storing data for extended periods of time. If the host is low on space, the price of storage may be set higher than the minimum. ")
  public String getMinstorageprice() {
    return minstorageprice;
  }

  public void setMinstorageprice(String minstorageprice) {
    this.minstorageprice = minstorageprice;
  }

  public InlineResponse2004Internalsettings minuploadbandwidthprice(String minuploadbandwidthprice) {
    this.minuploadbandwidthprice = minuploadbandwidthprice;
    return this;
  }

   /**
   * The minimum price that the host will demand from a renter when the renter is uploading data. If the host is saturated, the host may increase the price from the minimum. 
   * @return minuploadbandwidthprice
  **/
  @ApiModelProperty(example = "100000000000000", value = "The minimum price that the host will demand from a renter when the renter is uploading data. If the host is saturated, the host may increase the price from the minimum. ")
  public String getMinuploadbandwidthprice() {
    return minuploadbandwidthprice;
  }

  public void setMinuploadbandwidthprice(String minuploadbandwidthprice) {
    this.minuploadbandwidthprice = minuploadbandwidthprice;
  }

  public InlineResponse2004Internalsettings netaddress(String netaddress) {
    this.netaddress = netaddress;
    return this;
  }

   /**
   * The IP address or hostname (including port) that the host should be contacted at. If left blank, the host will automatically figure out its ip address and use that. If given, the host will use the address given. 
   * @return netaddress
  **/
  @ApiModelProperty(example = "123.456.789.0:9982", value = "The IP address or hostname (including port) that the host should be contacted at. If left blank, the host will automatically figure out its ip address and use that. If given, the host will use the address given. ")
  public String getNetaddress() {
    return netaddress;
  }

  public void setNetaddress(String netaddress) {
    this.netaddress = netaddress;
  }

  public InlineResponse2004Internalsettings windowsize(Integer windowsize) {
    this.windowsize = windowsize;
    return this;
  }

   /**
   * The storage proof window is the number of blocks that the host has to get a storage proof onto the blockchain. The window size is the minimum size of window that the host will accept in a file contract. 
   * @return windowsize
  **/
  @ApiModelProperty(example = "144", value = "The storage proof window is the number of blocks that the host has to get a storage proof onto the blockchain. The window size is the minimum size of window that the host will accept in a file contract. ")
  public Integer getWindowsize() {
    return windowsize;
  }

  public void setWindowsize(Integer windowsize) {
    this.windowsize = windowsize;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2004Internalsettings inlineResponse2004Internalsettings = (InlineResponse2004Internalsettings) o;
    return Objects.equals(this.acceptingcontracts, inlineResponse2004Internalsettings.acceptingcontracts) &&
        Objects.equals(this.collateral, inlineResponse2004Internalsettings.collateral) &&
        Objects.equals(this.collateralbudget, inlineResponse2004Internalsettings.collateralbudget) &&
        Objects.equals(this.maxcollateral, inlineResponse2004Internalsettings.maxcollateral) &&
        Objects.equals(this.maxdownloadbatchsize, inlineResponse2004Internalsettings.maxdownloadbatchsize) &&
        Objects.equals(this.maxduration, inlineResponse2004Internalsettings.maxduration) &&
        Objects.equals(this.maxrevisebatchsize, inlineResponse2004Internalsettings.maxrevisebatchsize) &&
        Objects.equals(this.mincontractprice, inlineResponse2004Internalsettings.mincontractprice) &&
        Objects.equals(this.mindownloadbandwidthprice, inlineResponse2004Internalsettings.mindownloadbandwidthprice) &&
        Objects.equals(this.minstorageprice, inlineResponse2004Internalsettings.minstorageprice) &&
        Objects.equals(this.minuploadbandwidthprice, inlineResponse2004Internalsettings.minuploadbandwidthprice) &&
        Objects.equals(this.netaddress, inlineResponse2004Internalsettings.netaddress) &&
        Objects.equals(this.windowsize, inlineResponse2004Internalsettings.windowsize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(acceptingcontracts, collateral, collateralbudget, maxcollateral, maxdownloadbatchsize, maxduration, maxrevisebatchsize, mincontractprice, mindownloadbandwidthprice, minstorageprice, minuploadbandwidthprice, netaddress, windowsize);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2004Internalsettings {\n");
    
    sb.append("    acceptingcontracts: ").append(toIndentedString(acceptingcontracts)).append("\n");
    sb.append("    collateral: ").append(toIndentedString(collateral)).append("\n");
    sb.append("    collateralbudget: ").append(toIndentedString(collateralbudget)).append("\n");
    sb.append("    maxcollateral: ").append(toIndentedString(maxcollateral)).append("\n");
    sb.append("    maxdownloadbatchsize: ").append(toIndentedString(maxdownloadbatchsize)).append("\n");
    sb.append("    maxduration: ").append(toIndentedString(maxduration)).append("\n");
    sb.append("    maxrevisebatchsize: ").append(toIndentedString(maxrevisebatchsize)).append("\n");
    sb.append("    mincontractprice: ").append(toIndentedString(mincontractprice)).append("\n");
    sb.append("    mindownloadbandwidthprice: ").append(toIndentedString(mindownloadbandwidthprice)).append("\n");
    sb.append("    minstorageprice: ").append(toIndentedString(minstorageprice)).append("\n");
    sb.append("    minuploadbandwidthprice: ").append(toIndentedString(minuploadbandwidthprice)).append("\n");
    sb.append("    netaddress: ").append(toIndentedString(netaddress)).append("\n");
    sb.append("    windowsize: ").append(toIndentedString(windowsize)).append("\n");
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

