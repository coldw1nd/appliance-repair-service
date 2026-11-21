const state = {
    token: null,
    user: null,
    requests: {page: 0, size: 25, totalPages: 0, sort: 'created,desc', selected: new Set(), query: ''},
    users: {page: 0, size: 25, totalPages: 0},
    audit: {page: 0, size: 25, totalPages: 0, query: ''},
    customers: [],
    types: [],
    notesRequestId: null,
    importPreview: null,
    activeTab: 'requests'
};

const $ = (id) => document.getElementById(id);
const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
}[c]));
const money = (value) => value == null ? '—' : new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 2
}).format(Number(value));
const date = (value) => value ? new Date(value).toLocaleString('ru-RU') : '—';
const dateOnly = (value) => value ? new Date(`${value}T00:00:00`).toLocaleDateString('ru-RU') : '—';
const roleNames = {VIEWER: 'Просмотр', REPAIRER: 'Редактор', ADMIN: 'Администратор'};
const statusNames = {
    NEW: 'Новая',
    DIAGNOSING: 'Диагностика',
    REPAIRING: 'В ремонте',
    COMPLETED: 'Завершена',
    CANCELLED: 'Отменена'
};
const priorityNames = {LOW: 'Низкий', MEDIUM: 'Средний', HIGH: 'Высокий', URGENT: 'Срочный'};
const riskNames = {
    NOT_APPLICABLE: 'Не рассчитывается',
    LOW: 'Низкий',
    MEDIUM: 'Средний',
    HIGH: 'Высокий',
    CRITICAL: 'Критический'
};

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.token) headers.set('Authorization', `Bearer ${state.token}`);
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
    const response = await fetch(path, {...options, headers, credentials: 'include'});
    if (response.status === 204) return null;
    const contentType = response.headers.get('content-type') || '';
    const payload = contentType.includes('application/json') ? await response.json().catch(() => null) : await response.text();
    if (!response.ok) {
        const message = payload?.message || payload?.detail || payload?.title || (typeof payload === 'string' ? payload : `Ошибка HTTP ${response.status}`);
        const error = new Error(message);
        error.status = response.status;
        error.payload = payload;
        if (response.status === 401 && !['/api/auth/login', '/api/auth/refresh'].includes(path)) {
            state.token = null;
            state.user = null;
            showLogin();
        }
        throw error;
    }
    return payload;
}

function toast(message, kind = 'success') {
    const el = $('toast');
    el.textContent = message;
    el.className = `toast ${kind}`;
    window.clearTimeout(toast.timer);
    toast.timer = window.setTimeout(() => el.classList.add('hidden'), 3800);
}

function showError(id, error) {
    const el = $(id);
    if (el) el.textContent = error?.message || String(error);
}

function clearError(id) {
    if ($(id)) $(id).textContent = '';
}

function showLogin() {
    $('login-panel').classList.remove('hidden');
    $('application').classList.add('hidden');
    $('logout-button').classList.add('hidden');
    $('user-label').textContent = '';
}

function showApplication() {
    $('login-panel').classList.add('hidden');
    $('application').classList.remove('hidden');
    $('logout-button').classList.remove('hidden');
    $('user-label').textContent = `${state.user.displayName} · ${state.user.roles.map(r => roleNames[r] || r).join(', ')}`;
    const canEdit = hasRole('REPAIRER', 'ADMIN');
    const isAdmin = hasRole('ADMIN');
    document.querySelectorAll('.edit-only').forEach(el => el.classList.toggle('hidden', !canEdit));
    document.querySelectorAll('.admin-only').forEach(el => el.classList.toggle('hidden', !isAdmin));
    $('password-panel').classList.toggle('hidden', !state.user.passwordChangeRequired);
    $('tab-users').classList.toggle('hidden', !isAdmin);
    $('tab-audit').classList.toggle('hidden', !isAdmin);
    if (!canEdit) {
        document.querySelectorAll('#tab-requests .bulk-toolbar, #export-csv').forEach(el => el.classList.add('hidden'));
    }
    switchTab('requests');
}

function hasRole(...roles) {
    return roles.some(role => state.user?.roles?.includes(role));
}

