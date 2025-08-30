'use strict';

/* ========================================================
   DOM HELPERS & GLOBALS
======================================================== */
const $ = (sel) => document.querySelector(sel);

// CSRF
const csrfToken = document.querySelector("meta[name='_csrf']")?.content;
const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.content;
const DEFAULT_AVATAR_URL = '/images/defaultAvt.jpg';
const avatarCache = new Map(); // username => url


// Xóa và rơì nhóm
const groupActions   = document.getElementById('groupActions');
const leaveGroupBtn  = document.getElementById('leaveGroupBtn');
const deleteGroupBtn = document.getElementById('deleteGroupBtn');

leaveGroupBtn?.addEventListener('click', leaveCurrentGroup);
deleteGroupBtn?.addEventListener('click', deleteCurrentGroup);




const editMailInput  = $("#editMail");
const editPhoneInput = $("#editPhone");
let   currentProfile = null; // nhớ profile mới nhất lấy từ server
// Main elements
const usernamePage = $('#username-page');
const chatPage = $('#chat-page');
const messageInput = $("#messageInput");
const sendButton = $("#sendButton");
const chatMessages = $("#chatMessages");
// lưu ảnh và vi déo
const attachBtn = document.getElementById("attachBtn");
const attachInput = document.getElementById("attachInput");
const attachmentPreviewBar = document.getElementById("attachmentPreviewBar");
let pendingAttachments = [];

// === Upload 1 file → trả {type, url, name, size}
async function uploadOneFile(file) {
  const fd = new FormData();
  fd.append('file', file);

  const headers = {};
  if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

  const res = await fetch('/api/uploads', { method: 'POST', headers, body: fd });
  if (!res.ok) {
    console.error('Upload failed:', await res.text());
    showErrorMessage('Không upload được tệp.');
    return null;
  }
  const data = await res.json(); // kỳ vọng { url, name?, size?, contentType? }
  const url  = data.url;
  const name = data.name || file.name;
  const size = data.size || file.size;
  const ct   = (data.contentType || file.type || '').toLowerCase();

  let type = 'file';
  if (ct.startsWith('image/')) type = 'image';
  else if (ct.startsWith('video/')) type = 'video';

  return { type, url, name, size };
}

// === Thanh preview cho file đính kèm (cho phép xóa trước khi gửi)
function addAttachmentPreview(att) {
  if (!attachmentPreviewBar) return;
  const wrap = document.createElement('div');
  wrap.className = 'att-prev flex items-center gap-2 px-2 py-1 rounded-lg bg-gray-100 dark:bg-gray-700 border';

  let inside = '';
  if (att.type === 'image') {
    inside = `<img src="${att.url}" alt="${att.name || ''}" class="w-12 h-12 object-cover rounded-md">`;
  } else if (att.type === 'video') {
    inside = `<video src="${att.url}" class="w-16 h-12 rounded-md" preload="metadata"></video>`;
  } else {
    inside = `<div class="text-xs font-medium truncate max-w-[160px]">${(att.name || att.url).split('/').pop()}</div>`;
  }

  wrap.innerHTML = `
    ${inside}
    <button type="button" class="ml-1 px-2 py-1 text-xs rounded bg-red-500 hover:bg-red-600 text-white">Xóa</button>
  `;

  wrap.querySelector('button').addEventListener('click', () => {
    const idx = pendingAttachments.indexOf(att);
    if (idx >= 0) pendingAttachments.splice(idx, 1);
    wrap.remove();
  });

  attachmentPreviewBar.appendChild(wrap);
}

// === Render attachments trong bong bóng
function renderAttachmentsHtml(attachments) {
  if (!Array.isArray(attachments) || attachments.length === 0) return '';
  return attachments.map(a => {
    if (!a || !a.url) return '';
    const name = (a.name || a.url).split('/').pop();
    if (a.type === 'image') {
      return `<div class="mt-2"><img src="${a.url}" alt="${name}" class="max-h-72 rounded-lg object-contain"></div>`;
    }
    if (a.type === 'video') {
      return `<div class="mt-2"><video src="${a.url}" controls class="max-h-72 rounded-lg"></video></div>`;
    }
    return `<div class="mt-2"><a href="${a.url}" target="_blank" rel="noopener" class="underline break-all">📎 ${name}</a></div>`;
  }).join('');
}
// --- Nhận diện URL file & chuẩn hoá message nhận về (URL → attachments[])
function detectFileTypeFromUrl(url = '') {
  const u = url.toLowerCase();
  if (u.match(/\.(png|jpe?g|gif|webp|bmp|svg)(\?|#|$)/)) return 'image';
  if (u.match(/\.(mp4|webm|ogg|mov|m4v)(\?|#|$)/)) return 'video';
  return 'file';
}
function looksLikeHttpOrUploads(s) {
  return /^https?:\/\//i.test((s || '').trim()) || /^\/uploads\//i.test((s || '').trim());
}
function normalizeIncomingMessage(msg) {
  const m = { ...msg };
  const hasAttArray = Array.isArray(m.attachments) && m.attachments.length > 0;
  const raw = (m.content || '').trim();
  const typeFromSrv = (m.type || m.messageType || '').toString().toLowerCase();

  if (!hasAttArray) {
    if (typeFromSrv && raw && looksLikeHttpOrUploads(raw)) {
      const inferred = typeFromSrv.includes('image') ? 'image'
          : typeFromSrv.includes('video') ? 'video'
              : 'file';
      m.attachments = [{ type: inferred, url: raw, name: raw.split('/').pop() }];
      if (inferred !== 'file') m.content = '';
    } else if (raw && looksLikeHttpOrUploads(raw)) {
      const inferred = detectFileTypeFromUrl(raw);
      m.attachments = [{ type: inferred, url: raw, name: raw.split('/').pop() }];
      if (inferred !== 'file') m.content = '';
    }
  }
  return m;
}

const groupMsgIndex = new Map();
let stompClient = null;
let username = null;
let currentChat = null; // {type: 'public'|'private'|'group', id, name, friendId?, friendUsername?}

/** Biệt danh theo thành viên cho phòng hiện tại: Map<scope|key, nickname>
 *  scope = `${type}:${id}`; key = `id:<id>` | `u:<username>`
 */
let memberNickMap = new Map();

/* ========================================================
   THEME (HỢP NHẤT)
======================================================== */
const html = document.documentElement;
const themeBtn  = document.getElementById("themeToggle");
const themeIcon = document.getElementById("themeIcon");
const themeText = document.getElementById("themeText");

function updateThemeIcons() {
  const theme = localStorage.getItem("webchat-theme") || "light";
  html.classList.toggle("dark", theme === "dark");
  if (themeBtn) {
    themeBtn.className =
        "px-4 py-2 rounded-xl border shadow toolbar-float flex items-center gap-2 transition " +
        (theme === "dark"
            ? "bg-gray-800 text-gray-100 border-gray-600 hover:bg-gray-700"
            : "bg-slate-100 text-gray-700 border-gray-300 hover:bg-slate-200");
  }
  if (themeIcon) themeIcon.textContent = theme === "dark" ? "🌙" : "🌞";
  if (themeText) themeText.textContent = theme === "dark" ? "Dark" : "Light";
}

(function () {
  const saved = localStorage.getItem("webchat-theme") || "light";
  localStorage.setItem("webchat-theme", saved);
  updateThemeIcons();

  themeBtn?.addEventListener("click", () => {
    const next = html.classList.contains("dark") ? "light" : "dark";
    localStorage.setItem("webchat-theme", next);
    updateThemeIcons();
  });
})();

/* ========================================================
   NAME/INITIALS + GRADIENT HELPERS
======================================================== */
const gradients = [
  "from-green-500 to-teal-500","from-blue-500 to-indigo-500",
  "from-red-500 to-pink-500","from-purple-500 to-pink-500",
  "from-emerald-500 to-cyan-500","from-amber-500 to-orange-600"
];
function pickGradient(seed = 0) { return gradients[seed % gradients.length]; }
function getInitials(name) {
  if (!name) return "??";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0,2).toUpperCase();
  return (parts[0][0] + parts[parts.length-1][0]).toUpperCase();
}
function simpleHash(str) {
  let h = 0;
  for (let i=0;i<str.length;i++){ h=(h<<5)-h+str.charCodeAt(i); h|=0; }
  return Math.abs(h);
}

/* ========================================================
   WEBSOCKET + STOMP
======================================================== */
let isConnected = false;
let subs = {}; // lưu các subscription để tránh đăng ký trùng
function connect(event) {
  // Nếu đã kết nối thì thôi
  if (isConnected && stompClient?.connected) {
    event?.preventDefault?.();
    return;
  }

  username = $('#username')?.value.trim();
  if (username) {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    const headers = {};
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
    stompClient.connect(headers, onConnected, onError);
  }
  event?.preventDefault?.();
}

function onConnected() {
  console.log('✅ Connected to WebSocket');
  isConnected = true;

  // Hủy subs cũ (nếu có) để không bị đăng ký chồng
  Object.values(subs).forEach(s => s?.unsubscribe?.());
  subs = {};
  stompClient.subscribe('/topic/user-status', onUserStatusChanged);
  stompClient.subscribe('/user/queue/private',        onPrivateMessageReceived);
  stompClient.subscribe('/user/queue/friend-request', onFriendRequestReceived);
  stompClient.subscribe('/user/queue/group',          onGroupMessageReceived);
  loadInitialData();
  loadPendingFriendRequests();
  refreshSidebar();
  loadProfileFromDatabase();
  // Nạp biệt danh nếu đang ở private chat
  if (currentChat?.type === 'private') {
    hydratePrivateNickname(currentChat.id);
  }
}

function onError(error) {
  console.error('❌ WebSocket connection error:', error);
  showErrorMessage('Lỗi kết nối. Vui lòng tải lại trang.');
}

/* ========================================================
   MESSAGE HANDLERS
======================================================== */
function onPrivateMessageReceived(payload) {
  const message = JSON.parse(payload.body);
  if (currentChat?.type === 'private' && blockToggle?.checked) return;

  const msgChatId = message.chatId ?? message.privateChatId ?? message.chat?.id;
  if (currentChat?.type === 'private' && currentChat?.id == msgChatId) {
    try { displayPrivateMessage(message, true); } catch (e) { console.error(e); }
  }
  updateChatListWithNewMessage();
}
function onGroupMessageReceived(payload) {
  const message = normalizeIncomingMessage(JSON.parse(payload.body));
  const msgGroupId = message.groupId ?? message.chatId ?? message.group?.id;
  if (!(currentChat?.type === 'group' && currentChat?.id == msgGroupId)) {
    updateChatListWithNewMessage();
    return;
  }

  const hasAtt = Array.isArray(message.attachments) && message.attachments.length > 0;
  const mid = message.id || null;

  if (mid) {
    const prev = groupMsgIndex.get(mid);
    if (prev) {

      if (prev.hasAtt || !hasAtt) {
        return;
      }
      updateGroupMessageBubble(message);
      groupMsgIndex.set(mid, { hasAtt: true });
      return;
    } else {
      displayGroupMessage(message, true);
      groupMsgIndex.set(mid, { hasAtt });
      return;
    }
  }

  displayGroupMessage(message, true);
}

function onFriendRequestReceived(payload) {
  const notification = JSON.parse(payload.body);
  showFriendRequestNotification(notification);
  scheduleSidebarRefresh();
}
function onUserStatusChanged() { scheduleSidebarRefresh(); }

/* ========================================================
   DATA LOADING
======================================================== */
async function loadInitialData() {
  try { await loadChatHistory(); }
  catch (e) { console.error('Error loading initial data:', e); }
}

async function loadChatHistory() {
  if (currentChat?.type === 'private') await loadPrivateChatHistory(currentChat.id);
  else if (currentChat?.type === 'group') await loadGroupChatHistory(currentChat.id);
}

async function loadPrivateChatHistory(chatId) {
  try {
    const res = await fetch(`/api/private-chat/${chatId}/messages`, { headers: { [csrfHeader]: csrfToken } });
    if (res.ok) {
      const messages = await res.json();
      chatMessages.innerHTML = '';
      messages.forEach(m => displayPrivateMessage(m, false));
      scrollToBottom();
    }
  } catch (e) { console.error('Error loading private chat history:', e); }
}

async function loadGroupChatHistory(groupId) {
  try {
    const res = await fetch(`/api/groups/${groupId}/messages`, { headers: { [csrfHeader]: csrfToken } });
    if (res.ok) {
      const messages = await res.json();
      chatMessages.innerHTML = '';
      messages.forEach(m => displayGroupMessage(normalizeIncomingMessage(m), false));
      scrollToBottom();
    }
  } catch (e) { console.error('Error loading group chat history:', e); }
}

/* ========================================================
   FRIENDS / GROUP LIST RENDER
======================================================== */

function createSection(title, count) {
  const s = document.createElement("div");
  s.className = "px-3 py-2 border-t border-gray-200 dark:border-gray-700 mt-2 first:mt-0 first:border-t-0";
  s.innerHTML = `<h4 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">${title} ${count>0?`(${count})`:''}</h4>`;
  return s;
}
function createFriendItem(friend) {
  const displayName = (friend.fullName && friend.fullName.trim()) ? friend.fullName : friend.username;
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(friend.username||''));
  const url = extractAvatarUrl(friend) || (friend.username && avatarCache.get(friend.username)) || null;

  const w = document.createElement('div');
  w.className = "chat-item p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors";
  w.onclick = () => switchToPrivateChat(friend);

  // 👉 thêm data-username để tìm lại node khi cần
  w.setAttribute('data-username', friend.username || '');

  w.innerHTML = `
    <div class="flex items-center space-x-3">
      <!-- 👉 holder để thay avatar in-place -->
      <div class="relative" data-avatar-for="${friend.username || ''}">
        ${renderAvatar(url, initials, gradient, 10)}
      </div>
      <div class="flex-1 min-w-0">
        <h4 class="chat-name font-medium text-gray-900 dark:text-white truncate text-sm">${displayName}</h4>
        <p class="text-xs text-gray-500 dark:text-gray-400 truncate">${friend.status==='ONLINE'?'Đang online':'Offline'}</p>
      </div>
    </div>`;
  return w;
}

function patchSidebarAvatar(username, url, displayName) {
  const esc = window.CSS?.escape ? CSS.escape(username) : String(username).replace(/"/g, '\\"');
  const holder = document.querySelector(`[data-avatar-for="${esc}"]`);
  if (!holder) return;
  const initials = getInitials(displayName || username);
  const gradient = pickGradient(simpleHash(username || ''));
  holder.innerHTML = renderAvatar(url, initials, gradient, 10);
}

function createGroupItem(group) {
  const initials = getInitials(group.name);
  const gradient = pickGradient(simpleHash(group.name||''));
  const w = document.createElement('div');
  w.className = "chat-item p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors";
  w.onclick = () => switchToGroupChat(group);
  w.innerHTML = `
    <div class="flex items-center space-x-3">
      <div class="relative">
        <div class="w-10 h-10 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center"><span class="text-white text-sm font-medium">${initials}</span></div>
        <div class="absolute -bottom-1 -right-1 w-3 h-3 bg-blue-500 rounded-full border-2 border-white dark:border-gray-900"></div>
      </div>
      <div class="flex-1 min-w-0">
        <h4 class="chat-name font-medium text-gray-900 dark:text-white truncate text-sm">${group.name}</h4>
        <p class="text-xs text-gray-500 dark:text-gray-400 truncate">${group.memberCount} thành viên</p>
      </div>
    </div>`;
  return w;
}

/* ========================================================
   CHAT TITLE NICKNAME (per room)
======================================================== */
const chatTitleEl = $("#chatTitle");
const chatNicknameInput = $("#chatNicknameInput");
const applyNicknameBtn = $("#applyNickname");

function titleKeyForCurrentChat(){
  if (!currentChat) return null;
  const id = currentChat.id ?? 'public';
  return `chatTitle:${currentChat.type}:${id}`;
}
function defaultTitleForCurrentChat(){
  if (!currentChat) return 'WebChat Pro';
  if (currentChat.type==='group')  return currentChat.name;
  if (currentChat.type==='private') {
    // Lấy theo biệt danh nếu có
    const partnerLike = {
      id: currentChat.friendId,
      username: currentChat.friendUsername,
      fullName: currentChat.name
    };
    const displayName = resolveDisplayNameFromMap(partnerLike);
    return `Chat với ${displayName}`;
  }
  return 'WebChat Pro';
}
function applyTitleNicknameFromStorage(){
  if (!currentChat) return;
  // Riêng PRIVATE: bỏ cơ chế nickname theo "title", luôn hiển thị theo biệt danh partner
  if (currentChat.type === 'private') {
    updatePrivateHeaderTitleWithNickname();
    return;
  }
  const key = titleKeyForCurrentChat();
  const nick = localStorage.getItem(key) || '';
  if (chatTitleEl) chatTitleEl.textContent = nick || defaultTitleForCurrentChat();
  if (chatNicknameInput) chatNicknameInput.value = nick;
}
applyNicknameBtn?.addEventListener('click', async () => {
  if (!currentChat) return;
  const nickname = (chatNicknameInput?.value || '').trim();

  try {
    if (currentChat.type === 'private') {
      const res = await fetch(`/api/private-chats/${currentChat.id}/nickname`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
        },
        body: JSON.stringify({ nickname })
      });

      if (!res.ok) {
        showErrorMessage('Không lưu được biệt danh.');
        return;
      }
      const data = await res.json();
      showSuccessMessage('Đã lưu biệt danh!');
      document.getElementById('chatTitle').textContent = data.nickname || currentChat.friendUsername;


      // Cập nhật header ngay
      const headerName = data.nickname || currentChat.name || currentChat.friendUsername || 'Người dùng';
      const chatTitle  = document.getElementById('chatTitle');
      if (chatTitle) chatTitle.textContent = `Chat với ${headerName}`;

      showSuccessMessage('Đã lưu biệt danh private.');
      return;
    }

    // GROUP: giữ nguyên cách cũ của bạn hoặc cũng dùng API nhóm nếu đã mở
    const res = await fetch(`/api/groups/${currentChat.id}/nickname`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
      },
      body: JSON.stringify({ nickname })
    });
    if (!res.ok) throw new Error(await res.text().catch(()=> ''));
    const data = await res.json();
    const chatTitle = document.getElementById('chatTitle');
    if (chatTitle) chatTitle.textContent = data.nickname || currentChat.name || 'Nhóm';
    showSuccessMessage('Đã lưu biệt danh nhóm.');
  } catch (err) {
    console.error(err);
    showErrorMessage('Không lưu được biệt danh.');
  }
});


/* ========================================================
   SWITCH CHAT
======================================================== */
function updateChatHeader(icon, title, subtitle) {
  const chatTitle = $("#chatTitle");
  const appStatusText = $("#appStatusText");
  if (chatTitle) chatTitle.textContent = title;
  if (appStatusText) appStatusText.textContent = subtitle;
}

async function switchToPrivateChat(friend) {
  try {
    const res = await fetch('/api/private-chat/start-chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
      body: JSON.stringify({ username: friend.username })
    });
    if (res.ok) {
      const chatData = await res.json();
      currentChat = {
        type: "private",
        id: chatData.chatId,
        name: friend.fullName || friend.username,
        friendId: friend.id,
        friendUsername: friend.username
      };
      groupActions?.classList.add('hidden');
      updateChatHeader(getInitials(currentChat.name), `Chat với ${currentChat.name}`, 'Chat riêng tư');

      // 👉 nạp nickname partner từ server
      await hydratePrivateNickname(currentChat.id);

      initBlockToggleForCurrentChat();
      await loadPrivateChatHistory(chatData.chatId);

      // 👉 cập nhật lại header theo biệt danh nếu có
      updatePrivateHeaderTitleWithNickname();
    } else {
      showErrorMessage('Không thể bắt đầu chat riêng');
    }
  } catch (e) {
    console.error('Error starting private chat:', e);
    showErrorMessage('Có lỗi xảy ra khi mở chat riêng');
  }
}


