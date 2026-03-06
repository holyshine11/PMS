package com.hola.room.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 객실 타입 웹 페이지 컨트롤러
 */
@Controller
@RequestMapping("/admin/room-types")
public class RoomTypeViewController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("currentURI", "/admin/room-types");
        return "room-type/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("currentURI", "/admin/room-types");
        return "room-type/form";
    }

    @GetMapping("/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("roomTypeId", id);
        model.addAttribute("currentURI", "/admin/room-types");
        return "room-type/form";
    }
}
