package benz.service;

import benz.dao.OilPriceNymexRepos;
import benz.dao.XiCheBaoWeatherRepos;
import common.Constant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import utils.HttpClientUtils;
import utils.ResourceUtils;
import utils.Utils;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

@Component
public class TimedTask {
    private static final Logger logger = LoggerFactory.getLogger(TimedTask.class);

    @Autowired
    public XiCheBaoWeatherRepos xiCheBaoWeatherRepos;



    //每30分钟执行一次
    @Scheduled(cron = "0 */30 *  * * * ")
    @Profile({"testing", "production"})
    public void testWeather() {
        Map<Integer, Integer> map = ResourceUtils.WEATHER_CSV;
        Iterator<Map.Entry<Integer, Integer>> iterable = map.entrySet().iterator();
        while (iterable.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterable.next();
            entry.getValue();
            //获取KEY VALUE
            int cityCode = entry.getValue();
            int region_id = entry.getKey();
            Calendar a = Calendar.getInstance();
            int year = a.get(Calendar.YEAR);
            int month = a.get(Calendar.MONTH) + 1;
            String t = Utils.getSDF(Constant.SDF_3).format(new Date());
            String url = "";
            if (month <= 9) {
                url = "http://tianqi.2345.com/t/wea_history/js/" + year + "0" + month + "/" + cityCode + "_" + year + "0" + month + ".js";
            } else {
                url = "http://tianqi.2345.com/t/wea_history/js/" + year + month + "/" + cityCode + "_" + year + month + ".js";
            }
            String res = Utils.TO_GB(HttpClientUtils.doGet(url));
            String json_res = res.substring(res.indexOf("=") + 1, res.indexOf(";"))
                    .replace("\'", "\"").replace(":", "\":")
                    .replace("{", "{\"").replace(",", ",\"")
                    .replace(",\"{", ",{").replace("{\"}", "{}");
            JSONObject jsonObject = new JSONObject(json_res);
            JSONArray jsonArray = jsonObject.getJSONArray("tqInfo");
            String city_name = jsonObject.getString("city");
            for (int i = 0; i < jsonArray.length(); i++) {
                if (jsonArray.getJSONObject(i).has("ymd")) {
                    String min_temp = jsonArray.getJSONObject(i).getString("bWendu").substring(0, jsonArray.getJSONObject(i).getString("bWendu").length() - 1);
                    String max_temp = jsonArray.getJSONObject(i).getString("yWendu").substring(0, jsonArray.getJSONObject(i).getString("yWendu").length() - 1);
                    Integer air_quality = 0;
                    air_quality = jsonArray.getJSONObject(i).has("aqi") ? jsonArray.getJSONObject(i).getInt("aqi") : 0;
                    Integer air_quality_level = 0;
                    air_quality_level = jsonArray.getJSONObject(i).has("aqiLevel") ? jsonArray.getJSONObject(i).getInt("aqiLevel") : 0;
                    String air_quality_desc = "";
                    air_quality_desc = jsonArray.getJSONObject(i).has("aqiInfo") ? jsonArray.getJSONObject(i).getString("aqiInfo") : "";
                    xiCheBaoWeatherRepos.insertOrder(jsonArray.getJSONObject(i).getString("ymd"), city_name, region_id, jsonArray.getJSONObject(i).getString("tianqi"), min_temp, max_temp, jsonArray.getJSONObject(i).getString("fengli"), jsonArray.getJSONObject(i).getString("fengxiang"), air_quality, air_quality_level, air_quality_desc);
                }
            }
        }

    }

}