function switchToGroupChat(group) {
  currentChat = { type: 'group', id: group.id, name: group.name };
  updateChatHeader(getInitials(group.name), group.name, `${group.memberCount} thành viên`);
  applyTitleNicknameFromStorage();
  initBlockToggleForCurrentChat();

  // Tải nickname trước, sau đó reload history để hiển thị đúng
  loadGroupMemberNicknames(group.id).then(()=>{
    loadGroupChatHistory(group.id);
  });
  updateGroupActionButtons(group.id);
}

/* ========================================================
   SEND MESSAGE
======================================================== */
function sendMessage(evt) {
  const content = messageInput?.value.trim();
  const hasFiles = pendingAttachments.length > 0;

  if ((!content && !hasFiles) || !stompClient || messageInput.disabled) return;

  if (currentChat?.type === 'private') sendPrivateMessage(content, pendingAttachments);
  else if (currentChat?.type === 'group') sendGroupMessage(content, pendingAttachments);
  else {
    showErrorMessage('Hãy chọn một cuộc trò chuyện (private hoặc group) trước khi gửi.');
    return;
  }
}

async function sendPrivateMessage(content, attachments = []) {
  try {
    const res = await fetch(`/api/private-chat/${currentChat.id}/send`, {
      method:'POST',
      headers:{ 'Content-Type':'application/json', ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) },
      body: JSON.stringify({ content: content || '', attachments })
    });
    // Không display ở đây; chờ onPrivateMessageReceived đẩy về để hiển thị
    if (!res.ok) console.error('Send private failed:', await res.text());
  } catch (e) { console.error('Error sending private message:', e); }
}

async function sendGroupMessage(content, attachments = []) {
  try {
    const res = await fetch(`/api/groups/${currentChat.id}/send`, {
      method:'POST',
      headers:{ 'Content-Type':'application/json', ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) },
      body: JSON.stringify({ content: content || '', attachments })
    });
    // Không display ở đây; chờ onGroupMessageReceived
    if (!res.ok) console.error('Send group failed:', await res.text());
  } catch (e) { console.error('Error sending group message:', e); }
}

/* ========================================================
   MESSAGE DISPLAY (ƯU TIÊN BIỆT DANH)
======================================================== */
function buildGroupBubble(message) {
  const div = document.createElement('div');
  div.className = 'flex items-start space-x-3 message-bubble';

  const s = normalizeSender(message);
  const isMe = s.username === username;

  const displayName = resolveDisplayNameFromMap(s);
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(s.username || String(s.id || '')));
  const t = message.timestamp ? new Date(message.timestamp) : null;
  const time = (t && !isNaN(t)) ? t.toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'})
      : new Date().toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'});
  const hasAtt = Array.isArray(message.attachments) && message.attachments.length > 0;

  if (isMe) {
    div.classList.add('justify-end');
    div.innerHTML = `
      <div class="bg-gradient-to-r from-purple-500 to-purple-700 rounded-2xl rounded-tr-md px-4 py-3 max-w-xs lg:max-w-md break-words">
        ${ hasAtt ? '' : `<p class="text-white">${message.content || ''}</p>` }
        ${renderAttachmentsHtml(message.attachments)}
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-purple-100">${time}</span></div>
      </div>
      <div class="w-8 h-8 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white text-sm font-bold">${initials}</span></div>`;
  } else {
    div.classList.add('items-start');
    div.innerHTML = `
      <div class="w-8 h-8 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white text-sm font-bold">${initials}</span></div>
      <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-md px-4 py-3 max-w-xs lg:max-w-md break-words">
        <div class="text-xs text-gray-500 dark:text-gray-400 mb-1 font-medium">${displayName}</div>
        ${ hasAtt ? '' : `<p class="text-gray-800 dark:text-gray-200">${message.content || ''}</p>` }
        ${renderAttachmentsHtml(message.attachments)}
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-gray-500 dark:text-gray-400">${time}</span></div>
      </div>`;
  }
  return div;
}

