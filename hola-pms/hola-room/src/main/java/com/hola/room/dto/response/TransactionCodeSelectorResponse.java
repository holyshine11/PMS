package com.hola.room.dto.response;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCodeSelectorResponse {

    private Long id;
    private String transactionCode;
    private String codeNameKo;
    private String revenueCategory;
}
