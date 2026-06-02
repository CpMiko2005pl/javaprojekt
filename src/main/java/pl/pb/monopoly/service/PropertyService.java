package pl.pb.monopoly.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.Property;
import pl.pb.monopoly.repository.PropertyRepository;

import java.util.List;

/**
 * Logika CRUD dla nieruchomosci. Obsluguje tez sortowanie listy
 * (wymaganie: sortowanie w obu kierunkach wg roznych kryteriow).
 */
@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    /** Dozwolone pola sortowania - chroni przed wstrzyknieciem dowolnej nazwy kolumny. */
    private static final List<String> ALLOWED_SORT = List.of("name", "location", "price");

    @Transactional(readOnly = true)
    public List<Property> findAll(String sortField, String direction) {
        String field = ALLOWED_SORT.contains(sortField) ? sortField : "name";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return propertyRepository.findAll(Sort.by(dir, field));
    }

    @Transactional(readOnly = true)
    public Property getById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma nieruchomosci o id " + id));
    }

    @Transactional
    public Property save(Property property) {
        return propertyRepository.save(property);
    }

    @Transactional
    public void delete(Long id) {
        propertyRepository.deleteById(id);
    }
}