function looksLikeFileUrl(s) {
  return /^https?:\/\/|^\/uploads\//i.test((s || '').trim());
}
function escapeHtml(s){
  return (s||'').replace(/[&<>"']/g, m => (
      {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]
  ));
}
function renderTextHtml(raw, className) {
  const t = (raw || '').trim();
  if (!t || looksLikeFileUrl(t)) return '';
  return `<p class="${className}">${escapeHtml(t)}</p>`;
}
function displayMessage(message, autoScroll = true) {
  const div = document.createElement('div');
  div.className = 'flex items-start space-x-3 message-bubble';
  const isMe = message.sender === username;

  const displayNameBase =
      message.nickname
      || message.fullName || message.full_name
      || message.senderFullName || message.sender_name
      || message.senderUsername || message.sender
      || 'Ẩn danh';

  // avatar
  const initials = getInitials(displayNameBase);
  const gradient = pickGradient(simpleHash(message.sender||''));
  const avatarUrl = extractAvatarUrl(message) || (message.senderUsername && avatarCache.get(message.senderUsername)) || null;

  const time = message.timestamp || new Date().toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'});
  const hasAtt = Array.isArray(message.attachments) && message.attachments.length > 0;
  const rawText = (message.content || '').trim();
  const meTextHtml = renderTextHtml(rawText, 'text-white');
  const otherTextHtml = renderTextHtml(rawText, 'text-gray-800 dark:text-gray-200');
  if (message.type === 'JOIN') {
    div.className = 'flex justify-center my-4';
    div.innerHTML = `<div class="glass-effect px-6 py-3 rounded-full text-sm text-green-600 dark:text-green-300">${displayNameBase} đã tham gia phòng chat 👋</div>`;
  } else if (message.type === 'LEAVE') {
    div.className = 'flex justify-center my-4';
    div.innerHTML = `<div class="glass-effect px-6 py-3 rounded-full text-sm text-red-600 dark:text-red-300">${displayNameBase} đã rời khỏi phòng chat 👋</div>`;
  } else if (isMe) {
    div.classList.add('justify-end');
    div.innerHTML = `
      <div class="bg-gradient-to-r from-purple-500 to-purple-700 rounded-2xl rounded-tr-md px-4 py-3 max-w-xs lg:max-w-md">
        ${meTextHtml}
    ${renderAttachmentsHtml(message.attachments)}
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-purple-100">${time}</span></div>
      </div>
      ${renderAvatar(avatarUrl, initials, gradient, 8)}
    `;
  } else {
    div.classList.add('items-start');
    div.innerHTML = `
      ${renderAvatar(avatarUrl, initials, gradient, 8)}
      <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-md px-4 py-3 max-w-xs lg:max-w-md">
        <div class="text-xs text-gray-500 dark:text-gray-400 mb-1 font-medium">${displayNameBase}</div>
        ${otherTextHtml}
        ${renderAttachmentsHtml(message.attachments)}
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-gray-500 dark:text-gray-400">${time}</span></div>
      </div>`;
  }
  chatMessages?.appendChild(div);
  if (autoScroll) scrollToBottom();
}

function normalizeSender(message) {
  const s = message.sender || {};
  return {
    id:
        s.id ?? s.userId ??
        message.senderId ?? message.userId ?? message.fromUserId ?? null,
    username:
        s.username ??
        message.senderUsername ?? message.username ?? message.fromUsername ?? null,
    fullName:
        s.fullName ?? s.full_name ??
        message.senderFullName ?? message.sender_name ??
        message.fullName ?? message.name ?? null,
    nickname: s.nickname ?? message.nickname ?? null,
    avatarUrl:
        s.avatarUrl ?? s.avatar_url ??
        message.avatarUrl ?? message.avatar_url ?? null
  };
}

/* ========= SCOPED NICKNAME LOOKUP (group/private) ========= */
// Trả về chuỗi phạm vi phòng hiện tại
function roomScope() {
  if (!currentChat) return 'global';
  const id = currentChat.id ?? 'public';
  return `${currentChat.type}:${id}`; // vd: "group:12", "private:34"
}

function resolveDisplayNameFromMap(senderLike) {
  if (!senderLike) return 'Ẩn danh';
  const idKey = senderLike.id ?? senderLike.userId ?? null;
  const usr   = senderLike.username ?? null;
  const full  = senderLike.fullName ?? senderLike.full_name ?? senderLike.name ?? null;
  const nickPayload = senderLike.nickname ?? null;

  const scope = roomScope();
  const scopedNick =
      (idKey != null ? memberNickMap.get(`${scope}|id:${String(idKey)}`) : null) ||
      (usr ? memberNickMap.get(`${scope}|u:${usr}`) : null);

  // Thứ tự ưu tiên: biệt danh (theo phòng) → nickPayload → full → username
  return scopedNick || nickPayload || full || usr || 'Ẩn danh';
}

function displayPrivateMessage(message, autoScroll = true) {
  const div = document.createElement('div');
  div.className = 'flex items-start space-x-3 message-bubble';

  const s = normalizeSender(message);
  const isMe = s.username === username;

  const displayName = resolveDisplayNameFromMap(s);
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(s.username || String(s.id || '')));
  const avatarUrl = s.avatarUrl || (s.username && avatarCache.get(s.username)) || null;

  const t = message.timestamp ? new Date(message.timestamp) : null;
  const time = (t && !isNaN(t)) ? t.toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'})
      : new Date().toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'});
  const rawText = (message.content || '').trim();
  const meTextHtml = renderTextHtml(rawText, 'text-white');
  const otherTextHtml = renderTextHtml(rawText, 'text-gray-800 dark:text-gray-200');
  if (isMe) {
    div.classList.add('justify-end');
    div.innerHTML = `
      <div class="bg-gradient-to-r from-purple-500 to-purple-700 rounded-2xl rounded-tr-md px-4 py-3 max-w-xs lg:max-w-md">
       ${meTextHtml}
        ${renderAttachmentsHtml(message.attachments)}
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-purple-100">${time}</span></div>
      </div>
      ${renderAvatar(avatarUrl, initials, gradient, 8)}
    `;
  } else {
    div.classList.add('items-start');
    div.innerHTML = `
      ${renderAvatar(avatarUrl, initials, gradient, 8)}
      <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-md px-4 py-3 max-w-xs lg:max-w-md">
        <div class="text-xs text-gray-500 dark:text-gray-400 mb-1 font-medium">${displayName}</div>
        ${otherTextHtml}
        ${renderAttachmentsHtml(message.attachments)}
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-gray-500 dark:text-gray-400">${time}</span></div>
      </div>
    `;
  }
  chatMessages?.appendChild(div);
  if (autoScroll) scrollToBottom();
}

function displayGroupMessage(message, autoScroll = true) {
  const div = document.createElement('div');
  div.className = 'flex items-start space-x-3 message-bubble';

  const s = normalizeSender(message);
  const isMe = s.username === username;

  const displayName = resolveDisplayNameFromMap(s);
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(s.username || String(s.id || '')));
  const avatarUrl = s.avatarUrl || (s.username && avatarCache.get(s.username)) || null;

  const t = message.timestamp ? new Date(message.timestamp) : null;
  const time = (t && !isNaN(t))
      ? t.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
      : new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

  if (isMe) {
    div.classList.add('justify-end');
    div.innerHTML = `
      <div class="bg-gradient-to-r from-purple-500 to-purple-700 rounded-2xl rounded-tr-md px-4 py-3 max-w-xs lg:max-w-md break-words">
        ${renderTextHtml(message.content, 'text-white')}
        ${renderAttachmentsHtml(message.attachments)}
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-purple-100">${time}</span></div>
      </div>
      ${renderAvatar(avatarUrl, initials, gradient, 8)}
    `;
  } else {
    div.classList.add('items-start');
    div.innerHTML = `
      ${renderAvatar(avatarUrl, initials, gradient, 8)}
      <div class="bg-gray-100 dark:bg-gray-700 rounded-2xl rounded-tl-md px-4 py-3 max-w-xs lg:max-w-md break-words">
        <div class="text-xs text-gray-500 dark:text-gray-400 mb-1 font-medium">${displayName}</div>
        ${renderTextHtml(message.content, 'text-gray-800 dark:text-gray-200')}
        ${renderAttachmentsHtml(message.attachments)}
        <div class="flex items-center justify-end mt-1"><span class="text-xs text-gray-500 dark:text-gray-400">${time}</span></div>
      </div>
    `;
  }
  chatMessages?.appendChild(div);
  if (autoScroll) scrollToBottom();
}

/* ========================================================
   FRIEND SYSTEM
======================================================== */
async function loadPendingFriendRequests() {
  try {
    const res = await fetch('/api/friends/pending-requests', { headers: { [csrfHeader]: csrfToken } });
    if (res.ok) { const list = await res.json(); updateFriendRequestsBadge(list.length); return list; }
  } catch (e) { console.error('Error loading pending friend requests:', e); }
  return [];
}
function updateFriendRequestsBadge(count) {
  const badge = $("#friendRequestBadge");
  if (!badge) return;
  if (count > 0) { badge.textContent = count; badge.classList.remove('hidden'); }
  else badge.classList.add('hidden');
}
async function showFriendRequestsModal() {
  const modal = $("#friendRequestsModal");
  const list = $("#friendRequestsList");
  const empty = $("#noFriendRequests");
  if (!modal || !list) return;
  modal.classList.remove('hidden');
  const requests = await loadPendingFriendRequests();
  list.innerHTML = '';
  if (requests.length === 0) list.appendChild(empty); else requests.forEach(r => list.appendChild(createFriendRequestItem(r)));
}
function createFriendRequestItem(request) {
  const displayName = request.fullName || request.username;
  const initials = getInitials(displayName);
  const gradient = pickGradient(simpleHash(request.username||''));
  const w = document.createElement('div');
  w.setAttribute('data-fr-item', '1');
  w.className = 'flex items-center space-x-3 p-3 bg-gray-50 dark:bg-gray-700 rounded-xl';
  w.innerHTML = `
    <div class="w-12 h-12 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0"><span class="text-white font-bold">${initials}</span></div>
    <div class="flex-1 min-w-0">
      <h4 class="font-medium text-gray-900 dark:text-white truncate">${displayName}</h4>
      <p class="text-sm text-gray-500 dark:text-gray-400">muốn kết bạn với bạn</p>
    </div>
    <div class="flex space-x-2">
      <button onclick="acceptFriendRequest(${request.id})" class="px-3 py-1 bg-green-500 hover:bg-green-600 text-white text-sm rounded-lg">Chấp nhận</button>
      <button onclick="rejectFriendRequest(${request.id})" class="px-3 py-1 bg-gray-500 hover:bg-gray-600 text-white text-sm rounded-lg">Từ chối</button>
    </div>`;
  return w;
}
function showFriendRequestNotification(n) {
  if (n.type === 'FRIEND_REQUEST_RECEIVED') {
    showNotificationToast(`${n.fromUser} muốn kết bạn với bạn`, 'info', () => showFriendRequestsModal());
    loadPendingFriendRequests(); scheduleSidebarRefresh();
  } else if (n.type === 'FRIEND_REQUEST_ACCEPTED') {
    showNotificationToast(`${n.fromUser} đã chấp nhận lời mời kết bạn`, 'success');
    scheduleSidebarRefresh();
  } else if (n.type === 'FRIEND_LIST_UPDATE') {
    scheduleSidebarRefresh(); loadPendingFriendRequests();
  }
}
async function acceptFriendRequest(userId) {
  try {
    const res = await fetch(`/api/friends/accept/${userId}`, {
      method:'POST', headers: { ...(csrfHeader&&csrfToken?{[csrfHeader]:csrfToken}:{}) }
    });
    if (!res.ok) throw new Error(await res.text());
    // Xoá DOM item tại chỗ
    document.querySelector(`[data-fr-item] button[onclick*="acceptFriendRequest(${userId})"]`)
        ?.closest('[data-fr-item]')?.remove();

    // Nếu không còn item nào -> hiện empty
    const frList  = document.getElementById('friendRequestsList');
    const frEmpty = document.getElementById('noFriendRequests');
    if (frList && frList.querySelectorAll('[data-fr-item]').length === 0) {
      frEmpty?.classList.remove('hidden');
      if (!frEmpty.parentElement) frList.appendChild(frEmpty);
    }

    showSuccessMessage('Đã chấp nhận lời mời kết bạn');
    scheduleSidebarRefresh();
    loadPendingFriendRequests(); // cập nhật badge
  } catch (e) {
    showErrorMessage('Có lỗi xảy ra');
  }
}

async function rejectFriendRequest(userId) {
  try {
    const res = await fetch(`/api/friends/reject/${userId}`, { method:'POST', headers:{ [csrfHeader]: csrfToken } });
    if (res.ok) { showSuccessMessage('Đã từ chối lời mời kết bạn'); showFriendRequestsModal(); }
    else showErrorMessage(`Lỗi: ${await res.text()}`);
  } catch (e) { console.error('Error rejecting friend request:', e); showErrorMessage('Có lỗi xảy ra'); }
}

