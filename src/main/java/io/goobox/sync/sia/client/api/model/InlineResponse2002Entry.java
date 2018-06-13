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
import io.goobox.sync.sia.client.api.model.InlineResponse2002EntryPublickey;
import io.goobox.sync.sia.client.api.model.InlineResponse2002EntryScorebreakdown;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * InlineResponse2002Entry
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-12T09:40:06.745-04:00")
public class InlineResponse2002Entry {
  @SerializedName("acceptingcontracts")
  private Boolean acceptingcontracts = null;

  @SerializedName("maxdownloadbatchsize")
  private Integer maxdownloadbatchsize = null;

  @SerializedName("maxduration")
  private Integer maxduration = null;

  @SerializedName("maxrevisebatchsize")
  private Integer maxrevisebatchsize = null;

  @SerializedName("netaddress")
  private String netaddress = null;

  @SerializedName("remainingstorage")
  private Integer remainingstorage = null;

  @SerializedName("sectorsize")
  private Integer sectorsize = null;

  @SerializedName("totalstorage")
  private Integer totalstorage = null;

  @SerializedName("unlockhash")
  private String unlockhash = null;

  @SerializedName("windowsize")
  private Integer windowsize = null;

  @SerializedName("publickey")
  private InlineResponse2002EntryPublickey publickey = null;

  @SerializedName("publickeystring")
  private String publickeystring = null;

  @SerializedName("scorebreakdown")
  private InlineResponse2002EntryScorebreakdown scorebreakdown = null;

  public InlineResponse2002Entry acceptingcontracts(Boolean acceptingcontracts) {
    this.acceptingcontracts = acceptingcontracts;
    return this;
  }

   /**
   * true if the host is accepting new contracts.
   * @return acceptingcontracts
  **/
  @ApiModelProperty(example = "true", value = "true if the host is accepting new contracts.")
  public Boolean isAcceptingcontracts() {
    return acceptingcontracts;
  }

  public void setAcceptingcontracts(Boolean acceptingcontracts) {
    this.acceptingcontracts = acceptingcontracts;
  }

  public InlineResponse2002Entry maxdownloadbatchsize(Integer maxdownloadbatchsize) {
    this.maxdownloadbatchsize = maxdownloadbatchsize;
    return this;
  }

   /**
   * Maximum number of bytes that the host will allow to be requested by a single download request.
   * @return maxdownloadbatchsize
  **/
  @ApiModelProperty(example = "17825792", value = "Maximum number of bytes that the host will allow to be requested by a single download request.")
  public Integer getMaxdownloadbatchsize() {
    return maxdownloadbatchsize;
  }

  public void setMaxdownloadbatchsize(Integer maxdownloadbatchsize) {
    this.maxdownloadbatchsize = maxdownloadbatchsize;
  }

  public InlineResponse2002Entry maxduration(Integer maxduration) {
    this.maxduration = maxduration;
    return this;
  }

   /**
   * Maximum duration in blocks that a host will allow for a file contract. The host commits to keeping files for the full duration under the threat of facing a large penalty for losing or dropping data before the duration is complete. The storage proof window of an incoming file contract must end before the current height + maxduration. There is a block approximately every 10 minutes. e.g. 1 day &#x3D; 144 blocks 
   * @return maxduration
  **/
  @ApiModelProperty(example = "25920", value = "Maximum duration in blocks that a host will allow for a file contract. The host commits to keeping files for the full duration under the threat of facing a large penalty for losing or dropping data before the duration is complete. The storage proof window of an incoming file contract must end before the current height + maxduration. There is a block approximately every 10 minutes. e.g. 1 day = 144 blocks ")
  public Integer getMaxduration() {
    return maxduration;
  }

  public void setMaxduration(Integer maxduration) {
    this.maxduration = maxduration;
  }

  public InlineResponse2002Entry maxrevisebatchsize(Integer maxrevisebatchsize) {
    this.maxrevisebatchsize = maxrevisebatchsize;
    return this;
  }

