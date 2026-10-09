package se.inna.bokningssystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.inna.bokningssystem.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    //Här finns inbyggda metorder man kan använda utan att behöva skriva koden själv. Ex. save(),
    // findById(), findAll(), deleteById()


    boolean  existsByUsername(String username); //kollar om  username redan används och svarar
    // true/false
    boolean existsByEmail(String email); //kollar om email redan användas och svarar true/false

    Optional<User> findByUsername(String username); //Leta efter användare och svarar med
    // användaren eller "ingen användare hittades", därav optional
}