/* ========================================================
   TOASTS / UTILITIES
======================================================== */
function showNotificationToast(message, type='info', onClick=null) {
  const toast = document.createElement('div');
  toast.className = `fixed top-4 right-4 max-w-sm bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg p-4 z-50 transform transition-all duration-300 translate-x-full`;
  const bg = type==='success'?'border-green-500': type==='error'?'border-red-500':'border-blue-500';
  toast.classList.add(bg);
  toast.innerHTML = `
    <div class="flex items-center justify-between">
      <div class="flex items-center space-x-3">
        <div class="text-2xl">${type==='success'?'✅': type==='error'?'❌':'📬'}</div>
        <div>
          <p class="text-sm font-medium text-gray-900 dark:text-white">${message}</p>
          ${onClick ? '<p class="text-xs text-gray-500 dark:text-gray-400 mt-1">Nhấn để xem chi tiết</p>' : ''}
        </div>
      </div>
      <button class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" onclick="this.parentElement.parentElement.remove()">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
      </button>
    </div>`;
  if (onClick) { toast.style.cursor='pointer'; toast.addEventListener('click', onClick); }
  document.body.appendChild(toast);
  setTimeout(()=>toast.classList.remove('translate-x-full'), 100);
  setTimeout(()=>{ toast.classList.add('translate-x-full'); setTimeout(()=>toast.remove(),300); }, 5000);
}
function scrollToBottom(){ if (chatMessages) chatMessages.scrollTop = chatMessages.scrollHeight; }
function showErrorMessage(m){ const d=document.createElement('div'); d.className='flex justify-center my-4'; d.innerHTML=`<div class="glass-effect px-6 py-3 rounded-full text-sm text-red-600 dark:text-red-300">${m}</div>`; chatMessages?.appendChild(d); scrollToBottom(); }
function showSuccessMessage(m){ const d=document.createElement('div'); d.className='flex justify-center my-4'; d.innerHTML=`<div class="glass-effect px-6 py-3 rounded-full text-sm text-green-600 dark:text-green-300">${m}</div>`; chatMessages?.appendChild(d); scrollToBottom(); }
function updateChatListWithNewMessage(){ scheduleSidebarRefresh();}
function handleEnterKey(e){ if (e.key==='Enter' && !e.shiftKey){ e.preventDefault(); sendMessage(e); } }

/* ========================================================
   OPTIONAL UX ADD-ONS
======================================================== */
// Chat Settings Drawer + overlay
const chatSettingsBtn   = $("#chatSettingsBtn");
const chatSettingsPanel = $("#chatSettingsPanel");
let   chatSettingsOverlay = $("#chatSettingsOverlay");
const closeChatSettings = $("#closeChatSettings");
const mainChatArea      = $("#mainChatArea");
function openChatSettings(){
  chatSettingsPanel?.classList.remove('translate-x-full');
  chatSettingsOverlay?.classList.remove('hidden');
  mainChatArea?.classList.add('mr-80');
  document.documentElement.classList.add('overflow-hidden'); // khoá scroll nền
}
function closeChatSettingsPanel(){
  chatSettingsPanel?.classList.add('translate-x-full');
  chatSettingsOverlay?.classList.add('hidden');
  mainChatArea?.classList.remove('mr-80');
  document.documentElement.classList.remove('overflow-hidden'); // mở lại scroll nền
}

if (!chatSettingsOverlay) {
  chatSettingsOverlay = document.createElement('div');
  chatSettingsOverlay.id = 'chatSettingsOverlay';
  chatSettingsOverlay.className = 'fixed inset-0 bg-black/40 hidden z-30';
  document.body.appendChild(chatSettingsOverlay);
  chatSettingsOverlay.addEventListener('click', closeChatSettingsPanel);
}
chatSettingsBtn   ?.addEventListener('click', openChatSettings);
closeChatSettings ?.addEventListener('click', closeChatSettingsPanel);
chatSettingsOverlay?.addEventListener('click', closeChatSettingsPanel);
window.addEventListener('keydown', (e)=>{ if (e.key==='Escape') closeChatSettingsPanel(); });

// Wallpaper
const wpPresetsEl  = $("#wpPresets");
const wpUrlInput   = $("#wpUrl");
const applyWpUrlBtn= $("#applyWpUrl");
const resetWpBtn   = $("#resetWp");
const WP_PRESETS = { none:{type:'preset',key:'none'}, 'grad-purple':{type:'preset',key:'grad-purple'}, 'grad-blue':{type:'preset',key:'grad-blue'}, 'grad-pink':{type:'preset',key:'grad-pink'}, dots:{type:'preset',key:'dots'}, grid:{type:'preset',key:'grid'} };
function applyWallpaperStyle(p){ if (!chatMessages) return; chatMessages.style.backgroundImage='none'; chatMessages.style.backgroundSize=''; chatMessages.style.backgroundPosition=''; chatMessages.style.backgroundAttachment=''; chatMessages.style.backgroundColor=''; if (!p) return; if (p.type==='url'){ chatMessages.style.backgroundImage=`url("${p.url}")`; chatMessages.style.backgroundSize='cover'; chatMessages.style.backgroundPosition='center'; chatMessages.style.backgroundAttachment='fixed'; return; } switch(p.key){ case 'grad-purple': chatMessages.style.backgroundImage='linear-gradient(135deg,#f5e1ff,#e7d4ff)'; break; case 'grad-blue': chatMessages.style.backgroundImage='linear-gradient(135deg,#dbeafe,#bfdbfe)'; break; case 'grad-pink': chatMessages.style.backgroundImage='linear-gradient(135deg,#ffe4e6,#fecdd3)'; break; case 'dots': chatMessages.style.backgroundImage='radial-gradient(#e5e7eb 1.2px, transparent 1.2px), radial-gradient(#e5e7eb 1.2px, transparent 1.2px)'; chatMessages.style.backgroundSize='20px 20px,20px 20px'; chatMessages.style.backgroundPosition='0 0,10px 10px'; chatMessages.style.backgroundColor='#ffffff'; break; case 'grid': chatMessages.style.backgroundImage='linear-gradient(rgba(0,0,0,.06) 1px, transparent 1px), linear-gradient(90deg, rgba(0,0,0,.06) 1px, transparent 1px)'; chatMessages.style.backgroundSize='24px 24px,24px 24px'; chatMessages.style.backgroundColor='#ffffff'; break; default: break; } }
function setActivePresetButton(key){ document.querySelectorAll('#wpPresets .wp-item').forEach(btn=>{ btn.setAttribute('aria-pressed', btn.getAttribute('data-wp')===key ? 'true':'false'); }); }
function saveWallpaper(d){ localStorage.setItem('chatWallpaper', JSON.stringify(d)); }
function loadWallpaper(){ try{return JSON.parse(localStorage.getItem('chatWallpaper')||'null');}catch{return null;} }
(function initWallpaper(){ const saved = loadWallpaper(); if (saved){ applyWallpaperStyle(saved); if (saved.type==='preset') setActivePresetButton(saved.key); else setActivePresetButton(''); } else { setActivePresetButton('none'); } })();
wpPresetsEl?.addEventListener('click', (e)=>{ const target = e.target.closest('.wp-item'); if (!target) return; const key = target.getAttribute('data-wp'); const data = WP_PRESETS[key] || WP_PRESETS.none; applyWallpaperStyle(data); setActivePresetButton(key); saveWallpaper(data); });
applyWpUrlBtn?.addEventListener('click', ()=>{ const url=(wpUrlInput?.value||'').trim(); if (!url) return; const data={type:'url',url}; applyWallpaperStyle(data); setActivePresetButton(''); saveWallpaper(data); });
resetWpBtn?.addEventListener('click', ()=>{ const data=WP_PRESETS.none; applyWallpaperStyle(data); setActivePresetButton('none'); saveWallpaper(data); if (wpUrlInput) wpUrlInput.value=''; });

// Big Emoji
const bigEmojiToggle = $("#bigEmojiToggle");
(function initBigEmoji(){ const v = localStorage.getItem('bigEmoji')==='1'; if (bigEmojiToggle) bigEmojiToggle.checked=v; document.body.classList.toggle('big-emoji', v); })();
bigEmojiToggle?.addEventListener('change', (e)=>{ const v=e.target.checked; localStorage.setItem('bigEmoji', v?'1':'0'); document.body.classList.toggle('big-emoji', v); });

/* ========================================================
   BLOCK / MUTE / CLEAR  (có đồng bộ server cho PRIVATE)
======================================================== */
const blockToggle = $("#blockToggle");

// key lưu local per-room
function blockKey() {
  if (!currentChat) return 'block:none';
  const id = currentChat.id ?? 0;
  return `block:${currentChat.type}:${id}`;
}

function applyBlockStateUI(v){
  if (!messageInput || !sendButton) return;
  messageInput.disabled = v;
  sendButton.disabled = v;
  messageInput.classList.toggle('opacity-60', v);
  sendButton.classList.toggle('opacity-60', v);
  messageInput.classList.toggle('cursor-not-allowed', v);
  sendButton.classList.toggle('cursor-not-allowed', v);
}

async function syncPrivateBlockFromServer() {
  if (!currentChat || currentChat.type !== 'private' || !blockToggle) return;
  try {
    // friendId đã được set trong switchToPrivateChat
    const res = await fetch(`/api/blocks/users/${currentChat.friendId}/status`, {
      headers: { [csrfHeader]: csrfToken }
    });
    if (res.ok) {
      const { blocked } = await res.json();
      blockToggle.checked = !!blocked;
      // cập nhật UI + LS theo server
      localStorage.setItem(blockKey(), blocked ? '1' : '0');
      applyBlockStateUI(blocked);
    }
  } catch (e) { console.error(e); }
}

function initBlockFromStorage() {
  const v = localStorage.getItem(blockKey()) === '1';
  if (blockToggle) blockToggle.checked = v;
  applyBlockStateUI(v);
}

// gọi khi đổi phòng:
window.initBlockToggleForCurrentChat = function () {
  if (!blockToggle) return;
  if (!currentChat) { applyBlockStateUI(false); return; }

  if (currentChat.type === 'group') {
    initBlockFromStorage();         // dùng local
    return;
  }
  if (currentChat.type === 'private') {
    syncPrivateBlockFromServer();   // đồng bộ server
  }
};

blockToggle?.addEventListener('change', async (e) => {
  const v = e.target.checked;
  // Cập nhật UI & local ngay
  localStorage.setItem(blockKey(), v ? '1' : '0');
  applyBlockStateUI(v);

  // Nếu là private => gọi server
  if (currentChat && currentChat.type === 'private') {
    try {
      const url = `/api/blocks/users/${currentChat.friendId}`;
      const res = await fetch(url, {
        method: v ? 'POST' : 'DELETE',
        headers: { [csrfHeader]: csrfToken }
      });
      if (!res.ok) throw new Error(await res.text());
      showSuccessMessage(v ? 'Đã chặn người này.' : 'Đã bỏ chặn.');
    } catch (err) {
      console.error(err);
      // rollback toggle + UI + LS nếu lỗi
      e.target.checked = !v;
      localStorage.setItem(blockKey(), e.target.checked ? '1' : '0');
      applyBlockStateUI(e.target.checked);
      showErrorMessage('Không cập nhật được trạng thái chặn.');
    }
  }
});

// MUTE (giữ nguyên)
const muteToggle = $("#muteToggle");
(function initMute(){ const muted=localStorage.getItem('chatMuted')==='1'; if (muteToggle) muteToggle.checked=muted; })();
muteToggle?.addEventListener('change',(e)=>{ const v=e.target.checked; localStorage.setItem('chatMuted', v?'1':'0'); });

// CLEAR (giữ nguyên)
const typingIndicator = $("#typingIndicator");
const clearChatBtn = $("#clearChatBtn");
clearChatBtn?.addEventListener('click', async () => {
  if (!currentChat) return;

  try {
    if (currentChat.type === 'private') {
      await fetch(`/api/private-chat/${currentChat.id}/clear`, {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken }
      });
      await loadPrivateChatHistory(currentChat.id);

    } else if (currentChat.type === 'group') {
      await fetch(`/api/groups/${currentChat.id}/clear`, {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken }
      });
      await loadGroupChatHistory(currentChat.id);

    } else {
      // public: chỉ xóa UI (tùy bạn)
      if (!chatMessages || !typingIndicator) return;
      Array.from(chatMessages.children).forEach(n => { if (n !== typingIndicator) n.remove(); });
    }

    showSuccessMessage('Đã xóa đoạn chat ở phía bạn.');
  } catch (e) {
    console.error(e);
    showErrorMessage('Không thể xóa đoạn chat.');
  } finally {
    closeChatSettingsPanel();
  }
});

