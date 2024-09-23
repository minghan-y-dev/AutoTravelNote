package com.bJavanica902.autoTravelNote.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Log4j2
public class LineService {

    private static final String REPLY = "reply";
    @Value("${line.bot.channel-token}")
    private String lineToken;

    Map<String, List<String>> jpCountries = new HashMap<>();
    Map<String, List<String>> twCountries = new HashMap<>();
    Map<String, String> reverseMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 添加都道府縣及其下轄市
        jpCountries.put("北海道", Arrays.asList("札幌", "函館", "小樽", "旭川", "釧路", "帶廣", "北見", "室蘭"));
        jpCountries.put("青森", Arrays.asList("青森", "弘前", "八戶"));
        jpCountries.put("岩手", Arrays.asList("盛岡", "一關", "花卷"));
        jpCountries.put("宮城", Arrays.asList("仙台", "石卷", "鹽竈"));
        jpCountries.put("秋田", Arrays.asList("秋田", "橫手", "大曲"));
        jpCountries.put("山形", Arrays.asList("山形", "米澤", "鶴岡"));
        jpCountries.put("福島", Arrays.asList("福島", "郡山", "磐城"));
        jpCountries.put("茨城", Arrays.asList("水戶", "土浦", "常總"));
        jpCountries.put("栃木", Arrays.asList("宇都宮", "小山", "足利"));
        jpCountries.put("群馬", Arrays.asList("前橋", "高崎", "澀川"));
        jpCountries.put("埼玉", Arrays.asList("埼玉", "川越", "越谷"));
        jpCountries.put("千葉", Arrays.asList("千葉", "船橋", "松戶"));
        jpCountries.put("東京", Arrays.asList("東京"));
        jpCountries.put("神奈川", Arrays.asList("橫濱", "川崎", "相模原"));
        jpCountries.put("新潟", Arrays.asList("新潟", "長岡", "上越"));
        jpCountries.put("富山", Arrays.asList("富山", "高岡"));
        jpCountries.put("石川", Arrays.asList("金澤", "小松", "七尾"));
        jpCountries.put("福井", Arrays.asList("福井", "敦賀"));
        jpCountries.put("山梨", Arrays.asList("甲府", "富士吉田"));
        jpCountries.put("長野", Arrays.asList("長野", "松本", "上田"));
        jpCountries.put("岐阜", Arrays.asList("岐阜", "大垣", "高山"));
        jpCountries.put("靜岡", Arrays.asList("靜岡", "濱松", "沼津"));
        jpCountries.put("愛知", Arrays.asList("名古屋", "豐橋", "岡崎"));
        jpCountries.put("三重", Arrays.asList("津", "四日市", "松阪"));
        jpCountries.put("滋賀", Arrays.asList("大津", "彦根"));
        jpCountries.put("京都", Arrays.asList("京都"));
        jpCountries.put("大阪", Arrays.asList("大阪"));
        jpCountries.put("兵庫", Arrays.asList("神戶", "姫路", "尼崎"));
        jpCountries.put("奈良", Arrays.asList("奈良", "大和郡山"));
        jpCountries.put("和歌山", Arrays.asList("和歌山", "橋本"));
        jpCountries.put("鳥取", Arrays.asList("鳥取", "米子"));
        jpCountries.put("島根", Arrays.asList("松江", "出雲"));
        jpCountries.put("岡山", Arrays.asList("岡山", "倉敷"));
        jpCountries.put("廣島", Arrays.asList("廣島", "福山"));
        jpCountries.put("山口", Arrays.asList("山口", "下關"));
        jpCountries.put("德島", Arrays.asList("德島", "鳴門"));
        jpCountries.put("香川", Arrays.asList("高松", "丸龜"));
        jpCountries.put("愛媛", Arrays.asList("松山", "今治"));
        jpCountries.put("高知", Arrays.asList("高知", "南國"));
        jpCountries.put("福岡", Arrays.asList("福岡", "北九州"));
        jpCountries.put("佐賀", Arrays.asList("佐賀", "唐津"));
        jpCountries.put("長崎", Arrays.asList("長崎", "佐世保"));
        jpCountries.put("熊本", Arrays.asList("熊本", "八代"));
        jpCountries.put("大分", Arrays.asList("大分", "別府"));
        jpCountries.put("宮崎", Arrays.asList("宮崎", "延岡"));
        jpCountries.put("鹿兒島", Arrays.asList("鹿兒島", "奄美"));
        jpCountries.put("沖繩", Arrays.asList("那覇", "石垣"));