   /**
   * Maximum size in bytes of a single batch of file contract revisions. Larger batch sizes allow for higher throughput as there is significant communication overhead associated with performing a batch upload. 
   * @return maxrevisebatchsize
  **/
  @ApiModelProperty(example = "17825792", value = "Maximum size in bytes of a single batch of file contract revisions. Larger batch sizes allow for higher throughput as there is significant communication overhead associated with performing a batch upload. ")
  public Integer getMaxrevisebatchsize() {
    return maxrevisebatchsize;
  }

  public void setMaxrevisebatchsize(Integer maxrevisebatchsize) {
    this.maxrevisebatchsize = maxrevisebatchsize;
  }

  public InlineResponse2002Entry netaddress(String netaddress) {
    this.netaddress = netaddress;
    return this;
  }

   /**
   * Remote address of the host. It can be an IPv4, IPv6, or hostname, along with the port. IPv6 addresses are enclosed in square brackets.
   * @return netaddress
  **/
  @ApiModelProperty(example = "123.456.789.0:9982", value = "Remote address of the host. It can be an IPv4, IPv6, or hostname, along with the port. IPv6 addresses are enclosed in square brackets.")
  public String getNetaddress() {
    return netaddress;
  }

  public void setNetaddress(String netaddress) {
    this.netaddress = netaddress;
  }

  public InlineResponse2002Entry remainingstorage(Integer remainingstorage) {
    this.remainingstorage = remainingstorage;
    return this;
  }

   /**
   * Unused storage capacity the host claims it has, in bytes.
   * @return remainingstorage
  **/
  @ApiModelProperty(example = "35000000000", value = "Unused storage capacity the host claims it has, in bytes.")
  public Integer getRemainingstorage() {
    return remainingstorage;
  }

  public void setRemainingstorage(Integer remainingstorage) {
    this.remainingstorage = remainingstorage;
  }

  public InlineResponse2002Entry sectorsize(Integer sectorsize) {
    this.sectorsize = sectorsize;
    return this;
  }

   /**
   * Smallest amount of data in bytes that can be uploaded or downloaded to or from the host.
   * @return sectorsize
  **/
  @ApiModelProperty(example = "4194304", value = "Smallest amount of data in bytes that can be uploaded or downloaded to or from the host.")
  public Integer getSectorsize() {
    return sectorsize;
  }

  public void setSectorsize(Integer sectorsize) {
    this.sectorsize = sectorsize;
  }

  public InlineResponse2002Entry totalstorage(Integer totalstorage) {
    this.totalstorage = totalstorage;
    return this;
  }

   /**
   * Total amount of storage capacity the host claims it has, in bytes.
   * @return totalstorage
  **/
  @ApiModelProperty(example = "35000000000", value = "Total amount of storage capacity the host claims it has, in bytes.")
  public Integer getTotalstorage() {
    return totalstorage;
  }

  public void setTotalstorage(Integer totalstorage) {
    this.totalstorage = totalstorage;
  }

  public InlineResponse2002Entry unlockhash(String unlockhash) {
    this.unlockhash = unlockhash;
    return this;
  }

