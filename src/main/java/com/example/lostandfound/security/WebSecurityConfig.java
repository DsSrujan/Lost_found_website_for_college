package com.example.lostandfound.security;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain; // <<< THE FIX IS HERE (Reverted to javax)

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery("select username, password, true as enabled from users where username=?");
        manager.setAuthoritiesByUsernameQuery("select username, 'ROLE_USER' as authority from users where username=?");
        manager.setCreateUserSql("insert into users (username, password, email, created_at) values (?,?,?,?)");
        return manager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/register", "/login", "/css/**", "/js/**", "/", "/uploads/**", "/search", "/h2-console/**").permitAll() // Public pages
                .requestMatchers("/profile", "/my-items").authenticated() // Protected pages
                .anyRequest().authenticated() // All other pages require login
            )
            .formLogin(form -> form
                .loginPage("/login") // Custom login page URL
                .defaultSuccessUrl("/", true) // Redirect to homepage on successful login
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout") // Redirect to login page on logout
                .permitAll()
            );

        // Allow H2 console and public registration, disable CSRF for them
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/register")
        );
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
}