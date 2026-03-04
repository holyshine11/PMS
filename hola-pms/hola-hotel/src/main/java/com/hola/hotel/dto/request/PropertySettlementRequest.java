package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PropertySettlementRequest {

    @NotBlank(message = "국가 타입은 필수입니다.")
    @Pattern(regexp = "KR|US", message = "국가 타입은 KR 또는 US만 가능합니다.")
    private String countryType;

    @Size(max = 50, message = "계좌번호는 50자 이하입니다.")
    private String accountNumber;

    @Size(max = 100, message = "은행명은 100자 이하입니다.")
    private String bankName;

    @Size(max = 20, message = "은행코드는 20자 이하입니다.")
    private String bankCode;

    @Size(max = 100, message = "예금주는 100자 이하입니다.")
    private String accountHolder;

    @Size(max = 50, message = "Routing Number는 50자 이하입니다.")
    private String routingNumber;

    @Size(max = 50, message = "SWIFT CODE는 50자 이하입니다.")
    private String swiftCode;

    @Size(max = 10, message = "정산일은 10자 이하입니다.")
    private String settlementDay;

    @Size(max = 500, message = "통장사본 경로는 500자 이하입니다.")
    private String bankBookPath;
}