function switchTab(tab) {
    state.activeTab = tab;
    document.querySelectorAll('.tab-button').forEach(el => el.classList.toggle('active', el.dataset.tab === tab));
    document.querySelectorAll('.tab-content').forEach(el => el.classList.toggle('hidden', el.id !== `tab-${tab}`));
    const jobs = {
        requests: loadRequests,
        analytics: loadAnalytics,
        references: loadTypes,
        users: loadUsers,
        audit: loadAudit
    };
    if (!state.user?.passwordChangeRequired) jobs[tab]?.().catch(showGlobalError);
}

function showGlobalError(error) {
    toast(error.message || 'Не удалось выполнить действие', 'error');
}

function queryString(values) {
    const q = new URLSearchParams();
    Object.entries(values).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') q.set(k, v);
    });
    return q.toString();
}

async function restoreSession() {
    try {
        const result = await api('/api/auth/refresh', {method: 'POST', body: '{}'});
        state.token = result.accessToken;
        state.user = result.user;
        await loadCurrentUser();
        showApplication();
    } catch {
        showLogin();
    }
}

async function loadCurrentUser() {
    state.user = await api('/api/auth/me');
}

$('login-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    clearError('login-error');
    try {
        const result = await api('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({
                email: $('login-email').value.trim(),
                password: $('login-password').value,
                clientType: 'WEB'
            })
        });
        state.token = result.accessToken;
        state.user = result.user;
        $('login-password').value = '';
        showApplication();
    } catch (error) {
        showError('login-error', error);
    }
});
$('logout-button').addEventListener('click', async () => {
    try {
        await api('/api/auth/logout', {method: 'POST', body: '{}'});
    } catch {
    }
    state.token = null;
    state.user = null;
    showLogin();
    toast('Вы вышли из системы');
});
$('password-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    clearError('password-error');
    try {
        await api('/api/auth/change-initial-password', {
            method: 'POST',
            body: JSON.stringify({newPassword: $('new-password').value})
        });
        const fresh = await api('/api/auth/refresh', {method: 'POST', body: '{}'});
        state.token = fresh.accessToken;
        state.user = fresh.user;
        $('new-password').value = '';
        await loadCurrentUser();
        showApplication();
        toast('Пароль изменён');
    } catch (error) {
        showError('password-error', error);
    }
});

document.querySelectorAll('.tab-button').forEach(button => button.addEventListener('click', () => switchTab(button.dataset.tab)));
document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => $(button.dataset.close).close()));

async function ensureLookups() {
    const [customers, types] = await Promise.all([api('/api/customers'), api('/api/references?activeOnly=true')]);
    state.customers = customers;
    state.types = types;
    $('request-customer').innerHTML = customers.map(c => `<option value="${c.id}">${esc(c.fullName)} · ${esc(c.phone || '')}</option>`).join('');
    $('request-type').innerHTML = types.map(t => `<option value="${t.id}">${esc(t.name)}</option>`).join('');
}

function currentRequestQuery() {
    state.requests.query = $('request-query').value.trim();
    state.requests.sort = $('request-sort').value;
    return queryString({
        q: state.requests.query,
        page: state.requests.page,
        size: state.requests.size,
        sort: state.requests.sort
    });
}

async function loadRequests() {
    const page = await api(`/api/repair-requests?${currentRequestQuery()}`);
    state.requests.totalPages = page.totalPages;
    state.requests.selected.clear();
    $('select-all').checked = false;
    updateSelectedCount();
    $('requests-body').innerHTML = page.content.length ? page.content.map(renderRequestRow).join('') : '<tr><td colspan="10" class="empty">Заявки не найдены</td></tr>';
    $('requests-page').textContent = `Страница ${page.page + 1} из ${Math.max(page.totalPages, 1)} · всего ${page.totalElements}`;
    $('requests-prev').disabled = page.first;
    $('requests-next').disabled = page.last || page.totalPages === 0;
}