/* ========================================================
   EMOJI PICKER
======================================================== */
const emojiToggleBtn = $("#emojiToggleBtn");
const emojiPicker    = $("#emojiPicker");
const emojiGrid      = $("#emojiGrid");
const EMOJI_CATEGORIES = {
  camxuc: "😀 😃 😄 😁 😆 😅 😂 🙂 🙃 😊 😇 😉 😍 🥰 😘 😗 😙 😚 🤗 🤩 🤔 🤨 😐 😑 😶 🙄 😏 😣 😥 😮 🤐 😯 😪 😫 🥱 😴 😌 😛 😝 😜 🤪 🤭 🤫 🤥 😬 🫠 😳 🥵 🥶 🥴 😵 🤯 🤠 🥳 😎 🤓 🧐 😕 😟 🙁 ☹️ 😮‍💨 😤 😢 😭 😖 😞 😓 😩 🤬 🤧 🤮 🤢 🤒 🤕 🥺 🙏".split(" "),
  cucchi: "👍 👎 👋 🤚 ✋ 🖐 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🙏 ✍️ 💅 🤳".split(" "),
  dongvat:"🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🐤 🐣 🐥 🐺 🦄 🐝 🐛 🦋 🐌 🐞 🪲 🐢 🐍 🐙 🐠 🐟 🐬 🐳 🐋 🐊 🦖".split(" "),
  doan:   "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🥑 🍆 🥔 🥕 🌽 🌶️ 🧄 🧅 🥬 🥦 🍄 🥜 🍞 🥐 🥖 🥯 🥞 🧇 🧀 🍗 🍖 🍤 🍣 🍕 🍔 🍟 🌭 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍥 🥮 🍡 🍦 🍰 🎂 🍩 🍪 🍫 🍬 🍭 🍯 🍼 ☕ 🍵 🧋 🥤 🍻 🍷 🥂 🍹".split(" "),
  hoatdong:"⚽ 🏀 🏈 ⚾ 🎾 🏐 🏉 🎱 🏓 🏸 🥅 🥊 🥋 ⛳ 🏒 🏑 🥍 🛹 🎿 ⛷️ 🏂 🏋️‍♀️ 🤼‍♂️ 🤺 🤾‍♂️ 🧗‍♀️ 🧘‍♂️ 🏄‍♀️ 🚴‍♂️ 🚵‍♀️ 🏇 🎯 🎮 🎲 🎻 🎸 🎺 🎷 🥁 🎤 🎧".split(" ")
};
function getRecentEmojis(){ try{return JSON.parse(localStorage.getItem('recentEmojis')||'[]');}catch{return [];} }
function saveRecentEmoji(e){ let arr=getRecentEmojis().filter(x=>x!==e); arr.unshift(e); if (arr.length>24) arr=arr.slice(0,24); localStorage.setItem('recentEmojis', JSON.stringify(arr)); }
function renderEmojiGrid(cat='recent'){ if (!emojiGrid) return; let list=[]; if (cat==='recent') list=getRecentEmojis(); if (!list || list.length===0){ cat = cat==='recent' ? 'camxuc' : cat; list = EMOJI_CATEGORIES[cat] || []; } emojiGrid.innerHTML=''; list.forEach(e=>{ const btn=document.createElement('button'); btn.type='button'; btn.className='emoji-btn'; btn.textContent=e; btn.addEventListener('click', ()=>{ insertAtCursor(messageInput, e); saveRecentEmoji(e); messageInput?.focus(); }); emojiGrid.appendChild(btn); }); }
function setActiveTab(cat){ document.querySelectorAll('#emojiPicker .emoji-tab').forEach(t=>{ t.setAttribute('aria-selected', t.getAttribute('data-cat')===cat ? 'true':'false'); }); renderEmojiGrid(cat); }
function insertAtCursor(input, text){ if (!input) return; const start=input.selectionStart ?? input.value.length; const end=input.selectionEnd ?? input.value.length; const before=input.value.slice(0,start); const after=input.value.slice(end); input.value = before + text + after; const newPos = start + text.length; input.setSelectionRange(newPos, newPos); }
function openEmojiPicker(){ if (!emojiPicker || !emojiToggleBtn) return; emojiPicker.classList.remove('hidden'); emojiToggleBtn.setAttribute('aria-expanded','true'); setActiveTab('recent'); }
function closeEmojiPicker(){ if (!emojiPicker || !emojiToggleBtn) return; emojiPicker.classList.add('hidden'); emojiToggleBtn.setAttribute('aria-expanded','false'); }
emojiToggleBtn?.addEventListener('click', (e)=>{ e.stopPropagation(); if (emojiPicker?.classList.contains('hidden')) openEmojiPicker(); else closeEmojiPicker(); });
emojiPicker?.addEventListener('click',(e)=>{ const tab=e.target.closest('.emoji-tab'); if (tab) setActiveTab(tab.getAttribute('data-cat')); });
document.addEventListener('click', (e)=>{ if (!emojiPicker || !emojiToggleBtn) return; if (!emojiPicker.classList.contains('hidden')){ if (!emojiPicker.contains(e.target) && e.target!==emojiToggleBtn) closeEmojiPicker(); } });
window.addEventListener('keydown',(e)=>{ if (e.key==='Escape') closeEmojiPicker(); });
renderEmojiGrid('recent');

// --- Profile / Avatar localStorage ---

const profileNameEl    = $("#profileName");
const profileStatusEl  = $("#profileStatus");
const editProfileBtn   = $("#editProfileBtn");
const editNameInput    = $("#editName");
const editStatusSelect = $("#editStatus");
const editPersistChk   = $("#editPersist");
const appStatusText    = $("#appStatusText");
const appStatusDot     = $("#appStatusDot");
const profileDot       = $("#profileDot");
const STATUS_MAP = { active:{label:'Đang hoạt động', colorClass:'bg-green-500'}, busy:{label:'Đang bận', colorClass:'bg-yellow-500'}, offline:{label:'Tắt trạng thái hoạt động', colorClass:'bg-red-500'} };
const STATUS_KEYS = Object.keys(STATUS_MAP);
const ALL_DOT_CLASSES = ['bg-green-500','bg-yellow-500','bg-red-500'];
function labelToKey(label=''){ label=(label||'').toLowerCase().trim(); if (label.includes('bận')) return 'busy'; if (label.includes('tắt')) return 'offline'; return 'active'; }
function updateDotsColor(dotEl, key){ if (!dotEl) return; dotEl.classList.remove(...ALL_DOT_CLASSES); dotEl.classList.add(STATUS_MAP[key].colorClass); }
function updateStatusUI(key){ const {label}=STATUS_MAP[key]||STATUS_MAP.active; if (profileStatusEl) profileStatusEl.textContent=label; if (appStatusText) appStatusText.textContent=label; updateDotsColor(profileDot,key); updateDotsColor(appStatusDot,key); }
(function initProfileFromStorage(){ const nameLS=localStorage.getItem('profileName'); const statusKeyLS=localStorage.getItem('profileStatusKey'); const statusTextLS=localStorage.getItem('profileStatus'); if (nameLS && profileNameEl) profileNameEl.textContent=nameLS; let initKey='active'; if (statusKeyLS && STATUS_MAP[statusKeyLS]) initKey=statusKeyLS; else if (statusTextLS) initKey=labelToKey(statusTextLS); updateStatusUI(initKey); if (editStatusSelect) editStatusSelect.value=initKey; })();
const profileAvatarImg      = $("#profileAvatarImg");
const profileAvatarFallback = $("#profileAvatarFallback");
const editAvatarPreview     = $("#editAvatarPreview");
const editAvatarFallback    = $("#editAvatarFallback");
const editAvatarBtn         = $("#editAvatarBtn");
const editAvatarFile        = $("#editAvatarFile");
const removeAvatarBtn       = $("#removeAvatarBtn");

editAvatarBtn?.addEventListener('click', () => {
  // reset để chọn lại cùng 1 file vẫn nhận change
  if (editAvatarFile) editAvatarFile.value = '';
  editAvatarFile?.click();
});

removeAvatarBtn?.addEventListener('click', async () => {
  try {
    const res = await fetch('/api/users/avatar', {
      method: 'DELETE',
      headers: { ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) }
    });
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();

    const url = data?.url || DEFAULT_AVATAR_URL;
    localStorage.setItem('profileAvatarServerUrl', url);
    applyAvatar(url);

    showSuccessMessage('Đã xóa ảnh đại diện.');
  } catch (e) {
    console.error(e);
    showErrorMessage('Không xóa được ảnh.');
  }
});

// 👉 Lắng nghe chọn file, đọc thành dataURL và preview + lưu
editAvatarFile?.addEventListener('change', async (e) => {
  const f = e.target.files?.[0];
  if (!f) return;

  const MAX_BYTES = 2 * 1024 * 1024; // 2MB
  if (f.size > MAX_BYTES) {
    showErrorMessage('Ảnh quá lớn (> 2MB). Vui lòng chọn ảnh nhỏ hơn.');
    e.target.value = '';
    return;
  }
  if (!/^image\/(png|jpe?g|gif|webp|bmp|svg\+xml)$/i.test(f.type)) {
    showErrorMessage('Định dạng ảnh không hợp lệ.');
    e.target.value = '';
    return;
  }

  try {
    // Upload multipart/form-data
    const fd = new FormData();
    fd.append('file', f);

    const res = await fetch('/api/users/avatar', {
      method: 'POST',
      headers: { ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) },
      body: fd
    });

    if (!res.ok) {
      const msg = await res.text();
      showErrorMessage(msg || 'Upload ảnh thất bại.');
      return;
    }

    const { url } = await res.json();
    if (!url) {
      showErrorMessage('Server không trả về URL ảnh.');
      return;
    }

    // Lưu URL server để dùng khi “Lưu hồ sơ”
    localStorage.setItem('profileAvatarServerUrl', url);

    // Preview luôn bằng URL server
    applyAvatar(url);
    showSuccessMessage('Đã tải ảnh lên.');
  } catch (err) {
    console.error(err);
    showErrorMessage('Không upload được ảnh.');
  }
});

function applyAvatar(url) {
  const finalUrl = (url && url.trim() !== '') ? url : DEFAULT_AVATAR_URL;

  if (profileAvatarImg && profileAvatarFallback) {
    profileAvatarImg.src = finalUrl;
    profileAvatarImg.style.display = 'block';
    profileAvatarFallback.style.display = 'none';
  }

  if (editAvatarPreview && editAvatarFallback) {
    editAvatarPreview.src = finalUrl;
    editAvatarPreview.style.display = 'block';
    editAvatarFallback.style.display = 'none';
  }
}

// (A) Khởi tạo từ localStorage
(function initAvatarFromStorage(){
  const saved =
      localStorage.getItem('profileAvatarServerUrl')  // ưu tiên URL từ server
      || localStorage.getItem('profileAvatar')        // (cũ) nếu còn giữ dataURL
      || '';

  applyAvatar(saved || null);
})();

// (B) Khi mở modal chỉnh sửa
editProfileBtn?.addEventListener('click', ()=> {
  const currentName = localStorage.getItem('profileName') || (profileNameEl?.textContent?.trim() || 'Bạn');
  const currentKey  = localStorage.getItem('profileStatusKey') || labelToKey(profileStatusEl?.textContent?.trim() || '');
  if (editNameInput)    editNameInput.value = currentName;
  if (editStatusSelect) editStatusSelect.value = STATUS_KEYS.includes(currentKey) ? currentKey : 'active';

  // Avatar preview
  const storedAvatar = localStorage.getItem('profileAvatarServerUrl') || localStorage.getItem('profileAvatar');
  applyAvatar(storedAvatar || null);

  // 👇 Prefill email/phone từ profile đang có
  const p = currentProfile || {};
  if (editMailInput)  editMailInput.value  = p.email ?? '';
  if (editPhoneInput) editPhoneInput.value = p.phone ?? '';

  document.getElementById('editProfileModal')?.classList.remove('hidden');
});



