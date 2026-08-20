package se.inna.bokningssystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.inna.bokningssystem.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    //Här finns metorder man kan använda utan att behöva skriva koden själv. Ex. save(), findById
    // (), findAll(), deleteById()
}
