package com.siglocc.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] passwords = {"region123"};
        for (String password : passwords) {
            System.out.println(password + " -> " + encoder.encode(password));
        }
    }
}
