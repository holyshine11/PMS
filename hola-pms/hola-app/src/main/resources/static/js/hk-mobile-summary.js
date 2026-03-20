/**
 * 하우스키핑 모바일 - 일일 요약
 */
var HkSummary = {

    propertyId: null,

    init: function () {
        this.propertyId = (typeof HK_PROPERTY_ID !== 'undefined') ? HK_PROPERTY_ID : null;
        var today = new Date().toISOString().split('T')[0];
        $('#summaryDate').text(today);

        if (this.propertyId) {
            this.loadSummary();
        }
    },

    loadSummary: function () {
        var self = this;
        var today = new Date().toISOString().split('T')[0];

        HolaPms.ajax({
            url: '/api/v1/properties/' + self.propertyId + '/hk-mobile/my-summary?date=' + today,
            method: 'GET',
            success: function (res) {
                if (res.success && res.data) {
                    self.render(res.data);
                }
            }
        });
    },

    render: function (data) {
        $('#sumCompleted').text(data.completedTasks || 0);
        $('#sumTotal').text(data.totalTasks || 0);
        $('#sumCredits').text(data.totalCredits || 0);

        var avgTime = data.avgDurationMinutes ? Math.round(data.avgDurationMinutes) + '분' : '-';
        $('#sumAvgTime').text(avgTime);

        var rate = data.completionRate || 0;
        $('#sumRate').text(rate + '%');
        $('#sumProgressBar').css('width', rate + '%');
    }
};

$(function () {
    HkSummary.init();
});
