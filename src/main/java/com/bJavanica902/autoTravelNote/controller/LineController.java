package com.bJavanica902.autoTravelNote.controller;

import com.bJavanica902.autoTravelNote.entity.Note;
import com.bJavanica902.autoTravelNote.service.GoogleSheetService;
import com.bJavanica902.autoTravelNote.service.LineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RequestMapping("/linebot")
@RestController
@RequiredArgsConstructor
@Log4j2
public class LineController {

    private final LineService lineService;
    private final GoogleSheetService googleSheetService;

    // Note Attr
    private String url;

    @PostMapping("/test")
    public ResponseEntity test() {
        return new ResponseEntity("Hello LineBot", HttpStatus.OK);
    }

    @PostMapping("/callback")
    public void callback(@RequestBody String message) {
        JSONObject json = new JSONObject(message);
        JSONArray events = json.getJSONArray("events");
        JSONObject event = events.getJSONObject(0);

        String result = lineService.processIncomingText(event);

        if (result.startsWith("1")) {
            url = event.getJSONObject("message").getString("text");
        } else if (result.startsWith("2")) {
            if (StringUtils.isNoneBlank(url)) {
                // 取得lineId
                String lineId = lineService.crapUserId(event);

                // 從msg取得分類/標籤
                String[] input = event.getJSONObject("message").getString("text").split("/");
                String cate = input[1];
                String tag = input[2];

                // 從判斷結果取得國別及區域
                String[] process = result.split("_");
                String nation = process[0];
                String area = process[1];

                Note note = Note.builder().dateTime(LocalDateTime.now()).area(area).cate(cate).tag(tag).url(url).lineId(lineId).build();

                boolean res = googleSheetService.saveToGoogle(note, nation);
                if (!res) {
                    lineService.text(event.getString("replyToken"), "reply", "儲存失敗");
                }

                url = "";
            }
        }
    }


}
