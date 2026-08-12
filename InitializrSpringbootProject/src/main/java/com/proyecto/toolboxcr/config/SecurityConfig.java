package com.proyecto.toolboxcr.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler,
                          CustomAuthenticationFailureHandler failureHandler) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    public static final String[] PUBLIC_URLS = {
        "/", "/index", "/login", "/registro", "/catalogo/**",
        "/css/**", "/js/**", "/webjars/**", "/acceso_denegado"
    };

    public static final String[] CLIENTE_URLS = {
        "/carrito/**", "/perfil/**", "/pedidos/**", "/pago/**"
    };

    public static final String[] VENDEDOR_URLS = {
        "/producto/listado", "/inventario/**"
    };

    public static final String[] ADMIN_URLS = {
        "/producto/**", "/usuario_rol/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(request -> request
                .requestMatchers(PUBLIC_URLS).permitAll()
                .requestMatchers(CLIENTE_URLS).hasAnyRole("CLIENTE", "BODEGA", "ADMINISTRADOR", "DUEÑO")
                .requestMatchers(VENDEDOR_URLS).hasAnyRole("BODEGA", "ADMINISTRADOR", "DUEÑO")
                .requestMatchers(ADMIN_URLS).hasAnyRole("ADMINISTRADOR", "DUEÑO")
                .anyRequest().authenticated()
        );

        http.formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("correo")
                .passwordParameter("contrasena")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
        );

        http.logout(logout -> logout
                .logoutRequestMatcher(org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );

        http.exceptionHandling(ex -> ex.accessDeniedPage("/acceso_denegado"));

        http.sessionManagement(ses -> ses
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
        );

        return http.build();
    }

    @Autowired
    public void configurerGlobal(AuthenticationManagerBuilder build,
                                 @Lazy PasswordEncoder passwordEncoder,
                                 @Lazy UserDetailsService userDetailsService) throws Exception {
        build.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }
}
