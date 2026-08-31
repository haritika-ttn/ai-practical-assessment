(function (window, document) {
    'use strict';

    var utils = window.SupportTicketsUtils;

    function initList(root) {
        var config = utils.readConfig(root);
        var api = window.SupportTicketsApi.create(config);
        var userMap = {};
        var searchInput = root.querySelector('[data-search-input]');
        var statusFilter = root.querySelector('[data-status-filter]');
        var tableBody = root.querySelector('[data-ticket-table-body]');
        var emptyState = root.querySelector('[data-empty-state]');
        var retryBtn = root.querySelector('[data-retry]');
        var clearFilters = root.querySelector('[data-clear-filters]');
        var debounceTimer;

        function setLoading(isLoading) {
            if (isLoading) {
                tableBody.innerHTML = '<tr><td colspan="5" class="support-app__loading">Loading tickets…</td></tr>';
            }
        }

        function renderTickets(tickets) {
            tableBody.innerHTML = '';
            if (!tickets || tickets.length === 0) {
                emptyState.hidden = false;
                var hasFilters = (searchInput.value && searchInput.value.trim()) || statusFilter.value;
                emptyState.textContent = hasFilters
                    ? 'No tickets match your search.'
                    : 'No tickets yet.';
                if (clearFilters) {
                    clearFilters.hidden = !hasFilters;
                }
                return;
            }

            emptyState.hidden = true;
            if (clearFilters) {
                clearFilters.hidden = true;
            }

            tickets.forEach(function (ticket) {
                var row = document.createElement('tr');
                var detailUrl = config.detailPageUrl + '?id=' + encodeURIComponent(ticket.id);
                row.innerHTML =
                    '<td><a href="' + detailUrl + '"></a></td>' +
                    '<td><span class="support-app__badge"></span></td>' +
                    '<td></td>' +
                    '<td></td>' +
                    '<td></td>';
                row.querySelector('a').textContent = ticket.title || '';
                row.querySelector('.support-app__badge').textContent = utils.formatStatus(ticket.status);
                row.cells[2].textContent = utils.formatPriority(ticket.priority);
                row.cells[3].textContent = utils.userDisplayName(userMap, ticket.assignedTo);
                row.cells[4].textContent = utils.formatDate(ticket.updatedAt);
                tableBody.appendChild(row);
            });
        }

        function loadTickets() {
            utils.hideAlert(root);
            utils.clearFieldErrors(root);
            setLoading(true);

            var q = searchInput.value ? searchInput.value.trim() : '';
            var status = statusFilter.value || '';

            if (q.length > 200) {
                utils.showAlert(root, 'error', 'Search keyword must not exceed 200 characters.');
                return;
            }

            api.listTickets(q || null, status || null)
                .then(function (tickets) {
                    if (retryBtn) {
                        retryBtn.hidden = true;
                    }
                    renderTickets(tickets);
                })
                .catch(function (error) {
                    tableBody.innerHTML = '';
                    if (retryBtn) {
                        retryBtn.hidden = false;
                    }
                    utils.handleApiError(root, error, 'Unable to load tickets.');
                });
        }

        api.getUsers()
            .then(function (users) {
                userMap = utils.buildUserMap(users);
                loadTickets();
            })
            .catch(function () {
                loadTickets();
            });

        searchInput.addEventListener('input', function () {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(loadTickets, 300);
        });

        statusFilter.addEventListener('change', loadTickets);

        if (retryBtn) {
            retryBtn.addEventListener('click', loadTickets);
        }

        if (clearFilters) {
            clearFilters.addEventListener('click', function (event) {
                event.preventDefault();
                searchInput.value = '';
                statusFilter.value = '';
                loadTickets();
            });
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-support-app][data-page="list"]').forEach(initList);
    });
})(window, document);
