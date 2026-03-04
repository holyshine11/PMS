/**
 * 호텔 관리자 등록/수정 폼 JS
 */
const HotelAdminForm = {
    isEdit: false,
    adminId: null,
    hotelId: null,
    loginIdChecked: false,
    originalLoginId: '',

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

        // 프로퍼티 로드
        if (this.hotelId) {
            this.loadProperties();
        }

        // 컨텍스트(호텔) 변경 이벤트
        $(document).on('hola:contextChange', function() {
            HotelAdminForm.hotelId = HolaPms.context.getHotelId();
            var name = HolaPms.context.getHotelName ? HolaPms.context.getHotelName() : '';
            $('#hotelName').val(name);
            if (HotelAdminForm.hotelId) {
                HotelAdminForm.loadProperties();
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
                $('#roleName').val(data.roleName || '');

                if (data.useYn === false) {
                    $('#useYnN').prop('checked', true);
                } else {
                    $('#useYnY').prop('checked', true);
                }

                // 호텔명 표시
                if (data.hotelName) {
                    $('#hotelName').val(data.hotelName);
                }

                // 프로퍼티 체크
                if (data.propertyIds && data.propertyIds.length > 0) {
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

                // 수정 모드일 때 프로퍼티 재체크
                if (HotelAdminForm.isEdit && HotelAdminForm._pendingPropertyIds) {
                    HotelAdminForm.checkProperties(HotelAdminForm._pendingPropertyIds);
                    HotelAdminForm._pendingPropertyIds = null;
                }
            }
        });
    },

    checkProperties: function(propertyIds) {
        if (!propertyIds) return;
        // 프로퍼티 체크박스가 아직 로드되지 않았으면 보류
        if ($('.property-check').length === 0) {
            this._pendingPropertyIds = propertyIds;
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
        var roleName = $.trim($('#roleName').val());

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
        if (!roleName) {
            HolaPms.alert('warning', '호텔관리자 권한을 입력해주세요.');
            $('#roleName').focus();
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
            roleName: roleName,
            useYn: $('input[name="useYn"]:checked').val() === 'true',
            propertyIds: propertyIds
        };

        if (!this.isEdit) {
            data.loginId = loginId;
        }

        HolaPms.ajax({
            url: this.isEdit
                ? '/api/v1/hotels/' + this.hotelId + '/admins/' + this.adminId
                : '/api/v1/hotels/' + this.hotelId + '/admins',
            type: this.isEdit ? 'PUT' : 'POST',
            data: data,
            success: function() {
                HolaPms.alert('success', HotelAdminForm.isEdit ? '호텔 관리자가 수정되었습니다.' : '호텔 관리자가 등록되었습니다.');
                setTimeout(function() {
                    location.href = '/admin/members/hotel-admins';
                }, 500);
            }
        });
    },

    remove: function() {
        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/hotels/' + HotelAdminForm.hotelId + '/admins/' + HotelAdminForm.adminId,
                type: 'DELETE',
                success: function() {
                    HolaPms.alert('success', '호텔 관리자가 삭제되었습니다.');
                    setTimeout(function() {
                        location.href = '/admin/members/hotel-admins';
                    }, 500);
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