function renderRequestRow(r) {
    const actions = `<button class="link-button" data-action="notes" data-id="${r.id}">История</button>${hasRole('REPAIRER', 'ADMIN') ? (r.archived ? `<button class="link-button" data-action="restore" data-id="${r.id}">Восстановить</button>` : `<button class="link-button" data-action="edit" data-id="${r.id}">Изменить</button><button class="link-button danger-text" data-action="archive" data-id="${r.id}">Архив</button>`) : ''}`;
    const checkbox = hasRole('REPAIRER', 'ADMIN') ? `<td><input class="row-select" type="checkbox" value="${r.id}" aria-label="Выбрать заявку"></td>` : '';
    return `<tr class="${r.archived ? 'archived-row' : ''}">${checkbox}<td><strong>${esc(r.requestNumber)}</strong><small>${esc(r.externalId || '—')}</small></td><td>${esc(r.customerName)}<small>${esc(r.customerPhone || '')}</small></td><td>${esc(r.applianceType)}<small>${esc(r.applianceBrand)}</small></td><td>${esc(r.issueTitle)}<small>${esc(r.issueDescription)}</small></td><td><span class="pill">${statusNames[r.status] || r.status}</span>${r.archived ? '<small>В архиве</small>' : ''}</td><td>${priorityNames[r.priority] || r.priority}<small>Риск: ${riskNames[r.riskLevel] || r.riskLevel}</small></td><td>${money(r.estimatedCost)}${r.urgent ? '<small class="urgent-text">Срочно</small>' : ''}</td><td>${dateOnly(r.plannedDate)}</td><td class="actions-cell">${actions}</td></tr>`;
}

$('search-requests').addEventListener('click', () => {
    state.requests.page = 0;
    loadRequests().catch(showGlobalError);
});
$('request-query').addEventListener('keydown', e => {
    if (e.key === 'Enter') $('search-requests').click();
});
$('request-sort').addEventListener('change', () => {
    state.requests.page = 0;
    loadRequests().catch(showGlobalError);
});
$('requests-prev').addEventListener('click', () => {
    state.requests.page = Math.max(0, state.requests.page - 1);
    loadRequests().catch(showGlobalError);
});
$('requests-next').addEventListener('click', () => {
    state.requests.page += 1;
    loadRequests().catch(showGlobalError);
});
$('requests-body').addEventListener('change', e => {
    if (e.target.matches('.row-select')) {
        e.target.checked ? state.requests.selected.add(e.target.value) : state.requests.selected.delete(e.target.value);
        updateSelectedCount();
    }
});
$('requests-body').addEventListener('click', e => {
    const button = e.target.closest('[data-action]');
    if (!button) return;
    const {action, id} = button.dataset;
    const handlers = {
        notes: () => openNotes(id),
        edit: () => editRequest(id),
        restore: () => restoreRequest(id),
        archive: () => archiveRequest(id)
    };
    handlers[action]?.().catch(showGlobalError);
});
$('select-all').addEventListener('change', e => {
    document.querySelectorAll('.row-select').forEach(box => {
        box.checked = e.target.checked;
        if (box.checked) state.requests.selected.add(box.value); else state.requests.selected.delete(box.value);
    });
    updateSelectedCount();
});

function updateSelectedCount() {
    $('selected-count').textContent = `Выбрано: ${state.requests.selected.size}`;
}

function openRequestDialog(request = null) {
    clearError('request-error');
    $('request-form').dataset.mode = request ? 'edit' : 'create';
    $('request-dialog-title').textContent = request ? `Заявка ${request.requestNumber}` : 'Новая заявка';
    $('request-id').value = request?.id || '';
    $('request-version').value = request?.version ?? '';
    $('request-external-id').value = request?.externalId || '';
    $('request-customer').value = String(request?.customerId ?? state.customers[0]?.id ?? '');
    $('request-type').value = request?.applianceTypeId || state.types[0]?.id || '';
    $('request-brand').value = request?.applianceBrand || '';
    $('request-title').value = request?.issueTitle || '';
    $('request-description').value = request?.issueDescription || '';
    $('request-status').value = request?.status || 'NEW';
    $('request-priority').value = request?.priority || 'MEDIUM';
    $('request-cost').value = request?.estimatedCost ?? '0';
    $('request-date').value = request?.plannedDate || '';
    $('request-urgent').checked = !!request?.urgent;
    $('request-tags').value = (request?.tags || []).join(', ');
    $('request-dialog').showModal();
}

$('add-request').addEventListener('click', async () => {
    try {
        await ensureLookups();
        openRequestDialog();
    } catch (e) {
        showGlobalError(e);
    }
});

async function editRequest(id) {
    await ensureLookups();
    const request = await api(`/api/repair-requests/${id}`);
    openRequestDialog(request);
}

