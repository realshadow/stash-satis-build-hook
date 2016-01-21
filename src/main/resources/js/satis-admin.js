AJS.$(document).ready(function ($) {
    var dialog = new AJS.Dialog({
        id: 'satis-config-dialog',
        width: 450,
        height: 190,
        closeOnOutsideClick: true
    });

    var urlRegex = new RegExp(
        '^' +
            // protocol identifier
        '(?:(?:https?|ftp)://)' +
            // user:pass authentication
        '(?:\\S+(?::\\S*)?@)?' +
        '(?:' +
            // IP address exclusion
            // private & local networks
        '(?!(?:10|127)(?:\\.\\d{1,3}){3})' +
        '(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})' +
        '(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})' +
            // IP address dotted notation octets
            // excludes loopback network 0.0.0.0
            // excludes reserved space >= 224.0.0.0
            // excludes network & broacast addresses
            // (first & last IP address of each class)
        '(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])' +
        '(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}' +
        '(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))' +
        '|' +
            // host name
        '(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)' +
            // domain name
        '(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*' +
            // TLD identifier
        '(?:\\.(?:[a-z\\u00a1-\\uffff]{2,}))' +
            // TLD may end with dot
        '\\.?' +
        ')' +
            // port number
        '(?::\\d{2,5})?' +
            // resource path
        '(?:[/?#]\\S*)?' +
        '$', 'i'
    );

    function save(dialog) {
        var form = $('#' + dialog.id).find('form');
        var input = form.find('#satis-api-url');

        form.find('span.satis-build-conf-error').remove();

        if(urlRegex.test(input.val())) {
            $.ajax({
                url: form.attr('action'),
                type: 'POST',
                data: form.serialize(),
                dataType: 'json',
                headers: {"X-Atlassian-Token": "no-check"},
                success: function (data) {
                    AJS.messages.success('#satis-bitbucket-aui-message-bar', {
                        title: 'Satis Control Panel URL has been saved successfully.'
                    });
                },
                complete: function (data) {
                    dialog.hide();
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    AJS.messages.error('#satis-bitbucket-aui-message-bar', {
                        title: 'Unable to save Satis Control Panel URL.',
                        body: textStatus
                    });
                }
            });
        } else {
            form.find('span.satis-build-conf-error').remove();

            input.after('<span class="satis-build-conf-error">Please provide a valid URL!</span>');
        }
    }

    function cancel(dialog) {
        dialog.hide();
    }

    dialog.addButton('Save', save);
    dialog.addButton('Cancel', cancel);

    var initAndShow = function () {
        $.ajax({
            url: AJS.contextPath() + '/rest/bitbucket-satis/1.0/api',
            type: 'GET',
            dataType: 'json',
            success: function (data) {
                data.context = AJS.contextPath();
                dialog.addPanel('Satis Control Panel Configuration', satis.stash.dialog(data));
                dialog.show();
            },
            error: function (jqXHR, textStatus, errorThrown) {
                AJS.messages.error('#satis-bitbucket-aui-message-bar', {
                    title: 'Could not load Satis Control Panel config.',
                    body: textStatus
                });
            }
        });
        initAndShow = function () {
            dialog.show();
        };
    };

    $('a.satis-config-link').on('click', function (e) {
        e.preventDefault();
        initAndShow();
    });
});