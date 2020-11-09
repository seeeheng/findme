package com.github.navisensfindme;

class NavisensZMQAdapter {
    static String[] pack_data(double x, double y){
        String x_packed = "lat," + x;
        String y_packed = "lng," + y;
        return new String[]{"NavisensLocUpdate",x_packed, y_packed};
    }
}
