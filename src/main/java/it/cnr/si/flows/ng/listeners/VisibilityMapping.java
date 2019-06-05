package it.cnr.si.flows.ng.listeners;

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
        UserVisibilityMappingForProcessInstance = new HashMap<>();
        UserVisibilityMappingForProcessInstance.put("acquistiTrasparenza-modificaDecisione", listOf("${iniziator}"));
    }

    public static List<String> listOf(String... in) {
        return Arrays.asList(in);
    }



}
