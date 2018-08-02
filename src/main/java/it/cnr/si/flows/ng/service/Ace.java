package it.cnr.si.flows.ng.service;

//import data.dto.Letture.PersonaEntitaOrganizzativaWebDto;
//import data.dto.Letture.PersonaWebDto;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import it.cnr.si.flows.ng.dto.PersonaWebDto;
import it.cnr.si.flows.ng.dto.RuoloUtenteWebDto;
import org.json.JSONObject;
import springfox.documentation.spring.web.json.Json;

import java.util.ArrayList;

/**
 * Ace.
 *
 * @author daniele
 * @since 08/06/18
 */
@Headers({"Content-Type: application/json"})
public interface Ace {

//  @RequestLine("GET /ace/v1/persona/{id}")
//  PersonaWebDto personaById(@Param("id") int id);


//  @RequestLine("GET /api/ace/v1/persona/{username}")
//  PersonaWebDto personaByUsername(@Param("username") String username);


//  http://acebuntudock.si.cnr.it:5555/api/ace/v1/ruoloutente?persona=paoloenricocirone
  @RequestLine("GET /api/ace/v1/ruoloutente/persona/{persona}")
  ArrayList<RuoloUtenteWebDto> ruoloUtente(@Param("persona") String persona);
//  @RequestLine("GET api/ace/v1/persona/{username}")
//  JSONObject personaByUsername(@Param("username") String username);

//  @RequestLine("GET /ace/v1/persona")
//  PageDto<PersonaWebDto> getPersone(@QueryMap Map<String, ?> query);

//  @RequestLine("GET /ace/v1/persona/find")
//  List<PersonaWebDto> find(@QueryMap Map<String, ?> query);

//  @RequestLine("POST /ace/v1/persona/create")
//  PersonaWebDto savePersona(PersonaDto personaDto);

//  @RequestLine("PUT /ace/v1/persona/update")
//  PersonaWebDto updatePersona(PersonaDto personaDto);

//  @RequestLine("DELETE /ace/v1/persona/delete/{id}")
//  Response deletePersona(@Param("id") int id);

//  @RequestLine("GET /ace/v1/personaentitaorganizzativa/{id}")
//  PersonaEntitaOrganizzativaWebDto personaEntitaOrganizzativaById(@Param("id") int id);

//  @RequestLine("POST /ace/v1/personaentitaorganizzativa/create")
//  PersonaEntitaOrganizzativaWebDto savePersonaEntitaOrganizzativa(
//      PersonaEntitaOrganizzativaDto personaDto);
//
//  @RequestLine("PUT /ace/v1/personaentitaorganizzativa/update")
//  PersonaEntitaOrganizzativaWebDto updatePersonaEntitaOrganizzativa(
//      PersonaEntitaOrganizzativaDto personaDto);


}
