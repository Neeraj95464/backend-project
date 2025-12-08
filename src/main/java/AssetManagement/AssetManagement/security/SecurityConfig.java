package AssetManagement.AssetManagement.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/info").hasAnyRole("USER", "ADMIN", "EXECUTIVE","HR_ADMIN")  // ✅ Only "USER", "ADMIN", "EXECUTIVE" roles can access
                        .requestMatchers("/api/assets/delete/**").hasRole("ADMIN")  // ✅ Only "ADMIN" can delete assets
                        .requestMatchers("/api/assets/**").hasAnyRole("ADMIN","EXECUTIVE")  // ✅ "ADMIN" & "EXECUTIVE" can create assets
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN","HR_ADMIN") // ✅ Only "ADMIN" can manage users
                        .requestMatchers("/api/user-assets/**").hasAnyRole("USER","ADMIN","HR_ADMIN")
                        .requestMatchers("/api/enum/**").hasAnyRole("USER","ADMIN","HR_ADMIN")
                        .requestMatchers("/api/sim-cards/**").hasAnyRole("USER","ADMIN","HR_ADMIN")
                        .requestMatchers("/api/asset-photos/uploads/**").permitAll()
                        .requestMatchers("/api/asset-documents/uploads/**").permitAll()
                        .requestMatchers("/api/feedback/**").permitAll()
                        .anyRequest().authenticated()  // ✅ Any other request must be authenticated
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
