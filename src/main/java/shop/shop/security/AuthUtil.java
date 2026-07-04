package shop.shop.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import shop.shop.user.entity.User;

@Component
public class AuthUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;
    private final long wsTicketExpirationMillis;
    private final long wsTicketExpirationSeconds;

    public AuthUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-minutes}") long accessTokenExpirationMinutes,
            @Value("${app.jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays,
            @Value("${app.jwt.ws-ticket-expiration-seconds:60}") long wsTicketExpirationSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = Duration.ofMinutes(accessTokenExpirationMinutes).toMillis();
        this.refreshTokenExpirationMillis = Duration.ofDays(refreshTokenExpirationDays).toMillis();
        this.wsTicketExpirationSeconds = wsTicketExpirationSeconds;
        this.wsTicketExpirationMillis = Duration.ofSeconds(wsTicketExpirationSeconds).toMillis();
    }

    private SecretKey getKey() {
        return secretKey;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, "access", accessTokenExpirationMillis);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, "refresh", refreshTokenExpirationMillis);
    }

    public String generateWsTicket(User user) {
        return generateToken(user, "ws-ticket", wsTicketExpirationMillis);
    }

    public long getWsTicketExpirationSeconds() {
        return wsTicketExpirationSeconds;
    }

    private String generateToken(User user, String tokenType, long expirationMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claims(Map.of(
                        "userId", user.getId(),
                        "role", user.getRoleName(),
                        "fullName", user.getFullName(),
                        "tokenType", tokenType))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(getKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get("tokenType", String.class);
    }

    // Kiểm tra access token.
    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, "access");
    }

    // Kiểm tra refresh token.
    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, "refresh");
    }

    // Kiểm tra ticket ngắn hạn chỉ dùng cho WebSocket CONNECT.
    public boolean isWsTicketValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, "ws-ticket");
    }

    private boolean isTokenValid(String token, UserDetails userDetails, String expectedTokenType) {
        final String email = extractEmail(token);
        final String tokenType = extractTokenType(token);
        return email.equals(userDetails.getUsername())
                && expectedTokenType.equals(tokenType)
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // Giải token.
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
