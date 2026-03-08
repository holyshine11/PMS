/**
 * 프로퍼티 관리자 권한 등록/수정 폼 JS
 */
const PropertyRoleForm = {
    isEdit: false,
    roleId: null,
    nameChecked: false,
    originalRoleName: '',
    originalHotelId: null,
    originalPropertyId: null,

    init: function() {
        this.roleId = $('#roleId').val() || null;
        this.isEdit = !!this.roleId;

        // 호텔 드롭다운 로드
        this.loadHotels();

        // 메뉴 트리 로드
        this.loadMenuTree();

        if (this.isEdit) {
            $('#pageTitle').html('<i class="fas fa-user-cog me-2"></i>프로퍼티 관리자 권한 수정');
            $('#btnSaveText').text('저장');
            $('#btnDelete').show();
            $('#updatedAtRow').show();
            this.nameChecked = true;
        }

        // 권한명 변경 시 중복확인 초기화
        $('#roleName').on('input', function() {
            PropertyRoleForm.nameChecked = false;
            $('#nameCheckResult').text('').removeClass('text-success text-danger');
        });
        // 호텔 변경 시 프로퍼티 로드 + 중복확인 초기화
        $('#hotelId').on('change', function() {
            PropertyRoleForm.nameChecked = false;
            $('#nameCheckResult').text('').removeClass('text-success text-danger');
            PropertyRoleForm.loadProperties($(this).val());
        });
        // 프로퍼티 변경 시 중복확인 초기화 + 기존 권한 확인
        $('#propertyId').on('change', function() {
            PropertyRoleForm.nameChecked = false;
            $('#nameCheckResult').text('').removeClass('text-success text-danger');
            PropertyRoleForm.checkPropertyRole($(this).val());
        });
    },

    /** 호텔 목록 로드 */
    loadHotels: function() {
        var userRole = HolaPms.context.userRole || null;
        var userHotelId = HolaPms.context.getHotelId ? HolaPms.context.getHotelId() : null;

        HolaPms.ajax({
            url: '/api/v1/hotels/selector',
            type: 'GET',
            success: function(res) {
                var hotels = res.data || [];
                var $select = $('#hotelId');
                $select.find('option:not(:first)').remove();
                hotels.forEach(function(h) {
                    $select.append('<option value="' + h.id + '">' + HolaPms.escapeHtml(h.hotelName) + '</option>');
                });

                // HOTEL_ADMIN: 자기 호텔로 고정 + 변경 불가
                if (userRole !== 'SUPER_ADMIN' && userHotelId) {
                    $select.val(userHotelId);
                    $select.prop('disabled', true);
                    if (!PropertyRoleForm.isEdit) {
                        PropertyRoleForm.loadProperties(userHotelId);
                    }
                }

                if (PropertyRoleForm.isEdit) {
                    // 수정 모드: 데이터 로드
                    PropertyRoleForm.loadRole();
                } else if (userRole === 'SUPER_ADMIN' && hotels.length === 1) {
                    // SUPER_ADMIN + 호텔 1개: 자동 선택
                    $select.val(hotels[0].id);
                    PropertyRoleForm.loadProperties(hotels[0].id);
                }
            }
        });
    },

    /** 프로퍼티 목록 로드 (호텔 변경 시 호출) */
    loadProperties: function(hotelId, selectPropertyId) {
        var $select = $('#propertyId');
        $select.find('option:not(:first)').remove();
        if (!hotelId) {
            $select.find('option:first').text('호텔을 먼저 선택하세요');
            return;
        }
        $select.find('option:first').text('로딩 중...');
        HolaPms.ajax({
            url: '/api/v1/properties/selector?hotelId=' + hotelId,
            type: 'GET',
            success: function(res) {
                var properties = res.data || [];
                $select.find('option:first').text('프로퍼티를 선택하세요');
                properties.forEach(function(p) {
                    $select.append('<option value="' + p.id + '">' + HolaPms.escapeHtml(p.propertyName) + '</option>');
                });
                if (selectPropertyId) {
                    $select.val(selectPropertyId);
                } else if (properties.length === 1) {
                    // 프로퍼티가 1개면 자동 선택 + 권한 존재 확인
                    $select.val(properties[0].id);
                    PropertyRoleForm.checkPropertyRole(properties[0].id);
                }
                // 수정 모드: 프로퍼티 로드 완료 후 변경 불가 처리
                if (PropertyRoleForm._disablePropertyAfterLoad) {
                    $select.prop('disabled', true);
                    PropertyRoleForm._disablePropertyAfterLoad = false;
                }
            },
            error: function(xhr) {
                console.error('프로퍼티 로드 실패:', xhr.status, xhr.responseText);
                $select.find('option:first').text('프로퍼티를 선택하세요');
                HolaPms.alert('error', '프로퍼티 목록을 불러올 수 없습니다.');
            }
        });
    },

    /** 프로퍼티에 기존 권한 존재 여부 확인 (등록 모드에서만) */
    checkPropertyRole: function(propertyId) {
        $('#propertyRoleWarning').remove();
        this._propertyBlocked = false;
        if (!propertyId || this.isEdit) return;

        HolaPms.ajax({
            url: '/api/v1/property-admin-roles/check-property?propertyId=' + propertyId,
            type: 'GET',
            success: function(res) {
                if (res.data.exists) {
                    PropertyRoleForm._propertyBlocked = true;
                    $('#propertyId').after(
                        '<small id="propertyRoleWarning" class="form-text text-danger">' +
                        '<i class="fas fa-exclamation-circle me-1"></i>해당 프로퍼티에 이미 권한이 설정되어 있습니다.</small>'
                    );
                }
            }
        });
    },

    /** 메뉴 트리 로드 */
    loadMenuTree: function() {
        HolaPms.ajax({
            url: '/api/v1/property-admin-roles/menu-tree',
            type: 'GET',
            success: function(res) {
                var tree = res.data || [];
                PropertyRoleForm.renderMenuTree(tree);
            }
        });
    },

    /** 메뉴 트리 렌더링 (3-depth 지원) */
    renderMenuTree: function(tree) {
        var html = '';
        tree.forEach(function(depth1) {
            html += '<div class="menu-group mb-3">';
            html += '  <div class="form-check">';
            html += '    <input class="form-check-input menu-depth1" type="checkbox" value="' + depth1.id + '" id="menu_' + depth1.id + '" data-depth="1" onchange="PropertyRoleForm.toggleNode(this)">';
            html += '    <label class="form-check-label fw-bold" for="menu_' + depth1.id + '">' + HolaPms.escapeHtml(depth1.menuName) + '</label>';
            html += '  </div>';

            if (depth1.children && depth1.children.length > 0) {
                depth1.children.forEach(function(depth2) {
                    html += '  <div class="ms-4 mt-1">';
                    html += '    <div class="form-check">';
                    html += '      <input class="form-check-input menu-depth2" type="checkbox" value="' + depth2.id + '" id="menu_' + depth2.id + '" data-depth="2" data-parent="' + depth1.id + '" onchange="PropertyRoleForm.toggleNode(this)">';
                    html += '      <label class="form-check-label fw-bold" for="menu_' + depth2.id + '">' + HolaPms.escapeHtml(depth2.menuName) + '</label>';
                    html += '    </div>';

                    if (depth2.children && depth2.children.length > 0) {
                        html += '    <div class="ms-4 mt-1">';
                        depth2.children.forEach(function(depth3) {
                            html += '      <div class="form-check form-check-inline mb-1">';
                            html += '        <input class="form-check-input menu-depth3" type="checkbox" value="' + depth3.id + '" id="menu_' + depth3.id + '" data-depth="3" data-parent="' + depth2.id + '" data-root="' + depth1.id + '" onchange="PropertyRoleForm.toggleNode(this)">';
                            html += '        <label class="form-check-label" for="menu_' + depth3.id + '">' + HolaPms.escapeHtml(depth3.menuName) + '</label>';
                            html += '      </div>';
                        });
                        html += '    </div>';
                    }

                    html += '  </div>';
                });
            }

            html += '</div>';
        });

        if (!tree.length) {
            html = '<span class="text-muted">등록된 메뉴가 없습니다.</span>';
        }
        $('#menuTree').html(html);
    },

    /** 노드 토글 → 상하위 연동 (3-depth 지원) */
    toggleNode: function(el) {
        var $el = $(el);
        var depth = parseInt($el.data('depth'), 10);
        var checked = $el.is(':checked');
        var nodeId = $el.val();

        if (depth === 1) {
            $('input.menu-depth2[data-parent="' + nodeId + '"]').prop('checked', checked);
            $('input.menu-depth3[data-root="' + nodeId + '"]').prop('checked', checked);
        } else if (depth === 2) {
            var rootId = $el.data('parent');
            $('input.menu-depth3[data-parent="' + nodeId + '"]').prop('checked', checked);
            this.syncParent('menu-depth2', rootId, 'menu-depth1');
        } else if (depth === 3) {
            var parentId = $el.data('parent');
            var rootId2 = $el.data('root');
            this.syncParent('menu-depth3', parentId, 'menu-depth2');
            this.syncParent('menu-depth2', rootId2, 'menu-depth1');
        }

        this.updateAllCheck();
    },

    syncParent: function(childClass, parentId, parentClass) {
        var total = $('input.' + childClass + '[data-parent="' + parentId + '"]').length;
        var checked = $('input.' + childClass + '[data-parent="' + parentId + '"]:checked').length;
        $('input.' + parentClass + '[value="' + parentId + '"]').prop('checked', total > 0 && checked === total);
    },

    toggleAllMenus: function(el) {
        var checked = $(el).is(':checked');
        $('input.menu-depth1, input.menu-depth2, input.menu-depth3').prop('checked', checked);
    },

    updateAllCheck: function() {
        var total = $('input.menu-depth1, input.menu-depth2, input.menu-depth3').length;
        var checked = $('input.menu-depth1:checked, input.menu-depth2:checked, input.menu-depth3:checked').length;
        $('#menuSelectAll').prop('checked', total > 0 && total === checked);
    },

    /** 수정 시 권한 데이터 로드 */
    loadRole: function() {
        HolaPms.ajax({
            url: '/api/v1/property-admin-roles/' + this.roleId,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                PropertyRoleForm.originalRoleName = data.roleName;
                PropertyRoleForm.originalHotelId = data.hotelId;
                PropertyRoleForm.originalPropertyId = data.propertyId;

                $('#roleName').val(data.roleName);
                $('#hotelId').val(data.hotelId);
                $('#hotelId').prop('disabled', true);
                // 프로퍼티 로드 후 선택값 설정, 수정 모드에서는 변경 불가
                PropertyRoleForm.loadProperties(data.hotelId, data.propertyId);
                PropertyRoleForm._disablePropertyAfterLoad = true;
                $('#updatedAt').text(data.updatedAt || '-');

                if (data.useYn === false) {
                    $('#useYnN').prop('checked', true);
                } else {
                    $('#useYnY').prop('checked', true);
                }

                if (data.menuIds && data.menuIds.length > 0) {
                    PropertyRoleForm.checkMenus(data.menuIds);
                }
            }
        });
    },

    /** 메뉴 체크 복원 (3-depth 지원) */
    checkMenus: function(menuIds) {
        if ($('input.menu-depth1').length === 0 && $('input.menu-depth2').length === 0) {
            setTimeout(function() { PropertyRoleForm.checkMenus(menuIds); }, 200);
            return;
        }
        $('input.menu-depth1, input.menu-depth2, input.menu-depth3').prop('checked', false);
        menuIds.forEach(function(id) {
            $('#menu_' + id).prop('checked', true);
        });
        $('input.menu-depth2').each(function() {
            var mid = $(this).val();
            var total = $('input.menu-depth3[data-parent="' + mid + '"]').length;
            if (total > 0) {
                var checked = $('input.menu-depth3[data-parent="' + mid + '"]:checked').length;
                $(this).prop('checked', checked === total);
            }
        });
        $('input.menu-depth1').each(function() {
            var mid = $(this).val();
            var total = $('input.menu-depth2[data-parent="' + mid + '"]').length;
            if (total > 0) {
                var checked = $('input.menu-depth2[data-parent="' + mid + '"]:checked').length;
                $(this).prop('checked', checked === total);
            }
        });
        this.updateAllCheck();
    },

    /** 권한명 중복확인 */
    checkName: function() {
        var roleName = $.trim($('#roleName').val());
        var hotelId = $('#hotelId').val();
        var propertyId = $('#propertyId').val();
        if (!roleName) {
            HolaPms.alert('warning', '권한명을 입력해주세요.');
            return;
        }
        if (!hotelId) {
            HolaPms.alert('warning', '호텔을 먼저 선택해주세요.');
            return;
        }
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var url = '/api/v1/property-admin-roles/check-name?hotelId=' + hotelId + '&propertyId=' + propertyId + '&roleName=' + encodeURIComponent(roleName);
        if (this.isEdit) {
            url += '&excludeId=' + this.roleId;
        }

        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function(res) {
                if (res.data.duplicate) {
                    PropertyRoleForm.nameChecked = false;
                    $('#nameCheckResult').text('이미 사용 중인 권한명입니다.').removeClass('text-success').addClass('text-danger');
                } else {
                    PropertyRoleForm.nameChecked = true;
                    $('#nameCheckResult').text('사용 가능한 권한명입니다.').removeClass('text-danger').addClass('text-success');
                }
            }
        });
    },

    /** 저장 */
    save: function() {
        var roleName = $.trim($('#roleName').val());
        var $hotelSelect = $('#hotelId');
        var $propertySelect = $('#propertyId');
        var hotelDisabled = $hotelSelect.prop('disabled');
        var propertyDisabled = $propertySelect.prop('disabled');
        if (hotelDisabled) $hotelSelect.prop('disabled', false);
        if (propertyDisabled) $propertySelect.prop('disabled', false);
        var hotelId = $hotelSelect.val();
        var propertyId = $propertySelect.val();
        if (hotelDisabled) $hotelSelect.prop('disabled', true);
        if (propertyDisabled) $propertySelect.prop('disabled', true);

        if (!roleName) {
            HolaPms.alert('warning', '권한명을 입력해주세요.');
            $('#roleName').focus();
            return;
        }
        if (!hotelId) {
            HolaPms.alert('warning', '호텔을 선택해주세요.');
            return;
        }
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 선택해주세요.');
            return;
        }

        if (!this.isEdit && this._propertyBlocked) {
            HolaPms.alert('warning', '해당 프로퍼티에 이미 권한이 설정되어 있습니다.');
            return;
        }

        if (this.isEdit && roleName === this.originalRoleName) {
            this.nameChecked = true;
        }

        if (!this.nameChecked) {
            HolaPms.alert('warning', '권한명 중복확인을 해주세요.');
            return;
        }

        // 선택된 메뉴 수집 (leaf 노드만)
        var menuIds = [];
        $('input.menu-depth3:checked').each(function() {
            menuIds.push(parseInt($(this).val(), 10));
        });
        $('input.menu-depth2:checked').each(function() {
            var mid = $(this).val();
            if ($('input.menu-depth3[data-parent="' + mid + '"]').length === 0) {
                menuIds.push(parseInt(mid, 10));
            }
        });

        var data = {
            roleName: roleName,
            useYn: $('input[name="useYn"]:checked').val() === 'true',
            menuIds: menuIds
        };

        if (!this.isEdit) {
            data.hotelId = parseInt(hotelId, 10);
            data.propertyId = parseInt(propertyId, 10);
        }

        HolaPms.ajax({
            url: this.isEdit
                ? '/api/v1/property-admin-roles/' + this.roleId
                : '/api/v1/property-admin-roles',
            type: this.isEdit ? 'PUT' : 'POST',
            data: data,
            success: function() {
                HolaPms.alertAndRedirect('success', PropertyRoleForm.isEdit ? '권한이 수정되었습니다.' : '권한이 등록되었습니다.', '/admin/roles/property-admins');
            }
        });
    },

    /** 삭제 */
    remove: function() {
        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/property-admin-roles/' + PropertyRoleForm.roleId,
                type: 'DELETE',
                success: function() {
                    HolaPms.alertAndRedirect('success', '권한이 삭제되었습니다.', '/admin/roles/property-admins');
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#roleForm').length) {
        PropertyRoleForm.init();
    }
});
