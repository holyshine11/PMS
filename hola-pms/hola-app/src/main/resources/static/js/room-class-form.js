/**
 * 객실 그룹 관리 - 등록/수정 폼 페이지
 */
var RoomClassForm = {
    editId: null,
    duplicateChecked: false,

    init: function() {
        this.editId = $('#roomClassId').val() || null;
        this.bindEvents();
        this.updateHotelPropertyName();

        if (this.editId) {
            this.loadData();
        } else {
            this.setCreateMode();
        }
    },

    bindEvents: function() {
        var self = this;

        // 컨텍스트 변경 시 호텔/프로퍼티명 갱신
        $(document).on('hola:contextChange', function() {
            self.updateHotelPropertyName();
        });

        // 코드 입력 시 중복확인 리셋
        $('#roomClassCode').on('input', function() {
            self.duplicateChecked = false;
            $('#codeCheckResult').text('');
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
        $('#roomClassCode').prop('readonly', false);
        $('#btnCheckDuplicate').show();
    },

    setEditMode: function() {
        $('#btnSave').html('<i class="fas fa-save me-1"></i>저장');
        $('#btnDelete').show();
        $('#roomClassCode').prop('readonly', true);
        $('#btnCheckDuplicate').hide();
        this.duplicateChecked = true;
    },

    loadData: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/room-classes/' + self.editId,
            method: 'GET',
            success: function(res) {
                if (res.success && res.data) {
                    var d = res.data;
                    $('#roomClassName').val(d.roomClassName);
                    $('#roomClassCode').val(d.roomClassCode);
                    $('#description').val(d.description || '');
                    $('input[name="useYn"][value="' + d.useYn + '"]').prop('checked', true);
                    self.setEditMode();
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    checkDuplicate: function() {
        var self = this;
        var code = $.trim($('#roomClassCode').val());
        if (!code) {
            HolaPms.alert('warning', '객실 클래스 코드를 입력해주세요.');
            $('#roomClassCode').focus();
            return;
        }

        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '프로퍼티를 먼저 선택해주세요.');
            return;
        }

        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/room-classes/check-code',
            method: 'GET',
            data: { roomClassCode: code },
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

    save: function() {
        var self = this;
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            HolaPms.alert('warning', '호텔과 프로퍼티를 먼저 선택해주세요.');
            return;
        }

        var className = $.trim($('#roomClassName').val());
        var classCode = $.trim($('#roomClassCode').val());
        var description = $.trim($('#description').val());
        var useYn = $('input[name="useYn"]:checked').val() === 'true';

        // 필수 검증
        if (!className) {
            HolaPms.alert('warning', '객실 클래스명을 입력해주세요.');
            $('#roomClassName').focus();
            return;
        }
        if (!classCode) {
            HolaPms.alert('warning', '객실 클래스 코드를 입력해주세요.');
            $('#roomClassCode').focus();
            return;
        }

        // 신규 등록 시 중복확인 필수
        if (!self.editId && !self.duplicateChecked) {
            HolaPms.alert('warning', '객실 클래스 코드 중복확인을 해주세요.');
            return;
        }

        var url, method, data;
        if (self.editId) {
            // 수정
            url = '/api/v1/properties/' + propertyId + '/room-classes/' + self.editId;
            method = 'PUT';
            data = {
                roomClassName: className,
                description: description,
                useYn: useYn
            };
        } else {
            // 등록
            url = '/api/v1/properties/' + propertyId + '/room-classes';
            method = 'POST';
            data = {
                roomClassCode: classCode,
                roomClassName: className,
                description: description,
                useYn: useYn
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
                    window.location.href = '/admin/room-classes';
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    },

    remove: function() {
        var self = this;
        if (!self.editId) return;

        if (!confirm('정말 삭제하시겠습니까?')) return;

        var propertyId = HolaPms.context.getPropertyId();
        $.ajax({
            url: '/api/v1/properties/' + propertyId + '/room-classes/' + self.editId,
            method: 'DELETE',
            success: function(res) {
                if (res.success) {
                    HolaPms.alert('success', '삭제되었습니다.');
                    window.location.href = '/admin/room-classes';
                }
            },
            error: function(xhr) {
                HolaPms.handleAjaxError(xhr);
            }
        });
    }
};

$(document).ready(function() {
    RoomClassForm.init();
});