document.getElementById('saveProfileChanges')?.addEventListener('click', async () => {
  const newName  = (editNameInput?.value || 'Bạn').trim();
  const key      = editStatusSelect?.value || 'active';
  const safeKey  = STATUS_KEYS.includes(key) ? key : 'active';
  const newEmail = (editMailInput?.value  || '').trim();
  const newPhone = (editPhoneInput?.value || '').trim();

  const payload = {
    fullName: newName,
    status:   safeKey,
    email:    newEmail || null,   // rỗng => xóa ở server
    phone:    newPhone || null    // rỗng => xóa ở server
  };

  const avatarUrlFromLS = localStorage.getItem('profileAvatarServerUrl');
  if (avatarUrlFromLS && avatarUrlFromLS.trim() !== '') {
    payload.avatarUrl = avatarUrlFromLS;
  }

  try {
    const response = await fetch('/api/users/profile', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
      body: JSON.stringify(payload)
    });

    const data = await response.json().catch(() => ({}));
    if (!response.ok || data?.success === false) {
      const msg = data?.message || data?.error || 'Lỗi không xác định';
      showErrorMessage('Không thể cập nhật hồ sơ: ' + msg);
      return;
    }

    // Đồng bộ lại UI từ server (tránh sai khác sau khi chuẩn hóa email/phone)
    const u = data?.user || {};
    if (profileNameEl) profileNameEl.textContent = u.fullName || newName;
    updateStatusUI(safeKey);
    if (typeof u.avatarUrl === 'string' && u.avatarUrl.trim() !== '') {
      localStorage.setItem('profileAvatarServerUrl', u.avatarUrl);
      applyAvatar(u.avatarUrl);
    }
    // cập nhật lại các input trong modal (đã lưu/chuẩn hoá)
    if (editMailInput)  editMailInput.value  = u.email ?? '';
    if (editPhoneInput) editPhoneInput.value = u.phone ?? '';

    if (editPersistChk?.checked) {
      localStorage.setItem('profileName', profileNameEl?.textContent || newName);
      localStorage.setItem('profileStatusKey', safeKey);
      localStorage.setItem('profileStatus', STATUS_MAP[safeKey].label);
    } else {
      localStorage.removeItem('profileName');
      localStorage.removeItem('profileStatusKey');
      localStorage.removeItem('profileStatus');
    }

    showSuccessMessage('Cập nhật hồ sơ thành công!');
  } catch (error) {
    console.error('Error updating profile:', error);
    showErrorMessage('Lỗi kết nối: Không thể cập nhật hồ sơ');
  }

  document.getElementById('editProfileModal')?.classList.add('hidden');
});

// === Load profile từ Database (GET /api/users/profile)
// === Load profile từ Database (GET /api/users/profile)
async function loadProfileFromDatabase() {
  try {
    const response = await fetch('/api/users/profile', {
      method: 'GET',
      headers: { ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) }
    });
    if (!response.ok) return;

    const profile = await response.json();
    currentProfile = profile; // 👈 lưu lại

    // Tên hiển thị
    if (profileNameEl && profile.fullName) {
      profileNameEl.textContent = profile.fullName;
      if (!localStorage.getItem('profileName')) {
        localStorage.setItem('profileName', profile.fullName);
      }
    }

    // Trạng thái
    if (profile.status) {
      updateStatusUI(profile.status);
      if (!localStorage.getItem('profileStatusKey')) {
        localStorage.setItem('profileStatusKey', profile.status);
        localStorage.setItem('profileStatus', STATUS_MAP[profile.status]?.label || 'Đang hoạt động');
      }
    }

    // Avatar
    const finalAvatarUrl =
        (profile.avatarUrl && profile.avatarUrl.trim() !== '')
            ? profile.avatarUrl
            : DEFAULT_AVATAR_URL;

    localStorage.setItem('profileAvatarServerUrl', finalAvatarUrl);
    applyAvatar(finalAvatarUrl);

    if (profile.username) {
      avatarCache.set(profile.username, finalAvatarUrl);
    }

    // 👇 NẠP SẴN email/phone vào các ô trong modal
    if (editMailInput)  editMailInput.value  = profile.email  ?? '';
    if (editPhoneInput) editPhoneInput.value = profile.phone  ?? '';

  } catch (error) {
    console.error('Error loading profile from database:', error);
  }
}



/* =========================== AVATAR HELPERS =========================== */


function extractAvatarUrl(obj) {
  // thử lấy từ nhiều field khác nhau
  return obj?.avatarUrl || obj?.avatar_url ||
      obj?.sender?.avatarUrl || obj?.sender?.avatar_url || null;
}

/** Trả về HTML ảnh + fallback (ẩn) — onerror sẽ hiện fallback */
function renderAvatar(avatarUrl, initials, gradient, sizeTailwind = 10) {
  const sizeClass = `w-${sizeTailwind} h-${sizeTailwind}`;
  const img = avatarUrl ? `
    <img src="${avatarUrl}"
         alt="${initials}"
         class="${sizeClass} rounded-full object-cover flex-shrink-0"
         onerror="this.style.display='none'; this.nextElementSibling?.classList.remove('hidden');">`
      : '';

  const fallback = `
    <div class="${sizeClass} bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0 ${avatarUrl ? 'hidden' : ''}">
      <span class="text-white text-sm font-bold">${initials}</span>
    </div>`;

  return img + fallback;
}

/* ========================================================
   DOM READY
======================================================== */
document.addEventListener('DOMContentLoaded', function(){
  const usernameInput = $('#username');
  attachBtn?.addEventListener('click', () => attachInput?.click());
  if (usernameInput && usernameInput.value) {
    username = usernameInput.value.trim();
    console.log('🔄 Auto-connecting WebSocket for user:', username);
    connect();
    loadProfileFromDatabase();
  }
  attachInput?.addEventListener('change', async (e) => {
    if (!e.target.files?.length) return;
    const files = Array.from(e.target.files);
    for (const f of files) {
      const uploaded = await uploadOneFile(f);
      if (uploaded) {
        pendingAttachments.push(uploaded);
        addAttachmentPreview(uploaded);
      }
    }
    attachInput.value = '';
  });
  $('#connectForm')?.addEventListener('submit', connect);
  sendButton?.addEventListener('click', sendMessage);
  messageInput?.addEventListener('keypress', handleEnterKey);

  // Bạn đã có showAddFriendDialog/showCreateGroupDialog ở nơi khác
  $('#addFriendBtn')?.addEventListener('click', showAddFriendDialog);

  const basicSearchInput = (!document.getElementById("chatSearchInput"))
      ? document.querySelector('input[placeholder="Tìm kiếm cuộc trò chuyện..."]')
      : null;

  if (basicSearchInput){
    basicSearchInput.addEventListener('input', function(e){
      const s = e.target.value.toLowerCase().trim();
      const chatList = document.getElementById('chatList'); if (!chatList) return;
      chatList.querySelectorAll('[onclick]').forEach(item=>{
        const nameEl = item.querySelector('h4, h3');
        if (nameEl){
          const name = nameEl.textContent.toLowerCase();
          item.style.display = (s===''||name.includes(s)) ? '' : 'none';
        }
      });
    });
  }

  setTimeout(()=>{ if (username) loadPendingFriendRequests(); }, 1000);

  updateThemeIcons();
});

console.log('🚀 WebChat Pro initialized');


// ===== Add Friend Modal wiring =====
function showAddFriendDialog(){
  document.getElementById('addFriendModal')?.classList.remove('hidden');
  const inp = document.getElementById('friendInput');
  inp && (inp.value = '', inp.focus());
}

function closeAddFriendDialog(){
  document.getElementById('addFriendModal')?.classList.add('hidden');
}

async function sendFriendRequestByUsername(usernameOrRaw){
  const username = (usernameOrRaw || '').trim();
  if (!username) { showErrorMessage('Vui lòng nhập tên đăng nhập.'); return; }

  try {
    const res = await fetch('/api/friends/send-request', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
      },
      body: JSON.stringify({ username })
    });

    if (!res.ok) {
      const msg = await res.text();
      showErrorMessage(msg || 'Không gửi được lời mời.');
      return;
    }

    showSuccessMessage('Đã gửi lời mời kết bạn.');
    closeAddFriendDialog();
    // refresh badges & list
    loadPendingFriendRequests();
    scheduleSidebarRefresh();
  } catch (e) {
    console.error(e);
    showErrorMessage('Có lỗi khi gửi lời mời.');
  }
}

// hook buttons
document.getElementById('confirmAddFriend')?.addEventListener('click', () => {
  const v = document.getElementById('friendInput')?.value || '';
  sendFriendRequestByUsername(v);
});
document.getElementById('friendInput')?.addEventListener('keydown', (e)=>{
  if (e.key === 'Enter') {
    e.preventDefault();
    const v = e.currentTarget.value || '';
    sendFriendRequestByUsername(v);
  }
});


/* ========================================================
   TÌM KIẾM THEO CHỮ CÁI (sidebar)
======================================================== */
document.addEventListener("DOMContentLoaded", () => {
  const searchInput = document.getElementById("chatSearchInput");
  const chatList = document.getElementById("chatList");
  if (!searchInput || !chatList) return;

  const normalize = (s) => (s||"").toString()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase();

  searchInput.addEventListener("input", () => {
    const keyword = normalize(searchInput.value);
    const items = Array.from(chatList.querySelectorAll(".chat-item"));
    const startsWith = [];
    const contains = [];

    items.forEach((item) => {
      const nameEl = item.querySelector("h3, .chat-name, h4");
      const name = nameEl ? normalize(nameEl.textContent) : "";
      if (!keyword) {
        item.classList.remove("hidden");
        contains.push(item);
        return;
      }
      if (name.startsWith(keyword)) {
        item.classList.remove("hidden");
        startsWith.push(item);
      } else if (name.includes(keyword)) {
        item.classList.remove("hidden");
        contains.push(item);
      } else {
        item.classList.add("hidden");
      }
    });

    // [...startsWith, ...contains].forEach((el) => chatList.appendChild(el));
  });
});

/* ========================================================
   MEMBER NICKNAMES (Group/Private) — SINGLE SOURCE OF TRUTH
======================================================== */
const memberNicknamesSection = document.getElementById('memberNicknamesSection');
const memberNicknameList     = document.getElementById('memberNicknameList');
const memberSearchInput      = document.getElementById('memberSearchInput');
const saveMemberNicknamesBtn = document.getElementById('saveMemberNicknamesBtn');

// XÓA các entry của scope hiện tại (giữ scope khác)
function clearScopeFromMemberNickMap(scope) {
  for (const k of Array.from(memberNickMap.keys())) {
    if (k.startsWith(scope + '|')) memberNickMap.delete(k);
  }
}

// Rebuild map CHO PHẠM VI PHÒNG HIỆN TẠI (có prefix scope)
function rebuildMemberNickMap(list){
  memberNickMap = new Map();
  const scope = roomScope(); // "group:<id>" hoặc "private:<id>"
  (list || []).forEach(m => {
    const nick = (m.nickname || '').trim();
    if (!nick) return;
    const idKey  = m.userId ?? m.id;
    const usrKey = m.username;
    if (idKey != null) memberNickMap.set(`${scope}|id:${String(idKey)}`, nick);
    if (usrKey)        memberNickMap.set(`${scope}|u:${usrKey}`,       nick);
  });
}




function renderMemberNicknameRow(member) {
  const initials = getInitials(member.fullName || member.username || '');
  const gradient = pickGradient(simpleHash(member.username || member.userId || ''));
  const id = member.userId ?? member.id;

  const row = document.createElement('div');
  row.className = "flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 chat-item";
  row.innerHTML = `
    <div class="w-9 h-9 bg-gradient-to-r ${gradient} rounded-full flex items-center justify-center flex-shrink-0">
      <span class="text-white text-xs font-bold">${initials}</span>
    </div>
    <div class="min-w-0 flex-1">
      <div class="flex items-center gap-2">
        <div class="chat-name font-medium text-sm text-gray-900 dark:text-white truncate">${member.fullName || member.username || ''}</div>
        ${member.username ? `<div class="text-xs px-2 py-0.5 rounded bg-gray-100 dark:bg-gray-600 text-gray-600 dark:text-gray-200">@${member.username}</div>` : ''}
      </div>
        <input
          data-user-id="${id ?? ''}"
          data-username="${member.username || ''}"
          type="text"
          placeholder="Biệt danh cho người này…"
          value="${member.nickname ? member.nickname.replace(/"/g,'&quot;') : ''}"
             class="mt-1 w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-800 dark:text-gray-200 text-sm">
    </div>
  `;
  return row;
}

