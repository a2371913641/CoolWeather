package cn.itcast.coolweather.db;

import org.litepal.crud.LitePalSupport;

public class City extends LitePalSupport {
    private int id;
    private  String cityName;
    private int cityCode;


    public void setCityCode(int cityCode) {
        this.cityCode = cityCode;
    }

    public int getCityCode(){
        return cityCode;
    }

    public void setId(int id){
        this.id=id;
    }

    public int getId(){
        return id;
    }

    public void setCityName(String cityName){
        this.cityName=cityName;
    }

    public String getCityName(){
        return cityName;
    }
}
