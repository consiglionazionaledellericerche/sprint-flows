package it.cnr.si.flows.ng.utils;

public final class Utils {

    public static boolean isEmpty(String in) {
        return in == null || in.equals("");
    }

    public static boolean isNotEmpty(String in) {
        return !isEmpty(in);
    }

}
