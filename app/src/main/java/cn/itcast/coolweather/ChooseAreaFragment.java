package cn.itcast.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;
import org.litepal.exceptions.DataSupportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.itcast.coolweather.db.City;
import cn.itcast.coolweather.db.County;
import cn.itcast.coolweather.db.Province;
import cn.itcast.coolweather.util.HttpUtil;
import cn.itcast.coolweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList=new ArrayList<>();

    //省列表
    private List<Province> provinceList;

    //市列表
    private List<City> cityList;

    //县列表
    private List<County> countyList;

    //选中的省分
    private Province selectProvince;

    //选中的城市
    private City selectCity;

    //选中的级别
    private int currentLevel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
      View view=inflater.inflate(R.layout.choose_area,container,false);
      titleText=(TextView) view.findViewById(R.id.title_text);
      backButton=(Button) view.findViewById(R.id.back_button);
      backButton.setBackgroundResource(R.mipmap.back);
      listView=(ListView) view.findViewById(R.id.list_view);
      adapter=new ArrayAdapter<String>(getContext(),R.layout.support_simple_spinner_dropdown_item,dataList);
      listView.setAdapter(adapter);
      return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE){
                    selectProvince=provinceList.get(position);
                    Log.e("MainActivity","selectProvince:"+selectProvince.getProvinceName());
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
//                    Log.e("MainActivity","position:"+position);
//                    Log.e("MainActivity","cityListSize:"+cityList.size());
//                    for(City c:cityList){
//                        Log.e("MainActivity",c.getCityName());
//                    }
                    Log.e("MainActivity","position="+position);
                    Log.e("MainActivity","city="+cityList.get(position));
                    selectCity=cityList.get(position);
                    queryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    String weatherId=countyList.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity activity=(WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefreshLayout.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    //查询全国的省份，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= LitePal.findAll(Province.class);
        dataList.clear();
        if(provinceList.size()>0){
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address="http:guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }


    //查询全国的城市，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryCities() {
        titleText.setText(selectProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=LitePal.where("provinceId=?",String.valueOf(selectProvince.getProvinceCode())).find(City.class);
        dataList.clear();
        if(cityList.size()>0){
            for(City city:cityList){
                dataList.add(city.getCityName());
                Log.e("MainActivity:","遍历city="+city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else {
            Log.e("MainActivity: ","selectProvince.getProvinceCode()="+selectProvince.getProvinceCode() );
            String address="http:guolin.tech/api/china/"+selectProvince.getProvinceCode();
            queryFromServer(address,"city");
        }
    }

    //查询全国的县，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryCounties() {
        titleText.setText(selectCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList=LitePal.where("CityId=?",String.valueOf(selectCity.getCityCode())).find(County.class);
        dataList.clear();
        if(countyList.size()>0){
            for (County county:countyList){
                Log.e("MainActivity","county.getCountyName()="+county.getCountyName());
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else {
            String address="http:guolin.tech/api/china/"+selectProvince.getProvinceCode()+"/"+selectCity.getCityCode();
            queryFromServer(address,"county");
        }
    }

    //根据传入的地址和类型从服务器上查询省市县数据
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e("ChooseAreaFragment","onResponse");
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result=Utility.handleCityResponse(responseText,selectProvince.getProvinceCode());
                }else if("county".equals(type)){
                    result=Utility.handleCountyResponse(responseText,selectCity.getCityCode());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnViThread（）方法回到主线程
                Log.e("ChooseAreaFragment","onFailure ");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                        Log.e("ChooseAreaFragment","加载失败");
                    }
                });
            }
        });
    }

    //显示进度对话框
    private void showProgressDialog() {
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            //设置触摸对话框外边界时不能取消对话框
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度对话框
    private void closeProgressDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }
}
