// Global variables
let currentUser = null;
let currentPage = 1;
let categories = [];
let currentChatTicket = null;
let chatMessages = [];

// DOM elements
const authContainer = document.getElementById('authContainer');
const appContainer = document.getElementById('appContainer');
const loadingSpinner = document.getElementById('loadingSpinner');

document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

function initializeApp() {
    const token = localStorage.getItem('token');
    if (token) {
        currentUser = JSON.parse(localStorage.getItem('user'));
        showApp();
        loadDashboard();
    } else {
        showAuth();
    }
    setupAuthListeners();
    setupNavigationListeners();
    setupFormListeners();
    setupFilterListeners();
}

function setupAuthListeners() {
    document.querySelectorAll('.auth-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            switchAuthTab(this.dataset.tab);
        });
    });
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    document.getElementById('registerForm').addEventListener('submit', handleRegister);
    document.getElementById('logoutBtn').addEventListener('click', handleLogout);
}

function switchAuthTab(tabName) {
    document.querySelectorAll('.auth-tab').forEach(tab => tab.classList.remove('active'));
    document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
    document.getElementById('loginForm').style.display = tabName === 'login' ? 'block' : 'none';
    document.getElementById('registerForm').style.display = tabName === 'register' ? 'block' : 'none';
}

async function handleLogin(e) {
    e.preventDefault();
    showLoading();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await response.json();
        if (response.ok) {
            localStorage.setItem('token', data.token);
            localStorage.setItem('user', JSON.stringify(data.user));
            currentUser = data.user;
            showToast('Login successful!', 'success');
            showApp();
            loadDashboard();
        } else {
            showToast(data.error || 'Login failed', 'error');
        }
    } catch (error) {
        showToast('Network error', 'error');
    } finally {
        hideLoading();
    }
}

async function handleRegister(e) {
    e.preventDefault();
    showLoading();
    const username = document.getElementById('registerUsername').value;
    const email = document.getElementById('registerEmail').value;
    const password = document.getElementById('registerPassword').value;
    const role = document.getElementById('registerRole').value;

    try {
        const response = await fetch('/api/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password, role, specializations: [] })
        });
        const data = await response.json();
        if (response.ok) {
            localStorage.setItem('token', data.token);
            localStorage.setItem('user', JSON.stringify(data.user));
            currentUser = data.user;
            showToast('Registration successful!', 'success');
            showApp();
            loadDashboard();
        } else {
            showToast(data.error || 'Registration failed', 'error');
        }
    } catch (error) {
        showToast('Network error', 'error');
    } finally {
        hideLoading();
    }
}

function handleLogout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    currentUser = null;
    showAuth();
    showToast('Logged out successfully', 'info');
}

function setupNavigationListeners() {
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            if (this.dataset.page) showPage(this.dataset.page);
        });
    });
}

function showPage(pageName) {
    if (!currentUser && (pageName === 'dashboard' || pageName === 'create-ticket')) {
        showToast('Please log in first', 'error');
        showAuth();
        return;
    }
    document.querySelectorAll('.page').forEach(page => page.classList.remove('active'));
    const targetPage = document.getElementById(pageName + 'Page');
    if (targetPage) targetPage.classList.add('active');

    switch (pageName) {
        case 'dashboard': loadDashboard(); break;
        case 'create-ticket': loadCreateTicket(); break;
        case 'tickets': loadMyTickets(); break;
        case 'admin': if (currentUser.role === 'admin') loadAdminPanel(); break;
    }
    document.querySelectorAll('.nav-link').forEach(link => link.classList.remove('active'));
    const activeLink = document.querySelector(`[data-page="${pageName}"]`);
    if (activeLink) activeLink.classList.add('active');
}

async function loadDashboard() {
    showLoading();
    try {
        await loadCategories();
        await loadTickets();
        updateStats();
    } catch (error) {} finally { hideLoading(); }
}

