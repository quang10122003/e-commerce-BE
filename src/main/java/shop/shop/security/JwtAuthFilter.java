package shop.shop.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import shop.shop.common.error.ErrorCode;
import shop.shop.config.RestAuthenticationEntryPoint;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;
    private final UserDetailsServiceCustom userDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    // Nếu endpoint thuộc nhóm public thì không cần chạy JWT filter.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/auth/login".equals(servletPath)
                || "/api/auth/signup".equals(servletPath)
                || "/api/auth/refresh-token".equals(servletPath)
                || "/api/auth/validate-token".equals(servletPath)
                ||"/api/auth/forgot-password".equals(servletPath)
                ||"/api/auth/reset-password".equals(servletPath)
                ||"/api/payments/sepay/wedhook".equals(servletPath);
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Neu khong co Bearer token thi cho request di tiep de phan phan quyen xu ly.
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        // Chỉ catch đúng các lỗi xác thực JWT/user.
        // Mọi lỗi hệ thống khác (SQL, NPE, v.v.) phải được rethrow để
        // GlobalExceptionHandler xử lý đúng, tránh bị nuốt thành UNAUTHORIZED.
        try {
            authenticateRequest(request, response, token);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Token sai, token hết hạn hoặc user trong token không còn tồn tại
            // → xóa context để Spring Security tự trả 401 cho API private.
            SecurityContextHolder.clearContext();
        }
        // Không catch RuntimeException tổng quát ở đây.
        // Lỗi hệ thống sẽ propagate lên DispatcherServlet → GlobalExceptionHandler.

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(HttpServletRequest request,
            HttpServletResponse response,
            String token) throws IOException, ServletException {

        // Giải mã token và lấy email. Nếu bước này lỗi thì đó là lỗi xác thực.
        String email = authUtil.extractEmail(token);

        if (email == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        // Lay user tu database de tao Authentication cho request hien tai.
        // Nếu DB lỗi ở đây (ví dụ SQL sai), exception sẽ được rethrow ra ngoài
        // doFilterInternal và được GlobalExceptionHandler xử lý thành 500.
        UserDetails user = userDetailsService.loadUserByUsername(email);

        // Nếu tài khoản bị khóa thì trả lỗi xác thực ngay.
        if (!user.isAccountNonLocked()) {
            // Tài khoản bị khóa là lỗi xác thực đúng nghĩa nên mới trả qua
            // RestAuthenticationEntryPoint thay vì để rơi sang lỗi hệ thống.
            SecurityContextHolder.clearContext();
            restAuthenticationEntryPoint.commence(
                    request,
                    response,
                    new LockedException(ErrorCode.ACCOUNT_LOCKED.getMessage()));
            return;
        }

        if (authUtil.isAccessTokenValid(token, user)) {
            // Tao doi tuong Authentication de Spring Security hieu request da dang nhap.
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user.getAuthorities());

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }
}
