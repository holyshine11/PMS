package com.hola.hotel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/reservation-channels")
public class ReservationChannelViewController {

    @GetMapping
    public String list() {
        return "reservation-channel/list";
    }
}
