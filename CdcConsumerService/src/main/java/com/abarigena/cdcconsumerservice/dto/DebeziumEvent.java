package com.abarigena.cdcconsumerservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class DebeziumEvent<T> {
    
    @JsonProperty("before")
    private T before;
    
    @JsonProperty("after")
    private T after;
    
    @JsonProperty("source")
    private SourceInfo source;
    
    @JsonProperty("op")
    private String operation; // c=create, u=update, d=delete, r=read
    
    @JsonProperty("ts_ms")
    private Long timestamp;
    
    @JsonProperty("transaction")
    private Object transaction;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SourceInfo {
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("connector")
        private String connector;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("ts_ms")
        private Long timestamp;
        
        @JsonProperty("snapshot")
        private String snapshot;
        
        @JsonProperty("db")
        private String database;
        
        @JsonProperty("schema")
        private String schema;
        
        @JsonProperty("table")
        private String table;
        
        @JsonProperty("txId")
        private Long transactionId;
        
        @JsonProperty("lsn")
        private Long logSequenceNumber;
    }
} 