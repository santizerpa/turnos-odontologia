package com.turnosodontologia.auth.principal;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DentistUserDetailsService implements UserDetailsService {

    private final DentistRepository dentists;

    public DentistUserDetailsService(DentistRepository dentists) {
        this.dentists = dentists;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        Dentist dentist = dentists.findByEmail(email)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return new DentistPrincipal(
            dentist.getId(),
            dentist.getEmail(),
            dentist.getPasswordHash(),
            dentist.getFirstName(),
            dentist.getLastName()
        );
    }
}
