package com.aihub.service;

import com.aihub.common.exception.BizException;
import com.aihub.dto.auth.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "aihub:captcha:";
    private static final String CAPTCHA_RATE_PREFIX = "aihub:captcha:rate:";

    private static final long CAPTCHA_EXPIRE_SECONDS = 120L;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60L;
    private static final long RATE_LIMIT_COUNT = 20L;

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final StringRedisTemplate stringRedisTemplate;

    public CaptchaResponse generateImageCaptcha(String clientIp) {
        checkRateLimit(clientIp);

        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode(4);

        BufferedImage image = buildImage(code);
        String base64 = encodeImage(image);

        stringRedisTemplate.opsForValue().set(
                CAPTCHA_KEY_PREFIX + captchaId,
                code,
                Duration.ofSeconds(CAPTCHA_EXPIRE_SECONDS)
        );

        return CaptchaResponse.builder()
                .captchaId(captchaId)
                .imageBase64("data:image/png;base64," + base64)
                .expireIn(CAPTCHA_EXPIRE_SECONDS)
                .build();
    }

    public void verifyAndConsume(String captchaId, String captchaCode) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BizException(400, "验证码不能为空");
        }

        String key = CAPTCHA_KEY_PREFIX + captchaId;
        String savedCode = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(savedCode)) {
            throw new BizException(400, "验证码已失效，请重新获取");
        }

        if (!savedCode.equalsIgnoreCase(captchaCode.trim())) {
            throw new BizException(400, "验证码错误");
        }

        stringRedisTemplate.delete(key);
    }

    private void checkRateLimit(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return;
        }

        String key = CAPTCHA_RATE_PREFIX + normalizeIp(clientIp);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(RATE_LIMIT_WINDOW_SECONDS));
        }
        if (count != null && count > RATE_LIMIT_COUNT) {
            throw new BizException(429, "验证码请求过于频繁，请稍后再试");
        }
    }

    private String normalizeIp(String clientIp) {
        return clientIp.replace(":", "_").replace(" ", "");
    }

    private String randomCode(int length) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private BufferedImage buildImage(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, width, height);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font("Arial", Font.BOLD, 28));

            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < 6; i++) {
                g2.setColor(new Color(random.nextInt(180), random.nextInt(180), random.nextInt(180)));
                int x1 = random.nextInt(width);
                int y1 = random.nextInt(height);
                int x2 = random.nextInt(width);
                int y2 = random.nextInt(height);
                g2.drawLine(x1, y1, x2, y2);
            }

            for (int i = 0; i < code.length(); i++) {
                g2.setColor(new Color(random.nextInt(120), random.nextInt(120), random.nextInt(120)));
                int x = 18 + i * 22;
                int y = 30 + random.nextInt(-3, 4);
                g2.drawString(String.valueOf(code.charAt(i)), x, y);
            }
        } finally {
            g2.dispose();
        }
        return image;
    }

    private String encodeImage(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new BizException(500, "生成验证码失败");
        }
    }
}