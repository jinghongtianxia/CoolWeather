package com.example.adminibm.coolweather.activity;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adminibm.coolweather.R;
import com.example.adminibm.coolweather.model.City;
import com.example.adminibm.coolweather.model.CoolWeatherDB;
import com.example.adminibm.coolweather.model.County;
import com.example.adminibm.coolweather.model.Province;
import com.example.adminibm.coolweather.util.HttpCallbackListener;
import com.example.adminibm.coolweather.util.HttpUtil;
import com.example.adminibm.coolweather.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ADMINIBM on 2016/3/27.
 */
public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<String>();
    /**
     * ʡ�б�
     */
    private List<Province> provinceList;
    /**
     * ���б�
     */
    private List<City> cityList;
    /**
     * ���б�
     */
    private List<County> countyList;
    /**
     * ѡ�е�ʡ��
     */
    private Province selectedProvince;
    /**
     * ѡ�еĳ���
     */
    private City selectedCity;
    /**
     * ��ǰѡ�еļ���
     */
    private int currentLevel;

    private boolean isFromWeatherAcityty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFromWeatherAcityty=getIntent().getBooleanExtra("from_weather_activity",false);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("city_selected",false)&&!isFromWeatherAcityty){
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        listView = (ListView) findViewById(R.id.list_view);
        titleText = (TextView) findViewById(R.id.title_text);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int index, long arg3) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(index);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(index);
                    queryCounties();
                }else if (currentLevel==LEVEL_COUNTY){
                    String countyCode=countyList.get(index).getCountyCode();
                    Intent intent=new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();//����ʡ������
    }

    private void queryProvinces() {
        provinceList = coolWeatherDB.loadProvinces();
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("�й�");
            currentLevel = LEVEL_PROVINCE;
        } else {
            queryFromServer(null, "province");
        }
    }
    /**
     * ��ѯѡ��ʡ�����е��У����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ��
     */
    private void queryCities() {
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        } else {
            queryFromServer(selectedProvince.getProvinceCode(), "city");
        }
    }
    /**
     * ��ѯѡ���������е��أ����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ��
     */
    private void queryCounties() {
        /*
      ���б�
     */
        List<County> countyList = coolWeatherDB.loadCounties(selectedCity.getId());
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        } else {
            queryFromServer(selectedCity.getCityCode(), "county");
        }
    }
/**
 * ���ݴ���Ĵ��ź����ʹӷ������ϲ�ѯʡ�������ݡ�
 * */
private void queryFromServer(final String code, final String type) {
    String address;
    if (!TextUtils.isEmpty(code)) {
        address = "http://www.weather.com.cn/data/list3/city" + code +
                ".xml";
    } else {
        address = "http://www.weather.com.cn/data/list3/city.xml";
    }
    showProgressDialog();
    HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
        @Override
        public void onFinish(String response) {
            boolean result = false;
            if ("province".equals(type)) {
                result = Utility.handleProvincesResponse(coolWeatherDB,
                        response);
            } else if ("city".equals(type)) {
                result = Utility.handleCitiesResponse(coolWeatherDB,
                        response, selectedProvince.getId());
            } else if ("county".equals(type)) {
                result = Utility.handleCountiesResponse(coolWeatherDB,
                        response, selectedCity.getId());
            }
            if (result) {
// ͨ��runOnUiThread()�����ص����̴߳����߼�
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        if ("province".equals(type)) {
                            queryProvinces();
                        } else if ("city".equals(type)) {
                            queryCities();
                        } else if ("county".equals(type)) {
                            queryCounties();
                        }
                    }
                });
            }
        }
        @Override
        public void onError(Exception e) {
// ͨ��runOnUiThread()�����ص����̴߳����߼�
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    closeProgressDialog();
                    Toast.makeText(ChooseAreaActivity.this,
                            "����ʧ��", Toast.LENGTH_SHORT).show();
                }
            });
        }
    });
}
    /**
     * ��ʾ���ȶԻ���
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("���ڼ���...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    /**
     * �رս��ȶԻ���
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
/**
 * ����Back���������ݵ�ǰ�ļ������жϣ���ʱӦ�÷������б�ʡ�б�����ֱ���˳���
 * */
@Override
public void onBackPressed() {
    if (currentLevel == LEVEL_COUNTY) {
        queryCities();
    } else if (currentLevel == LEVEL_CITY) {
        queryProvinces();
    } else {
        if (isFromWeatherAcityty){
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);
        }
        finish();
    }
}
}