$('request-form').addEventListener('submit', async event => {
    event.preventDefault();
    clearError('request-error');
    const id = $('request-id').value;
    const body = {
        externalId: $('request-external-id').value.trim() || null,
        customerId: Number($('request-customer').value),
        applianceType: state.types.find(t => t.id === $('request-type').value)?.name || '',
        applianceTypeId: $('request-type').value,
        applianceBrand: $('request-brand').value.trim(),
        issueTitle: $('request-title').value.trim(),
        issueDescription: $('request-description').value.trim(),
        status: $('request-status').value,
        priority: $('request-priority').value,
        estimatedCost: Number($('request-cost').value),
        plannedDate: $('request-date').value || null,
        urgent: $('request-urgent').checked,
        tags: $('request-tags').value.split(',').map(s => s.trim()).filter(Boolean),
        version: id ? Number($('request-version').value) : null
    };
    try {
        await api(id ? `/api/repair-requests/${id}` : '/api/repair-requests', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(body)
        });
        $('request-dialog').close();
        toast(id ? 'Заявка обновлена' : 'Заявка создана');
        await loadRequests();
    } catch (error) {
        showError('request-error', error);
    }
});

async function archiveRequest(id) {
    if (!confirm('Переместить заявку в архив?')) return;
    await api(`/api/repair-requests/${id}`, {method: 'DELETE'});
    toast('Заявка архивирована');
    await loadRequests();
}

async function restoreRequest(id) {
    await api(`/api/repair-requests/${id}/restore`, {method: 'POST'});
    toast('Заявка восстановлена');
    await loadRequests();
}

$('bulk-action').addEventListener('change', e => {
    const value = e.target.value;
    $('bulk-status').classList.toggle('hidden', value !== 'SET_STATUS');
    $('bulk-tag').classList.toggle('hidden', !['ADD_TAG', 'REMOVE_TAG'].includes(value));
});
$('apply-bulk').addEventListener('click', async () => {
    const action = $('bulk-action').value;
    if (!action || !state.requests.selected.size) {
        toast('Выберите заявки и массовую операцию', 'error');
        return;
    }
    if (!confirm(`Применить операцию к ${state.requests.selected.size} заявкам?`)) return;
    const body = {
        ids: [...state.requests.selected],
        action,
        status: action === 'SET_STATUS' ? $('bulk-status').value : null,
        tag: ['ADD_TAG', 'REMOVE_TAG'].includes(action) ? $('bulk-tag').value.trim() : null
    };
    try {
        const result = await api('/api/repair-requests/bulk', {method: 'POST', body: JSON.stringify(body)});
        toast(`Выполнено: ${result.succeeded} из ${result.requested}; ошибок: ${result.failures.length}`, result.failures.length ? 'error' : 'success');
        $('import-result').classList.remove('hidden');
        $('import-result').textContent = result.failures.length ? `Ошибки массовой операции:\n${result.failures.map(f => `${f.id}: ${f.message}`).join('\n')}` : `Массовая операция выполнена для ${result.succeeded} заявок без ошибок.`;
        await loadRequests();
    } catch (e) {
        showGlobalError(e);
    }
});
$('export-csv').addEventListener('click', async () => {
    try {
        const response = await fetch(`/api/repair-requests/export?${queryString({q: $('request-query').value.trim()})}`, {
            headers: {Authorization: `Bearer ${state.token}`},
            credentials: 'include'
        });
        if (!response.ok) throw new Error(`Ошибка экспорта: HTTP ${response.status}`);
        const blob = await response.blob();
        downloadBlob(blob, `repair-requests-${new Date().toISOString().slice(0, 10)}.csv`);
    } catch (e) {
        showGlobalError(e);
    }
});
$('import-file').addEventListener('change', () => {
    state.importPreview = null;
    $('apply-import').disabled = true;
    $('import-result').classList.add('hidden');
    $('import-result').textContent = '';
});
$('import-mode').addEventListener('change', () => {
    state.importPreview = null;
    $('apply-import').disabled = true;
    $('import-result').classList.add('hidden');
    $('import-result').textContent = '';
});
$('preview-import').addEventListener('click', async () => {
    const file = $('import-file').files[0];
    if (!file) {
        toast('Сначала выберите CSV-файл', 'error');
        return;
    }
    const form = new FormData();
    form.append('file', file);
    try {
        state.importPreview = await api(`/api/repair-requests/import/preview?mode=${encodeURIComponent($('import-mode').value)}`, {
            method: 'POST',
            body: form
        });
        const p = state.importPreview;
        $('import-result').classList.remove('hidden');
        $('import-result').textContent = `Строк: ${p.totalRows}; корректных: ${p.validRows}; с ошибками: ${p.invalidRows}. ${p.issues?.slice(0, 10).map(i => `Строка ${i.row}: ${i.message}`).join(' | ') || 'Ошибок нет.'}`;
        $('apply-import').disabled = p.invalidRows > 0 || p.validRows === 0;
    } catch (e) {
        $('import-result').classList.remove('hidden');
        $('import-result').textContent = e.message;
        $('apply-import').disabled = true;
    }
});
$('apply-import').addEventListener('click', async () => {
    const file = $('import-file').files[0];
    if (!file || !state.importPreview || state.importPreview.invalidRows > 0) return;
    const form = new FormData();
    form.append('file', file);
    try {
        const result = await api(`/api/repair-requests/import/apply?mode=${encodeURIComponent($('import-mode').value)}`, {
            method: 'POST',
            body: form
        });
        $('import-result').textContent = `Импорт завершён. Создано: ${result.created}; обновлено: ${result.updated}; пропущено: ${result.skipped}; ошибок: ${result.issues?.length || 0}.`;
        $('apply-import').disabled = true;
        state.importPreview = null;
        await loadRequests();
    } catch (e) {
        $('import-result').textContent = e.message;
    }
});

