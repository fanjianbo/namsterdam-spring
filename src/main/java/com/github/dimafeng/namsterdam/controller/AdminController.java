package com.github.dimafeng.namsterdam.controller;

import com.github.dimafeng.namsterdam.dao.*;
import com.github.dimafeng.namsterdam.model.*;
import com.github.dimafeng.namsterdam.service.HTMLService;
import com.github.dimafeng.namsterdam.service.ImageService;
import com.github.dimafeng.namsterdam.service.MarkdownService;
import com.github.dimafeng.namsterdam.service.UserService;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;


@Controller
@RequestMapping("/admin")
@Secured("ROLE_ADMIN")
public class AdminController {

    @Value("${mongo.db.name:namsterdam}")
    private String databaseName;

    @Value("${mongo.db.server}")
    private String databaseServer;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private AbstractPostRepository abstractPostRepository;

    @Autowired
    private MarkdownService markdownService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private HTMLService htmlService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ViewCountRepository viewCountRepository;

    @RequestMapping("/")
    public String index() {
        return "admin";
    }

    @RequestMapping(value = "/menus", method = RequestMethod.GET)
    @ResponseBody
    public List<Menu> menus() {
        return menuRepository.findAll();
    }

    @RequestMapping(value = "/menus/{menuId}", method = RequestMethod.GET)
    @ResponseBody
    public Menu menu(@PathVariable("menuId") String id) {
        return menuRepository.findOne(id);
    }

    @RequestMapping(value = "/menus/{menuId}", method = RequestMethod.POST)
    @ResponseBody
    public Menu save(@PathVariable("menuId") String id, @RequestBody Menu menu) throws Exception {
        menu.setId(id);
        return saveUpdateMenu(menu);
    }

    @RequestMapping(value = "/menus", method = RequestMethod.POST)
    @ResponseBody
    public Menu saveUpdateMenu(@RequestBody Menu menu) throws Exception {
        if (menu.getBody() != null) {
            menu.setBodyHTML(markdownService.processALL(menu.getBody()));
        }
        return menuRepository.save(menu);
    }

    @RequestMapping(value = "/menus/{menuId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteMenu(@PathVariable("menuId") String id) {
        menuRepository.delete(id);
    }

    @RequestMapping(value = "/articles/{articleId}/images", method = RequestMethod.POST)
    @ResponseBody
    public void uploadImage(@PathVariable("articleId") String articleId,
                            @RequestParam("file") MultipartFile file) throws Exception {
        Image image = new Image();
        image.setData(file.getBytes());
        image.setDataJPG(imageService.convertToJpgOriginalSize(file.getBytes()));
        image.setArticleId(articleId);
        imageRepository.save(image);
    }

    @RequestMapping(value = "/image/{imageId}.jpg", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public byte[] renderImage(@PathVariable("imageId") String imageId) {
        return imageRepository.findOne(imageId).getDataJPG();
    }

    @RequestMapping(value = "/property", method = RequestMethod.GET)
    @ResponseBody
    public Property getProperty() {
        return getOrCreateProperty();
    }

    private Property getOrCreateProperty() {
        List<Property> properties = propertyRepository.findAll();
        Property property = new Property();
        if (!properties.isEmpty()) {
            property = properties.get(0);
        }
        return property;
    }

    @RequestMapping(value = "/property", method = RequestMethod.POST)
    @ResponseBody
    public Property saveProperty(@RequestBody Property property) {
        property.setId(getOrCreateProperty().getId());
        return propertyRepository.save(property);
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    @ResponseBody
    public List<User> users() {
        return userRepository.findAll();
    }

    @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public User user(@PathVariable("userId") String id) {
        return userRepository.findOne(id);
    }

    @RequestMapping(value = "/users/{userId}", method = RequestMethod.POST)
    @ResponseBody
    public User save(@PathVariable("userId") String id, @RequestBody User user) throws Exception {
        user.setId(id);
        return saveUpdateUser(user);
    }

    @RequestMapping(value = "/users", method = RequestMethod.POST)
    @ResponseBody
    public User saveUpdateUser(@RequestBody User user) throws Exception {
        if (user.getId() != null && !user.getId().isEmpty() && (user.getPassword() == null || user.getPassword().isEmpty())) {
            user.setPassword(userRepository.findOne(user.getId()).getPassword());
        } else {
            user.setPassword(userService.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @RequestMapping(value = "/users/{userId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteUser(@PathVariable("userId") String id) {
        userRepository.delete(id);
    }

    @RequestMapping(value = "/backup.zip", method = RequestMethod.GET)
    public void downloadBackup(HttpServletResponse response) throws Exception {
        Runtime.getRuntime().exec(new String[]{"rm", "-rf /tmp/backup & mongodump -db " + databaseName + " --out=/tmp/backup"}).waitFor();
        Runtime.getRuntime().exec(new String[]{"zip", "-r /tmp/backup.zip /tmp/backup"}).waitFor();
        response.setHeader("Content-Type", "application/octet-stream");
        Files.copy(Paths.get("/tmp/backup.zip"), response.getOutputStream());
        response.flushBuffer();
    }
}