// Load members for group
async function loadGroupMemberNicknames(groupId) {
  if (!memberNicknamesSection || !memberNicknameList) return;
  try {
    const res = await fetch(`/api/groups/${groupId}/members-with-nickname`, { headers: { [csrfHeader]: csrfToken }});
    if (!res.ok) throw new Error(await res.text());
    const members = await res.json();

    rebuildMemberNickMap(members);

    memberNicknameList.innerHTML = '';
    members.forEach(m => memberNicknameList.appendChild(renderMemberNicknameRow(m)));
    memberNicknamesSection.classList.remove('hidden');
  } catch (e) {
    console.error(e);
    memberNicknamesSection.classList.add('hidden');
    showErrorMessage('Không tải được danh sách thành viên.');
  }
}
function applyLocalNicknameEditsToMap() {
  if (!memberNicknameList) return;
  const scope = roomScope();
  memberNicknameList.querySelectorAll('input[data-user-id]').forEach(i => {
    const id  = i.getAttribute('data-user-id');
    const usr = i.getAttribute('data-username');
    const nick = (i.value || '').trim();
    if (!nick) return;
    if (id)  memberNickMap.set(`${scope}|id:${String(id)}`, nick);
    if (usr) memberNickMap.set(`${scope}|u:${usr}`,         nick);
  });
}



// Load partner for private
async function loadPrivatePartnerNickname(privateChatId) {
  const privRow = document.getElementById('privateNicknameRow');
  const privInput = document.getElementById('privateNicknameInput');

  if (!privRow || !privInput) return;

  try {
    const res = await fetch(`/api/private-chat/${privateChatId}/partner-with-nickname`, {
      headers: { [csrfHeader]: csrfToken }
    });
    if (!res.ok) throw new Error(await res.text());
    const partner = await res.json();

    // lưu vào map
    rebuildMemberNickMap([partner]);

    // hiển thị biệt danh trong input
    privInput.value = partner.nickname || '';
    privRow.classList.remove('hidden');

    // cập nhật header ngay khi load
    updatePrivateHeaderTitleWithNickname();

  } catch (e) {
    console.error(e);
    privRow?.classList.remove('hidden'); // vẫn show input trống
  }
}


// tách save groupchat ra riêng
async function saveMemberNicknames(){
  if (!currentChat || currentChat.type !== 'group') return; // ✅ chặn private
  const inputs = memberNicknameList?.querySelectorAll('input[data-user-id]') ?? [];
  const payload = Array.from(inputs).map(i => ({
    userId: Number(i.getAttribute('data-user-id')),
    nickname: i.value.trim()
  }));

  try {
    const res = await fetch(`/api/groups/${currentChat.id}/nicknames`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error(await res.text());
    applyLocalNicknameEditsToMap();
    showSuccessMessage('Đã lưu biệt danh thành viên.');
    await loadGroupMemberNicknames(currentChat.id);
    await loadGroupChatHistory(currentChat.id);
  } catch (e) {
    console.error(e);
    showErrorMessage('Lưu biệt danh thất bại.');
  }
}


//Tách save privatechat ra riêng
document.getElementById('savePrivateNicknameBtn')
    ?.addEventListener('click', savePrivateNickname);

// Tách save privatechat ra riêng
document.getElementById('savePrivateNicknameBtn')
    ?.addEventListener('click', savePrivateNickname);

// Hàm lưu biệt danh
async function savePrivateNickname() {
  if (!currentChat || currentChat.type !== 'private') {
    console.warn('Không thể lưu biệt danh: Không ở trong private chat.');
    return;
  }

  const input = document.getElementById('privateNicknameInput');
  const nickname = (input?.value || '').trim();

  if (!nickname) {
    showErrorMessage('Vui lòng nhập biệt danh.');
    return;
  }

  try {
    const res = await fetch(`/api/private-chats/${currentChat.id}/nickname`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
      },
      body: JSON.stringify({ nickname })
    });

    if (!res.ok) {
      const msg = await res.text().catch(() => 'Lỗi server');
      showErrorMessage(msg || 'Không lưu được biệt danh.');
      return;
    }

    const data = await res.json();
    const nick = (data.nickname || '').trim();

    const scope = `private:${currentChat.id}`;
    console.log(`Saving nickname for chat ${currentChat.id}:`, { nick, friendId: currentChat.friendId, friendUsername: currentChat.friendUsername });

    if (currentChat.friendId != null) {
      memberNickMap.set(`${scope}|id:${String(currentChat.friendId)}`, nick);
    }
    if (currentChat.friendUsername) {
      memberNickMap.set(`${scope}|u:${currentChat.friendUsername}`, nick);
    }

    input.value = nick;
    updatePrivateHeaderTitleWithNickname();
    showSuccessMessage('Đã lưu biệt danh!');
  } catch (err) {
    console.error('Lỗi khi lưu biệt danh private:', err);
    showErrorMessage('Không thể kết nối server.');
  }
}






// Search filter in member list
memberSearchInput?.addEventListener('input', () => {
  const kw = (memberSearchInput.value || '').toLowerCase().trim();
  memberNicknameList?.querySelectorAll('.chat-item').forEach(item => {
    const nameEl = item.querySelector('.chat-name');
    const name = (nameEl?.textContent || '').toLowerCase();
    item.style.display = (!kw || name.includes(kw)) ? '' : 'none';
  });
});

// Save button
saveMemberNicknamesBtn?.addEventListener('click', saveMemberNicknames);


// === Friend Requests Modal wiring (thêm vào cuối file JS của bạn) ===
const frOpenBtn  = document.getElementById('friendRequestsBtn');
const frModal    = document.getElementById('friendRequestsModal');
const frCloseBtn = document.getElementById('closeFriendRequestsModal');
const frList     = document.getElementById('friendRequestsList');
const frEmpty    = document.getElementById('noFriendRequests');

frOpenBtn?.addEventListener('click', showFriendRequestsModal);
frCloseBtn?.addEventListener('click', () => frModal?.classList.add('hidden'));
frModal?.addEventListener('click', (e) => { if (e.target === frModal) frModal.classList.add('hidden'); });

// Ghi đè nhẹ để đảm bảo hiển thị danh sách đúng
let frLoading = false;

async function showFriendRequestsModal() {
  const frModal = document.getElementById('friendRequestsModal');
  const frList  = document.getElementById('friendRequestsList');
  const frEmpty = document.getElementById('noFriendRequests');
  if (!frModal || !frList) return;
  if (frLoading) return;              // chống nháy nhiều lần khi bấm nhanh

  frLoading = true;
  frModal.classList.remove('hidden');

  const requests = await loadPendingFriendRequests();

  // Xoá tất cả item cũ nhưng giữ node "empty"
  frList.querySelectorAll('[data-fr-item]').forEach(el => el.remove());

  if (!requests || requests.length === 0) {
    frEmpty?.classList.remove('hidden');
    if (!frEmpty.parentElement) frList.appendChild(frEmpty); // nếu empty đã bị tách khỏi DOM
  } else {
    frEmpty?.classList.add('hidden');
    requests.forEach(r => frList.appendChild(createFriendRequestItem(r)));
  }

  frLoading = false;
}


// helper: chống spam gọi
function debounce(fn, ms = 250) {
  let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); };
}

let refreshVersion = 0;             // tránh ghi đè kết quả cũ

async function refreshSidebar() {
  const myVer = ++refreshVersion;
  try {
    const [friendsRes, groupsRes] = await Promise.all([
      fetch('/api/friends/list',    { headers: csrfHeader?{[csrfHeader]:csrfToken}:{} }),
      fetch('/api/groups/my-groups',{ headers: csrfHeader?{[csrfHeader]:csrfToken}:{} })
    ]);
    if (myVer !== refreshVersion) return;          // đã có lần refresh mới hơn

    const friends = friendsRes.ok ? await friendsRes.json() : [];
    const groups  = groupsRes.ok  ? await groupsRes.json()  : [];
    renderSidebar(friends, groups);
  } catch (err) { console.error('refreshSidebar', err); }
}

const scheduleSidebarRefresh = debounce(refreshSidebar, 300);
function renderSidebar(friends = [], groups = []) {
  const chatList = document.getElementById('chatList');
  if (!chatList) return;

  // Giữ lại keyword đang search
  const kw = (document.getElementById('chatSearchInput')?.value || '').trim().toLowerCase();

  chatList.innerHTML = '';

  // Friends
  const fSec = createSection('👥 Bạn bè', friends.length);
  chatList.appendChild(fSec);
  friends.forEach(f => chatList.appendChild(createFriendItem(f)));

  // Groups
  const gSec = createSection('🏠 Nhóm chat', groups.length);
  chatList.appendChild(gSec);
  groups.forEach(g => chatList.appendChild(createGroupItem(g)));

  // Re-apply filter (ẩn/hiện, **không reposition**)
  if (kw) chatList.querySelectorAll('.chat-item').forEach(it => {
    const name = it.querySelector('h3, .chat-name, h4')?.textContent.toLowerCase() || '';
    it.style.display = name.includes(kw) ? '' : 'none';
  });
  enrichAvatarsFromAPI(friends);
}

async function enrichAvatarsFromAPI(friends = []) {
  // Những bạn thiếu avatarUrl
  const need = friends.filter(f => f.username && !extractAvatarUrl(f) && !avatarCache.get(f.username));
  if (need.length === 0) return;

  const updated = []; // usernames đã cập nhật
  await Promise.allSettled(
      need.map(async (f) => {
        try {
          const res = await fetch(`/api/users/by-username/${encodeURIComponent(f.username)}`, {
            headers: csrfHeader ? { [csrfHeader]: csrfToken } : {}
          });
          if (!res.ok) return;
          const u = await res.json();

          const url = u?.avatarUrl || u?.avatar_url || '';
          if (u?.username && url) {
            avatarCache.set(u.username, url);
            updated.push(u.username);

            // Patch ngay ở sidebar (nếu node đang hiển thị)
            patchSidebarAvatar(u.username, url, u.fullName || u.username);
          }
        } catch { /* noop */ }
      })
  );

  // 👉 Chỉ refresh 1 lần nếu có cập nhật (phòng trường hợp DOM chưa có holder để patch)
  if (updated.length) {
    scheduleSidebarRefresh();
  }
}

/* ========================================================
   HEADER PRIVATE: HIỂN THỊ THEO BIỆT DANH (tách khỏi nhóm)
======================================================== */
function updatePrivateHeaderTitleWithNickname() {
  if (!currentChat || currentChat.type !== 'private') return;

  const scope = `private:${currentChat.id}`;
  const byId = currentChat.friendId != null ? memberNickMap.get(`${scope}|id:${String(currentChat.friendId)}`) : null;
  const byUsr = currentChat.friendUsername ? memberNickMap.get(`${scope}|u:${currentChat.friendUsername}`) : null;

  const nick = (byId || byUsr || '').trim();
  const base = currentChat.name || currentChat.friendUsername || 'Người dùng';
  const nameForHeader = nick || base;

  const chatTitle = document.getElementById('chatTitle');
  if (chatTitle) chatTitle.textContent = `Chat với ${nameForHeader}`;
}




/* ========================================================
   GỬI THÔNG ĐIỆP XONG → DỌN COMPOSER/FILE ĐÍNH KÈM
======================================================== */
function clearPendingAttachmentsUI() {
  pendingAttachments = [];
  if (attachmentPreviewBar) attachmentPreviewBar.innerHTML = '';
}
function clearComposer() {
  if (messageInput) messageInput.value = '';
  clearPendingAttachmentsUI();
}

/* ========================================================
   CẬP NHẬT BONG BÓNG NHÓM KHI FILE ĐẾN TRỄ (fallback thêm mới)
======================================================== */
function updateGroupMessageBubble(message) {
  // Nếu trước đó chưa đánh data-mid, xử lý đơn giản: thêm bong bóng mới
  displayGroupMessage(message, true);
}

/* ========================================================
   DÁN/KÉO THẢ FILE VÀO Ô NHẬP (tùy chọn, không thay đổi logic khác)
======================================================== */
messageInput?.addEventListener('paste', async (e) => {
  if (!e.clipboardData || !e.clipboardData.files || e.clipboardData.files.length === 0) return;
  const files = Array.from(e.clipboardData.files);
  for (const f of files) {
    const uploaded = await uploadOneFile(f);
    if (uploaded) {
      pendingAttachments.push(uploaded);
      addAttachmentPreview(uploaded);
    }
  }
});

