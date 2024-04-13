package cn.itcast.coolweather.util;

import android.text.TextUtils;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.itcast.coolweather.db.City;
import cn.itcast.coolweather.db.County;
import cn.itcast.coolweather.db.Province;
import cn.itcast.coolweather.gson.Bing;
import cn.itcast.coolweather.gson.Weather;

public class Utility {
    //解析和处理服务器返回的省级数据
    public static  boolean handleProvinceResponse(String response){
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allProvinces=new JSONArray(response);
                for(int i=0;i<allProvinces.length();i++){
                    JSONObject provinceObject=allProvinces.getJSONObject(i);
                    Province province=new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    //解析和处理服务器返回的市级数据
    public static  boolean handleCityResponse(String response,int provinceCode){
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allCities=new JSONArray(response);
                for(int i=0;i<allCities.length();i++){
                    JSONObject provinceObject=allCities.getJSONObject(i);
                    City city=new City();
                    city.setCityName(provinceObject.getString("name"));
                    city.setCityCode(provinceObject.getInt("id"));
                    city.setProvinceId(provinceCode);
                    city.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    //解析和处理服务器返回的县级数据
    public static  boolean handleCountyResponse(String response,int cityId){
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allCounties=new JSONArray(response);
                for(int i=0;i<allCounties.length();i++){
                    JSONObject provinceObject=allCounties.getJSONObject(i);
                    County county =new County();
                    county.setCountyName(provinceObject.getString("name"));
                    county.setWeatherId(provinceObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    //将返回的JOSN数据解析成Weather实体类
    public static Weather handleWeatherResponse(String response){
        try{
            JSONObject jsonObject=new JSONObject(response);
            JSONArray jsonArray=jsonObject.getJSONArray("HeWeather");
            String weartherContent=jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weartherContent,Weather.class);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void handleBingResponse(String input, Bing bing){

        String strStart="<div class=\"hp_top_cover\" style=\"background-image: url(&quot;";
        String strEnd=";); opacity: ; display: block;\">";
        int strStartIndex=input.indexOf(strStart);
        int strEndIndex=input.indexOf(strEnd);
        String src=input.substring(strStartIndex,strEndIndex).replace(strStart,"");
        bing.setImg(src);
    }

}
