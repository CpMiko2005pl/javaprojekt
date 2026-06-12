package pl.pb.monopoly.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.Property;
import pl.pb.monopoly.repository.PropertyRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    private static final List<String> ALLOWED_SORT = List.of("name", "location", "price");

    /**
     * Lista nieruchomosci z sortowaniem i opcjonalnym filtrowaniem.
     * @param sortField pole sortowania
     * @param direction "asc" lub "desc"
     * @param dateFrom  filtr: od tej daty (null = bez filtrowania)
     * @param colorGroup filtr: tylko ta grupa kolorow (null lub "" = bez filtrowania)
     */
    @Transactional(readOnly = true)
    public List<Property> findAll(String sortField, String direction,
                                  LocalDate dateFrom, String colorGroup) {
        String field = ALLOWED_SORT.contains(sortField) ? sortField : "name";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(dir, field);

        boolean hasDate = dateFrom != null;
        boolean hasColor = colorGroup != null && !colorGroup.isBlank();

        if (hasDate && hasColor) {
            List<Property> raw = propertyRepository
                    .findByCreatedAtAfterAndColorGroupIgnoreCase(dateFrom.atStartOfDay(), colorGroup);
            raw.sort(sort.getOrderFor(field) != null
                    ? buildComparator(field, dir) : buildComparator("name", Sort.Direction.ASC));
            return raw;
        }
        if (hasDate) {
            List<Property> raw = propertyRepository.findByCreatedAtAfter(dateFrom.atStartOfDay());
            raw.sort(buildComparator(field, dir));
            return raw;
        }
        if (hasColor) {
            List<Property> raw = propertyRepository.findByColorGroupIgnoreCase(colorGroup);
            raw.sort(buildComparator(field, dir));
            return raw;
        }
        return propertyRepository.findAll(sort);
    }

    @Transactional(readOnly = true)
    public List<Property> findAll(String sortField, String direction) {
        return findAll(sortField, direction, null, null);
    }

    @Transactional(readOnly = true)
    public List<String> colorGroupsByPopularity() {
        return propertyRepository.findColorGroupsByPopularity();
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

    private java.util.Comparator<Property> buildComparator(String field, Sort.Direction dir) {
        java.util.Comparator<Property> cmp = switch (field) {
            case "location" -> java.util.Comparator.comparing(
                    p -> p.getLocation() != null ? p.getLocation() : "");
            case "price" -> java.util.Comparator.comparing(
                    p -> p.getPrice() != null ? p.getPrice() : java.math.BigDecimal.ZERO);
            default -> java.util.Comparator.comparing(
                    p -> p.getName() != null ? p.getName() : "");
        };
        return dir == Sort.Direction.DESC ? cmp.reversed() : cmp;
    }
}
