package cn.itcast.coolweather.db;

import org.litepal.crud.LitePalSupport;

public class County extends LitePalSupport {
    private int id;
    private  String countyName;
    private int countyCode;
    private String weatherId;


    public void setCountyCode(int countyCode) {
        this.countyCode = countyCode;
    }

    public int getCountyCode(){
        return countyCode;
    }

    public void setId(int id){
        this.id=id;
    }

    public int getId(){
        return id;
    }

    public void setCountyName(String countyName){
        this.countyName=countyName;
    }

    public String getCountyName(){
        return countyName;
    }

    public void setWeatherId(String weatherId){
        this.weatherId=weatherId;
    }

    public String getWeatherId(){
        return weatherId;
    }

}
