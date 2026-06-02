package pl.pb.monopoly.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.Property;
import pl.pb.monopoly.service.PropertyService;

/**
 * Panel CRUD nieruchomosci (model MVC). Pelny zestaw operacji:
 * lista (z sortowaniem w obu kierunkach wg 3 kryteriow), dodawanie, edycja, usuwanie.
 *
 * Kryteria i kierunek sortowania zapamietujemy w ciasteczku (wymaganie:
 * zapamietanie kierunkow i kryteriow sortowania + uzycie ciasteczek).
 */
@Controller
@RequestMapping("/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String sort,
                       @RequestParam(required = false) String dir,
                       @CookieValue(value = "propSort", required = false) String cookieSort,
                       @CookieValue(value = "propDir", required = false) String cookieDir,
                       HttpServletResponse response,
                       Model model) {
        // Jesli brak parametru w URL - uzyj zapamietanego w ciasteczku (domyslnie name/asc).
        String sortField = sort != null ? sort : (cookieSort != null ? cookieSort : "name");
        String direction = dir != null ? dir : (cookieDir != null ? cookieDir : "asc");

        // Zapamietanie wyboru w ciasteczku na 7 dni.
        response.addCookie(makeCookie("propSort", sortField));
        response.addCookie(makeCookie("propDir", direction));

        model.addAttribute("properties", propertyService.findAll(sortField, direction));
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction);
        return "property/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("property", new Property());
        return "property/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("property", propertyService.getById(id));
        return "property/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("property") Property property,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "property/form";
        }
        propertyService.save(property);
        redirectAttributes.addFlashAttribute("message", "Zapisano nieruchomosc: " + property.getName());
        return "redirect:/properties";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        propertyService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Usunieto nieruchomosc.");
        return "redirect:/properties";
    }

    private Cookie makeCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        cookie.setHttpOnly(true);
        return cookie;
    }
}
