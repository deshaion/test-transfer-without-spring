package com.deshaion.transfertest.model;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
@AllArgsConstructor
@ToString
public class Transfer {
    private String requestId;
    private String sourceAccountId;
    private String targetAccountId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime created;

    public Transfer(JsonObject json) {
        requestId = json.getString("REQUESTID");
        sourceAccountId = json.getString("SOURCEACCOUNTID");
        targetAccountId = json.getString("TARGETACCOUNTID");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        String createdString = json.getString("CREATED");
        if (createdString != null) {
            created = LocalDateTime.parse(createdString, dateTimeFormatter);
        }

        Object amountObject = json.getValue("AMOUNT");
        if (amountObject != null) {
            amount = new BigDecimal(String.valueOf(amountObject));
        }
        currency = json.getString("CURRENCY");
    }
}
