package com.bJavanica902.autoTravelNote.service;

import com.bJavanica902.autoTravelNote.entity.Note;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Log4j2
public class NoteService {

    public Note createNote(String url, String lineId, String msg, String processResult) {
        // 從msg取得分類/標籤 宜蘭/景點/室內免費考古
        String[] input = msg.split("/");
        String cate = input[1];
        String tag = input[2];

        // 從判斷結果取得國別及區域 2_TW_宜蘭
        String[] process = processResult.split("_");
        String area = process[2];

        Note note = Note.builder().dateTime(LocalDateTime.now()).area(area).cate(cate).tag(tag).url(url).lineId(lineId).build();
        log.info(note.toString());
        return note;
    }
}
