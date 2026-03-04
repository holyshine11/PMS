/**
 * 호텔 등록/수정 폼 JS
 */
const HotelForm = {
    isEdit: false,
    hotelId: null,
    nameChecked: false,
    originalName: '',

    init: function() {
        this.hotelId = $('#hotelId').val() || null;
        this.isEdit = !!this.hotelId;

        if (this.isEdit) {
            $('#pageTitle').html('<i class="fas fa-hotel me-2"></i>호텔 수정');
            $('#hotelCodeRow').show();
            $('#btnDelete').show();
            this.loadHotel();
        }

        // 호텔명 변경 시 중복확인 초기화
        $('#hotelName').on('input', function() {
            HotelForm.nameChecked = false;
            $('#nameCheckResult').text('').removeClass('text-success text-danger');
        });
    },

    loadHotel: function() {
        HolaPms.ajax({
            url: '/api/v1/hotels/' + this.hotelId,
            type: 'GET',
            success: function(res) {
                var data = res.data;
                HotelForm.originalName = data.hotelName;
                $('#hotelCode').val(data.hotelCode);
                $('#hotelName').val(data.hotelName);
                $('#representativeName').val(data.representativeName || '');
                $('#representativeNameEn').val(data.representativeNameEn || '');
                $('#countryCode').val(data.countryCode || '');
                $('#phone').val(data.phone || '');
                $('#zipCode').val(data.zipCode || '');
                $('#address').val(data.address || '');
                $('#addressDetail').val(data.addressDetail || '');
                $('#addressEn').val(data.addressEn || '');
                $('#addressDetailEn').val(data.addressDetailEn || '');
                $('#introduction').val(data.introduction || '');

                if (data.useYn === false) {
                    $('#useYnN').prop('checked', true);
                } else {
                    $('#useYnY').prop('checked', true);
                }

                // 수정 모드에서 호텔명 안 바꿨으면 중복확인 불필요
                HotelForm.nameChecked = true;
            }
        });
    },

    checkName: function() {
        var hotelName = $.trim($('#hotelName').val());
        if (!hotelName) {
            HolaPms.alert('warning', '호텔명을 입력해주세요.');
            return;
        }

        // 수정 모드에서 원래 이름과 같으면 통과
        if (this.isEdit && hotelName === this.originalName) {
            this.nameChecked = true;
            $('#nameCheckResult').text('사용 가능한 호텔명입니다.').removeClass('text-danger').addClass('text-success');
            return;
        }

        HolaPms.ajax({
            url: '/api/v1/hotels/check-name?hotelName=' + encodeURIComponent(hotelName),
            type: 'GET',
            success: function(res) {
                if (res.data.duplicate) {
                    HotelForm.nameChecked = false;
                    $('#nameCheckResult').text('이미 존재하는 호텔명입니다.').removeClass('text-success').addClass('text-danger');
                } else {
                    HotelForm.nameChecked = true;
                    $('#nameCheckResult').text('사용 가능한 호텔명입니다.').removeClass('text-danger').addClass('text-success');
                }
            }
        });
    },

    save: function() {
        var hotelName = $.trim($('#hotelName').val());
        if (!hotelName) {
            HolaPms.alert('warning', '호텔명을 입력해주세요.');
            $('#hotelName').focus();
            return;
        }

        // 호텔명이 변경된 경우 중복확인 필수
        if (!this.isEdit || hotelName !== this.originalName) {
            if (!this.nameChecked) {
                HolaPms.alert('warning', '호텔명 중복확인을 해주세요.');
                return;
            }
        }

        var data = {
            hotelName: hotelName,
            representativeName: HolaPms.form.val('#representativeName'),
            representativeNameEn: HolaPms.form.val('#representativeNameEn'),
            countryCode: $('#countryCode').val() || null,
            phone: HolaPms.form.val('#phone'),
            zipCode: HolaPms.form.val('#zipCode'),
            address: HolaPms.form.val('#address'),
            addressDetail: HolaPms.form.val('#addressDetail'),
            addressEn: HolaPms.form.val('#addressEn'),
            addressDetailEn: HolaPms.form.val('#addressDetailEn'),
            introduction: HolaPms.form.val('#introduction'),
            useYn: $('input[name="useYn"]:checked').val() === 'true'
        };

        HolaPms.ajax({
            url: this.isEdit ? '/api/v1/hotels/' + this.hotelId : '/api/v1/hotels',
            type: this.isEdit ? 'PUT' : 'POST',
            data: data,
            success: function() {
                HolaPms.alert('success', HotelForm.isEdit ? '호텔이 수정되었습니다.' : '호텔이 등록되었습니다.');
                setTimeout(function() {
                    location.href = '/admin/hotels';
                }, 500);
            }
        });
    },

    remove: function() {
        HolaPms.confirm('정말 삭제하시겠습니까?', function() {
            HolaPms.ajax({
                url: '/api/v1/hotels/' + HotelForm.hotelId,
                type: 'DELETE',
                success: function() {
                    HolaPms.alert('success', '호텔이 삭제되었습니다.');
                    setTimeout(function() {
                        location.href = '/admin/hotels';
                    }, 500);
                }
            });
        });
    }
};

$(document).ready(function() {
    if ($('#hotelForm').length) {
        HotelForm.init();
    }
});