async function loadTickets() {
    try {
        const response = await fetch(`/api/tickets`, {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        const data = await response.json();
        if (response.ok) displayTickets(data.tickets, 'ticketsContainer');
    } catch (error) { showToast('Error loading tickets', 'error'); }
}

async function loadMyTickets() {
    try {
        const response = await fetch(`/api/tickets/my`, {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        const data = await response.json();
        if (response.ok) displayTickets(data.tickets, 'myTicketsContainer');
    } catch (error) { showToast('Error loading my tickets', 'error'); }
}

function displayTickets(tickets, containerId) {
    const container = document.getElementById(containerId);
    container.innerHTML = '';
    if (!tickets || tickets.length === 0) {
        container.innerHTML = '<div class="no-tickets">No tickets found</div>';
        return;
    }
    tickets.forEach(ticket => {
        const div = document.createElement('div');
        div.className = 'ticket-card';
        div.onclick = () => showTicketDetail(ticket.id);
        const catName = ticket.categoryName || 'General';
        div.innerHTML = `
            <div class="ticket-header">
                <div>
                    <div class="ticket-title">${ticket.subject}</div>
                    <div class="ticket-meta">
                        <span class="ticket-status status-${ticket.status.replace(' ', '-')}">${ticket.status}</span>
                        <span>${catName}</span>
                    </div>
                </div>
            </div>
            <div class="ticket-description" style="white-space: pre-wrap;">${ticket.description.substring(0, 150)}...</div>
            <div class="ticket-footer">
                <button class="btn btn-primary btn-sm" onclick="event.stopPropagation(); openChat('${ticket.id}')">
                    <i class="fas fa-comments"></i> Chat
                </button>
            </div>
        `;
        container.appendChild(div);
    });
}

async function updateStats() {
    try {
        const response = await fetch('/api/stats', { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } });
        if (response.ok) {
            const stats = await response.json();
            document.getElementById('totalTickets').textContent = stats.totalTickets;
            document.getElementById('openTickets').textContent = stats.openTickets;
            document.getElementById('resolvedTickets').textContent = stats.resolvedTickets;
        }
    } catch (error) {}
}

function setupFilterListeners() {
    document.getElementById('searchInput').addEventListener('input', loadTickets);
}

function setupFormListeners() {
    document.getElementById('createTicketForm').addEventListener('submit', handleCreateTicket);
}

// -------------------------------------------------------------
// EXACTLY 7 BLOCKS AND 5 HOSTELS LOGIC HERE
// -------------------------------------------------------------
function updateLocations() {
    const type = document.getElementById('ticketLocationType').value;
    const locationDropdown = document.getElementById('ticketLocation');
    
    locationDropdown.innerHTML = '<option value="">Select Location</option>';
    
    let options = [];
    if (type === 'Academic') {
        options = ['Block 1', 'Block 2', 'Block 3', 'Block 4', 'Block 5', 'Block 6', 'Block 7'];
    } else if (type === 'Hostel') {
        options = ['Hostel 1', 'Hostel 2', 'Hostel 3', 'Hostel 4', 'Hostel 5'];
    }

    options.forEach(opt => {
        const el = document.createElement('option');
        el.value = opt;
        el.textContent = opt;
        locationDropdown.appendChild(el);
    });
}
window.updateLocations = updateLocations;

async function loadCreateTicket() {
    try {
        await loadCategories();
        const select = document.getElementById('ticketCategory');
        select.innerHTML = '';
        if(categories.length > 0) {
            categories.forEach(c => {
                select.innerHTML += `<option value="${c.id}">${c.name}</option>`;
            });
        } else {
            select.innerHTML = '<option value="1">General</option>';
        }
    } catch (error) {}
}

// -------------------------------------------------------------
// COLLECTING ALL GLA FIELDS INTO BACKEND-FRIENDLY FORMAT
// -------------------------------------------------------------
async function handleCreateTicket(e) {
    e.preventDefault();
    showLoading();

    const formData = new FormData();
    
    // Fetch all form inputs
    const area = document.getElementById('ticketArea').value;
    const related = document.getElementById('ticketRelatedTo').value;
    const compType = document.getElementById('ticketComplaintType').value;
    const priority = document.getElementById('ticketPriority').value;
    
    const locType = document.getElementById('ticketLocationType').value;
    const loc = document.getElementById('ticketLocation').value;
    const floor = document.getElementById('ticketFloor').value || "Not specified";
    const room = document.getElementById('ticketRoomNo').value;
    
    const availTime = document.getElementById('ticketAvailableTime').value || "Any time";
    const mobile = document.getElementById('ticketMobile').value;
    const detail = document.getElementById('ticketDescription').value;
    
    // Combining Subject: "Academic Area - Electrical (Fan not working)"
    const combinedSubject = `${area} - ${related} (${compType})`;
    
    // Combining Details properly for the ticket body
    const combinedDescription = 
`[Priority: ${priority}] | [Contact: ${mobile}] | [Available: ${availTime}]
[Location: ${locType} > ${loc} | Floor: ${floor} | Room: ${room}]

Complaint Details:
${detail}`;

    formData.append('subject', combinedSubject);
    formData.append('description', combinedDescription);
    
    // Adding the hidden category ID so backend doesn't crash
    const catId = document.getElementById('ticketCategory').value || 1;
    formData.append('categoryId', catId);

    const attachment = document.getElementById('ticketAttachment').files[0];
    if (attachment) formData.append('attachment', attachment);

    try {
        const response = await fetch('/api/tickets', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` },
            body: formData
        });
        if (response.ok) {
            showToast('Grievance submitted successfully!', 'success');
            document.getElementById('createTicketForm').reset();
            showPage('dashboard');
        } else {
            showToast('Error creating ticket', 'error');
        }
    } catch (error) {
        showToast('Network error', 'error');
    } finally {
        hideLoading();
    }
}

async function showTicketDetail(ticketId) {
    showLoading();
    try {
        const response = await fetch(`/api/tickets/${ticketId}`, {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        });
        const ticket = await response.json();
        if (response.ok) {
            const container = document.getElementById('ticketDetailContainer');
            const statusClass = `status-${ticket.status.replace(' ', '-')}`;
            container.innerHTML = `
                <div class="ticket-detail-header">
                    <h2>${ticket.subject}</h2>
                    <div class="ticket-meta">
                        <span class="ticket-status ${statusClass}">${ticket.status}</span>
                    </div>
                </div>
                <div class="ticket-detail-content">
                    <p style="white-space: pre-wrap; font-family: monospace; font-size: 14px;">${ticket.description}</p>
                </div>
            `;
            showPage('ticketDetail');
        }
    } catch (error) {} finally { hideLoading(); }
}

async function loadCategories() {
    try {
        const response = await fetch('/api/categories', { headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } });
        if (response.ok) categories = await response.json();
    } catch (error) {}
}

async function openChat(ticketId) { showPage('chat'); }

function showAuth() { authContainer.style.display = 'flex'; appContainer.style.display = 'none'; }
function showApp() { authContainer.style.display = 'none'; appContainer.style.display = 'block'; }
function showLoading() { loadingSpinner.style.display = 'flex'; }
function hideLoading() { loadingSpinner.style.display = 'none'; }
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

window.showPage = showPage;
window.openChat = openChat;