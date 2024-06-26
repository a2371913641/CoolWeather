package cn.itcast.coolweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;

import cn.itcast.coolweather.gson.Bing;
import cn.itcast.coolweather.gson.Forecast;
import cn.itcast.coolweather.gson.Suggestion;
import cn.itcast.coolweather.gson.Weather;
import cn.itcast.coolweather.service.AutoUpdateService;
import cn.itcast.coolweather.util.HttpUtil;
import cn.itcast.coolweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdataTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private String Key="&key=326d842ee2cd4f7eb58b5885e828caf5";

    private ImageView bingPicImg;

    ArrayList<Bing> bings=new ArrayList<>();

    Document document=null;

    public SwipeRefreshLayout swipeRefreshLayout;

    private String mWeatherId;

    public DrawerLayout drawerLayout;

    private Button navButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //去除标题栏
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        //融合背景
//        if(Build.VERSION.SDK_INT>=21){
//            Log.e("WeatherActivity","SDK_INT>=21");
//            View decorView=getWindow().getDecorView();
//            decorView.setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    |View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//            );
//            getWindow().setStatusBarColor(R.layout.activity_weather);
//        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        //初始化各控件
        swipeRefreshLayout=(SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        bingPicImg=(ImageView)findViewById(R.id.bing_pic_img);
        weatherLayout=(ScrollView) findViewById(R.id.weather_layout);
        titleCity=(TextView) findViewById(R.id.title_city);
        titleUpdataTime=(TextView) findViewById(R.id.title_update_time);
        degreeText=(TextView) findViewById(R.id.degree_text);
        weatherInfoText=(TextView) findViewById(R.id.weather_info_text);
        forecastLayout=(LinearLayout) findViewById(R.id.forecast_layout);
        aqiText=(TextView) findViewById(R.id.aqi_text);
        pm25Text=(TextView) findViewById(R.id.pm25_text);
        comfortText=(TextView) findViewById(R.id.comfort_text);
        carWashText=(TextView) findViewById(R.id.car_wash_text);
        sportText=(TextView) findViewById(R.id.sport_text);
        drawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);
        navButton=(Button)findViewById(R.id.nav_button);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=prefs.getString("weather",null);

//        swipeRefreshLayout.setColorSchemeResources(R.color.design_default_color_error);
        if(weatherString!=null){
            //有缓存时直接解析天气数据
            Weather weather= Utility.handleWeatherResponse(weatherString);
            mWeatherId=weather.basic.weatherId;
            showWeatherInfo(weather);
        }else {
            //无缓存时去服务器查询天气m
            mWeatherId=getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        //加载图片
        String bingPic=prefs.getString("bing_pic",null);
        if(bingPic!=null){
            Log.e("WeatherActivity","加载图片");
            Log.e("WeatherActivity","bingPic="+bingPic);
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            loadBingPic();
        }

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    //加载必应每日一图
    private void loadBingPic() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences.Editor editor=PreferenceManager.
                        getDefaultSharedPreferences(WeatherActivity.this).edit();
                try {
                    String url="https://cn.bing.com";
                    document= Jsoup.connect(url).get();
                    if(document!=null){
                        getBingImg(document);
                    }
                    editor.putString("bing_pic",bings.get(0).getImg());
                    editor.apply();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("MainActivity",bings.size()+"size");
                        Log.e("MainActivity","bings.get(1)="+bings.get(0).getImg());
                        Glide.with(WeatherActivity.this).load(bings.get(0).getImg()).into(bingPicImg);
                    }
                });
            }
        }).start();
    }

    //根据天气id请求城市天气信息
    public void requestWeather(String weatherId) {
        String weatherUrl="http://guolin.tech/api/weather?cityid="+weatherId+Key;
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor=PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            mWeatherId=weather.basic.weatherId;
                            Log.e("WeatherActivity","mWeatherId="+mWeatherId+"");
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    //处理并展示Weather实体类中的数据
    private void showWeatherInfo(Weather weather) {
        String cityName=weather.basic.cityName;
        String updateTime=weather.basic.update.updataTime.split(" ")[1];
        String degree=weather.now.tmperature+"℃\n";
        String weatherInfo=weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdataTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast:weather.forecastList){
            View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText=(TextView) view.findViewById(R.id.date_text);
            TextView infoText=(TextView) view.findViewById(R.id.info_text);
            TextView maxText=(TextView) view.findViewById(R.id.max_text);
            TextView minText=(TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if(weather.aqi!=null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort="舒适度："+weather.suggestion.comfort.info;
        String carWash="洗车指数："+weather.suggestion.carWash.info;
        String sport="运动建议："+weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent=new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    public void getBingImg(Document document) {
        String input = document + "";
        Bing bing = new Bing();
        Utility.handleBingResponse(input, bing);
        bings.add(bing);
    }
}