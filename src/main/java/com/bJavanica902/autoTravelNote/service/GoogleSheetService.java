package com.bJavanica902.autoTravelNote.service;

import com.bJavanica902.autoTravelNote.entity.Note;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@Log4j2
public class GoogleSheetService {

    @Value("${google.spreadsheet.id}")
    private String spreadSheetId;
    @Value("${google.credentials.file}")
    private String credentialsFile;
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String APPLICATION_NAME = "AutoTravelNote";

    public boolean saveToGoogle(Note note) {
        boolean result = false;
        String range = note.getArea() + "!A1:E";

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Sheets service = getSheetsService();
                ValueRange responses = service.spreadsheets().values().get(spreadSheetId, range).execute();
                List<List<Object>> values = responses.getValues();

                // 使用日期時間格式化器將 LocalDateTime 轉換為字串
                String formattedDateTime = note.getDateTime().format(DATE_TIME_FORMATTER);

                // 準備要插入的新資料
                List<Object> newRow = Arrays.asList(
                        formattedDateTime, // 日期時間轉為字串
                        note.getCate(), // 分類
                        note.getTag(), // 標籤
                        note.getUrl(), // 網址
                        note.getLineId() // LineId
                );

                // 決定插入的行號
                int rowIndex = (values != null && !values.isEmpty()) ? values.size() + 1 : 2;

                // 插入新資料
                ValueRange body = new ValueRange()
                        .setValues(Arrays.asList(newRow));
                String insertRange = note.getArea() + "!A" + rowIndex + ":E" + rowIndex; // 使用 UTF-8 編碼的工作表名稱
                service.spreadsheets().values()
                        .update(spreadSheetId, insertRange, body)
                        .setValueInputOption("RAW")
                        .execute();

                log.info("Success, " + note.toString());
                result = true;
                break; // 成功後退出重試循環
            } catch (Exception e) {
                log.error("Failed attempt " + (attempt + 1) + ", " + e.getMessage());
                try {
                    Thread.sleep(30000); // 等待30秒後重試
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // 恢復中斷狀態
                }
            }
        }

        return result;
    }

    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        // 讀取憑證
        InputStream credentialsStream = GoogleSheetService.class.getClassLoader().getResourceAsStream(credentialsFile);

        // 建立 GoogleCredentials
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(Arrays.asList("https://www.googleapis.com/auth/spreadsheets"));

        // 使用 GoogleNetHttpTransport 和 GsonFactory 來建立 Sheets 服務
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

}
