package com.company.DAO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Value {
    /**
     * not nul for put response
     */
    private byte[] bytesOfFile;
    private Long lastModifiedTime;
    private Status status;

    public void updateValue(byte[] bytesOfFile, Long lastModifiedTime, Status status) {
        this.bytesOfFile = bytesOfFile;
        this.lastModifiedTime = lastModifiedTime;
        this.status = status;
    }

    public enum Status {
        DELETED("deleted"),
        EXISTING("existing"),
        ABSENT("absent");

        private final String value;

        Status(String value) {
            this.value = value;
        }
    }

}


