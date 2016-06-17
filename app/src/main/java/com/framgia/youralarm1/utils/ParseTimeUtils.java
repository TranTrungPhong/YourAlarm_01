package com.framgia.youralarm1.utils;
/**
 * Created by vuduychuong1994 on 6/15/16.
 */
public class ParseTimeUtils {

    public static String formatTextTime(int time) {
        return new StringBuilder().append( time / 60 < 10 ? 0 : "" )
                .append( time / 60 + ":" )
                .append( time % 60 < 10 ? 0 : "" )
                .append( time % 60 ).toString();
    }
}