   /**
   * Address at which the host can be paid when forming file contracts.
   * @return unlockhash
  **/
  @ApiModelProperty(example = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789ab", value = "Address at which the host can be paid when forming file contracts.")
  public String getUnlockhash() {
    return unlockhash;
  }

  public void setUnlockhash(String unlockhash) {
    this.unlockhash = unlockhash;
  }

  public InlineResponse2002Entry windowsize(Integer windowsize) {
    this.windowsize = windowsize;
    return this;
  }

   /**
   * A storage proof window is the number of blocks that the host has to get a storage proof onto the blockchain. The window size is the minimum size of window that the host will accept in a file contract. 
   * @return windowsize
  **/
  @ApiModelProperty(example = "144", value = "A storage proof window is the number of blocks that the host has to get a storage proof onto the blockchain. The window size is the minimum size of window that the host will accept in a file contract. ")
  public Integer getWindowsize() {
    return windowsize;
  }

  public void setWindowsize(Integer windowsize) {
    this.windowsize = windowsize;
  }

  public InlineResponse2002Entry publickey(InlineResponse2002EntryPublickey publickey) {
    this.publickey = publickey;
    return this;
  }

   /**
   * Get publickey
   * @return publickey
  **/
  @ApiModelProperty(value = "")
  public InlineResponse2002EntryPublickey getPublickey() {
    return publickey;
  }

  public void setPublickey(InlineResponse2002EntryPublickey publickey) {
    this.publickey = publickey;
  }

  public InlineResponse2002Entry publickeystring(String publickeystring) {
    this.publickeystring = publickeystring;
    return this;
  }

   /**
   * The string representation of the full public key, used when calling /hostdb/hosts.
   * @return publickeystring
  **/
  @ApiModelProperty(example = "ed25519:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", value = "The string representation of the full public key, used when calling /hostdb/hosts.")
  public String getPublickeystring() {
    return publickeystring;
  }

  public void setPublickeystring(String publickeystring) {
    this.publickeystring = publickeystring;
  }

  public InlineResponse2002Entry scorebreakdown(InlineResponse2002EntryScorebreakdown scorebreakdown) {
    this.scorebreakdown = scorebreakdown;
    return this;
  }

   /**
   * Get scorebreakdown
   * @return scorebreakdown
  **/
  @ApiModelProperty(value = "")
  public InlineResponse2002EntryScorebreakdown getScorebreakdown() {
    return scorebreakdown;
  }

  public void setScorebreakdown(InlineResponse2002EntryScorebreakdown scorebreakdown) {
    this.scorebreakdown = scorebreakdown;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2002Entry inlineResponse2002Entry = (InlineResponse2002Entry) o;
    return Objects.equals(this.acceptingcontracts, inlineResponse2002Entry.acceptingcontracts) &&
        Objects.equals(this.maxdownloadbatchsize, inlineResponse2002Entry.maxdownloadbatchsize) &&
        Objects.equals(this.maxduration, inlineResponse2002Entry.maxduration) &&
        Objects.equals(this.maxrevisebatchsize, inlineResponse2002Entry.maxrevisebatchsize) &&
        Objects.equals(this.netaddress, inlineResponse2002Entry.netaddress) &&
        Objects.equals(this.remainingstorage, inlineResponse2002Entry.remainingstorage) &&
        Objects.equals(this.sectorsize, inlineResponse2002Entry.sectorsize) &&
        Objects.equals(this.totalstorage, inlineResponse2002Entry.totalstorage) &&
        Objects.equals(this.unlockhash, inlineResponse2002Entry.unlockhash) &&
        Objects.equals(this.windowsize, inlineResponse2002Entry.windowsize) &&
        Objects.equals(this.publickey, inlineResponse2002Entry.publickey) &&
        Objects.equals(this.publickeystring, inlineResponse2002Entry.publickeystring) &&
        Objects.equals(this.scorebreakdown, inlineResponse2002Entry.scorebreakdown);
  }

  @Override
  public int hashCode() {
    return Objects.hash(acceptingcontracts, maxdownloadbatchsize, maxduration, maxrevisebatchsize, netaddress, remainingstorage, sectorsize, totalstorage, unlockhash, windowsize, publickey, publickeystring, scorebreakdown);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2002Entry {\n");
    
    sb.append("    acceptingcontracts: ").append(toIndentedString(acceptingcontracts)).append("\n");
    sb.append("    maxdownloadbatchsize: ").append(toIndentedString(maxdownloadbatchsize)).append("\n");
    sb.append("    maxduration: ").append(toIndentedString(maxduration)).append("\n");
    sb.append("    maxrevisebatchsize: ").append(toIndentedString(maxrevisebatchsize)).append("\n");
    sb.append("    netaddress: ").append(toIndentedString(netaddress)).append("\n");
    sb.append("    remainingstorage: ").append(toIndentedString(remainingstorage)).append("\n");
    sb.append("    sectorsize: ").append(toIndentedString(sectorsize)).append("\n");
    sb.append("    totalstorage: ").append(toIndentedString(totalstorage)).append("\n");
    sb.append("    unlockhash: ").append(toIndentedString(unlockhash)).append("\n");
    sb.append("    windowsize: ").append(toIndentedString(windowsize)).append("\n");
    sb.append("    publickey: ").append(toIndentedString(publickey)).append("\n");
    sb.append("    publickeystring: ").append(toIndentedString(publickeystring)).append("\n");
    sb.append("    scorebreakdown: ").append(toIndentedString(scorebreakdown)).append("\n");
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

