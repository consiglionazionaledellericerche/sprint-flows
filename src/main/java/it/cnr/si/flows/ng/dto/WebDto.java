package it.cnr.si.flows.ng.dto;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
public class WebDto extends BaseDto {

  @JsonSerialize(include = Inclusion.NON_EMPTY)
  private Set<GrantPermission> permissions = new HashSet<>();

  public enum GrantPermission {
    SHOW,
    UPDATE,
    UPDATE_VALIDITY,
    DELETE
  }

}
