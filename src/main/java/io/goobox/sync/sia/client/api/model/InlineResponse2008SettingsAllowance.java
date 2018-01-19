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

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

/**
 * AllowanceInfo dictates how much the renter is allowed to spend in a given period. Note that funds are spent on both storage and bandwidth.
 */
@ApiModel(description = "AllowanceInfo dictates how much the renter is allowed to spend in a given period. Note that funds are spent on both storage and bandwidth.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2017-11-23T01:54:58.054-05:00")
public class InlineResponse2008SettingsAllowance {
  @SerializedName("funds")
  private String funds = null;

  @SerializedName("hosts")
  private Integer hosts = null;

  @SerializedName("period")
  private Long period = null;

  @SerializedName("renewwindow")
  private Long renewwindow = null;

  public InlineResponse2008SettingsAllowance funds(String funds) {
    this.funds = funds;
    return this;
  }

   /**
   * Amount of money allocated for contracts. Funds are spent on both storage and bandwidth.
   * @return funds
  **/
  @ApiModelProperty(example = "1234", value = "Amount of money allocated for contracts. Funds are spent on both storage and bandwidth.")
  public String getFunds() {
    return funds;
  }

  public void setFunds(String funds) {
    this.funds = funds;
  }

  public InlineResponse2008SettingsAllowance hosts(Integer hosts) {
    this.hosts = hosts;
    return this;
  }

   /**
   * Number of hosts that contracts will be formed with.
   * @return hosts
  **/
  @ApiModelProperty(example = "24", value = "Number of hosts that contracts will be formed with.")
  public Integer getHosts() {
    return hosts;
  }

  public void setHosts(Integer hosts) {
    this.hosts = hosts;
  }

  public InlineResponse2008SettingsAllowance period(Long period) {
    this.period = period;
    return this;
  }

   /**
   * Duration of contracts formed, in number of blocks.
   * @return period
  **/
  @ApiModelProperty(example = "6048", value = "Duration of contracts formed, in number of blocks.")
  public Long getPeriod() {
    return period;
  }

  public void setPeriod(Long period) {
    this.period = period;
  }

  public InlineResponse2008SettingsAllowance renewwindow(Long renewwindow) {
    this.renewwindow = renewwindow;
    return this;
  }

   /**
   * If the current blockheight + the renew window &gt;&#x3D; the height the contract is scheduled to end, the contract is renewed automatically. Is always nonzero.
   * @return renewwindow
  **/
  @ApiModelProperty(example = "3024", value = "If the current blockheight + the renew window >= the height the contract is scheduled to end, the contract is renewed automatically. Is always nonzero.")
  public Long getRenewwindow() {
    return renewwindow;
  }

  public void setRenewwindow(Long renewwindow) {
    this.renewwindow = renewwindow;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2008SettingsAllowance inlineResponse2008SettingsAllowance = (InlineResponse2008SettingsAllowance) o;
    return Objects.equals(this.funds, inlineResponse2008SettingsAllowance.funds) &&
        Objects.equals(this.hosts, inlineResponse2008SettingsAllowance.hosts) &&
        Objects.equals(this.period, inlineResponse2008SettingsAllowance.period) &&
        Objects.equals(this.renewwindow, inlineResponse2008SettingsAllowance.renewwindow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(funds, hosts, period, renewwindow);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2008SettingsAllowance {\n");
    
    sb.append("    funds: ").append(toIndentedString(funds)).append("\n");
    sb.append("    hosts: ").append(toIndentedString(hosts)).append("\n");
    sb.append("    period: ").append(toIndentedString(period)).append("\n");
    sb.append("    renewwindow: ").append(toIndentedString(renewwindow)).append("\n");
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

