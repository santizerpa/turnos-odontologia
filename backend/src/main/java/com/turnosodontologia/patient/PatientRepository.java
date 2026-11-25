package com.turnosodontologia.patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByDni(String dni);

    // Portable between H2 (PostgreSQL mode) and PostgreSQL; accents are not normalized.
    @Query("""
        select p from Patient p
        where lower(p.firstName) like concat('%', :term, '%')
           or lower(p.lastName) like concat('%', :term, '%')
           or lower(concat(p.firstName, ' ', p.lastName)) like concat('%', :term, '%')
        order by p.lastName, p.firstName
        """)
    Page<Patient> searchByName(@Param("term") String lowerCaseTerm, Pageable pageable);
}
