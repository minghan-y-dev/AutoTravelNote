package com.bJavanica902.autoTravelNote.service;

import com.bJavanica902.autoTravelNote.entity.Note;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
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
import java.util.Collections;
import java.util.List;

@Service
@Log4j2
public class GoogleSheetService {

    @Value("${google.jp.spreadsheet.id}")
    private String jpSpreadSheetId;
    @Value("${google.tw.spreadsheet.id}")
    private String twSpreadSheetId;
    @Value("${google.credentials.file}")
    private String credentialsFile;
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String APPLICATION_NAME = "AutoTravelNote";

    public boolean saveToGoogle(Note note, String nation) {
        boolean result = false;
        String sheetTitle = note.getArea(); // 取得 Sheet 名稱
        String spreadSheetId = "";

        switch (nation) {
            case "JP":
                spreadSheetId = jpSpreadSheetId;
                break;
            case "TW":
                spreadSheetId = twSpreadSheetId;
                break;
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Sheets service = getSheetsService();

                // 使用日期時間格式化器將 LocalDateTime 轉換為字串
                String formattedDateTime = note.getDateTime().format(DATE_TIME_FORMATTER);

                // 準備 CellData 格式的資料（對應 B~F 欄）
                List<CellData> cellDataList = Arrays.asList(
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formattedDateTime)), // B 時間
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(note.getCate())),    // C 分類
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(note.getTag())),     // D 標籤
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(note.getUrl())),     // E 網址
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(note.getLineId()))   // F LineId
                );

                // 插入第二行（index 1，因為 index 從 0 開始）
                Request insertRowRequest = new Request().setInsertDimension(new InsertDimensionRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(getSheetIdByTitle(service, spreadSheetId, sheetTitle)) // 透過 title 找 sheetId
                                .setDimension("ROWS")
                                .setStartIndex(1)
                                .setEndIndex(2))
                        .setInheritFromBefore(false));

                // 寫入資料到第二行 B~F
                Request updateCellsRequest = new Request().setUpdateCells(new UpdateCellsRequest()
                        .setStart(new GridCoordinate()
                                .setSheetId(getSheetIdByTitle(service, spreadSheetId, sheetTitle))
                                .setRowIndex(1)
                                .setColumnIndex(1)) // 從 B 欄開始（A 是 index 0）
                        .setRows(Collections.singletonList(new RowData().setValues(cellDataList)))
                        .setFields("userEnteredValue"));

                // 一起 batchUpdate 執行
                BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                        .setRequests(Arrays.asList(insertRowRequest, updateCellsRequest));

                service.spreadsheets().batchUpdate(spreadSheetId, batchRequest).execute();
                log.info("Save success");
                result = true;
                break;
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


    /*
        在Google Sheet中留儲存紀錄
     */
    public void addLog(Note note, String nation) {
        String sheetTitle = "Log"; // title 名稱
        String spreadSheetId = "";

        switch (nation) {
            case "JP":
                spreadSheetId = jpSpreadSheetId;
                break;
            case "TW":
                spreadSheetId = twSpreadSheetId;
                break;
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Sheets service = getSheetsService();

                // 取得 spreadsheet 所有 sheet
                Spreadsheet spreadsheet = service.spreadsheets().get(spreadSheetId).execute();
                Integer sheetId = null;

                for (Sheet sheet : spreadsheet.getSheets()) {
                    if (sheet.getProperties().getTitle().equals(sheetTitle)) {
                        sheetId = sheet.getProperties().getSheetId();
                        break;
                    }
                }

                if (sheetId == null) {
                    throw new RuntimeException("找不到名稱為 " + sheetTitle + " 的工作表");
                }

                // 準備新資料
                String formattedDateTime = note.getDateTime().format(DATE_TIME_FORMATTER);
                List<CellData> cellDataList = Arrays.asList(
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(formattedDateTime)),
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(note.getArea())),
                        new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(note.getLineId()))
                );

                // 插入第 2 行
                InsertDimensionRequest insertRowRequest = new InsertDimensionRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(sheetId)
                                .setDimension("ROWS")
                                .setStartIndex(1)  // row index = 1 => 第 2 行
                                .setEndIndex(2))
                        .setInheritFromBefore(false);

                // 更新第 2 行資料
                UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest()
                        .setStart(new GridCoordinate()
                                .setSheetId(sheetId)
                                .setRowIndex(1)
                                .setColumnIndex(0))
                        .setRows(Collections.singletonList(new RowData().setValues(cellDataList)))
                        .setFields("userEnteredValue");

                // 打包批次請求
                BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                        .setRequests(Arrays.asList(
                                new Request().setInsertDimension(insertRowRequest),
                                new Request().setUpdateCells(updateCellsRequest)
                        ));

                service.spreadsheets().batchUpdate(spreadSheetId, batchRequest).execute();
                break;

            } catch (Exception e) {
                log.error("Failed attempt " + (attempt + 1) + ", " + e.getMessage());
                try {
                    Thread.sleep(30000); // 等待30秒後重試
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    public void printSheetId(String nation) throws Exception{
        Sheets service = getSheetsService();

        String spreadSheetId = "";
        switch (nation) {
            case "JP":
                spreadSheetId = jpSpreadSheetId;
                break;
            case "TW":
                spreadSheetId = twSpreadSheetId;
                break;
        }

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadSheetId).execute();
        for (Sheet sheet : spreadsheet.getSheets()) {
            System.out.println(sheet.getProperties().getTitle() + " : " + sheet.getProperties().getSheetId());
        }
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

    private Integer getSheetIdByTitle(Sheets service, String spreadSheetId, String sheetTitle) throws IOException {
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadSheetId).execute();
        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheet.getProperties().getTitle().equals(sheetTitle)) {
                return sheet.getProperties().getSheetId();
            }
        }
        throw new IllegalArgumentException("Sheet with title '" + sheetTitle + "' not found");
    }

}