function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}

async function openNotes(requestId) {
    state.notesRequestId = requestId;
    $('notes-list').innerHTML = '<p class="muted">Загрузка…</p>';
    const rows = await api(`/api/repair-requests/${requestId}/notes`);
    $('notes-list').innerHTML = rows.length ? rows.map(n => `<article class="note-card"><div class="note-meta"><strong>${esc(n.authorName)}</strong><span>${date(n.createdAt)}</span>${n.pinned ? '<span class="pill">Закреплён</span>' : ''}</div><p>${esc(n.body)}</p>${hasRole('REPAIRER', 'ADMIN') ? `<div class="note-actions"><button class="link-button" data-note-edit="${n.id}" data-version="${n.version}" data-pinned="${n.pinned}">Изменить</button><button class="link-button danger-text" data-note-delete="${n.id}">Удалить</button></div>` : ''}</article>`).join('') : '<p class="empty">Комментариев пока нет</p>';
    $('notes-dialog').showModal();
}

$('notes-list').addEventListener('click', async e => {
    const del = e.target.closest('[data-note-delete]');
    const edit = e.target.closest('[data-note-edit]');
    try {
        if (del && confirm('Удалить этот комментарий?')) {
            await api(`/api/repair-requests/${state.notesRequestId}/notes/${del.dataset.noteDelete}`, {method: 'DELETE'});
            await openNotes(state.notesRequestId);
        }
        if (edit) {
            const text = prompt('Новый текст комментария:');
            if (text === null || !text.trim()) return;
            const rows = await api(`/api/repair-requests/${state.notesRequestId}/notes`);
            const n = rows.find(x => x.id === edit.dataset.noteEdit);
            await api(`/api/repair-requests/${state.notesRequestId}/notes/${n.id}`, {
                method: 'PUT',
                body: JSON.stringify({
                    body: text,
                    pinned: edit.dataset.pinned === 'true',
                    version: Number(edit.dataset.version)
                })
            });
            await openNotes(state.notesRequestId);
        }
    } catch (error) {
        showGlobalError(error);
    }
});
$('note-form').addEventListener('submit', async e => {
    e.preventDefault();
    try {
        await api(`/api/repair-requests/${state.notesRequestId}/notes`, {
            method: 'POST',
            body: JSON.stringify({body: $('note-body').value.trim(), pinned: $('note-pinned').checked})
        });
        $('note-body').value = '';
        $('note-pinned').checked = false;
        await openNotes(state.notesRequestId);
        toast('Комментарий добавлен');
    } catch (error) {
        showGlobalError(error);
    }
});

async function loadAnalytics() {
    const a = await api('/api/analytics/summary');
    $('analytics-cards').innerHTML = [['Всего заявок', a.total], ['Активных', a.active], ['В архиве', a.archived], ['Срочных', a.urgent], ['Средняя оценка', money(a.averageEstimatedCost)]].map(([label, value]) => `<article class="stat-card"><span>${esc(label)}</span><strong>${esc(value)}</strong></article>`).join('');
    renderBars('analytics-types', a.byApplianceType);
    renderBars('analytics-status', a.byStatus.map(x => ({...x, name: statusNames[x.name] || x.name})));
    renderBars('analytics-risk', a.byRisk.map(x => ({...x, name: riskNames[x.name] || x.name})));
}

