package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.MarketCodeRequest;
import com.hola.hotel.dto.response.MarketCodeResponse;
import com.hola.hotel.entity.MarketCode;
import com.hola.hotel.entity.Property;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.MarketCodeRepository;
import com.hola.hotel.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketCodeServiceImpl implements MarketCodeService {

    private final MarketCodeRepository marketCodeRepository;
    private final PropertyRepository propertyRepository;
    private final HotelMapper hotelMapper;

    @Override
    public List<MarketCodeResponse> getMarketCodes(Long propertyId) {
        return marketCodeRepository.findAllByPropertyIdOrderBySortOrderAsc(propertyId).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MarketCodeResponse getMarketCode(Long id) {
        return hotelMapper.toResponse(findMarketCodeById(id));
    }

    @Override
    @Transactional
    public MarketCodeResponse createMarketCode(Long propertyId, MarketCodeRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        if (marketCodeRepository.existsByPropertyIdAndMarketCode(propertyId, request.getMarketCode())) {
            throw new HolaException(ErrorCode.MARKET_CODE_DUPLICATE);
        }

        MarketCode marketCode = MarketCode.builder()
                .property(property)
                .marketCode(request.getMarketCode())
                .marketName(request.getMarketName())
                .descriptionKo(request.getDescriptionKo())
                .descriptionEn(request.getDescriptionEn())
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            marketCode.deactivate();
        }
        if (request.getSortOrder() != null) marketCode.changeSortOrder(request.getSortOrder());

        MarketCode saved = marketCodeRepository.save(marketCode);
        log.info("마켓코드 생성: {} ({}) - 프로퍼티: {}", saved.getMarketName(), saved.getMarketCode(), property.getPropertyName());
        return hotelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MarketCodeResponse updateMarketCode(Long id, MarketCodeRequest request) {
        MarketCode marketCode = findMarketCodeById(id);
        marketCode.update(request.getMarketCode(), request.getMarketName(),
                         request.getDescriptionKo(), request.getDescriptionEn());

        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                marketCode.activate();
            } else {
                marketCode.deactivate();
            }
        }
        if (request.getSortOrder() != null) marketCode.changeSortOrder(request.getSortOrder());
        return hotelMapper.toResponse(marketCode);
    }

    @Override
    @Transactional
    public void deleteMarketCode(Long id) {
        MarketCode marketCode = findMarketCodeById(id);
        marketCode.softDelete();
        log.info("마켓코드 삭제: {} ({})", marketCode.getMarketName(), marketCode.getMarketCode());
    }

    @Override
    public boolean existsMarketCode(Long propertyId, String marketCode) {
        return marketCodeRepository.existsByPropertyIdAndMarketCode(propertyId, marketCode);
    }

    private MarketCode findMarketCodeById(Long id) {
        return marketCodeRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.MARKET_CODE_NOT_FOUND));
    }
}
