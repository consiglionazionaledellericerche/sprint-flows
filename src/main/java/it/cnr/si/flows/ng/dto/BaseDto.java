package it.cnr.si.flows.ng.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Articolo interessante per fare la fetch della entity. https://stackoverflow.com/questions/42367081/map-a-dto-to-an-entity-retrieved-from-database-if-dto-has-id-using-mapstruct
 *
 * @author alessandro
 */
@Getter
@Setter
@ToString
public class BaseDto {

  private Integer id;

}