/**
 * 개인정보 수정 폼 JS
 */
const MyProfileForm = {

    ACCOUNT_TYPE_LABELS: {
        'BLUEWAVE_ADMIN': '블루웨이브 관리자',
        'HOTEL_ADMIN': '호텔 관리자',
        'PROPERTY_ADMIN': '프로퍼티 관리자'
    },

    init: function() {
        this.loadProfile();
    },

    loadProfile: function() {
        HolaPms.ajax({
            url: '/api/v1/my-profile',
            type: 'GET',
            success: function(res) {
                var data = res.data;
                // 읽기전용 필드
                $('#loginId').text(data.loginId || '-');
                $('#memberNumber').text(data.memberNumber || '-');
                $('#accountType').text(MyProfileForm.ACCOUNT_TYPE_LABELS[data.accountType] || data.accountType || '-');
                $('#roleName').text(data.roleName || '-');
                $('#createdAt').text(data.createdAt || '-');

                // 수정 가능 필드
                $('#userName').val(data.userName || '');
                $('#email').val(data.email || '');
                $('#phoneCountryCode').val(data.phoneCountryCode || '');
                $('#phone').val(data.phone || '');
                $('#mobileCountryCode').val(data.mobileCountryCode || '');
                $('#mobile').val(data.mobile || '');
                $('#department').val(data.department || '');
                $('#position').val(data.position || '');
            }
        });
    },

    saveProfile: function() {
        var userName = $.trim($('#userName').val());
        if (!userName) {
            HolaPms.alert('warning', '담당자명을 입력해주세요.');
            $('#userName').focus();
            return;
        }

        var data = {
            userName: userName,
            email: HolaPms.form.val('#email'),
            phoneCountryCode: $('#phoneCountryCode').val() || null,
            phone: HolaPms.form.val('#phone'),
            mobileCountryCode: $('#mobileCountryCode').val() || null,
            mobile: HolaPms.form.val('#mobile'),
            department: HolaPms.form.val('#department'),
            position: HolaPms.form.val('#position')
        };

        HolaPms.ajax({
            url: '/api/v1/my-profile',
            type: 'PUT',
            data: data,
            success: function() {
                HolaPms.alert('success', '프로필이 수정되었습니다.');
                MyProfileForm.loadProfile();
            }
        });
    },

    changePassword: function() {
        var currentPassword = $('#currentPassword').val();
        var newPassword = $('#newPassword').val();
        var confirmPassword = $('#confirmPassword').val();

        if (!currentPassword) {
            HolaPms.alert('warning', '현재 비밀번호를 입력해주세요.');
            $('#currentPassword').focus();
            return;
        }
        if (!newPassword) {
            HolaPms.alert('warning', '새 비밀번호를 입력해주세요.');
            $('#newPassword').focus();
            return;
        }
        if (newPassword.length < 10 || newPassword.length > 20) {
            HolaPms.alert('warning', '비밀번호는 10~20자입니다.');
            $('#newPassword').focus();
            return;
        }
        if (!confirmPassword) {
            HolaPms.alert('warning', '비밀번호 확인을 입력해주세요.');
            $('#confirmPassword').focus();
            return;
        }
        if (newPassword !== confirmPassword) {
            HolaPms.alert('warning', '새 비밀번호와 확인 비밀번호가 일치하지 않습니다.');
            $('#confirmPassword').focus();
            return;
        }

        // 영문+숫자+특수문자 조합 검증
        var pwdPattern = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{10,20}$/;
        if (!pwdPattern.test(newPassword)) {
            HolaPms.alert('warning', '비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.');
            $('#newPassword').focus();
            return;
        }

        HolaPms.confirm('비밀번호를 변경하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/my-profile/password',
                type: 'PUT',
                data: {
                    currentPassword: currentPassword,
                    newPassword: newPassword,
                    confirmPassword: confirmPassword
                },
                success: function() {
                    HolaPms.alert('success', '비밀번호가 변경되었습니다.');
                    // 비밀번호 필드 초기화
                    $('#currentPassword').val('');
                    $('#newPassword').val('');
                    $('#confirmPassword').val('');
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#profileForm').length) {
        MyProfileForm.init();
    }
});
