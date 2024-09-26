package com.bJavanica902.autoTravelNote.controller;

import com.bJavanica902.autoTravelNote.entity.Note;
import com.bJavanica902.autoTravelNote.service.GoogleSheetService;
import com.bJavanica902.autoTravelNote.service.LineService;
import com.bJavanica902.autoTravelNote.service.NoteService;
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
    private final NoteService noteService;

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

                Note note = noteService.createNote(this.url, lineId, event.getJSONObject("message").getString("text"), result);

                // 從判斷結果取得國別 2_TW_宜蘭
                String[] process = result.split("_");
                String nation = process[1];

                boolean res = googleSheetService.saveToGoogle(note, nation);
                if (!res) {
                    lineService.text(event.getString("replyToken"), "reply", "儲存失敗");
                }

                url = "";
            }
        }
    }


}
