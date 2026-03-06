/**
 * 객실 타입 관리 - 등록/수정 폼 페이지
 */
var RoomTypeForm = {
    editId: null,
    duplicateChecked: false,
    selectedClassId: null,
    selectedClassName: null,
    selectedClassCode: null,
    classPopupTable: null,

    // 층/호수 데이터
    allFloors: [],
    allRoomNumbers: [],
    floorRows: [],  // [{floorId, roomNumberIds: [...]}]

    // 서비스 옵션 데이터
    selectedFreeOptions: [],   // [{id, serviceOptionCode, serviceNameKo, serviceType, quantity}]
    selectedPaidOptions: [],   // [{id, serviceOptionCode, serviceNameKo, serviceType, quantity, vatIncludedPrice, currencyCode}]
    currentServiceType: null,
    freeServicePopupTable: null,
    paidServicePopupTable: null,

    init: function() {
        this.editId = $('#roomTypeId').val() || null;
        this.bindEvents();
        this.updateHotelPropertyName();
        this.loadFloorAndRoomData();

        if (this.editId) {
            this.loadData();
        } else {
            this.setCreateMode();
            this.toggleBedSection();
        }
    },

    bindEvents: function() {
        var self = this;

        $(document).on('hola:contextChange', function() {
            self.updateHotelPropertyName();
            self.loadFloorAndRoomData();
        });

        // 코드 입력 시 중복확인 리셋
        $('#roomTypeCode').on('input', function() {
            self.duplicateChecked = false;
            $('#codeCheckResult').text('');
        });

        // 팝업 검색 엔터키
        $('#popupClassName').on('keypress', function(e) {
            if (e.which === 13) self.searchClass();
        });
        $('#freeServiceSearchKeyword').on('keypress', function(e) {
            if (e.which === 13) self.searchFreeService();
        });
        $('#paidServiceSearchKeyword').on('keypress', function(e) {
            if (e.which === 13) self.searchPaidService();
        });

        // 엑스트라 배드 여부 변경 시 배드 선택 초기화
        $('input[name="extraBedYn"]').on('change', function() {
            var isNo = $(this).val() === 'false';
            if (isNo) {
                self.selectedFreeOptions = self.selectedFreeOptions.filter(function(o) {
                    return o.serviceType !== 'BED';
                });
                self.updateServiceDisplay('BED');
            }
            self.toggleBedSection();
        });
    },

    updateHotelPropertyName: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#hotelPropertyName').text('-');
            return;
        }
        $('#contextAlert').addClass('d-none');

        var hotelName = $('#headerHotelSelect option:selected').text();
        var propertyName = $('#headerPropertySelect option:selected').text();
        if (hotelName && propertyName && hotelName !== '호텔 선택' && propertyName !== '프로퍼티 선택') {
            $('#hotelPropertyName').text(hotelName + ' > ' + propertyName);
        }
    },

    setCreateMode: function() {
        $('#btnSave').html('<i class="fas fa-save me-1"></i>등록');
        $('#btnDelete').hide();
        $('#roomTypeCode').prop('readonly', false);
        $('#btnCheckDuplicate').show();
        $('#btnSelectClass').show();
    },

    setEditMode: function() {
        $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');
        $('#btnDelete').show();
        $('#roomTypeCode').prop('readonly', true);
        $('#btnCheckDuplicate').hide();
        $('#btnSelectClass').hide();
        $('#updatedAtRow').show();
        this.duplicateChecked = true;
    },

    // 층/호수 기본 데이터 로드
    loadFloorAndRoomData: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) return;

        // 층 목록 + 호수 목록 병렬 로드
        $.when(
            $.ajax({ url: '/api/v1/properties/' + propertyId + '/floors', method: 'GET' }),
            $.ajax({ url: '/api/v1/properties/' + propertyId + '/room-numbers', method: 'GET' })
        ).done(function(floorRes, roomRes) {
            self.allFloors = (floorRes[0] && floorRes[0].data) ? floorRes[0].data : [];
            self.allRoomNumbers = (roomRes[0] && roomRes[0].data) ? roomRes[0].data : [];
        });
    },

    loadData: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/room-types/' + self.editId,
            method: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    var d = res.data;
                    // 기본정보
                    self.selectedClassId = d.roomClassId;
                    self.selectedClassCode = d.roomClassCode;
                    self.selectedClassName = d.roomClassName;
                    $('#roomClassId').val(d.roomClassId);
                    $('#roomClassDisplay').text(d.roomClassCode + ' - ' + (d.roomClassName || ''));
                    $('#roomTypeCode').val(d.roomTypeCode);
                    $('#description').val(d.description || '');
                    $('#roomSize').val(d.roomSize || '');
                    $('#features').val(d.features || '');
                    $('input[name="useYn"][value="' + d.useYn + '"]').prop('checked', true);

                    // 수용 인원
                    $('#maxAdults').val(d.maxAdults || 2);
                    $('#maxChildren').val(d.maxChildren || 0);

                    // 엑스트라 배드
                    $('input[name="extraBedYn"][value="' + d.extraBedYn + '"]').prop('checked', true);
                    self.toggleBedSection();

                    // 최종 수정일시
                    if (d.updatedAt) {
                        $('#updatedAt').text(d.updatedAt.replace('T', ' ').substring(0, 19));
                    }

                    // 층/호수 데이터 복원
                    if (d.floors && d.floors.length > 0) {
                        self.floorRows = d.floors.map(function(f) {
                            return { floorId: f.floorId, roomNumberIds: f.roomNumberIds || [] };
                        });
                        self.updateFloorRoomDisplay();
                    }

                    // 무료 서비스 옵션 복원
                    if (d.freeServiceOptions && d.freeServiceOptions.length > 0) {
                        self.selectedFreeOptions = d.freeServiceOptions.map(function(o) {
                            return {
                                id: o.id,
                                serviceOptionCode: o.serviceOptionCode,
                                serviceNameKo: o.serviceNameKo,
                                serviceType: o.serviceType,
                                quantity: o.quantity || 1
                            };
                        });
                        self.updateServiceDisplay('AMENITY');
                        self.updateServiceDisplay('BED');
                        self.updateServiceDisplay('VIEW');
                    }

                    // 유료 서비스 옵션 복원
                    if (d.paidServiceOptions && d.paidServiceOptions.length > 0) {
                        self.selectedPaidOptions = d.paidServiceOptions.map(function(o) {
                            return {
                                id: o.id,
                                serviceOptionCode: o.serviceOptionCode,
                                serviceNameKo: o.serviceNameKo,
                                serviceType: o.serviceType,
                                quantity: o.quantity || 1
                            };
                        });
                        self.updatePaidServiceDisplay();
                    }

                    self.setEditMode();
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    // === 객실 클래스 팝업 ===
    openClassPopup: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var propertyName = $('#headerPropertySelect option:selected').text();
        $('#popupPropertyName').val(propertyName);
        $('#popupClassName').val('');
        $('#popupUseYnAll').prop('checked', true);

        // DataTable 초기화 (처음 한 번만)
        if (!this.classPopupTable) {
            this.classPopupTable = $('#classPopupTable').DataTable({
                processing: true,
                serverSide: false,
                paging: true,
                pageLength: 10,
                ordering: false,
                searching: true,
                dom: 'rtip',
                info: false,
                language: HolaPms.dataTableLanguage,
                ajax: {
                    url: '/api/v1/properties/' + propertyId + '/room-classes',
                    dataSrc: 'data',
                    error: function(xhr) { HolaPms.handleAjaxError(xhr); }
                },
                columns: [
                    { data: null, className: 'text-center', render: function(d, t, r, m) { return m.row + 1; } },
                    { data: 'roomClassCode', render: function(d) { return HolaPms.escapeHtml(d); } },
                    { data: 'roomClassName', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                    { data: 'useYn', className: 'text-center', render: function(d) { return d ? 'Y' : 'N'; } },
                    {
                        data: null, className: 'text-center',
                        render: function(d, t, row) {
                            return '<input type="radio" name="classSelect" value="' + row.id
                                + '" data-code="' + HolaPms.escapeHtml(row.roomClassCode)
                                + '" data-name="' + HolaPms.escapeHtml(row.roomClassName || '') + '">';
                        }
                    }
                ]
            });
        } else {
            this.classPopupTable.ajax.url('/api/v1/properties/' + propertyId + '/room-classes').load();
        }

        var modal = new bootstrap.Modal(document.getElementById('classPopupModal'));
        modal.show();
    },

    searchClass: function() {
        if (!this.classPopupTable) return;
        var keyword = $.trim($('#popupClassName').val());
        var useYn = $('input[name="popupUseYn"]:checked').val();

        this.classPopupTable.search(keyword);
        // 사용여부 필터는 4번째 컬럼(index 3)
        if (useYn === 'true') {
            this.classPopupTable.column(3).search('^Y$', true, false);
        } else if (useYn === 'false') {
            this.classPopupTable.column(3).search('^N$', true, false);
        } else {
            this.classPopupTable.column(3).search('');
        }
        this.classPopupTable.draw();
    },

    applyClass: function() {
        var selected = $('input[name="classSelect"]:checked');
        if (!selected.length) {
            HolaPms.alert('warning', '객실 클래스를 선택해주세요.');
            return;
        }

        this.selectedClassId = selected.val();
        this.selectedClassCode = selected.data('code');
        this.selectedClassName = selected.data('name');
        $('#roomClassId').val(this.selectedClassId);
        $('#roomClassDisplay').text(this.selectedClassCode + ' - ' + this.selectedClassName);

        bootstrap.Modal.getInstance(document.getElementById('classPopupModal')).hide();
    },

    // === 층/호수 팝업 ===
    openFloorPopup: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        this.renderFloorPopup();
        var modal = new bootstrap.Modal(document.getElementById('floorPopupModal'));
        modal.show();
    },

    renderFloorPopup: function() {
        var self = this;
        var container = $('#floorRowContainer');
        container.empty();

        if (self.floorRows.length === 0) {
            $('#floorEmptyMessage').show();
        } else {
            $('#floorEmptyMessage').hide();
            self.floorRows.forEach(function(row, idx) {
                container.append(self.createFloorRowHtml(idx, row));
            });
        }
        self.updateFloorRoomSummary();
    },

    createFloorRowHtml: function(idx, row) {
        var self = this;
        var floor = self.allFloors.find(function(f) { return f.id === row.floorId; });
        var floorLabel = floor ? (floor.floorNumber + ' | ' + (floor.floorName || '')) : '층 ' + row.floorId;

        // 선택된 호수 이름 목록
        var roomLabels = row.roomNumberIds.map(function(rid) {
            var rn = self.allRoomNumbers.find(function(r) { return r.id === rid; });
            return rn ? rn.roomNumber : rid;
        });

        var isEditing = row._editing;

        var html = '<div class="border rounded p-3 mb-3" data-floor-idx="' + idx + '">';
        html += '<div class="d-flex justify-content-between align-items-start mb-2">';
        html += '<div class="d-flex">';
        html += '<div class="me-3"><strong>층</strong><br>' + HolaPms.escapeHtml(floorLabel) + '</div>';
        html += '<div><strong>호수</strong><br>';

        if (isEditing) {
            // 수정 모드: 층 선택 + 호수 다중 선택
            html += '</div></div>';
            html += '<div>';
            html += '<button type="button" class="btn btn-sm btn-outline-danger me-1" onclick="RoomTypeForm.removeFloorRow(' + idx + ')">삭제</button>';
            html += '</div></div>';

            // 층 선택 리스트
            html += '<div class="row mt-2">';
            html += '<div class="col-md-4">';
            html += '<label class="fw-bold mb-1">층 <small class="text-danger">단일 선택</small></label>';
            html += '<select class="form-select" id="floorSelect_' + idx + '" onchange="RoomTypeForm.onFloorSelectChange(' + idx + ')">';
            self.allFloors.forEach(function(f) {
                var sel = (f.id === row.floorId) ? ' selected' : '';
                html += '<option value="' + f.id + '"' + sel + '>'
                    + HolaPms.escapeHtml(f.floorNumber + ' | ' + (f.floorName || '')) + '</option>';
            });
            html += '</select></div>';

            html += '<div class="col-md-6">';
            html += '<label class="fw-bold mb-1">호수 <small class="text-primary">다중 선택 가능</small></label>';
            html += '<select class="form-select" id="roomSelect_' + idx + '" multiple size="6">';
            self.allRoomNumbers.forEach(function(rn) {
                var sel = row.roomNumberIds.indexOf(rn.id) >= 0 ? ' selected' : '';
                html += '<option value="' + rn.id + '"' + sel + '>'
                    + HolaPms.escapeHtml(rn.roomNumber + (rn.descriptionKo ? ' | ' + rn.descriptionKo : ''))
                    + '</option>';
            });
            html += '</select></div>';

            html += '<div class="col-md-2 d-flex align-items-end">';
            html += '<button type="button" class="btn btn-primary btn-sm" onclick="RoomTypeForm.saveFloorRow(' + idx + ')">저장</button>';
            html += ' <button type="button" class="btn btn-secondary btn-sm ms-1" onclick="RoomTypeForm.cancelFloorEdit(' + idx + ')">취소</button>';
            html += '</div></div>';
        } else {
            // 조회 모드
            html += HolaPms.escapeHtml(roomLabels.join(', ') || '-') + '</div></div>';
            html += '<div>';
            html += '<button type="button" class="btn btn-sm btn-outline-danger me-1" onclick="RoomTypeForm.removeFloorRow(' + idx + ')">삭제</button>';
            html += '<button type="button" class="btn btn-sm btn-outline-primary" onclick="RoomTypeForm.editFloorRow(' + idx + ')">수정</button>';
            html += '</div></div>';
        }

        html += '</div>';
        return html;
    },

    addFloorRow: function() {
        if (this.allFloors.length === 0) {
            HolaPms.alert('warning', '등록된 층 정보가 없습니다. 층코드관리에서 먼저 등록해주세요.');
            return;
        }
        this.floorRows.push({
            floorId: this.allFloors[0].id,
            roomNumberIds: [],
            _editing: true
        });
        this.renderFloorPopup();
    },

    editFloorRow: function(idx) {
        this.floorRows[idx]._editing = true;
        this.renderFloorPopup();
    },

    cancelFloorEdit: function(idx) {
        // 새로 추가한 행이면 삭제, 기존 행이면 편집 취소
        if (this.floorRows[idx].roomNumberIds.length === 0) {
            this.floorRows.splice(idx, 1);
        } else {
            delete this.floorRows[idx]._editing;
        }
        this.renderFloorPopup();
    },

    saveFloorRow: function(idx) {
        var floorId = parseInt($('#floorSelect_' + idx).val());
        var roomNumberIds = $('#roomSelect_' + idx).val() || [];
        roomNumberIds = roomNumberIds.map(function(v) { return parseInt(v); });

        if (roomNumberIds.length === 0) {
            HolaPms.alert('warning', '호수를 1개 이상 선택해주세요.');
            return;
        }

        this.floorRows[idx].floorId = floorId;
        this.floorRows[idx].roomNumberIds = roomNumberIds;
        delete this.floorRows[idx]._editing;
        this.renderFloorPopup();
        this.updateFloorRoomDisplay();
    },

    removeFloorRow: function(idx) {
        this.floorRows.splice(idx, 1);
        this.renderFloorPopup();
        this.updateFloorRoomDisplay();
    },

    onFloorSelectChange: function(idx) {
        // 층 변경 시 선택된 호수 초기화 (선택적)
    },

    updateFloorRoomSummary: function() {
        var total = 0;
        this.floorRows.forEach(function(row) {
            total += row.roomNumberIds.length;
        });
        $('#floorRoomSummary').text('총 선택 호수(객실) ' + total + ' 개');
    },

    updateFloorRoomDisplay: function() {
        var self = this;
        if (self.floorRows.length === 0) {
            $('#floorRoomDisplay').text('-');
            return;
        }

        var total = 0;
        var floorLabels = [];
        var roomLabels = [];

        self.floorRows.forEach(function(row) {
            var floor = self.allFloors.find(function(f) { return f.id === row.floorId; });
            if (floor) floorLabels.push(floor.floorNumber);
            total += row.roomNumberIds.length;

            row.roomNumberIds.forEach(function(rid) {
                var rn = self.allRoomNumbers.find(function(r) { return r.id === rid; });
                if (rn) roomLabels.push(rn.roomNumber);
            });
        });

        var text = '(총 ' + total + '개) 층 : ' + floorLabels.join(', ');
        if (roomLabels.length > 3) {
            text += ' /호수 : ' + roomLabels.slice(0, 3).join(', ') + ' ...';
        } else {
            text += ' /호수 : ' + roomLabels.join(', ');
        }
        $('#floorRoomDisplay').text(text);
    },

    toggleBedSection: function() {
        var isYes = $('input[name="extraBedYn"]:checked').val() === 'true';
        if (isYes) {
            $('#bedDisplay').closest('.row').find('button').prop('disabled', false);
        } else {
            $('#bedDisplay').closest('.row').find('button').prop('disabled', true);
        }
    },

    // === 무료 서비스 옵션 팝업 ===
    SERVICE_TYPE_LABELS: {
        // 무료 (FREE)
        'BED': '베드',
        'VIEW': '뷰',
        'FLOOR': '층수/위치',
        'AMENITY': '어메니티',
        'BREAKFAST': '조식',
        'TRANSFER': '교통/셔틀',
        'PARKING': '주차',
        'INTERNET': '인터넷',
        'WELCOME': '웰컴 서비스',
        'EARLY_CHECKIN': '얼리 체크인',
        'LATE_CHECKOUT': '레이트 체크아웃',
        'LOUNGE': '라운지 이용',
        'POOL_FITNESS': '부대시설',
        'KIDS': '키즈 서비스',
        'PET': '반려동물',
        'SPECIAL_REQUEST': '특별 요청',
        // 유료 (PAID)
        'ROOM_UPGRADE': '객실 업그레이드',
        'BED_EXTRA': '추가 침대',
        'BREAKFAST_PAID': '유료 조식',
        'MEAL': '식사 패키지',
        'MINIBAR': '미니바',
        'ROOM_SERVICE': '룸서비스',
        'AMENITY_PREMIUM': '유료 어메니티',
        'SPA_WELLNESS': '스파/웰니스',
        'LAUNDRY': '세탁 서비스',
        'TRANSFER_PAID': '유료 교통',
        'PARKING_PAID': '유료 주차',
        'TOUR_ACTIVITY': '투어/액티비티',
        'CHILDCARE': '육아 서비스',
        'BUSINESS': '비즈니스 서비스',
        'DECORATION': '객실 데코레이션',
        'PHOTO': '촬영 서비스',
        'INTERNET_PAID': '유료 인터넷',
        'SAFE_LOCKER': '금고/보관',
        'MINI_KITCHEN': '주방 서비스',
        'GIFT_PACKAGE': '기프트 패키지'
    },

    openServicePopup: function(serviceType) {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        self.currentServiceType = serviceType;
        var typeLabel = self.SERVICE_TYPE_LABELS[serviceType] || serviceType;
        $('#freeServicePopupTitle').text(typeLabel + ' 옵션 선택');
        $('#freeServiceSearchKeyword').val('');

        // 기존 DataTable 파괴 후 재생성 (serviceType 필터 변경 대응)
        if (self.freeServicePopupTable) {
            self.freeServicePopupTable.destroy();
            self.freeServicePopupTable = null;
        }

        // 해당 serviceType + 이미 선택된 옵션 체크 표시
        var selectedIds = self.selectedFreeOptions
            .filter(function(o) { return o.serviceType === serviceType; })
            .map(function(o) { return o.id; });

        var selectedQtyMap = {};
        self.selectedFreeOptions.forEach(function(o) {
            if (o.serviceType === serviceType) selectedQtyMap[o.id] = o.quantity;
        });

        self.freeServicePopupTable = $('#freeServicePopupTable').DataTable({
            processing: true,
            serverSide: false,
            paging: true,
            pageLength: 10,
            ordering: false,
            searching: true,
            dom: 'rtip',
            info: false,
            language: HolaPms.dataTableLanguage,
            ajax: {
                url: '/api/v1/properties/' + propertyId + '/free-service-options',
                dataSrc: function(json) {
                    // serviceType 필터 + useYn=true만
                    return (json.data || []).filter(function(item) {
                        return item.serviceType === serviceType && item.useYn === true;
                    });
                },
                error: function(xhr) { HolaPms.handleAjaxError(xhr); }
            },
            columns: [
                {
                    data: 'id', className: 'text-center',
                    render: function(d) {
                        var checked = selectedIds.indexOf(d) >= 0 ? ' checked' : '';
                        return '<input type="checkbox" class="free-svc-check" value="' + d + '"' + checked + '>';
                    }
                },
                { data: 'serviceOptionCode', render: function(d) { return HolaPms.escapeHtml(d); } },
                { data: 'serviceNameKo', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                { data: 'serviceType', className: 'text-center', render: function(d) { return self.SERVICE_TYPE_LABELS[d] || d; } },
                {
                    data: null, className: 'text-center',
                    render: function(d) {
                        var qty = selectedQtyMap[d.id] || d.quantity || 1;
                        return '<input type="number" class="form-control form-control-sm free-svc-qty" '
                            + 'data-id="' + d.id + '" min="1" max="99" value="' + qty + '" style="width:70px;margin:0 auto;">';
                    }
                }
            ],
            drawCallback: function() {
                // 전체선택 체크박스 상태 동기화
                var all = $('#freeServicePopupTable .free-svc-check').length;
                var checked = $('#freeServicePopupTable .free-svc-check:checked').length;
                $('#freeServiceCheckAll').prop('checked', all > 0 && all === checked);
            }
        });

        // 전체선택 이벤트
        $('#freeServiceCheckAll').off('change').on('change', function() {
            var isChecked = $(this).prop('checked');
            $('#freeServicePopupTable .free-svc-check').prop('checked', isChecked);
        });

        var modal = new bootstrap.Modal(document.getElementById('freeServicePopupModal'));
        modal.show();
    },

    searchFreeService: function() {
        if (!this.freeServicePopupTable) return;
        var keyword = $.trim($('#freeServiceSearchKeyword').val());
        this.freeServicePopupTable.search(keyword).draw();
    },

    applyFreeService: function() {
        var self = this;
        var serviceType = self.currentServiceType;

        // 현재 serviceType이 아닌 기존 선택은 유지
        self.selectedFreeOptions = self.selectedFreeOptions.filter(function(o) {
            return o.serviceType !== serviceType;
        });

        // 체크된 항목 추가
        $('#freeServicePopupTable .free-svc-check:checked').each(function() {
            var id = parseInt($(this).val());
            var rowData = self.freeServicePopupTable.row($(this).closest('tr')).data();
            var qty = parseInt($('#freeServicePopupTable .free-svc-qty[data-id="' + id + '"]').val()) || 1;

            self.selectedFreeOptions.push({
                id: rowData.id,
                serviceOptionCode: rowData.serviceOptionCode,
                serviceNameKo: rowData.serviceNameKo,
                serviceType: rowData.serviceType,
                quantity: qty
            });
        });

        self.updateServiceDisplay(serviceType);
        bootstrap.Modal.getInstance(document.getElementById('freeServicePopupModal')).hide();
    },

    removeServiceOption: function(serviceType, optionId) {
        this.selectedFreeOptions = this.selectedFreeOptions.filter(function(o) {
            return !(o.serviceType === serviceType && o.id === optionId);
        });
        this.updateServiceDisplay(serviceType);
    },

    updateServiceDisplay: function(serviceType) {
        var self = this;
        var items = self.selectedFreeOptions.filter(function(o) { return o.serviceType === serviceType; });
        var displayId;

        if (serviceType === 'AMENITY') displayId = '#amenityDisplay';
        else if (serviceType === 'BED') displayId = '#bedDisplay';
        else if (serviceType === 'VIEW') displayId = '#viewDisplay';
        else return;

        if (items.length === 0) {
            $(displayId).html('-');
            return;
        }

        var html = '<div class="d-flex flex-wrap gap-2">';
        items.forEach(function(item) {
            html += '<span class="badge bg-light text-dark border px-2 py-1">'
                + HolaPms.escapeHtml(item.serviceNameKo)
                + (item.quantity > 1 ? ' x' + item.quantity : '')
                + ' <i class="fas fa-times ms-1 text-danger" style="cursor:pointer;" '
                + 'onclick="RoomTypeForm.removeServiceOption(\'' + serviceType + '\', ' + item.id + ')"></i>'
                + '</span>';
        });
        html += '</div>';
        $(displayId).html(html);
    },

    // === 유료 서비스 옵션 팝업 ===
    openPaidServicePopup: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        $('#paidServiceSearchKeyword').val('');

        if (self.paidServicePopupTable) {
            self.paidServicePopupTable.destroy();
            self.paidServicePopupTable = null;
        }

        var selectedIds = self.selectedPaidOptions.map(function(o) { return o.id; });
        var selectedQtyMap = {};
        self.selectedPaidOptions.forEach(function(o) { selectedQtyMap[o.id] = o.quantity; });

        self.paidServicePopupTable = $('#paidServicePopupTable').DataTable({
            processing: true,
            serverSide: false,
            paging: true,
            pageLength: 10,
            ordering: false,
            searching: true,
            dom: 'rtip',
            info: false,
            language: HolaPms.dataTableLanguage,
            ajax: {
                url: '/api/v1/properties/' + propertyId + '/paid-service-options',
                dataSrc: function(json) {
                    return (json.data || []).filter(function(item) { return item.useYn === true; });
                },
                error: function(xhr) { HolaPms.handleAjaxError(xhr); }
            },
            columns: [
                {
                    data: 'id', className: 'text-center',
                    render: function(d) {
                        var checked = selectedIds.indexOf(d) >= 0 ? ' checked' : '';
                        return '<input type="checkbox" class="paid-svc-check" value="' + d + '"' + checked + '>';
                    }
                },
                { data: 'serviceOptionCode', render: function(d) { return HolaPms.escapeHtml(d); } },
                { data: 'serviceNameKo', render: function(d) { return HolaPms.escapeHtml(d || '-'); } },
                { data: 'serviceType', className: 'text-center', render: function(d) { return self.SERVICE_TYPE_LABELS[d] || d; } },
                {
                    data: null, className: 'text-end',
                    render: function(d) {
                        var price = d.vatIncludedPrice || 0;
                        var currency = d.currencyCode === 'USD' ? '$' : '₩';
                        return currency + ' ' + Number(price).toLocaleString();
                    }
                },
                {
                    data: null, className: 'text-center',
                    render: function(d) {
                        var qty = selectedQtyMap[d.id] || 1;
                        return '<input type="number" class="form-control form-control-sm paid-svc-qty" '
                            + 'data-id="' + d.id + '" min="1" max="99" value="' + qty + '" style="width:70px;margin:0 auto;">';
                    }
                }
            ],
            drawCallback: function() {
                var all = $('#paidServicePopupTable .paid-svc-check').length;
                var checked = $('#paidServicePopupTable .paid-svc-check:checked').length;
                $('#paidServiceCheckAll').prop('checked', all > 0 && all === checked);
            }
        });

        $('#paidServiceCheckAll').off('change').on('change', function() {
            var isChecked = $(this).prop('checked');
            $('#paidServicePopupTable .paid-svc-check').prop('checked', isChecked);
        });

        var modal = new bootstrap.Modal(document.getElementById('paidServicePopupModal'));
        modal.show();
    },

    searchPaidService: function() {
        if (!this.paidServicePopupTable) return;
        var keyword = $.trim($('#paidServiceSearchKeyword').val());
        this.paidServicePopupTable.search(keyword).draw();
    },

    applyPaidService: function() {
        var self = this;
        self.selectedPaidOptions = [];

        $('#paidServicePopupTable .paid-svc-check:checked').each(function() {
            var id = parseInt($(this).val());
            var rowData = self.paidServicePopupTable.row($(this).closest('tr')).data();
            var qty = parseInt($('#paidServicePopupTable .paid-svc-qty[data-id="' + id + '"]').val()) || 1;

            self.selectedPaidOptions.push({
                id: rowData.id,
                serviceOptionCode: rowData.serviceOptionCode,
                serviceNameKo: rowData.serviceNameKo,
                serviceType: rowData.serviceType,
                quantity: qty,
                vatIncludedPrice: rowData.vatIncludedPrice,
                currencyCode: rowData.currencyCode
            });
        });

        self.updatePaidServiceDisplay();
        bootstrap.Modal.getInstance(document.getElementById('paidServicePopupModal')).hide();
    },

    removePaidServiceOption: function(optionId) {
        this.selectedPaidOptions = this.selectedPaidOptions.filter(function(o) { return o.id !== optionId; });
        this.updatePaidServiceDisplay();
    },

    updatePaidServiceDisplay: function() {
        var self = this;
        if (self.selectedPaidOptions.length === 0) {
            $('#paidOptionDisplay').html('-');
            return;
        }

        var html = '<div class="d-flex flex-wrap gap-2">';
        self.selectedPaidOptions.forEach(function(item) {
            var currency = item.currencyCode === 'USD' ? '$' : '₩';
            var price = item.vatIncludedPrice ? (currency + Number(item.vatIncludedPrice).toLocaleString()) : '';
            html += '<span class="badge bg-light text-dark border px-2 py-1">'
                + HolaPms.escapeHtml(item.serviceNameKo)
                + (price ? ' (' + price + ')' : '')
                + (item.quantity > 1 ? ' x' + item.quantity : '')
                + ' <i class="fas fa-times ms-1 text-danger" style="cursor:pointer;" '
                + 'onclick="RoomTypeForm.removePaidServiceOption(' + item.id + ')"></i>'
                + '</span>';
        });
        html += '</div>';
        $('#paidOptionDisplay').html(html);
    },

    // === 중복확인 ===
    checkDuplicate: function() {
        var self = this;
        var code = $.trim($('#roomTypeCode').val());
        if (!code) {
            HolaPms.alert('warning', '객실 타입 코드를 입력해주세요.');
            $('#roomTypeCode').focus();
            return;
        }

        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/room-types/check-code',
            method: 'GET',
            data: { roomTypeCode: code },
            success: function(res) {
                if (res.data.duplicate) {
                    $('#codeCheckResult').text('이미 사용 중인 코드입니다.').removeClass('text-primary').addClass('text-danger');
                    self.duplicateChecked = false;
                } else {
                    $('#codeCheckResult').text('사용 가능한 코드입니다.').removeClass('text-danger').addClass('text-primary');
                    self.duplicateChecked = true;
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    // === 저장 ===
    save: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        // 필수값 검증
        if (!$('#roomClassId').val()) {
            HolaPms.alert('warning', '객실 클래스를 선택해주세요.');
            return;
        }
        var typeCode = $.trim($('#roomTypeCode').val());
        if (!typeCode) {
            HolaPms.alert('warning', '객실 타입 코드를 입력해주세요.');
            $('#roomTypeCode').focus();
            return;
        }

        // 신규 등록 시 중복확인 필수
        if (!self.editId && !self.duplicateChecked) {
            HolaPms.alert('warning', '객실 타입 코드 중복확인을 해주세요.');
            return;
        }

        var maxAdults = parseInt($('#maxAdults').val());
        var maxChildren = parseInt($('#maxChildren').val());
        if (isNaN(maxAdults) || maxAdults < 1 || maxAdults > 99) {
            HolaPms.alert('warning', '어른 최대 수용 인원은 1~99 사이 값을 입력해주세요.');
            $('#maxAdults').focus();
            return;
        }
        if (isNaN(maxChildren) || maxChildren < 0 || maxChildren > 99) {
            HolaPms.alert('warning', '어린이 최대 수용 인원은 0~99 사이 값을 입력해주세요.');
            $('#maxChildren').focus();
            return;
        }

        // 층/호수 데이터 (editing 상태인 행 제외)
        var floors = self.floorRows
            .filter(function(r) { return !r._editing && r.roomNumberIds.length > 0; })
            .map(function(r) {
                return { floorId: r.floorId, roomNumberIds: r.roomNumberIds };
            });

        var url, method, data;
        // 서비스 옵션 데이터
        var freeServiceOptions = self.selectedFreeOptions.map(function(o) {
            return { id: o.id, quantity: o.quantity };
        });
        var paidServiceOptions = self.selectedPaidOptions.map(function(o) {
            return { id: o.id, quantity: o.quantity };
        });

        if (self.editId) {
            url = '/api/v1/properties/' + propertyId + '/room-types/' + self.editId;
            method = 'PUT';
            data = {
                description: $.trim($('#description').val()),
                roomSize: $('#roomSize').val() ? parseFloat($('#roomSize').val()) : null,
                features: $.trim($('#features').val()),
                maxAdults: maxAdults,
                maxChildren: maxChildren,
                extraBedYn: $('input[name="extraBedYn"]:checked').val() === 'true',
                useYn: $('input[name="useYn"]:checked').val() === 'true',
                floors: floors,
                freeServiceOptions: freeServiceOptions,
                paidServiceOptions: paidServiceOptions
            };
        } else {
            url = '/api/v1/properties/' + propertyId + '/room-types';
            method = 'POST';
            data = {
                roomClassId: parseInt($('#roomClassId').val()),
                roomTypeCode: typeCode,
                description: $.trim($('#description').val()),
                roomSize: $('#roomSize').val() ? parseFloat($('#roomSize').val()) : null,
                features: $.trim($('#features').val()),
                maxAdults: maxAdults,
                maxChildren: maxChildren,
                extraBedYn: $('input[name="extraBedYn"]:checked').val() === 'true',
                useYn: $('input[name="useYn"]:checked').val() === 'true',
                floors: floors,
                freeServiceOptions: freeServiceOptions,
                paidServiceOptions: paidServiceOptions
            };
        }

        $.ajax({
            url: url,
            method: method,
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', self.editId ? '수정되었습니다.' : '등록되었습니다.');
                    window.location.href = '/admin/room-types';
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    // === 삭제 ===
    remove: function() {
        var self = this;
        if (!self.editId) return;

        if (!confirm('정말 삭제하시겠습니까?')) return;

        var propertyId = HolaPms.context.getPropertyId();
        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/room-types/' + self.editId,
            method: 'DELETE',
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '삭제되었습니다.');
                    window.location.href = '/admin/room-types';
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    }
};

$(document).ready(function() {
    RoomTypeForm.init();
});
