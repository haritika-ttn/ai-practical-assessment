(function (window) {
    'use strict';

    function parseError(response) {
        return response.json().catch(function () {
            return {
                code: 'INTERNAL_ERROR',
                message: 'An unexpected error occurred. Please try again later.'
            };
        }).then(function (body) {
            var error = new Error(body.message || 'Request failed');
            error.status = response.status;
            error.code = body.code;
            error.fields = body.fields || {};
            error.details = body.details || {};
            throw error;
        });
    }

    function buildUrl(base, path, params) {
        var url = base + path;
        if (!params) {
            return url;
        }
        var query = Object.keys(params)
            .filter(function (key) {
                return params[key] !== null && params[key] !== undefined && params[key] !== '';
            })
            .map(function (key) {
                return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]);
            })
            .join('&');
        return query ? url + '?' + query : url;
    }

    function createClient(config) {
        var apiBase = config.apiBase || '/bin/support-tickets';
        var csrfUrl = config.csrfTokenUrl || '/libs/granite/csrf/token.json';

        function request(method, path, body, params) {
            var headers = {
                Accept: 'application/json'
            };
            var options = {
                method: method,
                credentials: 'same-origin',
                headers: headers
            };

            if (body !== undefined) {
                headers['Content-Type'] = 'application/json; charset=utf-8';
                options.body = JSON.stringify(body);
            }

            var execute = function () {
                return fetch(buildUrl(apiBase, path, params), options).then(function (response) {
                    if (!response.ok) {
                        return parseError(response);
                    }
                    if (response.status === 204) {
                        return null;
                    }
                    return response.json();
                });
            };

            if (method === 'GET') {
                return execute();
            }

            return window.SupportTicketsCsrf.getToken(csrfUrl).then(function (token) {
                headers['CSRF-Token'] = token;
                return execute();
            });
        }

        return {
            listTickets: function (q, status) {
                return request('GET', '.json', undefined, { q: q, status: status });
            },
            createTicket: function (payload) {
                return request('POST', '.json', payload);
            },
            getTicket: function (ticketId) {
                return request('GET', '/' + ticketId + '.json');
            },
            updateTicket: function (ticketId, payload) {
                return request('PUT', '/' + ticketId + '.json', payload);
            },
            updateStatus: function (ticketId, status) {
                return request('PATCH', '/' + ticketId + '/status.json', { status: status });
            },
            addComment: function (ticketId, payload) {
                return request('POST', '/' + ticketId + '/comments.json', payload);
            },
            getUsers: function () {
                return request('GET', '/users.json');
            }
        };
    }

    window.SupportTicketsApi = {
        create: createClient
    };
})(window);
