package uth.edu.appchat.Configs;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import uth.edu.appchat.Repositories.UserRepository;

@Configuration
@EnableMethodSecurity // nếu bạn cần @PreAuthorize ở controller/service
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 10 rounds là ổn cho dev; production có thể 10-12
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repo) {
        return usernameOrEmail -> {
            var u = repo.findByUsername(usernameOrEmail)
                    .or(() -> repo.findByEmail(usernameOrEmail))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getUsername())            // Principal sẽ là username
                    .password(u.getPasswordHash())            // NHỚ: password đã mã hoá BCrypt trong DB
                    .roles("USER")                            // tuỳ vai trò của bạn
                    .build();
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder pe) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(pe);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Bật CSRF và dùng CookieCsrfTokenRepository để Thymeleaf/JS đọc được token từ cookie XSRF-TOKEN
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )

                // Quy tắc truy cập
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/login", "/signup", "/register",
                                "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .anyRequest().authenticated()
                )

                // Không dùng httpBasic (chúng ta dùng form login)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Form login
                .formLogin(form -> form
                        .loginPage("/login")                // GET /login hiển thị trang login custom
                        .loginProcessingUrl("/login")       // POST /login để Spring xử lý xác thực
                        .usernameParameter("username")      // tên input trong <form> (mặc định "username")
                        .passwordParameter("password")      // tên input trong <form> (mặc định "password")
                        .defaultSuccessUrl("/", false)      // về trang trước đó nếu có SavedRequest; đặt true để luôn về "/"
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN") // xoá thêm CSRF cookie nếu muốn
                        .permitAll()
                )

                // Tuỳ chọn: trả JSON 401 nếu ai đó gọi API khi chưa đăng nhập (thay vì redirect)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            if (req.getRequestURI().startsWith("/api/")) {
                                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                res.setContentType("application/json;charset=UTF-8");
                                res.getWriter().write("{\"error\":\"Unauthorized\"}");
                            } else {
                                res.sendRedirect("/login");
                            }
                        })
                );

        return http.build();
    }
}
