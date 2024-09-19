package com.bJavanica902.autoTravelNote.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class Note {

    private LocalDateTime dateTime;
    private String area;
    private String cate;
    private String tag;
    private String url;
    private String lineId;

    @Override
    public String toString() {
        return "Note{" +
                "dateTime=" + dateTime +
                ", area='" + area + '\'' +
                ", cate='" + cate + '\'' +
                ", tag='" + tag + '\'' +
                ", url='" + url + '\'' +
                ", lineId='" + lineId + '\'' +
                '}';
    }
}
