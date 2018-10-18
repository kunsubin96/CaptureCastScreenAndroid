package com.example.kunsubin.capturecastscreenandroid.utils;

public class Utils {
    public static boolean validateIP(String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }
            
            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }
            
            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }
            
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
    public static boolean isEmptyText(String s){
        return s.length()>0?false:true;
    }
}
