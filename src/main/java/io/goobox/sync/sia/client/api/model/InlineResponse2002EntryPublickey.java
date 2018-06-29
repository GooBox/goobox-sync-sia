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
 * Public key used to identify and verify hosts.
 */
@ApiModel(description = "Public key used to identify and verify hosts.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-12T09:40:06.745-04:00")
public class InlineResponse2002EntryPublickey {
  @SerializedName("algorithm")
  private String algorithm = null;

  @SerializedName("key")
  private String key = null;

  public InlineResponse2002EntryPublickey algorithm(String algorithm) {
    this.algorithm = algorithm;
    return this;
  }

   /**
   * Algorithm used for signing and verification. Typically \&quot;ed25519\&quot;.
   * @return algorithm
  **/
  @ApiModelProperty(example = "ed25519", value = "Algorithm used for signing and verification. Typically \"ed25519\".")
  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public InlineResponse2002EntryPublickey key(String key) {
    this.key = key;
    return this;
  }

   /**
   * Key used to verify signed host messages.
   * @return key
  **/
  @ApiModelProperty(example = "RW50cm9weSBpc24ndCB3aGF0IGl0IHVzZWQgdG8gYmU=", value = "Key used to verify signed host messages.")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InlineResponse2002EntryPublickey inlineResponse2002EntryPublickey = (InlineResponse2002EntryPublickey) o;
    return Objects.equals(this.algorithm, inlineResponse2002EntryPublickey.algorithm) &&
        Objects.equals(this.key, inlineResponse2002EntryPublickey.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(algorithm, key);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse2002EntryPublickey {\n");
    
    sb.append("    algorithm: ").append(toIndentedString(algorithm)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
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

