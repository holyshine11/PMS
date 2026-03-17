/**
 * 프론트데스크 현장투숙 페이지
 * Walk-In은 예약 등록 → 체크인 2단계로 안내
 */
var FdWalkIn = {
    init: function() {
        this.bindEvents();
        this.reload();
    },

    bindEvents: function() {
        var self = this;
        $(document).on('hola:contextChange', function() { self.reload(); });
    },

    reload: function() {
        var propertyId = HolaPms.context.getPropertyId();
        if (!propertyId) {
            $('#contextAlert').removeClass('d-none');
            $('#walkInForm').hide();
            return;
        }
        $('#contextAlert').addClass('d-none');
        $('#walkInForm').show();
    }
};

$(document).ready(function() { FdWalkIn.init(); });
