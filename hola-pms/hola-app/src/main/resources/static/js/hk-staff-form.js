/**
 * 하우스키퍼 담당자 관리 - 등록/수정 폼
 */
var HkStaffForm = {

    propertyId: null,
    staffId: null,
    isEdit: false,
    idChecked: false,

    init: function () {
        this.propertyId = HolaPms.context.getPropertyId();
        this.staffId = (typeof STAFF_ID !== 'undefined' && STAFF_ID) ? STAFF_ID : null;
        this.isEdit = !!this.staffId;

        this.bindEvents();

        if (!this.propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#formCard').hide();
            return;
        }

        if (this.isEdit) {
            this.loadDetail();
            $('#pageTitle').html('<i class="fas fa-user-edit me-2"></i>담당자 수정');
            $('#loginId').prop('readonly', true);
            $('#btnCheckId').hide();
            $('#useYnRow, #passwordRow, #sectionRow, #btnDelete').removeClass('d-none');
            // SUPER_ADMIN만 초기화 버튼 표시
            if (typeof IS_SUPER_ADMIN !== 'undefined' && IS_SUPER_ADMIN) {
                $('#btnResetPw').removeClass('d-none');
            }
            // SUPER_ADMIN 외 담당자명 수정 불가
            if (typeof IS_SUPER_ADMIN === 'undefined' || !IS_SUPER_ADMIN) {
                $('#userName').prop('readonly', true);
            }
        }
    },

    bindEvents: function () {
        var self = this;

        $(document).on('hola:contextChange', function () {
            self.propertyId = HolaPms.context.getPropertyId();
            if (!self.propertyId) {
                $('#contextAlert').removeClass('d-none');
                $('#formCard').hide();
            } else {
                $('#contextAlert').addClass('d-none');
                $('#formCard').show();
            }
        });

        $('#btnCheckId').on('click', function () { self.checkLoginId(); });
        $('#btnSave').on('click', function () { self.save(); });
        $('#btnDelete').on('click', function () { self.deleteStaff(); });
        $('#btnResetPw').on('click', function () { self.resetPassword(); });
        $('#btnChangePw').on('click', function () { self.changePassword(); });

        // 아이디 변경 시 중복확인 리셋
        $('#loginId').on('input', function () { self.idChecked = false; });
    },

    loadDetail: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeepers/' + self.staffId,
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    var d = res.data;
                    $('#loginId').val(d.loginId);
                    $('#userName').val(d.userName);
                    $('#role').val(d.role);
                    $('#phone').val(d.phone || '');
                    $('#email').val(d.email || '');
                    $('#department').val(d.department || '하우스키핑');
                    $('#position').val(d.position || '');
                    $('input[name="useYn"][value="' + d.useYn + '"]').prop('checked', true);
                    // 담당 구역 표시
                    if (d.sectionName) {
                        $('#sectionName').text(d.sectionName);
                        $('#sectionBtnLabel').text('변경');
                    } else {
                        $('#sectionName').text('미배정');
                        $('#sectionBtnLabel').text('구역 설정');
                    }
                }
            }
        });
    },

    checkLoginId: function () {
        var self = this;
        var loginId = $('#loginId').val().trim();
        if (!loginId) {
            HolaPms.alert('warning', '아이디를 입력해주세요.');
            return;
        }
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeepers/check-login-id?loginId=' + loginId,
            method: 'GET',
            success: function (res) {
                if (res.success && res.data.available) {
                    HolaPms.alert('success', '사용 가능한 아이디입니다.');
                    self.idChecked = true;
                } else {
                    HolaPms.alert('danger', '이미 사용 중인 아이디입니다.');
                    self.idChecked = false;
                }
            }
        });
    },

    save: function () {
        var self = this;
        var loginId = $('#loginId').val().trim();
        var userName = $('#userName').val().trim();

        if (!loginId || !userName) {
            HolaPms.alert('warning', '아이디와 담당자명은 필수입니다.');
            return;
        }

        if (!self.isEdit && !self.idChecked) {
            HolaPms.alert('warning', '아이디 중복확인을 해주세요.');
            return;
        }

        var data = {
            userName: userName,
            role: $('#role').val(),
            phone: HolaPms.form.val('#phone'),
            email: HolaPms.form.val('#email'),
            department: HolaPms.form.val('#department'),
            position: HolaPms.form.val('#position')
        };

        if (self.isEdit) {
            data.useYn = $('input[name="useYn"]:checked').val() === 'true';
            HolaPms.ajax({
                url: '/api/v1/properties/' + self.propertyId + '/housekeepers/' + self.staffId,
                method: 'PUT',
                data: JSON.stringify(data),
                success: function (res) {
                    if (res.success) {
                        HolaPms.alertAndRedirect('success', '담당자가 수정되었습니다.', '/admin/housekeeping/staff');
                    }
                }
            });
        } else {
            data.loginId = loginId;
            HolaPms.ajax({
                url: '/api/v1/properties/' + self.propertyId + '/housekeepers',
                method: 'POST',
                data: JSON.stringify(data),
                success: function (res) {
                    if (res.success) {
                        HolaPms.alertAndRedirect('success',
                            '담당자가 등록되었습니다.\n초기 비밀번호: holapms1!',
                            '/admin/housekeeping/staff');
                    }
                }
            });
        }
    },

    deleteStaff: function () {
        var self = this;
        if (!confirm('이 담당자를 삭제하시겠습니까?')) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeepers/' + self.staffId,
            method: 'DELETE',
            success: function (res) {
                if (res.success) {
                    HolaPms.alertAndRedirect('success', '담당자가 삭제되었습니다.', '/admin/housekeeping/staff');
                }
            }
        });
    },

    changePassword: function () {
        var self = this;
        var newPw = $('#newPassword').val();
        var confirmPw = $('#confirmPassword').val();

        if (!newPw) {
            HolaPms.alert('warning', '새 비밀번호를 입력해주세요.');
            return;
        }
        if (newPw.length < 10 || newPw.length > 20) {
            HolaPms.alert('warning', '비밀번호는 10~20자여야 합니다.');
            return;
        }
        // 영문+숫자+특수문자 포함 검증
        if (!/[a-zA-Z]/.test(newPw) || !/[0-9]/.test(newPw) || !/[!@#$%^&*(),.?":{}|<>]/.test(newPw)) {
            HolaPms.alert('warning', '영문, 숫자, 특수문자를 모두 포함해야 합니다.');
            return;
        }
        if (newPw !== confirmPw) {
            HolaPms.alert('warning', '비밀번호와 확인이 일치하지 않습니다.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeepers/' + self.staffId + '/change-password',
            method: 'PUT',
            data: JSON.stringify({ newPassword: newPw }),
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '비밀번호가 변경되었습니다.');
                    $('#newPassword, #confirmPassword').val('');
                }
            }
        });
    },

    resetPassword: function () {
        var self = this;
        if (!confirm('비밀번호를 초기화하시겠습니까?\n초기화 비밀번호: holapms1!')) return;

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/housekeepers/' + self.staffId + '/reset-password',
            method: 'PUT',
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '비밀번호가 초기화되었습니다. (holapms1!)');
                }
            }
        });
    }
};

$(function () {
    HkStaffForm.init();
});
