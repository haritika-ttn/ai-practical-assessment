(function (window, document) {
    'use strict';

    var utils = window.SupportTicketsUtils;

    function populateUserSelect(select, users, includeUnassigned) {
        select.innerHTML = '';
        if (includeUnassigned) {
            var empty = document.createElement('option');
            empty.value = '';
            empty.textContent = 'Unassigned';
            select.appendChild(empty);
        }
        (users || []).forEach(function (user) {
            var option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.name || user.id;
            select.appendChild(option);
        });
    }

    function initCreate(root) {
        var config = utils.readConfig(root);
        var api = window.SupportTicketsApi.create(config);
        var form = root.querySelector('[data-create-form]');
        var submitBtn = root.querySelector('[data-submit]');
        var createdBySelect = root.querySelector('[data-created-by]');
        var assignedToSelect = root.querySelector('[data-assigned-to]');

        function setFormLoading(isLoading) {
            submitBtn.disabled = isLoading;
            submitBtn.textContent = isLoading ? 'Creating…' : 'Create Ticket';
            form.querySelectorAll('input, select, textarea').forEach(function (el) {
                el.disabled = isLoading;
            });
        }

        api.getUsers()
            .then(function (users) {
                if (!users || users.length === 0) {
                    utils.showAlert(root, 'error', 'No users configured.');
                    submitBtn.disabled = true;
                    return;
                }
                populateUserSelect(createdBySelect, users, false);
                populateUserSelect(assignedToSelect, users, true);
            })
            .catch(function () {
                utils.showAlert(root, 'error', 'Unable to load users.');
                submitBtn.disabled = true;
            });

        form.addEventListener('submit', function (event) {
            event.preventDefault();
            utils.hideAlert(root);
            utils.clearFieldErrors(root);

            var title = form.querySelector('[name="title"]').value.trim();
            var priority = form.querySelector('[name="priority"]').value;
            var createdBy = createdBySelect.value;

            if (!title) {
                utils.applyFieldErrors(root, { title: 'Title is required.' });
                return;
            }
            if (!priority) {
                utils.applyFieldErrors(root, { priority: 'Priority is required.' });
                return;
            }
            if (!createdBy) {
                utils.applyFieldErrors(root, { createdBy: 'Please select who is creating this ticket.' });
                return;
            }

            var payload = {
                title: title,
                description: form.querySelector('[name="description"]').value,
                priority: priority,
                createdBy: createdBy
            };

            var assignedTo = assignedToSelect.value;
            if (assignedTo) {
                payload.assignedTo = assignedTo;
            }

            setFormLoading(true);
            api.createTicket(payload)
                .then(function (ticket) {
                    var detailUrl = (config.detailPageUrl || '/content/support-app/ticket.html')
                        + '?id=' + encodeURIComponent(ticket.id) + '&created=1';
                    window.location.href = detailUrl;
                })
                .catch(function (error) {
                    setFormLoading(false);
                    utils.handleApiError(root, error, 'Unable to create ticket.');
                });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-support-app][data-page="create"]').forEach(initCreate);
    });
})(window, document);