(function initDragDrop(){
  const dropArea = chatMessages;
  if (!dropArea) return;

  const stop = (ev) => { ev.preventDefault(); ev.stopPropagation(); };
  ['dragenter','dragover','dragleave','drop'].forEach(ev => {
    dropArea.addEventListener(ev, stop, false);
  });

  dropArea.addEventListener('drop', async (e) => {
    const files = Array.from(e.dataTransfer?.files || []);
    if (!files.length) return;
    for (const f of files) {
      const uploaded = await uploadOneFile(f);
      if (uploaded) {
        pendingAttachments.push(uploaded);
        addAttachmentPreview(uploaded);
      }
    }
  });
})();

/* ========================================================
   NHẬN ĐÚNG TIN CỦA CHÍNH MÌNH → DỌN COMPOSER
======================================================== */
function isMyMessageLike(msg) {
  const s = msg?.sender || {};
  const u = s.username ?? msg.senderUsername ?? msg.username ?? msg.fromUsername;
  return u && username && u === username;
}

// Wrap handler private
const __orig_onPrivateMessageReceived = onPrivateMessageReceived;
onPrivateMessageReceived = function(payload) {
  try {
    const message = JSON.parse(payload.body);
    if (isMyMessageLike(message)) clearComposer();
  } catch {}
  return __orig_onPrivateMessageReceived.call(this, payload);
};

// Wrap handler group
const __orig_onGroupMessageReceived = onGroupMessageReceived;
onGroupMessageReceived = function(payload) {
  try {
    const message = JSON.parse(payload.body);
    if (isMyMessageLike(message)) clearComposer();
  } catch {}
  return __orig_onGroupMessageReceived.call(this, payload);
};

/* ========================================================
   KHI GÕ BIỆT DANH Ở PRIVATE → CẬP NHẬT HEADER TỨC THỜI
======================================================== */
document.addEventListener('input', (e) => {
  const inp = e.target;
  if (!inp || inp.tagName !== 'INPUT') return;
  if (!currentChat || currentChat.type !== 'private') return;

  if (inp.id === 'privateNicknameInput') {  // ✅ sửa điều kiện
    const temp = (inp.value || '').trim();
    const fallbackName = currentChat.name || currentChat.friendUsername || 'Người dùng';
    const nameForHeader = temp || fallbackName;
    const chatTitle = document.getElementById('chatTitle');
    if (chatTitle) chatTitle.textContent = `Chat với ${nameForHeader}`;
  }
});


/* ========================================================
   CHẤM ONLINE NHẸ TRONG SIDEBAR (không ảnh hưởng logic khác)
======================================================== */
function applyOnlineDotToItem(el, online) {
  let dot = el.querySelector('.online-dot-mini');
  if (!dot) {
    dot = document.createElement('span');
    dot.className = `online-dot-mini absolute -bottom-1 -right-1 w-3 h-3 rounded-full border-2 border-white dark:border-gray-900`;
    const holder = el.querySelector('[data-avatar-for], .relative');
    if (holder) holder.appendChild(dot);
  }
  dot.classList.remove('bg-green-500','bg-gray-400');
  dot.classList.add(online ? 'bg-green-500' : 'bg-gray-400');
}

// Ghi đè nhẹ – nếu payload khác format sẽ fallback refresh sidebar
const __orig_onUserStatusChanged = onUserStatusChanged;
onUserStatusChanged = function(payload) {
  try {
    const ev = JSON.parse(payload.body);
    const user = ev?.username;
    if (!user) return __orig_onUserStatusChanged?.(payload);

    const safe = window.CSS?.escape ? CSS.escape(user) : user.replace(/"/g, '\\"');
    const item = document.querySelector(`.chat-item[data-username="${safe}"]`);
    if (item) applyOnlineDotToItem(item, ev.status === 'ONLINE');
  } catch {
    scheduleSidebarRefresh();
  }
  return __orig_onUserStatusChanged?.(payload);
};

/* ========================================================
   ĐỔI PHÒNG → NHẮC LẠI TIÊU ĐỀ PRIVATE THEO BIỆT DANH
======================================================== */
const __orig_switchToPrivateChat = switchToPrivateChat;
switchToPrivateChat = async function(friend) {
  await __orig_switchToPrivateChat.call(this, friend);
  updatePrivateHeaderTitleWithNickname();
  window.__setNicknameAreaMode?.('private');
};

const __orig_switchToGroupChat = switchToGroupChat;
switchToGroupChat = function(group) {
  __orig_switchToGroupChat.call(this, group);
  window.__setNicknameAreaMode?.('group');

  // nhóm giữ nguyên tiêu đề theo tên nhóm
};

/* ========================================================
   BẮT LỖI TOÀN CỤC (nhẹ nhàng, không thay đổi luồng)
======================================================== */
window.addEventListener('error', (e) => {
  // console.error('Global error:', e?.error || e?.message || e);
});
window.addEventListener('unhandledrejection', (e) => {
  // console.warn('Unhandled promise:', e?.reason);
});

/* ========================================================
   PUBLIC API (nếu cần)
======================================================== */
window.WebChatPro = Object.assign(window.WebChatPro || {}, {
  clearComposer,
  clearPendingAttachmentsUI,
  scheduleSidebarRefresh,
  updatePrivateHeaderTitleWithNickname
});


async function hydratePrivateNickname(chatId) {
  try {
    const res = await fetch(`/api/private-chats/${chatId}/nickname`, {
      headers: { ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) }
    });
    if (!res.ok) {
      console.warn(`Không lấy được biệt danh cho chat ${chatId}: ${await res.text()}`);
      return;
    }

    const data = await res.json();
    const nick = (data.nickname || '').trim();

    const scope = `private:${chatId}`;
    console.log(`Hydrating nickname for chat ${chatId}:`, { nick, friendId: currentChat.friendId, friendUsername: currentChat.friendUsername });

    if (currentChat.friendId != null) {
      memberNickMap.set(`${scope}|id:${String(currentChat.friendId)}`, nick);
    }
    if (currentChat.friendUsername) {
      memberNickMap.set(`${scope}|u:${currentChat.friendUsername}`, nick);
    }

    const privInput = document.getElementById('privateNicknameInput');
    if (privInput) privInput.value = nick;
    updatePrivateHeaderTitleWithNickname();
  } catch (e) {
    console.warn('hydratePrivateNickname failed', e);
  }
}

// hàm xử lý Xóa và rời nhóm
async function leaveCurrentGroup() {
  if (!currentChat || currentChat.type !== 'group') return;
  if (!confirm(`Rời nhóm "${currentChat.name}"?`)) return;

  try {
    const res = await fetch(`/api/groups/${currentChat.id}/leave`, {
      method: 'POST',
      headers: { ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) }
    });
    if (!res.ok) throw new Error(await res.text());
    showSuccessMessage('Bạn đã rời nhóm.');
    scheduleSidebarRefresh();
    typeof switchToPublicChat === 'function' ? switchToPublicChat() : location.reload();
  } catch (e) {
    console.error(e);
    showErrorMessage('Không rời nhóm được.');
  } finally {
    // đóng panel settings nếu muốn:
    // closeChatSettingsPanel();
  }
}

async function deleteCurrentGroup() {
  if (!currentChat || currentChat.type !== 'group') return;
  if (!confirm(`Xóa nhóm "${currentChat.name}" cho TẤT CẢ thành viên?`)) return;

  try {
    const res = await fetch(`/api/groups/${currentChat.id}`, {
      method: 'DELETE',
      headers: { ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) }
    });
    if (!res.ok) throw new Error(await res.text());
    showSuccessMessage('Đã xóa nhóm.');
    scheduleSidebarRefresh();
    typeof switchToPublicChat === 'function' ? switchToPublicChat() : location.reload();
  } catch (e) {
    console.error(e);
    showErrorMessage('Không xóa nhóm được (chỉ Admin/Owner).');
  } finally {
    // closeChatSettingsPanel();
  }
}


async function updateGroupActionButtons(groupId) {
  if (!groupActions || !leaveGroupBtn || !deleteGroupBtn) return;
  if (!currentChat || currentChat.type !== 'group') {
    groupActions.classList.add('hidden');
    return;
  }

  groupActions.classList.remove('hidden');
  leaveGroupBtn.classList.remove('hidden');
  deleteGroupBtn.classList.add('hidden');

  const addBtn = document.getElementById('addMembersBtn');
  addBtn?.classList.add('hidden');

  try {
    const res = await fetch(`/api/groups/${groupId}/me`, {
      headers: { ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) }
    });
    if (!res.ok) return;
    const me = await res.json(); // { role, admin, owner }

    const canDelete = !!(me?.admin || me?.owner);
    if (canDelete) deleteGroupBtn.classList.remove('hidden');

    // Chỉ Owner/Admin mới được thêm
    if (me?.admin || me?.owner) addBtn?.classList.remove('hidden');

  } catch (e) {
    console.error('updateGroupActionButtons', e);
  }
}



// === Add Members (Group) ===
const addMembersBtn        = document.getElementById('addMembersBtn');
const addMembersModal      = document.getElementById('addMembersModal');
const closeAddMembersModal = document.getElementById('closeAddMembersModal');
const cancelAddMembersBtn  = document.getElementById('cancelAddMembersBtn');
const confirmAddMembersBtn = document.getElementById('confirmAddMembersBtn');
const addMembersInput      = document.getElementById('addMembersInput');

function openAddMembersModal() {
  if (!currentChat || currentChat.type !== 'group') {
    showErrorMessage('Chỉ thêm thành viên trong đoạn chat nhóm.');
    return;
  }
  addMembersInput && (addMembersInput.value = '');
  addMembersModal?.classList.remove('hidden');
}
function closeAddMembers() { addMembersModal?.classList.add('hidden'); }

addMembersBtn   ?.addEventListener('click', openAddMembersModal);
closeAddMembersModal?.addEventListener('click', closeAddMembers);
cancelAddMembersBtn ?.addEventListener('click', closeAddMembers);
addMembersModal ?.addEventListener('click', (e)=>{ if (e.target === addMembersModal) closeAddMembers(); });

confirmAddMembersBtn?.addEventListener('click', addMembersToCurrentGroup);

async function addMembersToCurrentGroup() {
  if (!currentChat || currentChat.type !== 'group') return;

  const raw = (addMembersInput?.value || '').trim();
  if (!raw) {
    showErrorMessage('Vui lòng nhập danh sách người dùng.');
    showNotificationToast('Vui lòng nhập danh sách người dùng', 'error');
    return;
  }

  // chống double-click
  confirmAddMembersBtn?.setAttribute('disabled', 'true');

  try {
    const res = await fetch(`/api/groups/${currentChat.id}/members`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
      },
      body: JSON.stringify({ membersRaw: raw })
    });

    const data = await res.json().catch(() => ({}));

    if (!res.ok || data.success === false) {
      const msg = data?.message || 'Không thêm được thành viên.';
      showErrorMessage(msg);
      showNotificationToast(msg, 'error');
      return;
    }

    const r = data.result || {};
    const added       = r.added       || [];
    const reactivated = r.reactivated || [];
    const existed     = r.existed     || [];
    const notFound    = r.notFound    || [];

    // Ghép thông điệp chi tiết
    const parts = [];
    if (added.length)       parts.push(`+ Thêm mới: ${added.join(', ')}`);
    if (reactivated.length) parts.push(`+ Kích hoạt lại: ${reactivated.join(', ')}`);
    if (existed.length)     parts.push(`+ Đã là thành viên: ${existed.join(', ')}`);
    if (notFound.length)    parts.push(`+ Không tìm thấy: ${notFound.join(', ')}`);

    // Chọn tone: success / warning
    const hasSuccess = (added.length + reactivated.length) > 0;
    const tone = hasSuccess ? 'success' : (notFound.length ? 'error' : 'info');

    showSuccessMessage(parts.join('<br>'));
    showNotificationToast(
        hasSuccess ? 'Đã thêm thành viên vào nhóm!' :
            notFound.length ? 'Một số thành viên không tìm thấy.' :
                'Không có thay đổi.',
        tone
    );

    // Cập nhật UI
    await loadGroupMemberNicknames(currentChat.id);
    await loadGroupChatHistory(currentChat.id);
    scheduleSidebarRefresh();

    closeAddMembers();
  } catch (e) {
    console.error(e);
    showErrorMessage('Không thêm được thành viên.');
    showNotificationToast('Không thêm được thành viên', 'error');
  } finally {
    confirmAddMembersBtn?.removeAttribute('disabled');
  }
}

