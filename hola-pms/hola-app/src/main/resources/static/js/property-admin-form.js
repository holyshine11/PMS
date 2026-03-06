/**
 * 프로퍼티 관리자 등록/수정 폼 JS
 */
const PropertyAdminForm = {
    isEdit: false,
    adminId: null,
    propertyId: null,
    loginIdChecked: false,
    originalLoginId: '',
    _savedRoleId: null,

    init: function() {
        this.adminId = $('#adminId').val() || null;
        this.isEdit = !!this.adminId;
        this.propertyId = HolaPms.context.getPropertyId();

        // 호텔명/프로퍼티명 표시
        var hotelName = HolaPms.context.getHotelName ? HolaPms.context.getHotelName() : '';
        var propertyName = HolaPms.context.getPropertyName ? HolaPms.context.getPropertyName() : '';
        $('#hotelName').val(hotelName);
        $('#propertyName').val(propertyName);

        if (this.isEdit) {
            $('#pageTitle').html('<i class="fas fa-user-cog me-2"></i>프로퍼티 관리자 수정');
            $('#btnSaveText').text('저장');
            $('#loginId').prop('readonly', true);
            $('#btnCheckLoginId').hide();
            $('#btnDelete').show();
            $('#resetPwdArea').show();
            this.loginIdChecked = true;
            this.loadAdmin();
        }

        // 권한 로드 (호텔 기준)
        var hotelId = HolaPms.context.getHotelId ? HolaPms.context.getHotelId() : null;
        if (hotelId) {
            this.loadRoles(hotelId);
        }

        // 컨텍스트(호텔+프로퍼티) 변경 이벤트
        $(document).on('hola:contextChange', function() {
            // 수정 모드: 프로퍼티/호텔 변경 시 목록 페이지로 이동
            if (PropertyAdminForm.isEdit) {
                var newPropertyId = HolaPms.context.getPropertyId();
                if (newPropertyId && newPropertyId !== String(PropertyAdminForm.propertyId)) {
                    location.href = '/admin/members/property-admins';
                }
                return;
            }

            PropertyAdminForm.propertyId = HolaPms.context.getPropertyId();
            var hName = HolaPms.context.getHotelName ? HolaPms.context.getHotelName() : '';
            var pName = HolaPms.context.getPropertyName ? HolaPms.context.getPropertyName() : '';
            $('#hotelName').val(hName);
            $('#propertyName').val(pName);
            // 호텔 변경 시 권한 목록도 갱신
            var newHotelId = HolaPms.context.getHotelId ? HolaPms.context.getHotelId() : null;
            if (newHotelId) {
                PropertyAdminForm.loadRoles(newHotelId);
            }
        });

        // 아이디 변경 시 중복확인 초기화
        $('#loginId').on('input', function() {
            PropertyAdminForm.loginIdChecked = false;
            $('#loginIdCheckResult').text('').removeClass('text-success text-danger');
        });
    },

    loadAdmin: function() {
        if (!this.propertyId) return;
        HolaPms.ajax({
            url: '/api/v1/properties/' + this.propertyId + '/admins/' + this.adminId,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                PropertyAdminForm.originalLoginId = data.loginId;
                PropertyAdminForm.propertyId = data.propertyId;

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
                    PropertyAdminForm._savedRoleId = data.roleId;
                    if ($('#roleId option').length > 1) {
                        $('#roleId').val(data.roleId);
                    }
                }

                if (data.useYn === false) {
                    $('#useYnN').prop('checked', true);
                } else {
                    $('#useYnY').prop('checked', true);
                }

                // 호텔명/프로퍼티명 표시
                if (data.hotelName) {
                    $('#hotelName').val(data.hotelName);
                }
                if (data.propertyName) {
                    $('#propertyName').val(data.propertyName);
                }

                // 호텔 기준 권한 로드
                if (data.hotelId) {
                    PropertyAdminForm.loadRoles(data.hotelId);
                }
            }
        });
    },

    /** 권한 목록 로드 (드롭다운) - 프로퍼티 관리자 권한 */
    loadRoles: function(hotelId) {
        if (!hotelId) return;
        var propertyId = PropertyAdminForm.propertyId;
        var url = '/api/v1/property-admin-roles/selector?hotelId=' + hotelId;
        if (propertyId) {
            url += '&propertyId=' + propertyId;
        }
        HolaPms.ajax({
            url: url,
            type: 'GET',
            success: function(res) {
                var roles = res.data || [];
                var $select = $('#roleId');
                $select.find('option:not(:first)').remove();
                roles.forEach(function(r) {
                    $select.append('<option value="' + r.id + '">' + HolaPms.escapeHtml(r.roleName) + '</option>');
                });
                // 수정 모드: 저장된 roleId로 항상 재설정
                if (PropertyAdminForm._savedRoleId) {
                    $select.val(PropertyAdminForm._savedRoleId);
                }
            }
        });
    },

    checkLoginId: function() {
        var loginId = $.trim($('#loginId').val());
        if (!loginId) {
            HolaPms.alert('warning', '아이디를 입력해주세요.');
            return;
        }
        if (!this.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + this.propertyId + '/admins/check-login-id?loginId=' + encodeURIComponent(loginId),
            type: 'GET',
            success: function(res) {
                if (res.data.duplicate) {
                    PropertyAdminForm.loginIdChecked = false;
                    $('#loginIdCheckResult').text('이미 사용 중인 아이디입니다.').removeClass('text-success').addClass('text-danger');
                } else {
                    PropertyAdminForm.loginIdChecked = true;
                    $('#loginIdCheckResult').text('사용 가능한 아이디입니다.').removeClass('text-danger').addClass('text-success');
                }
            }
        });
    },

    resetPassword: function() {
        HolaPms.confirm('비밀번호를 초기화하시겠습니까?\n초기화 비밀번호: holapms1!', function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + PropertyAdminForm.propertyId + '/admins/' + PropertyAdminForm.adminId + '/reset-password',
                type: 'PUT',
                success: function() {
                    HolaPms.alert('success', '비밀번호가 초기화되었습니다. (holapms1!)');
                }
            });
        });
    },

    save: function() {
        // 프로퍼티 선택 확인
        if (!this.propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
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
            useYn: $('input[name="useYn"]:checked').val() === 'true'
        };

        if (!this.isEdit) {
            data.loginId = loginId;
        }

        var url = this.isEdit
            ? '/api/v1/properties/' + this.propertyId + '/admins/' + this.adminId
            : '/api/v1/properties/' + this.propertyId + '/admins';
        var method = this.isEdit ? 'PUT' : 'POST';

        HolaPms.ajax({
            url: url,
            type: method,
            data: data,
            success: function(res) {
                HolaPms.alert('success', PropertyAdminForm.isEdit ? '프로퍼티 관리자가 수정되었습니다.' : '프로퍼티 관리자가 등록되었습니다.');
                setTimeout(function() {
                    location.href = '/admin/members/property-admins';
                }, 500);
            }
        });
    },

    remove: function() {
        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/properties/' + PropertyAdminForm.propertyId + '/admins/' + PropertyAdminForm.adminId,
                type: 'DELETE',
                success: function() {
                    HolaPms.alert('success', '프로퍼티 관리자가 삭제되었습니다.');
                    setTimeout(function() {
                        location.href = '/admin/members/property-admins';
                    }, 500);
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#propertyAdminForm').length) {
        PropertyAdminForm.init();
    }
});
