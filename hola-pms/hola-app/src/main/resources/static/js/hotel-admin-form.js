/**
 * 호텔 관리자 등록/수정 폼 JS
 */
const HotelAdminForm = {
    isEdit: false,
    adminId: null,
    hotelId: null,
    loginIdChecked: false,
    originalLoginId: '',
    _savedRoleId: null,
    _savedPropertyIds: null,

    init: function() {
        this.adminId = $('#adminId').val() || null;
        this.isEdit = !!this.adminId;
        this.hotelId = HolaPms.context.getHotelId();

        // 호텔명 표시
        var hotelName = HolaPms.context.getHotelName ? HolaPms.context.getHotelName() : '';
        $('#hotelName').val(hotelName);

        if (this.isEdit) {
            $('#pageTitle').html('<i class="fas fa-user-tie me-2"></i>호텔 관리자 수정');
            $('#btnSaveText').text('저장');
            $('#loginId').prop('readonly', true);
            $('#btnCheckLoginId').hide();
            $('#btnDelete').show();
            $('#resetPwdArea').show();
            this.loginIdChecked = true;
            this.loadAdmin();
        }

        // 프로퍼티 + 권한 로드
        if (this.hotelId) {
            this.loadProperties();
            this.loadRoles();
        }

        // 컨텍스트(호텔) 변경 이벤트
        $(document).on('hola:contextChange', function() {
            // 수정 모드: 호텔 변경 시 목록 페이지로 이동
            if (HotelAdminForm.isEdit) {
                var newHotelId = HolaPms.context.getHotelId();
                if (newHotelId && newHotelId !== String(HotelAdminForm.hotelId)) {
                    location.href = '/admin/members/hotel-admins';
                }
                return;
            }

            HotelAdminForm.hotelId = HolaPms.context.getHotelId();
            var name = HolaPms.context.getHotelName ? HolaPms.context.getHotelName() : '';
            $('#hotelName').val(name);
            if (HotelAdminForm.hotelId) {
                HotelAdminForm.loadProperties();
                HotelAdminForm.loadRoles();
            }
        });

        // 아이디 변경 시 중복확인 초기화
        $('#loginId').on('input', function() {
            HotelAdminForm.loginIdChecked = false;
            $('#loginIdCheckResult').text('').removeClass('text-success text-danger');
        });
    },

    loadAdmin: function() {
        if (!this.hotelId) return;
        HolaPms.ajax({
            url: '/api/v1/hotels/' + this.hotelId + '/admins/' + this.adminId,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                HotelAdminForm.originalLoginId = data.loginId;
                HotelAdminForm.hotelId = data.hotelId;

                $('#memberNumber').text(data.memberNumber || '-');
                $('#createdAt').text(data.createdAt || '-');
                $('#loginId').val(data.loginId);
                $('#userName').val(data.userName);
                $('#email').val(data.email || '');
                $('#phone').val(data.phone || '');
                $('#phoneCountryCode').val(data.phoneCountryCode || '');
                $('#mobileCountryCode').val(data.mobileCountryCode || '');
                $('#mobile').val(data.mobile || '');
                $('#department').val(data.department || '');
                $('#position').val(data.position || '');

                // 권한 저장 후 드롭다운 설정 (영구 보관)
                if (data.roleId) {
                    HotelAdminForm._savedRoleId = data.roleId;
                    if ($('#roleId option').length > 1) {
                        $('#roleId').val(data.roleId);
                    }
                }

                if (data.useYn === false) {
                    $('#useYnN').prop('checked', true);
                } else {
                    $('#useYnY').prop('checked', true);
                }

                // 호텔명 표시
                if (data.hotelName) {
                    $('#hotelName').val(data.hotelName);
                }

                // 프로퍼티 저장 후 체크 (영구 보관)
                if (data.propertyIds && data.propertyIds.length > 0) {
                    HotelAdminForm._savedPropertyIds = data.propertyIds;
                    HotelAdminForm.checkProperties(data.propertyIds);
                }
            }
        });
    },

    loadProperties: function() {
        if (!this.hotelId) return;
        HolaPms.ajax({
            url: '/api/v1/hotels/' + this.hotelId + '/properties',
            type: 'GET',
            success: function(res) {
                var properties = res.data || [];
                var html = '';
                properties.forEach(function(p) {
                    html += '<div class="form-check form-check-inline mb-1">';
                    html += '<input class="form-check-input property-check" type="checkbox" value="' + p.id + '" id="prop_' + p.id + '" onchange="HotelAdminForm.updateAllCheck()">';
                    html += '<label class="form-check-label" for="prop_' + p.id + '">' + HolaPms.escapeHtml(p.propertyName) + '</label>';
                    html += '</div>';
                });
                if (!properties.length) {
                    html = '<span class="text-muted">등록된 프로퍼티가 없습니다.</span>';
                }
                $('#propertyList').html(html);

                // 수정 모드: 저장된 프로퍼티 ID로 항상 재체크
                if (HotelAdminForm.isEdit && HotelAdminForm._savedPropertyIds) {
                    HotelAdminForm.checkProperties(HotelAdminForm._savedPropertyIds);
                }
            }
        });
    },

    /** 권한 목록 로드 (드롭다운) */
    loadRoles: function() {
        if (!this.hotelId) return;
        HolaPms.ajax({
            url: '/api/v1/hotel-admin-roles/selector?hotelId=' + this.hotelId,
            type: 'GET',
            success: function(res) {
                var roles = res.data || [];
                var $select = $('#roleId');
                $select.find('option:not(:first)').remove();
                roles.forEach(function(r) {
                    $select.append('<option value="' + r.id + '">' + HolaPms.escapeHtml(r.roleName) + '</option>');
                });
                // 수정 모드: 저장된 roleId로 항상 재설정
                if (HotelAdminForm._savedRoleId) {
                    $select.val(HotelAdminForm._savedRoleId);
                }
            }
        });
    },

    checkProperties: function(propertyIds) {
        if (!propertyIds) return;
        // 프로퍼티 체크박스가 아직 로드되지 않았으면 무시 (loadProperties 완료 시 재시도)
        if ($('.property-check').length === 0) {
            return;
        }
        $('.property-check').prop('checked', false);
        propertyIds.forEach(function(id) {
            $('#prop_' + id).prop('checked', true);
        });
        this.updateAllCheck();
    },

    toggleAllProperties: function(el) {
        $('.property-check').prop('checked', $(el).is(':checked'));
    },

    updateAllCheck: function() {
        var total = $('.property-check').length;
        var checked = $('.property-check:checked').length;
        $('#propertyAll').prop('checked', total > 0 && total === checked);
    },

    checkLoginId: function() {
        var loginId = $.trim($('#loginId').val());
        if (!loginId) {
            HolaPms.alert('warning', '아이디를 입력해주세요.');
            return;
        }
        if (!this.hotelId) {
            HolaPms.alert('warning', '호텔을 먼저 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/hotels/' + this.hotelId + '/admins/check-login-id?loginId=' + encodeURIComponent(loginId),
            type: 'GET',
            success: function(res) {
                if (res.data.duplicate) {
                    HotelAdminForm.loginIdChecked = false;
                    $('#loginIdCheckResult').text('이미 사용 중인 아이디입니다.').removeClass('text-success').addClass('text-danger');
                } else {
                    HotelAdminForm.loginIdChecked = true;
                    $('#loginIdCheckResult').text('사용 가능한 아이디입니다.').removeClass('text-danger').addClass('text-success');
                }
            }
        });
    },

    resetPassword: function() {
        HolaPms.confirm('비밀번호를 초기화하시겠습니까?\n초기화 비밀번호: holapms1!', function() {
            HolaPms.ajax({
                url: '/api/v1/hotels/' + HotelAdminForm.hotelId + '/admins/' + HotelAdminForm.adminId + '/reset-password',
                type: 'PUT',
                success: function() {
                    HolaPms.alert('success', '비밀번호가 초기화되었습니다. (holapms1!)');
                }
            });
        });
    },

    save: function() {
        // 호텔 선택 확인
        if (!this.hotelId) {
            HolaPms.alert('warning', '호텔을 먼저 선택해주세요.');
            return;
        }

        // 필수값 검증
        var loginId = $.trim($('#loginId').val());
        var userName = $.trim($('#userName').val());
        var email = $.trim($('#email').val());
        var phone = $.trim($('#phone').val());
        var roleId = $('#roleId').val();

        if (!this.isEdit && !loginId) {
            HolaPms.alert('warning', '아이디를 입력해주세요.');
            $('#loginId').focus();
            return;
        }
        if (!userName) {
            HolaPms.alert('warning', '담당자명을 입력해주세요.');
            $('#userName').focus();
            return;
        }
        if (!email) {
            HolaPms.alert('warning', '이메일을 입력해주세요.');
            $('#email').focus();
            return;
        }
        if (!phone) {
            HolaPms.alert('warning', '연락처를 입력해주세요.');
            $('#phone').focus();
            return;
        }

        // 아이디 중복확인 (등록 시만)
        if (!this.isEdit && !this.loginIdChecked) {
            HolaPms.alert('warning', '아이디 중복확인을 해주세요.');
            return;
        }

        // 프로퍼티 선택 확인
        var propertyIds = [];
        $('.property-check:checked').each(function() {
            propertyIds.push(parseInt($(this).val(), 10));
        });
        if (propertyIds.length === 0) {
            HolaPms.alert('warning', '프로퍼티를 1개 이상 선택해주세요.');
            return;
        }

        var data = {
            userName: userName,
            email: email,
            phone: phone,
            phoneCountryCode: $('#phoneCountryCode').val() || null,
            mobileCountryCode: $('#mobileCountryCode').val() || null,
            mobile: HolaPms.form.val('#mobile'),
            department: HolaPms.form.val('#department'),
            position: HolaPms.form.val('#position'),
            roleId: roleId ? parseInt(roleId, 10) : null,
            useYn: $('input[name="useYn"]:checked').val() === 'true',
            propertyIds: propertyIds
        };

        if (!this.isEdit) {
            data.loginId = loginId;
        }

        var url = this.isEdit
            ? '/api/v1/hotels/' + this.hotelId + '/admins/' + this.adminId
            : '/api/v1/hotels/' + this.hotelId + '/admins';
        var method = this.isEdit ? 'PUT' : 'POST';

        HolaPms.ajax({
            url: url,
            type: method,
            data: data,
            success: function(res) {
                HolaPms.alertAndRedirect('success', HotelAdminForm.isEdit ? '호텔 관리자가 수정되었습니다.' : '호텔 관리자가 등록되었습니다.', '/admin/members/hotel-admins');
            }
        });
    },

    remove: function() {
        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/hotels/' + HotelAdminForm.hotelId + '/admins/' + HotelAdminForm.adminId,
                type: 'DELETE',
                success: function() {
                    HolaPms.alertAndRedirect('success', '호텔 관리자가 삭제되었습니다.', '/admin/members/hotel-admins');
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#hotelAdminForm').length) {
        HotelAdminForm.init();
    }
});
