/**
 * 하우스키핑 모바일 - 내 정보 수정
 */
var HkMobileProfile = {

    propertyId: null,
    userId: null,

    init: function () {
        this.propertyId = (typeof HK_PROPERTY_ID !== 'undefined') ? HK_PROPERTY_ID : null;
        this.userId = (typeof HK_USER_ID !== 'undefined') ? HK_USER_ID : null;
        if (!this.propertyId) return;
        this.bindEvents();
        this.loadProfile();
    },

    bindEvents: function () {
        var self = this;
        $('#btnSaveProfile').on('click', function () { self.saveProfile(); });
        $('#btnChangePw').on('click', function () { self.changePassword(); });
    },

    loadProfile: function () {
        var self = this;
        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-profile',
            method: 'GET',
            success: function (res) {
                if (res.success) {
                    var d = res.data;
                    $('#profileName').text(d.userName);
                    var roleMap = { 'HOUSEKEEPER': '청소 담당', 'HOUSEKEEPING_SUPERVISOR': '감독자' };
                    $('#profileRole').text(roleMap[d.role] || d.role);
                    $('#profilePhone').val(d.phone || '');
                    $('#profileEmail').val(d.email || '');
                }
            }
        });
    },

    saveProfile: function () {
        var self = this;
        var data = {
            phone: HolaPms.form.val('#profilePhone'),
            email: HolaPms.form.val('#profileEmail')
        };

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-profile',
            method: 'PUT',
            data: JSON.stringify(data),
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '정보가 저장되었습니다.');
                }
            }
        });
    },

    changePassword: function () {
        var self = this;
        var currentPw = $('#currentPassword').val();
        var newPw = $('#newPassword').val();
        var confirmPw = $('#confirmPassword').val();

        if (!currentPw) {
            HolaPms.alert('warning', '현재 비밀번호를 입력해주세요.');
            return;
        }
        if (!newPw) {
            HolaPms.alert('warning', '새 비밀번호를 입력해주세요.');
            return;
        }
        if (newPw.length < 10 || newPw.length > 20) {
            HolaPms.alert('warning', '비밀번호는 10~20자여야 합니다.');
            return;
        }
        if (!/[a-zA-Z]/.test(newPw) || !/[0-9]/.test(newPw) || !/[!@#$%^&*(),.?":{}|<>]/.test(newPw)) {
            HolaPms.alert('warning', '영문, 숫자, 특수문자를 모두 포함해야 합니다.');
            return;
        }
        if (newPw !== confirmPw) {
            HolaPms.alert('warning', '비밀번호가 일치하지 않습니다.');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-profile/change-password',
            method: 'PUT',
            data: JSON.stringify({ currentPassword: currentPw, newPassword: newPw }),
            success: function (res) {
                if (res.success) {
                    HolaPms.alert('success', '비밀번호가 변경되었습니다.');
                    $('#currentPassword, #newPassword, #confirmPassword').val('');
                }
            }
        });
    }
};

$(function () {
    HkMobileProfile.init();
});
