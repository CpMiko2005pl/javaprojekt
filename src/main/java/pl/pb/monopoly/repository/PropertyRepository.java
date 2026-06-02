package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.Property;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    // Sortowanie list realizujemy przez Sort przekazywany z kontrolera (findAll(Sort)).
}