function renderBars(id, values) {
    const max = Math.max(1, ...values.map(v => Number(v.count)));
    $(id).innerHTML = values.length ? values.map(v => {
        const level = Math.max(5, Math.ceil(Number(v.count) / max * 20) * 5);
        return `<div class="bar-row"><div><span>${esc(v.name)}</span><strong>${v.count}</strong></div><div class="bar-track"><i class="bar-width-${level}"></i></div></div>`;
    }).join('') : '<p class="empty">Нет данных</p>';
}

$('refresh-analytics').addEventListener('click', () => loadAnalytics().catch(showGlobalError));

async function loadTypes() {
    state.types = await api('/api/references?activeOnly=false');
    $('types-body').innerHTML = state.types.map(t => `<tr><td>${esc(t.code)}</td><td>${esc(t.name)}</td><td>${esc(t.description || '—')}</td><td>${t.active ? '<span class="status-active">Активен</span>' : '<span class="status-inactive">Неактивен</span>'}</td><td>${hasRole('ADMIN') ? `<button class="link-button" data-type-edit="${t.id}">Изменить</button><button class="link-button" data-type-toggle="${t.id}" data-active="${t.active}" data-version="${t.version}">${t.active ? 'Отключить' : 'Включить'}</button>` : '—'}</td></tr>`).join('') || '<tr><td colspan="5" class="empty">Справочник пуст</td></tr>';
}

$('add-type').addEventListener('click', () => openTypeDialog());

function openTypeDialog(type = null) {
    clearError('type-error');
    $('type-dialog-title').textContent = type ? 'Изменить тип техники' : 'Новый тип техники';
    $('type-id').value = type?.id || '';
    $('type-version').value = type?.version ?? '';
    $('type-code').value = type?.code || '';
    $('type-name').value = type?.name || '';
    $('type-description').value = type?.description || '';
    $('type-dialog').showModal();
}

$('types-body').addEventListener('click', async e => {
    const edit = e.target.closest('[data-type-edit]'), toggle = e.target.closest('[data-type-toggle]');
    try {
        if (edit) openTypeDialog(state.types.find(t => t.id === edit.dataset.typeEdit));
        if (toggle) {
            const id = toggle.dataset.typeToggle;
            await api(`/api/references/${id}/active?active=${toggle.dataset.active !== 'true'}&version=${toggle.dataset.version}`, {method: 'PATCH'});
            await loadTypes();
            toast('Состояние типа изменено');
        }
    } catch (error) {
        showGlobalError(error);
    }
});
$('type-form').addEventListener('submit', async e => {
    e.preventDefault();
    const id = $('type-id').value;
    const body = {
        code: $('type-code').value.trim(),
        name: $('type-name').value.trim(),
        description: $('type-description').value.trim(),
        active: null,
        version: id ? Number($('type-version').value) : null
    };
    try {
        await api(id ? `/api/references/${id}` : '/api/references', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(body)
        });
        $('type-dialog').close();
        await loadTypes();
        toast('Справочник сохранён');
    } catch (error) {
        showError('type-error', error);
    }
});

async function loadUsers() {
    const p = await api(`/api/admin/users?${queryString({
        page: state.users.page,
        size: state.users.size,
        sort: 'email,asc'
    })}`);
    state.users.totalPages = p.totalPages;
    state.users.content = p.content;
    $('users-body').innerHTML = p.content.map(u => `<tr><td>${esc(u.displayName)}</td><td>${esc(u.email)}</td><td>${u.roles.map(r => `<span class="pill">${r}</span>`).join(' ')}</td><td>${u.enabled ? 'Да' : 'Нет'}</td><td>${u.passwordChangeRequired ? 'Требуется' : 'Нет'}</td><td><button class="link-button" data-user-edit="${u.id}">Изменить</button><button class="link-button" data-user-reset="${u.id}">Сброс пароля</button></td></tr>`).join('') || '<tr><td colspan="6" class="empty">Пользователей нет</td></tr>';
    $('users-page').textContent = `Страница ${p.page + 1} из ${Math.max(p.totalPages, 1)} · всего ${p.totalElements}`;
    $('users-prev').disabled = p.first;
    $('users-next').disabled = p.last || p.totalPages === 0;
}

