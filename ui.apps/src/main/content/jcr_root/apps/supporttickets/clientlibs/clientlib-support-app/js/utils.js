(function (window) {
    'use strict';

    var STATUS_LABELS = {
        OPEN: 'Open',
        IN_PROGRESS: 'In Progress',
        RESOLVED: 'Resolved',
        CLOSED: 'Closed',
        CANCELLED: 'Cancelled'
    };

    var PRIORITY_LABELS = {
        LOW: 'Low',
        MEDIUM: 'Medium',
        HIGH: 'High',
        CRITICAL: 'Critical'
    };

    function formatStatus(value) {
        return STATUS_LABELS[value] || value || '';
    }

    function formatPriority(value) {
        return PRIORITY_LABELS[value] || value || '';
    }

    function formatDate(value) {
        if (!value) {
            return '';
        }
        try {
            var date = new Date(value);
            return date.toLocaleString();
        } catch (e) {
            return value;
        }
    }

    function getQueryParam(name) {
        var params = new URLSearchParams(window.location.search);
        return params.get(name);
    }

    function showAlert(root, type, message) {
        var alert = root.querySelector('[data-alert]');
        if (!alert) {
            return;
        }
        alert.textContent = message;
        alert.className = 'support-app__alert support-app__alert--' + type + ' is-visible';
        alert.setAttribute('role', 'alert');
        if (type === 'success') {
            window.setTimeout(function () {
                if (alert.textContent === message) {
                    hideAlert(root);
                }
            }, 3000);
        }
    }

    function hideAlert(root) {
        var alert = root.querySelector('[data-alert]');
        if (alert) {
            alert.className = 'support-app__alert';
            alert.textContent = '';
        }
    }

    function clearFieldErrors(root) {
        root.querySelectorAll('.support-app__field.is-invalid').forEach(function (field) {
            field.classList.remove('is-invalid');
            var error = field.querySelector('[data-field-error]');
            if (error) {
                error.textContent = '';
            }
        });
    }

    function applyFieldErrors(root, fields) {
        if (!fields) {
            return;
        }
        Object.keys(fields).forEach(function (name) {
            var field = root.querySelector('[data-field="' + name + '"]');
            if (!field) {
                return;
            }
            field.classList.add('is-invalid');
            var error = field.querySelector('[data-field-error]');
            if (error) {
                error.textContent = fields[name];
            }
        });
    }

    function handleApiError(root, error, fallbackMessage) {
        if (!error.status) {
            showAlert(root, 'error', 'Unable to reach server. Check connection and try again.');
            return;
        }
        if (error.status === 500) {
            showAlert(root, 'error', 'Something went wrong. Please try again.');
            return;
        }
        showAlert(root, 'error', error.message || fallbackMessage || 'Request failed.');
        applyFieldErrors(root, error.fields);
    }

    function buildUserMap(users) {
        var map = {};
        (users || []).forEach(function (user) {
            map[user.id] = user.name || user.id;
        });
        return map;
    }

    function userDisplayName(userMap, path) {
        if (!path) {
            return 'Unassigned';
        }
        return userMap[path] || path;
    }

    function readConfig(root) {
        return {
            apiBase: root.dataset.apiBase,
            csrfTokenUrl: root.dataset.csrfUrl,
            listPageUrl: root.dataset.listUrl,
            createPageUrl: root.dataset.createUrl,
            detailPageUrl: root.dataset.detailUrl
        };
    }

    window.SupportTicketsUtils = {
        formatStatus: formatStatus,
        formatPriority: formatPriority,
        formatDate: formatDate,
        getQueryParam: getQueryParam,
        showAlert: showAlert,
        hideAlert: hideAlert,
        clearFieldErrors: clearFieldErrors,
        applyFieldErrors: applyFieldErrors,
        handleApiError: handleApiError,
        buildUserMap: buildUserMap,
        userDisplayName: userDisplayName,
        readConfig: readConfig,
        STATUS_OPTIONS: [
            { value: '', label: 'All statuses' },
            { value: 'OPEN', label: 'Open' },
            { value: 'IN_PROGRESS', label: 'In Progress' },
            { value: 'RESOLVED', label: 'Resolved' },
            { value: 'CLOSED', label: 'Closed' },
            { value: 'CANCELLED', label: 'Cancelled' }
        ],
        PRIORITY_OPTIONS: [
            { value: 'LOW', label: 'Low' },
            { value: 'MEDIUM', label: 'Medium' },
            { value: 'HIGH', label: 'High' },
            { value: 'CRITICAL', label: 'Critical' }
        ]
    };
})(window);
