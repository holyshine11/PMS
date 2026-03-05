/**
 * 블루웨이브 관리자 등록/수정 폼 JS
 */
const BluewaveAdminForm = {
    isEdit: false,
    adminId: null,
    loginIdChecked: false,
    originalLoginId: '',

    init: function() {
        this.adminId = $('#adminId').val() || null;
        this.isEdit = !!this.adminId;

        if (this.isEdit) {
            $('#pageTitle').html('<i class="fas fa-user-shield me-2"></i>블루웨이브 관리자 수정');
            $('#btnSaveText').text('저장');
            $('#loginId').prop('readonly', true);
            $('#btnCheckLoginId').hide();
            $('#btnDelete').show();
            $('#resetPwdArea').show();
            this.loginIdChecked = true;
            this.loadAdmin();
        }

        // 아이디 변경 시 중복확인 초기화
        $('#loginId').on('input', function() {
            BluewaveAdminForm.loginIdChecked = false;
            $('#loginIdCheckResult').text('').removeClass('text-success text-danger');
        });
    },

    loadAdmin: function() {
        HolaPms.ajax({
            url: '/api/v1/bluewave-admins/' + this.adminId,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                BluewaveAdminForm.originalLoginId = data.loginId;

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
            }
        });
    },

    checkLoginId: function() {
        var loginId = $.trim($('#loginId').val());
        if (!loginId) {
            HolaPms.alert('warning', '아이디를 입력해주세요.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/bluewave-admins/check-login-id?loginId=' + encodeURIComponent(loginId),
            type: 'GET',
            success: function(res) {
                if (res.data.duplicate) {
                    BluewaveAdminForm.loginIdChecked = false;
                    $('#loginIdCheckResult').text('이미 사용 중인 아이디입니다.').removeClass('text-success').addClass('text-danger');
                } else {
                    BluewaveAdminForm.loginIdChecked = true;
                    $('#loginIdCheckResult').text('사용 가능한 아이디입니다.').removeClass('text-danger').addClass('text-success');
                }
            }
        });
    },

    resetPassword: function() {
        HolaPms.confirm('비밀번호를 초기화하시겠습니까?\n초기화 비밀번호: holapms1!', function() {
            HolaPms.ajax({
                url: '/api/v1/bluewave-admins/' + BluewaveAdminForm.adminId + '/reset-password',
                type: 'PUT',
                success: function() {
                    HolaPms.alert('success', '비밀번호가 초기화되었습니다. (holapms1!)');
                }
            });
        });
    },

    save: function() {
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
            HolaPms.alert('warning', '블루웨이브관리자 권한을 입력해주세요.');
            $('#roleName').focus();
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
            roleName: roleName,
            useYn: $('input[name="useYn"]:checked').val() === 'true'
        };

        if (!this.isEdit) {
            data.loginId = loginId;
        }

        HolaPms.ajax({
            url: this.isEdit
                ? '/api/v1/bluewave-admins/' + this.adminId
                : '/api/v1/bluewave-admins',
            type: this.isEdit ? 'PUT' : 'POST',
            data: data,
            success: function() {
                HolaPms.alert('success', BluewaveAdminForm.isEdit ? '블루웨이브 관리자가 수정되었습니다.' : '블루웨이브 관리자가 등록되었습니다.');
                setTimeout(function() {
                    location.href = '/admin/members/bluewave-admins';
                }, 500);
            }
        });
    },

    remove: function() {
        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/bluewave-admins/' + BluewaveAdminForm.adminId,
                type: 'DELETE',
                success: function() {
                    HolaPms.alert('success', '블루웨이브 관리자가 삭제되었습니다.');
                    setTimeout(function() {
                        location.href = '/admin/members/bluewave-admins';
                    }, 500);
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#bluewaveAdminForm').length) {
        BluewaveAdminForm.init();
    }
});
