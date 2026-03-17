package com.hola.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 프론트데스크 View 컨트롤러
 */
@Controller
@RequestMapping("/admin/front-desk")
public class FrontDeskViewController {

    @GetMapping("/arrivals")
    public String arrivals() {
        return "front-desk/arrivals";
    }

    @GetMapping("/in-house")
    public String inHouse() {
        return "front-desk/in-house";
    }

    @GetMapping("/departures")
    public String departures() {
        return "front-desk/departures";
    }

    @GetMapping("/walk-in")
    public String walkIn() {
        return "front-desk/walk-in";
    }

    @GetMapping("/room-rack")
    public String roomRack() {
        return "front-desk/room-rack";
    }
}