$('users-prev').addEventListener('click', () => {
    state.users.page = Math.max(0, state.users.page - 1);
    loadUsers().catch(showGlobalError);
});
$('users-next').addEventListener('click', () => {
    state.users.page++;
    loadUsers().catch(showGlobalError);
});
$('add-user').addEventListener('click', () => openUserDialog());

function openUserDialog(user = null) {
    clearError('user-error');
    $('user-dialog-title').textContent = user ? 'Изменить пользователя' : 'Новый пользователь';
    $('user-id').value = user?.id || '';
    $('user-version').value = user?.version ?? '';
    $('user-name').value = user?.displayName || '';
    $('user-email').value = user?.email || '';
    $('user-password').value = '';
    $('user-password-label').classList.toggle('hidden', !!user);
    $('user-enabled-label').classList.toggle('hidden', !user);
    $('user-enabled').checked = user?.enabled ?? true;
    [...$('user-roles').options].forEach(option => option.selected = user?.roles?.includes(option.value) || (!user && option.value === 'VIEWER'));
    $('user-dialog').showModal();
}

$('users-body').addEventListener('click', async e => {
    const edit = e.target.closest('[data-user-edit]'), reset = e.target.closest('[data-user-reset]');
    try {
        if (edit) openUserDialog(state.users.content.find(u => u.id === edit.dataset.userEdit));
        if (reset) {
            const pass = prompt('Введите временный пароль (не менее 12 символов):');
            if (!pass) return;
            await api(`/api/admin/users/${reset.dataset.userReset}/reset-password`, {
                method: 'POST',
                body: JSON.stringify({temporaryPassword: pass})
            });
            toast('Временный пароль установлен');
            await loadUsers();
        }
    } catch (error) {
        showGlobalError(error);
    }
});
$('user-form').addEventListener('submit', async e => {
    e.preventDefault();
    const id = $('user-id').value;
    const roles = [...$('user-roles').selectedOptions].map(o => o.value);
    const body = id ? {
        email: $('user-email').value.trim(),
        displayName: $('user-name').value.trim(),
        enabled: $('user-enabled').checked,
        roles,
        version: Number($('user-version').value)
    } : {
        email: $('user-email').value.trim(),
        displayName: $('user-name').value.trim(),
        temporaryPassword: $('user-password').value,
        roles
    };
    try {
        await api(id ? `/api/admin/users/${id}` : '/api/admin/users', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(body)
        });
        $('user-dialog').close();
        await loadUsers();
        toast('Пользователь сохранён');
    } catch (error) {
        showError('user-error', error);
    }
});

async function loadAudit() {
    state.audit.query = $('audit-query').value.trim();
    const p = await api(`/api/admin/audit?${queryString({
        q: state.audit.query,
        page: state.audit.page,
        size: state.audit.size,
        sort: 'occurred,desc'
    })}`);
    state.audit.totalPages = p.totalPages;
    $('audit-body').innerHTML = p.content.map(a => `<tr><td>${date(a.occurredAt)}</td><td>${esc(a.actorEmail || 'Система')}</td><td>${esc(a.eventType)}</td><td>${esc(a.entityType || '')}<small>${esc(a.entityId || '')}</small></td><td>${esc(a.summary)}</td><td><code>${esc(a.correlationId || '—')}</code></td></tr>`).join('') || '<tr><td colspan="6" class="empty">Записей нет</td></tr>';
    $('audit-page').textContent = `Страница ${p.page + 1} из ${Math.max(p.totalPages, 1)} · всего ${p.totalElements}`;
    $('audit-prev').disabled = p.first;
    $('audit-next').disabled = p.last || p.totalPages === 0;
}

$('search-audit').addEventListener('click', () => {
    state.audit.page = 0;
    loadAudit().catch(showGlobalError);
});
$('audit-query').addEventListener('keydown', e => {
    if (e.key === 'Enter') $('search-audit').click();
});
$('audit-prev').addEventListener('click', () => {
    state.audit.page = Math.max(0, state.audit.page - 1);
    loadAudit().catch(showGlobalError);
});
$('audit-next').addEventListener('click', () => {
    state.audit.page++;
    loadAudit().catch(showGlobalError);
});

restoreSession();
