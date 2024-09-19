package com.bJavanica902.autoTravelNote.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
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

    Map<String, List<String>> countries = new HashMap<>();

    @PostConstruct
    public void init() {
        // 添加都道府縣及其下轄市
        countries.put("北海道", Arrays.asList("札幌", "函館", "小樽", "旭川", "釧路", "帶廣", "北見", "室蘭"));
        countries.put("青森", Arrays.asList("青森", "弘前", "八戶"));
        countries.put("岩手", Arrays.asList("盛岡", "一關", "花卷"));
        countries.put("宮城", Arrays.asList("仙台", "石卷", "鹽竈"));
        countries.put("秋田", Arrays.asList("秋田", "橫手", "大曲"));
        countries.put("山形", Arrays.asList("山形", "米澤", "鶴岡"));
        countries.put("福島", Arrays.asList("福島", "郡山", "磐城"));
        countries.put("茨城", Arrays.asList("水戶", "土浦", "常總"));
        countries.put("栃木", Arrays.asList("宇都宮", "小山", "足利"));
        countries.put("群馬", Arrays.asList("前橋", "高崎", "澀川"));
        countries.put("埼玉", Arrays.asList("埼玉", "川越", "越谷"));
        countries.put("千葉", Arrays.asList("千葉", "船橋", "松戶"));
        countries.put("東京", Arrays.asList("東京"));
        countries.put("神奈川", Arrays.asList("橫濱", "川崎", "相模原"));
        countries.put("新潟", Arrays.asList("新潟", "長岡", "上越"));
        countries.put("富山", Arrays.asList("富山", "高岡"));
        countries.put("石川", Arrays.asList("金澤", "小松", "七尾"));
        countries.put("福井", Arrays.asList("福井", "敦賀"));
        countries.put("山梨", Arrays.asList("甲府", "富士吉田"));
        countries.put("長野", Arrays.asList("長野", "松本", "上田"));
        countries.put("岐阜", Arrays.asList("岐阜", "大垣", "高山"));
        countries.put("靜岡", Arrays.asList("靜岡", "濱松", "沼津"));
        countries.put("愛知", Arrays.asList("名古屋", "豐橋", "岡崎"));
        countries.put("三重", Arrays.asList("津", "四日市", "松阪"));
        countries.put("滋賀", Arrays.asList("大津", "彦根"));
        countries.put("京都", Arrays.asList("京都"));
        countries.put("大阪", Arrays.asList("大阪"));
        countries.put("兵庫", Arrays.asList("神戶", "姫路", "尼崎"));
        countries.put("奈良", Arrays.asList("奈良", "大和郡山"));
        countries.put("和歌山", Arrays.asList("和歌山", "橋本"));
        countries.put("鳥取", Arrays.asList("鳥取", "米子"));
        countries.put("島根", Arrays.asList("松江", "出雲"));
        countries.put("岡山", Arrays.asList("岡山", "倉敷"));
        countries.put("廣島", Arrays.asList("廣島", "福山"));
        countries.put("山口", Arrays.asList("山口", "下關"));
        countries.put("德島", Arrays.asList("德島", "鳴門"));
        countries.put("香川", Arrays.asList("高松", "丸龜"));
        countries.put("愛媛", Arrays.asList("松山", "今治"));
        countries.put("高知", Arrays.asList("高知", "南國"));
        countries.put("福岡", Arrays.asList("福岡", "北九州"));
        countries.put("佐賀", Arrays.asList("佐賀", "唐津"));
        countries.put("長崎", Arrays.asList("長崎", "佐世保"));
        countries.put("熊本", Arrays.asList("熊本", "八代"));
        countries.put("大分", Arrays.asList("大分", "別府"));
        countries.put("宮崎", Arrays.asList("宮崎", "延岡"));
        countries.put("鹿兒島", Arrays.asList("鹿兒島", "奄美"));
        countries.put("沖繩", Arrays.asList("那覇", "石垣"));

        log.info("init countries map successfully");
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

        String result = "0";

        switch (event.getJSONObject("message").getString("type")) {
            case "text":
                String msg = event.getJSONObject("message").getString("text");
                log.info(msg);
                if (msg.startsWith("https")) {
                    //text(event.getString("replyToken"), REPLY, "請輸入都道府縣及分類，例如沖繩-吃。先是地點，後是分類，以-分開。");
                    result = "1";
                } else if (msg.contains("/") && containsCity(msg)) {
                    result = "2_" + findPrefecture(msg);
                } else {
                    text(event.getString("replyToken"), REPLY, "請依規定輸入訊息");
                }
                break;
            default:
                text(event.getString("replyToken"), REPLY, "僅接收文字訊息");
                break;
        }

        return result;
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

    private boolean containsCity(String inputs) {
        String[] input = inputs.split("/");
        String city = input[0];

        // 判斷是否為都道府縣
        if (countries.containsKey(city)) {
            return true;
        }
        // 判斷是否為下轄市
        for (List<String> cities : countries.values()) {
            if (cities.contains(city)) {
                return true;
            }
        }
        return false;
    }

    private String findPrefecture(String inputs) {
        String[] input = inputs.split("/");
        String name = input[0];

        // 如果傳入的是都道府縣名稱
        if (countries.containsKey(name)) {
            return name;
        }
        // 如果傳入的是城市名稱，尋找對應的都道府縣
        for (Map.Entry<String, List<String>> entry : countries.entrySet()) {
            String prefecture = entry.getKey();
            List<String> cities = entry.getValue();
            if (cities.contains(name)) {
                return prefecture;
            }
        }
        return ""; // 如果找不到對應的都道府縣，返回空字串
    }
}
