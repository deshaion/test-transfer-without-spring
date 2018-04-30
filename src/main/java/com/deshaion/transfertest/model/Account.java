package com.deshaion.transfertest.model;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Builder
@Getter
@AllArgsConstructor
@Log4j2
@ToString
public class Account {
    private String token;
    private String name;
    private Boolean active;
    private LocalDateTime created;
    private LocalDateTime updated;
    private BigDecimal balance;
    private String currency;

    public Account(JsonObject json) {
        token = json.getString("TOKEN");
        name = json.getString("NAME");
        active = json.getBoolean("ACTIVE");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        String createdString = json.getString("CREATED");
        if (createdString != null) {
            created = LocalDateTime.parse(createdString, dateTimeFormatter);
        }

        String updatedString = json.getString("UPDATED");
        if (updatedString != null) {
            updated = LocalDateTime.parse(updatedString, dateTimeFormatter);
        }

        String balanceString = json.getString("BALANCE");
        if (balanceString != null) {
            balance = new BigDecimal(balanceString);
        }
        currency = json.getString("CURRENCY");
    }
}
