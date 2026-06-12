package pl.pb.monopoly.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.Property;
import pl.pb.monopoly.service.PropertyService;

import java.time.LocalDate;

/**
 * Panel CRUD nieruchomosci z sortowaniem, ciasteczkami i filtrowaniem wg daty/grupy.
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
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                       @RequestParam(required = false) String colorGroup,
                       @CookieValue(value = "propSort", required = false) String cookieSort,
                       @CookieValue(value = "propDir", required = false) String cookieDir,
                       HttpServletResponse response,
                       Model model) {
        String sortField = sort != null ? sort : (cookieSort != null ? cookieSort : "name");
        String direction = dir != null ? dir : (cookieDir != null ? cookieDir : "asc");

        response.addCookie(makeCookie("propSort", sortField));
        response.addCookie(makeCookie("propDir", direction));

        model.addAttribute("properties", propertyService.findAll(sortField, direction, dateFrom, colorGroup));
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", direction);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("colorGroup", colorGroup);
        model.addAttribute("colorGroups", propertyService.colorGroupsByPopularity());
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
