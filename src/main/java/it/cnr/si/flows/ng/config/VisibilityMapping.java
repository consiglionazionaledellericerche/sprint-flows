package it.cnr.si.flows.ng.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisibilityMapping {

    public static Map<String, List<String>> GroupVisibilityMappingForProcessInstance;
    public static Map<String, List<String>> UserVisibilityMappingForProcessInstance;


    static {
        GroupVisibilityMappingForProcessInstance = new HashMap<>();
        GroupVisibilityMappingForProcessInstance.put("acquistiTrasparenza-predisposizioneDecisione", listOf("${direttore}", "ADMIN", "sisinfo_rt"));
    }

    public static List<String> listOf(String... in) {
        return Arrays.asList(in);
    }



}
