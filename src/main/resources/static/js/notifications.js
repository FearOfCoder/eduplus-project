/**
 * Notification System - AJAX load, badge, mark as read
 * Hỗ trợ: Navbar dropdown (bell) + Sidebar collapse (Dark Mode)
 */
(function() {
    const API_UNREAD = '/api/notifications/unread';
    const API_LIST = '/api/notifications';
    const API_READ = '/api/notifications/read/';

    function getBadgeEls() {
        const ids = ['notificationBadge', 'notificationBadgeSidebar', 'sidebar-notif-badge'];
        return ids.map(id => document.getElementById(id)).filter(Boolean);
    }
    function getListEl() {
        return document.getElementById('notificationList') || document.getElementById('notificationListSidebar');
    }
    function getSidebarListEl() {
        return document.getElementById('sidebar-notif-list');
    }
    function getEmptyEl() {
        return document.getElementById('notificationEmpty') || document.getElementById('notificationEmptySidebar');
    }
    function getBellEl() {
        return document.getElementById('notificationBell') || document.getElementById('notificationBellSidebar');
    }
    function getBtnLoadNotif() {
        return document.getElementById('btn-load-notif');
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Flag để tránh chồng chéo request
    let isLoadingUnreadCount = false;

    function markRead(id, callback) {
        if (!id) return;
        fetch(API_READ + id, { method: 'POST', credentials: 'same-origin' })
            .then(() => { loadUnreadCount(); if (callback) callback(); })
            .catch(() => {});
    }

    function loadUnreadCount() {
        // Tránh chồng chéo request: chỉ gọi khi request trước đó đã hoàn thành
        if (isLoadingUnreadCount) {
            return;
        }
        
        isLoadingUnreadCount = true;
        fetch(API_UNREAD, { credentials: 'same-origin' })
            .then(r => r.json())
            .then(count => {
                getBadgeEls().forEach(badge => {
                    badge.textContent = count > 99 ? '99+' : count;
                    badge.style.display = count > 0 ? 'inline-block' : 'none';
                    badge.classList.toggle('d-none', count === 0);
                });
            })
            .catch(() => {})
            .finally(() => {
                isLoadingUnreadCount = false;
            });
    }

    function renderNotificationsSidebar(list) {
        const listEl = getSidebarListEl();
        if (!listEl) return;
        let html = '';
        if (list.length === 0) {
            html = '<div class="p-3 text-center text-white-50 small">Không có thông báo mới</div>';
        } else {
            list.forEach(n => {
                /* Icon: xanh lá PAYMENT, xanh dương SYSTEM, vàng COURSE_UPDATE */
                const iconColor = n.type === 'PAYMENT' ? 'text-success' : (n.type === 'SYSTEM' ? 'text-info' : 'text-warning');
                const unreadClass = n.read ? '' : ' unread';
                const link = (n.link || '#').replace(/"/g, '&quot;');
                const title = escapeHtml(n.title);
                const message = escapeHtml(n.message);
                const time = escapeHtml(n.createdAtFormatted || '');
                html += '<a href="' + link + '" class="sidebar-notif-item notification-item-sidebar text-decoration-none d-block p-2 border-bottom border-secondary border-opacity-25' + unreadClass + '" data-id="' + n.id + '" data-read="' + n.read + '">' +
                    '<div class="d-flex align-items-start">' +
                    '<i class="fas fa-circle ' + iconColor + ' me-2 mt-1 flex-shrink-0" style="font-size: 6px;"></i>' +
                    '<div class="flex-grow-1 min-w-0" style="font-size: 0.875rem; white-space: normal; word-break: break-word;">' +
                    '<div class="text-white fw-medium">' + title + '</div>' +
                    '<small class="d-block text-light" style="font-size: 0.8rem; opacity: 0.9;">' + message + '</small>' +
                    '<small class="d-block text-white-50" style="font-size: 0.7rem;">' + time + '</small>' +
                    '</div></div></a>';
            });
        }
        listEl.innerHTML = html;
        listEl.querySelectorAll('.notification-item-sidebar').forEach(el => {
            el.addEventListener('click', function(e) {
                e.preventDefault();
                const id = parseInt(el.getAttribute('data-id'), 10);
                const isRead = el.getAttribute('data-read') === 'true';
                if (!isRead && id) markRead(id);
                const href = el.getAttribute('href');
                if (href && href !== '#') window.location.href = href;
            });
        });
    }

    function loadNotificationsSidebar() {
        const listEl = getSidebarListEl();
        if (!listEl) return;
        listEl.innerHTML = '<div class="p-3 text-center text-white-50 small">Đang tải...</div>';
        fetch(API_LIST, { credentials: 'same-origin' })
            .then(r => r.json())
            .then(renderNotificationsSidebar)
            .catch(() => {
                listEl.innerHTML = '<div class="p-3 text-center text-white-50 small">Không tải được thông báo</div>';
            });
    }

    function loadNotifications() {
        fetch(API_LIST, { credentials: 'same-origin' })
            .then(r => r.json())
            .then(list => {
                const listEl = getListEl();
                const emptyEl = getEmptyEl();
                if (!listEl) return;
                if (emptyEl) emptyEl.style.display = list.length === 0 ? 'block' : 'none';
                listEl.querySelectorAll('.notification-item').forEach(el => el.remove());
                list.forEach(n => {
                    const item = document.createElement('a');
                    item.href = n.link || '#';
                    item.className = 'notification-item dropdown-item d-block py-2 px-3 rounded mb-1 text-decoration-none ' + (n.read ? 'bg-white' : 'bg-light');
                    item.style.cursor = 'pointer';
                    item.innerHTML = '<div class="fw-semibold small text-dark">' + escapeHtml(n.title) + '</div>' +
                        '<div class="small text-muted">' + escapeHtml(n.message) + '</div>' +
                        '<div class="small text-muted">' + (n.createdAtFormatted || '') + '</div>';
                    item.addEventListener('click', function(e) {
                        e.preventDefault();
                        if (!n.read && n.id) markRead(n.id);
                        if (n.link) window.location.href = n.link;
                    });
                    listEl.appendChild(item);
                });
            })
            .catch(() => {});
    }

    document.addEventListener('DOMContentLoaded', function() {
        const bellEl = getBellEl();
        const btnLoadNotif = getBtnLoadNotif();
        const collapseEl = document.getElementById('notificationCollapse');

        if (bellEl || btnLoadNotif) {
            loadUnreadCount();
            // Tăng interval lên 60 giây (60000ms) để giảm tải server và tránh lag trình duyệt
            setInterval(loadUnreadCount, 60000);
        }
        if (bellEl) {
            bellEl.addEventListener('shown.bs.dropdown', loadNotifications);
        }
        if (collapseEl) {
            collapseEl.addEventListener('shown.bs.collapse', loadNotificationsSidebar);
        }
    });
})();
