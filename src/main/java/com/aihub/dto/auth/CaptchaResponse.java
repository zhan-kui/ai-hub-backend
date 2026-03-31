package com.aihub.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    private String captchaId;

    private String imageBase64;

    private Long expireIn;
}