        //添加台灣鄉鎮縣市
        twCountries.put("台北", Arrays.asList("臺北", "中正", "大同", "中山", "松山", "大安", "萬華", "信義", "士林", "北投", "內湖", "南港", "文山"));
        twCountries.put("新北", Arrays.asList("板橋", "三重", "中和", "永和", "新莊", "新店", "樹林", "鶯歌", "三峽", "淡水", "瑞芳", "土城", "蘆洲", "五股", "泰山", "林口", "深坑", "石碇", "坪林", "三芝", "石門", "八里", "平溪", "雙溪", "貢寮", "金山", "萬里", "烏來"));
        twCountries.put("桃園", Arrays.asList("中壢", "平鎮", "龍潭", "楊梅", "新屋", "觀音", "桃園", "龜山", "八德", "大溪", "復興", "大園", "蘆竹"));
        twCountries.put("台中", Arrays.asList("臺中", "中", "東", "南", "西", "北", "北屯", "西屯", "南屯", "太平", "大里", "霧峰", "烏日", "豐原", "后里", "石岡", "東勢", "和平", "新社", "潭子", "大雅", "神岡", "大肚", "沙鹿", "龍井", "梧棲", "清水", "大甲", "外埔", "大安"));
        twCountries.put("台南", Arrays.asList("臺南", "中西", "東", "南", "北", "安平", "安南", "永康", "歸仁", "新化", "左鎮", "玉井", "楠西", "南化", "仁德", "關廟", "龍崎", "官田", "麻豆", "佳里", "西港", "七股", "將軍", "學甲", "北門", "新營", "後壁", "白河", "東山", "六甲", "下營", "柳營", "鹽水", "善化", "大內", "山上", "新市", "安定"));
        twCountries.put("高雄", Arrays.asList("楠梓", "左營", "鼓山", "三民", "鹽埕", "前金", "新興", "苓雅", "前鎮", "旗津", "小港", "鳳山", "林園", "大寮", "大樹", "大社", "仁武", "鳥松", "岡山", "橋頭", "燕巢", "田寮", "阿蓮", "路竹", "湖內", "茄萣", "永安", "彌陀", "梓官", "旗山", "美濃", "六龜", "甲仙", "杉林", "內門", "茂林", "桃源", "那瑪夏"));
        twCountries.put("基隆", Arrays.asList("仁愛", "信義", "中正", "中山", "安樂", "暖暖", "七堵"));
        twCountries.put("新竹", Arrays.asList("新竹", "東", "北", "香山", "竹北", "竹東", "新埔", "關西", "湖口", "新豐", "芎林", "橫山", "北埔", "寶山", "峨眉", "尖石", "五峰"));
        twCountries.put("嘉義", Arrays.asList("嘉義", "東", "西", "番路", "梅山", "竹崎", "阿里山", "中埔", "大埔", "水上", "鹿草", "太保", "朴子", "東石", "六腳", "新港", "民雄", "大林", "溪口", "義竹", "布袋"));
        twCountries.put("苗栗", Arrays.asList("竹南", "頭份", "三灣", "南庄", "獅潭", "後龍", "通霄", "苑裡", "苗栗", "造橋", "頭屋", "公館", "大湖", "泰安", "銅鑼", "三義", "西湖", "卓蘭"));
        twCountries.put("南投", Arrays.asList("南投", "中寮", "草屯", "國姓", "埔里", "仁愛", "名間", "集集", "水里", "魚池", "信義", "竹山", "鹿谷"));
        twCountries.put("雲林", Arrays.asList("斗南", "大埤", "虎尾", "土庫", "褒忠", "東勢", "臺西", "崙背", "麥寮", "斗六", "林內", "古坑", "莿桐", "西螺", "二崙", "北港", "水林", "口湖", "四湖", "元長"));
        twCountries.put("屏東", Arrays.asList("屏東", "三地門", "霧臺", "瑪家", "九如", "里港", "高樹", "鹽埔", "長治", "麟洛", "竹田", "內埔", "萬丹", "潮州", "泰武", "來義", "萬巒", "崁頂", "新埤", "南州", "林邊", "東港", "琉球", "佳冬", "新園", "枋寮", "枋山", "春日", "獅子", "車城", "牡丹", "恆春", "滿州"));
        twCountries.put("宜蘭", Arrays.asList("宜蘭", "頭城", "礁溪", "壯圍", "員山", "羅東", "三星", "大同", "五結", "冬山", "蘇澳", "南澳", "釣魚臺"));
        twCountries.put("花蓮", Arrays.asList("花蓮", "新城", "秀林", "吉安", "壽豐", "鳳林", "光復", "豐濱", "瑞穗", "萬榮", "玉里", "卓溪", "富里"));
        twCountries.put("台東", Arrays.asList("臺東", "綠島", "蘭嶼", "延平", "卑南", "鹿野", "關山", "海端", "池上", "東河", "成功", "長濱", "太麻里", "金峰", "大武", "達仁"));
        twCountries.put("澎湖", Arrays.asList("馬公", "西嶼", "望安", "七美", "白沙", "湖西"));
        twCountries.put("金門", Arrays.asList("金沙", "金湖", "金寧", "金城", "烈嶼", "烏坵"));
        twCountries.put("連江", Arrays.asList("馬祖", "南竿", "北竿", "莒光", "東引"));

