package com.sportcourt.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class GenerarPasswordTest {

    @Test
    void generarHash() {
        String password = "carlospacheco";
        String hash = new BCryptPasswordEncoder(10).encode(password);

        System.out.println("====================================");
        System.out.println("HASH BCrypt:");
        System.out.println(hash);
        System.out.println("====================================");
    }
}