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
 * InlineResponse2007
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-12-01T00:02:50.340-05:00")
public class InlineResponse2007 {
  @SerializedName("blocksmined")
  private Integer blocksmined = null;

  @SerializedName("cpuhashrate")
  private Integer cpuhashrate = null;

  @SerializedName("cpumining")
  private Boolean cpumining = null;

  @SerializedName("staleblocksmined")
  private Integer staleblocksmined = null;

  public InlineResponse2007 blocksmined(Integer blocksmined) {
    this.blocksmined = blocksmined;
    return this;
  }

   /**
   * Number of mined blocks. This value is remembered after restarting.
   * @return blocksmined
  **/
  @ApiModelProperty(example = "9001", value = "Number of mined blocks. This value is remembered after restarting.")
  public Integer getBlocksmined() {
    return blocksmined;
  }

  public void setBlocksmined(Integer blocksmined) {
    this.blocksmined = blocksmined;
  }

  public InlineResponse2007 cpuhashrate(Integer cpuhashrate) {
    this.cpuhashrate = cpuhashrate;
    return this;
  }

   /**
   * How fast the cpu is hashing, in hashes per second.
   * @return cpuhashrate
  **/
  @ApiModelProperty(example = "1337", value = "How fast the cpu is hashing, in hashes per second.")
  public Integer getCpuhashrate() {
    return cpuhashrate;
  }

  public void setCpuhashrate(Integer cpuhashrate) {
    this.cpuhashrate = cpuhashrate;
  }

  public InlineResponse2007 cpumining(Boolean cpumining) {
    this.cpumining = cpumining;
    return this;
  }

   /**
   * true if the cpu miner is active.
   * @return cpumining
  **/
  @ApiModelProperty(example = "false", value = "true if the cpu miner is active.")
  public Boolean isCpumining() {
    return cpumining;
  }

  public void setCpumining(Boolean cpumining) {
    this.cpumining = cpumining;
  }

  public InlineResponse2007 staleblocksmined(Integer staleblocksmined) {
    this.staleblocksmined = staleblocksmined;
    return this;
  }

   /**
   * Number of mined blocks that are stale, indicating that they are not included in the current longest chain, likely because some other block at the same height had its chain extended first. 
   * @return staleblocksmined
  **/
  @ApiModelProperty(example = "0", value = "Number of mined blocks that are stale, indicating that they are not included in the current longest chain, likely because some other block at the same height had its chain extended first. ")
  public Integer getStaleblocksmined() {
    return staleblocksmined;
  }

  public void setStaleblocksmined(Integer staleblocksmined) {
    this.staleblocksmined = staleblocksmined;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2007 inlineResponse2007 = (InlineResponse2007) o;
    return Objects.equals(this.blocksmined, inlineResponse2007.blocksmined) &&
        Objects.equals(this.cpuhashrate, inlineResponse2007.cpuhashrate) &&
        Objects.equals(this.cpumining, inlineResponse2007.cpumining) &&
        Objects.equals(this.staleblocksmined, inlineResponse2007.staleblocksmined);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blocksmined, cpuhashrate, cpumining, staleblocksmined);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2007 {\n");
    
    sb.append("    blocksmined: ").append(toIndentedString(blocksmined)).append("\n");
    sb.append("    cpuhashrate: ").append(toIndentedString(cpuhashrate)).append("\n");
    sb.append("    cpumining: ").append(toIndentedString(cpumining)).append("\n");
    sb.append("    staleblocksmined: ").append(toIndentedString(staleblocksmined)).append("\n");
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