        createReverseMap(jpCountries, "JP");
        createReverseMap(twCountries, "TW");

        log.info("init country maps successfully");
    }


    private OkHttpClient client = new OkHttpClient();

    public String crapUserId(JSONObject event) {
        String userId = event.getJSONObject("source").getString("userId");
        if (userId.length() >= 10) {
            String result = userId.substring(0, 5) + "-" + userId.substring(userId.length() - 5);
            return result;
        } else {
            return userId;
        }
    }

    public String processIncomingText(JSONObject event) {

        switch (event.getJSONObject("message").getString("type")) {
            case "text":
                String msg = event.getJSONObject("message").getString("text");
                log.info(msg);
                if (msg.startsWith("https")) {
                    return "1";
                }
                if (msg.contains("/")) {
                    String locate = findLocation(msg);
                    if(StringUtils.isNoneBlank(locate)) return "2_" + locate;
                }
                text(event.getString("replyToken"), REPLY, "請依規定輸入訊息");
                break;
            default:
                text(event.getString("replyToken"), REPLY, "僅接收文字訊息");
                break;
        }

        return "0";
    }


    public void text(String receipient, String type, String text) {
        JSONObject body = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("type", "text");
        message.put("text", text);
        messages.put(message);

        if ("reply".equals(type)) {
            body.put("replyToken", receipient);
        } else if ("push".equals(type)) {
            body.put("to", receipient);
        } else if ("muti".equals(type)) {
            String[] receipients = receipient.split(",");
            body.put("to", receipients);
        }

        body.put("messages", messages);
        sendLinePlatform(body, type);
    }

    private void sendLinePlatform(JSONObject json, String type) {

        String url = null;
        if ("reply".equals(type)) {
            url = "https://api.line.me/v2/bot/message/reply";
        } else if ("push".equals(type)) {
            url = "https://api.line.me/v2/bot/message/push";
        } else if ("muti".equals(type)) {
            url = "https://api.line.me/v2/bot/message/multicast";
        }

        Request request = new Request.Builder().url(url)
                .header("Authorization", "Bearer {" + lineToken + "}")
                .post(RequestBody.Companion.create(json.toString(), MediaType.parse("application/json; charset=utf-8"))).build();
        client.newCall(request).enqueue((Callback) new Callback() {

            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }

            public void onFailure(Call call, IOException e) {
                log.error(e);
            }
        });
    }

//    private boolean containsCity(String inputs) {
//        String[] input = inputs.split("/");
//        String city = input[0];
//
//        // 判斷是否為都道府縣
//        if (jpCountries.containsKey(city)) {
//            return true;
//        }
//        // 判斷是否為下轄市
//        for (List<String> cities : jpCountries.values()) {
//            if (cities.contains(city)) {
//                return true;
//            }
//        }
//        return false;
//    }

//    private String findPrefecture(String inputs) {
//        String[] input = inputs.split("/");
//        String name = input[0];
//
//        // 如果傳入的是都道府縣名稱
//        if (jpCountries.containsKey(name)) {
//            return name;
//        }
//        // 如果傳入的是城市名稱，尋找對應的都道府縣
//        for (Map.Entry<String, List<String>> entry : jpCountries.entrySet()) {
//            String prefecture = entry.getKey();
//            List<String> cities = entry.getValue();
//            if (cities.contains(name)) {
//                return prefecture;
//            }
//        }
//        return ""; // 如果找不到對應的都道府縣，返回空字串
//    }

    private void createReverseMap(Map<String, List<String>> countriesMap, String prefix) {
        for (Map.Entry<String, List<String>> entry : countriesMap.entrySet()) {
            String key = entry.getKey();
            reverseMap.put(key, prefix + "_" + key);
            for (String value : entry.getValue()) {
                reverseMap.put(value, prefix + "_" + key);
            }
        }
    }

    private String findLocation(String input) {
       String[] inputs = input.split("/");
       String locate = inputs[0];
       return reverseMap.getOrDefault(locate, "");
    }
}
