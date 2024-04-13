package cn.itcast.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.util.Log;

import com.bumptech.glide.Glide;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;

import cn.itcast.coolweather.WeatherActivity;
import cn.itcast.coolweather.gson.Bing;
import cn.itcast.coolweather.gson.Weather;
import cn.itcast.coolweather.util.HttpUtil;
import cn.itcast.coolweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class AutoUpdateService extends Service {

    private ArrayList<Bing> bings=new ArrayList<>();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        //看不懂
        AlarmManager alarmManager=(AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour=8*60*60*1000;//这是8小时的毫秒数
        long triggerAtTime= SystemClock.elapsedRealtime()+anHour;
        Intent i=new Intent(this,AutoUpdateService.class);
        PendingIntent pi=PendingIntent.getService(this,0,i,0);
        alarmManager.cancel(pi);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent, flags, startId);
    }

    //更新必应每日一图
    private void updateBingPic() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences.Editor editor=PreferenceManager.
                        getDefaultSharedPreferences(AutoUpdateService.this).edit();
                try {
                    String url="https://cn.bing.com";
                    Document document= Jsoup.connect(url).get();
                    if(document!=null){
                        getBingImg(document);
                    }
                    editor.putString("bing_pic",bings.get(0).getImg());
                    editor.apply();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //更新天气信息
    private void updateWeather() {
        SharedPreferences pref= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=pref.getString("weather",null);
        if(weatherString!=null){
            //有缓存时直接解析天气数据
            Weather weather= Utility.handleWeatherResponse(weatherString);
            String weatherId=weather.basic.weatherId;

            String weatherUrl="http://guolin.tech/api/weather?cityid="+weatherId+"&key=326d842ee2cd4f7eb58b5885e828caf5";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText=response.body().string();
                    Weather weather=Utility.handleWeatherResponse(responseText);
                    if(weather!=null&&"ok".equals(weather.status)){
                        SharedPreferences.Editor editor=PreferenceManager.
                                getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather",responseText);
                        editor.apply();
                    }
                }
            });
        }
    }

    public void getBingImg(Document document) {
        String input = document + "";
        Bing bing = new Bing();
        Utility.handleBingResponse(input, bing);
        bings.add(bing);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}