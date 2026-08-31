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

    function populatePrioritySelect(select) {
        select.innerHTML = '';
        utils.PRIORITY_OPTIONS.forEach(function (item) {
            var option = document.createElement('option');
            option.value = item.value;
            option.textContent = item.label;
            select.appendChild(option);
        });
    }

    function populateStatusSelect(select, allowedTransitions, currentStatus) {
        select.innerHTML = '';
        if (!allowedTransitions || allowedTransitions.length === 0) {
            select.disabled = true;
            return;
        }
        select.disabled = false;
        allowedTransitions.forEach(function (status) {
            var option = document.createElement('option');
            option.value = status;
            option.textContent = utils.formatStatus(status);
            select.appendChild(option);
        });
        select.value = allowedTransitions[0];
    }

    function initDetail(root) {
        var config = utils.readConfig(root);
        var api = window.SupportTicketsApi.create(config);
        var ticketId = utils.getQueryParam('id');
        var userMap = {};
        var currentTicket = null;

        var loadingEl = root.querySelector('[data-detail-loading]');
        var contentEl = root.querySelector('[data-detail-content]');
        var notFoundEl = root.querySelector('[data-not-found]');
        var editForm = root.querySelector('[data-edit-form]');
        var saveBtn = root.querySelector('[data-save]');
        var statusSelect = root.querySelector('[data-status-select]');
        var statusBtn = root.querySelector('[data-status-submit]');
        var statusSection = root.querySelector('[data-status-section]');
        var statusMessage = root.querySelector('[data-status-message]');
        var currentStatusBadge = root.querySelector('[data-current-status]');
        var commentsList = root.querySelector('[data-comments-list]');
        var commentForm = root.querySelector('[data-comment-form]');
        var commentBtn = root.querySelector('[data-comment-submit]');
        var commentAuthor = root.querySelector('[data-comment-author]');

        if (!ticketId) {
            loadingEl.hidden = true;
            notFoundEl.hidden = false;
            return;
        }

        if (utils.getQueryParam('created') === '1') {
            utils.showAlert(root, 'success', 'Ticket created successfully.');
        }

        function showContent(ticket) {
            currentTicket = ticket;
            loadingEl.hidden = true;
            contentEl.hidden = false;

            editForm.querySelector('[name="title"]').value = ticket.title || '';
            editForm.querySelector('[name="description"]').value = ticket.description || '';
            editForm.querySelector('[name="priority"]').value = ticket.priority || 'MEDIUM';
            root.querySelector('[data-assignee]').value = ticket.assignedTo || '';

            root.querySelector('[data-meta-id]').textContent = ticket.id || '';
            root.querySelector('[data-meta-created-by]').textContent =
                utils.userDisplayName(userMap, ticket.createdBy);
            root.querySelector('[data-meta-created-at]').textContent = utils.formatDate(ticket.createdAt);
            root.querySelector('[data-meta-updated-at]').textContent = utils.formatDate(ticket.updatedAt);

            currentStatusBadge.textContent = utils.formatStatus(ticket.status);

            if (!ticket.allowedTransitions || ticket.allowedTransitions.length === 0) {
                statusSelect.hidden = true;
                statusBtn.hidden = true;
                statusMessage.hidden = false;
                statusMessage.textContent = 'No further status changes available.';
            } else {
                statusSelect.hidden = false;
                statusBtn.hidden = false;
                statusMessage.hidden = true;
                populateStatusSelect(statusSelect, ticket.allowedTransitions, ticket.status);
            }

            renderComments(ticket.comments || []);
        }

        function renderComments(comments) {
            commentsList.innerHTML = '';
            if (!comments.length) {
                var empty = document.createElement('li');
                empty.className = 'support-app__loading';
                empty.textContent = 'No comments yet.';
                commentsList.appendChild(empty);
                return;
            }
            comments.forEach(function (comment) {
                var item = document.createElement('li');
                item.className = 'support-app__comment';
                var meta = document.createElement('div');
                meta.className = 'support-app__comment-meta';
                meta.textContent = utils.userDisplayName(userMap, comment.createdBy)
                    + ' · ' + utils.formatDate(comment.createdAt);
                var body = document.createElement('div');
                body.textContent = comment.message || '';
                item.appendChild(meta);
                item.appendChild(body);
                commentsList.appendChild(item);
            });
        }

        function loadDetail() {
            api.getTicket(ticketId)
                .then(showContent)
                .catch(function (error) {
                    loadingEl.hidden = true;
                    if (error.status === 404) {
                        notFoundEl.hidden = false;
                        utils.showAlert(root, 'error', 'Ticket not found.');
                    } else {
                        utils.handleApiError(root, error, 'Unable to load ticket.');
                    }
                });
        }

        api.getUsers()
            .then(function (users) {
                userMap = utils.buildUserMap(users);
                populateUserSelect(root.querySelector('[data-assignee]'), users, true);
                populateUserSelect(commentAuthor, users, false);
                populatePrioritySelect(editForm.querySelector('[name="priority"]'));
                loadDetail();
            })
            .catch(function () {
                populatePrioritySelect(editForm.querySelector('[name="priority"]'));
                loadDetail();
            });

        editForm.addEventListener('submit', function (event) {
            event.preventDefault();
            utils.hideAlert(root);
            utils.clearFieldErrors(root);

            var title = editForm.querySelector('[name="title"]').value.trim();
            if (!title) {
                utils.applyFieldErrors(root, { title: 'Title is required.' });
                return;
            }

            saveBtn.disabled = true;
            saveBtn.textContent = 'Saving…';

            var payload = {
                title: title,
                description: editForm.querySelector('[name="description"]').value,
                priority: editForm.querySelector('[name="priority"]').value,
                assignedTo: root.querySelector('[data-assignee]').value || null
            };

            api.updateTicket(ticketId, payload)
                .then(function (ticket) {
                    saveBtn.disabled = false;
                    saveBtn.textContent = 'Save changes';
                    utils.showAlert(root, 'success', 'Ticket saved.');
                    showContent(ticket);
                })
                .catch(function (error) {
                    saveBtn.disabled = false;
                    saveBtn.textContent = 'Save changes';
                    utils.handleApiError(root, error, 'Unable to save ticket.');
                });
        });

        statusBtn.addEventListener('click', function () {
            if (!statusSelect.value) {
                return;
            }
            utils.hideAlert(root);
            statusBtn.disabled = true;
            statusBtn.textContent = 'Updating…';

            api.updateStatus(ticketId, statusSelect.value)
                .then(function (ticket) {
                    statusBtn.disabled = false;
                    statusBtn.textContent = 'Update status';
                    utils.showAlert(root, 'success', 'Status updated.');
                    showContent(ticket);
                })
                .catch(function (error) {
                    statusBtn.disabled = false;
                    statusBtn.textContent = 'Update status';
                    if (error.status === 409) {
                        var message = error.message || 'Status change not allowed.';
                        utils.showAlert(root, 'error', message);
                        if (currentTicket) {
                            populateStatusSelect(
                                statusSelect,
                                error.details.allowedTransitions || currentTicket.allowedTransitions,
                                currentTicket.status);
                        }
                        loadDetail();
                        return;
                    }
                    utils.handleApiError(root, error, 'Unable to update status.');
                });
        });

        commentForm.addEventListener('submit', function (event) {
            event.preventDefault();
            utils.hideAlert(root);
            utils.clearFieldErrors(root);

            var message = commentForm.querySelector('[name="message"]').value.trim();
            var author = commentAuthor.value;
            if (!message) {
                utils.applyFieldErrors(root, { message: 'Comment is required.' });
                return;
            }
            if (!author) {
                utils.applyFieldErrors(root, { createdBy: 'Please select who is commenting.' });
                return;
            }

            commentBtn.disabled = true;
            commentBtn.textContent = 'Adding…';

            api.addComment(ticketId, { message: message, createdBy: author })
                .then(function () {
                    commentBtn.disabled = false;
                    commentBtn.textContent = 'Add comment';
                    commentForm.querySelector('[name="message"]').value = '';
                    utils.showAlert(root, 'success', 'Comment added.');
                    loadDetail();
                })
                .catch(function (error) {
                    commentBtn.disabled = false;
                    commentBtn.textContent = 'Add comment';
                    utils.handleApiError(root, error, 'Unable to add comment.');
                });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-support-app][data-page="detail"]').forEach(initDetail);
    });
})(window, document);
