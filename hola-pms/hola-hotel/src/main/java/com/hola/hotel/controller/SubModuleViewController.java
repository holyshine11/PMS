package com.hola.hotel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 하위 모듈 독립 페이지 (층/호수/마켓코드/프로퍼티)
 */
@Controller
public class SubModuleViewController {

    /** 프로퍼티 목록 */
    @GetMapping("/admin/properties")
    public String propertyList() {
        return "property/list";
    }

    /** 프로퍼티 등록 */
    @GetMapping("/admin/properties/new")
    public String propertyCreateForm() {
        return "property/form";
    }

    /** 프로퍼티 수정 */
    @GetMapping("/admin/properties/{id}/edit")
    public String propertyEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("propertyId", id);
        return "property/form";
    }

    /** 층코드 관리 */
    @GetMapping("/admin/floors")
    public String floorList() {
        return "floor/list";
    }

    /** 호수코드 관리 */
    @GetMapping("/admin/room-numbers")
    public String roomNumberList() {
        return "room-number/list";
    }

    /** 마켓코드 관리 */
    @GetMapping("/admin/market-codes")
    public String marketCodeList() {
        return "market-code/list";
    }
}
