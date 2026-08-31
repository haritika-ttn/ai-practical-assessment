(function (window) {
    'use strict';

    var tokenPromise = null;

    function fetchToken(csrfUrl) {
        if (!tokenPromise) {
            tokenPromise = fetch(csrfUrl, {
                credentials: 'same-origin',
                headers: { Accept: 'application/json' }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('CSRF token fetch failed');
                    }
                    return response.json();
                })
                .then(function (data) {
                    return data.token;
                })
                .catch(function () {
                    tokenPromise = null;
                    throw new Error('Unable to obtain CSRF token');
                });
        }
        return tokenPromise;
    }

    window.SupportTicketsCsrf = {
        getToken: fetchToken,
        clearCache: function () {
            tokenPromise = null;
        }
    };
})(window);
