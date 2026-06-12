package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.pb.monopoly.domain.Property;

import java.time.LocalDateTime;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    /** Filtrowanie wg daty dodania (od podanej daty) i grupy kolorow. */
    List<Property> findByCreatedAtAfterAndColorGroupIgnoreCase(LocalDateTime after, String colorGroup);

    List<Property> findByCreatedAtAfter(LocalDateTime after);

    List<Property> findByColorGroupIgnoreCase(String colorGroup);

    /** Grupy kolorow posortowane od najczesciej uzywanych. */
    @Query("SELECT p.colorGroup FROM Property p WHERE p.colorGroup IS NOT NULL " +
           "GROUP BY p.colorGroup ORDER BY COUNT(p.colorGroup) DESC")
    List<String> findColorGroupsByPopularity();
}
